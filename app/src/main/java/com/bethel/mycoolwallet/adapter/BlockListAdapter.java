package com.bethel.mycoolwallet.adapter;

import android.content.Context;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bethel.mycoolwallet.R;
import com.bethel.mycoolwallet.utils.Constants;
import com.bethel.mycoolwallet.utils.CurrencyTools;
import com.bethel.mycoolwallet.utils.Utils;
import com.bethel.mycoolwallet.utils.ViewUtil;
import com.bethel.mycoolwallet.utils.WalletUtils;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.StoredBlock;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.utils.MonetaryFormat;
import org.bitcoinj.wallet.Wallet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class BlockListAdapter extends ListAdapter<BlockListAdapter.ListItem, BlockListAdapter.MViewHolder> {
    private final LayoutInflater inflater;
    private  OnItemClickListener itemClickListener;

    private static final Logger log = LoggerFactory.getLogger(BlockListAdapter.class);

    public BlockListAdapter(Context context) {
        super(new DiffUtil.ItemCallback<ListItem>() {
            @Override
            public boolean areItemsTheSame(@NonNull ListItem oldItem, @NonNull ListItem newItem) {
                return Utils.equals(oldItem.blockHash, newItem.blockHash);
            }

            @Override
            public boolean areContentsTheSame(@NonNull ListItem oldItem, @NonNull ListItem newItem) {
                return TextUtils.equals(oldItem.time, newItem.time);
            }
        });

        inflater = LayoutInflater.from(context);
    }

    @NonNull
    @Override
    public MViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.block_list_row, parent, false);
        return new MViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MViewHolder holder, int position) {
        final ListItem item = getItem(position);
        holder.heightView.setText(String.valueOf(item.height));
        holder.timeView.setText(item.time);
        ViewUtil.showView(holder.miningDifficultyAdjustmentView, item.isDifficultyTransitionPoint);
        ViewUtil.showView(holder.miningRewardAdjustmentView, item.isMiningRewardHalvingPoint);
        holder.hashView.setText(WalletUtils.formatHash(null,
                item.blockHash.toString(), 8, 0, ' '));
        if (null!= itemClickListener) {
            holder.menuView.setOnClickListener(view ->
                    itemClickListener.onBlockMenuClick(view, item.blockHash));
        }

        //  transactions ui
        holder.transactionsViewGroup.removeAllViewsInLayout();
        for (ListItem.ListTransaction tx: item.transactions ) {
            View  view = inflater.inflate(R.layout.block_list_row_transaction, null);
            holder.transactionsViewGroup.addView(view);
            bindTransactionView(view, item.format, tx);
        }
    }

    private void bindTransactionView(View view, MonetaryFormat format, ListItem.ListTransaction tx) {
        TextView fromToTv = view.findViewById(R.id.block_row_transaction_fromto);
        TextView addressTv = view.findViewById(R.id.block_row_transaction_address);
        TextView valueTv = view.findViewById(R.id.block_row_transaction_value);

        fromToTv.setText(tx.fromTo);
        addressTv.setText(tx.label != null ? tx.label : tx.address.toString());
        addressTv.setTypeface(tx.label != null ? Typeface.DEFAULT : Typeface.MONOSPACE);
        CurrencyTools.setText(valueTv, format, tx.value);
    }

    public interface OnItemClickListener {
        void onBlockMenuClick(View view, Sha256Hash blockHash);
    }

    public void setOnItemClickListener(OnItemClickListener itemClickListener) {
        this.itemClickListener = itemClickListener;
    }

    public static List<ListItem> buildListItems(final Context context, final List<StoredBlock> blocks,
                                                final Date time, final MonetaryFormat format,
                                                final @Nullable Set<Transaction> transactions,
                                                final @Nullable Wallet wallet){
        List<ListItem> list = new ArrayList<>(blocks.size());
        for (StoredBlock b: blocks   ) {
            ListItem item = new ListItem(context, b, time, format, transactions, wallet);
            list.add(item);
        }
        return list;
    }

    public static class ListItem {
        public final Sha256Hash blockHash;
        public final int height;
        public final String time;
        public final boolean isMiningRewardHalvingPoint;
        public final boolean isDifficultyTransitionPoint;
        public final MonetaryFormat format;
        public final List<ListTransaction> transactions;

        public ListItem(final Context context, final StoredBlock block, final Date time, final MonetaryFormat format,
                        final @Nullable Set<Transaction> transactionSet, final @Nullable Wallet wallet)
//    todo                final @Nullable Map<String, AddressBookEntry> addressBook)
                        {
                            blockHash = block.getHeader().getHash();
                            height = block.getHeight();
                            final long timeMs = block.getHeader().getTimeSeconds() * DateUtils.SECOND_IN_MILLIS;
                            if (timeMs < time.getTime() - DateUtils.MINUTE_IN_MILLIS) {
                                // 1分钟之前
                                this.time = DateUtils.getRelativeDateTimeString(context, timeMs,
                                        DateUtils.MINUTE_IN_MILLIS, DateUtils.WEEK_IN_MILLIS, 0).toString();
                            } else {
                                this.time = context.getString(R.string.block_row_now);
                            }

                            this.format = format;
                            isMiningRewardHalvingPoint = isMiningRewardHalvingPoint(block);
                            isDifficultyTransitionPoint = isDifficultyTransitionPoint(block);

                            transactions = new LinkedList<>();
                            if (null!=transactionSet && null!= wallet) {
                                for (Transaction tx: transactionSet ) {
                                    Map<Sha256Hash, Integer> map =  tx.getAppearsInHashes();
                                    if (null!= map && map.containsKey(blockHash)) {
                                        ListTransaction ltx = new ListTransaction(context, tx, wallet);
                                        transactions.add(ltx);
                                    }
                                }
                            }
                            log.info("ListItem, transactionSet: {}, transactions: {}",
                                    Utils.size(transactionSet), transactions.size());
                        }

        private  static boolean isDifficultyTransitionPoint(StoredBlock block) {
            // 挖矿难度调整
            return ((block.getHeight() + 1) % Constants.NETWORK_PARAMETERS.getInterval()) == 0;
        }

        private  static boolean isMiningRewardHalvingPoint(StoredBlock block) {
            return ((block.getHeight() + 1) % 210000) == 0; // 挖矿收益减半
        }

        private static class ListTransaction {
            public final String fromTo;
            public final Address address;
            public final String label;
            public final Coin value;

            public ListTransaction(final Context context, final Transaction tx, final Wallet wallet)
//       todo                        final @Nullable Map<String, AddressBookEntry> addressBook)
            {
                this.value = tx.getValue(wallet);
                final boolean sent = value.signum() < 0;
                if (sent) {
                    address = WalletUtils.getToAddressOfSent(tx, wallet);
                } else {
                    address = WalletUtils.getWalletAddressOfReceived(tx, wallet);
                }

                final boolean self = WalletUtils.isEntirelySelf(tx, wallet);
                final boolean internal = tx.getPurpose() == Transaction.Purpose.KEY_ROTATION;
                if (internal || self) {
                    fromTo = context.getString(R.string.symbol_internal);
                } else if (self) {
                    fromTo = context.getString(R.string.symbol_to);
                } else {
                    fromTo = context.getString(R.string.symbol_from);
                }

                final boolean coinbase = tx.isCoinBase();
                if (coinbase) {
                    label = context.getString(R.string.wallet_transactions_fragment_coinbase);
                } else if (internal || self) {
                    label =context.getString(R.string.wallet_transactions_fragment_internal);
                } else {
                    // todo address 数据库
                    label = "?";
                }
            }

        }

    }

    public static class MViewHolder extends RecyclerView.ViewHolder {
        private final ViewGroup transactionsViewGroup;
        private final View miningRewardAdjustmentView;
        private final View miningDifficultyAdjustmentView;
        private final TextView heightView;
        private final TextView timeView;
        private final TextView hashView;
        private final ImageButton menuView;

        public MViewHolder(@NonNull View itemView) {
            super(itemView);
            transactionsViewGroup =  itemView.findViewById(R.id.block_list_row_transactions_group);
            miningRewardAdjustmentView = itemView.findViewById(R.id.block_list_row_mining_reward_adjustment);
            miningDifficultyAdjustmentView = itemView.findViewById(R.id.block_list_row_mining_difficulty_adjustment);
            heightView = itemView.findViewById(R.id.block_list_row_height);
            timeView = itemView.findViewById(R.id.block_list_row_time);
            hashView =  itemView.findViewById(R.id.block_list_row_hash);
            menuView =  itemView.findViewById(R.id.block_list_row_menu);
        }
    }
}
