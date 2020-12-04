package com.bethel.mycoolwallet.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bethel.mycoolwallet.R;
import com.bethel.mycoolwallet.data.tx_list.item.IListItem;
import com.bethel.mycoolwallet.data.tx_list.OnTxItemClickListener;
import com.bethel.mycoolwallet.data.tx_list.TransactionChangeType;
import com.bethel.mycoolwallet.data.tx_list.TransactionDiffItem;
import com.bethel.mycoolwallet.data.tx_list.item.TransactionListItem;
import com.bethel.mycoolwallet.data.tx_list.item.TransactionWarningItem;
import com.bethel.mycoolwallet.data.tx_list.holder.TransactionViewHolder;
import com.bethel.mycoolwallet.data.tx_list.holder.WarningViewHolder;

import java.util.EnumSet;
import java.util.List;

public class TransactionListAdapter extends ListAdapter<IListItem, RecyclerView.ViewHolder> {
    private static final int VIEW_TYPE_TRANSACTION = 0;
    private static final int VIEW_TYPE_WARNING = 1;

//    private final Context context;
    private final LayoutInflater inflater;
    private OnTxItemClickListener itemClickListener;

    public TransactionListAdapter(final Context context) {
        super(new TransactionDiffItem());
//        this.context = context;
        inflater = LayoutInflater.from(context);
    }

    @Override
    public int getItemViewType(int position) {
        final IListItem item = getItem(position);
        if (item instanceof TransactionListItem) return VIEW_TYPE_TRANSACTION;
        if (item instanceof TransactionWarningItem) return VIEW_TYPE_WARNING;

        throw new IllegalStateException("unknown type: " + item.getClass());
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_TRANSACTION) {
            final CardView cardView = (CardView) inflater.inflate(R.layout.transaction_row_card, parent, false);
            cardView.setPreventCornerOverlap(false);
            cardView.setUseCompatPadding(false);
            cardView.setMaxCardElevation(0); // we're using Lollipop elevation
            return new TransactionViewHolder(cardView);
        }

        if (viewType == VIEW_TYPE_WARNING) {
            return new WarningViewHolder(inflater.inflate(R.layout.transaction_row_warning, parent, false));
        }

        throw new IllegalStateException("unknown type: " + viewType);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        final IListItem item = getItem(position);
        if (holder instanceof TransactionViewHolder) {
            final TransactionViewHolder txHolder = (TransactionViewHolder) holder;
            final TransactionListItem txItem = (TransactionListItem) item;
            // bind
            txHolder.itemView.setActivated(txItem.isSelected());
            txHolder.bind(txItem);
            if (null!=itemClickListener) {
                txHolder.itemView.setOnClickListener(view ->
                        itemClickListener.onTransactionClick(view, txItem.getTransactionHash()));
                txHolder.menuView.setOnClickListener(view ->
                        itemClickListener.onTransactionMenuClick(view, txItem.getTransactionHash()));
            }
            return;
        }

        if (! (holder instanceof WarningViewHolder)) {
            return;
        }
        final WarningViewHolder warningHolder = (WarningViewHolder) holder;
        final TransactionWarningItem warningItem = (TransactionWarningItem) item;
        // WarningType
        warningHolder.bind(warningItem, getItemCount());
        if (null!=itemClickListener) {
            warningHolder.itemView.setOnClickListener(view -> itemClickListener.onWarningClick(view));
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position, @NonNull List<Object> payloads) {
        if (payloads.isEmpty()) {
            super.onBindViewHolder(holder, position, payloads);
            return;
        }
        if (!(holder instanceof TransactionViewHolder)) {
            super.onBindViewHolder(holder, position, payloads);
            return;
        }

        // 部分绑定
        final IListItem item = getItem(position);
        final TransactionViewHolder txHolder = (TransactionViewHolder) holder;
        final TransactionListItem txItem = (TransactionListItem) item;

        for (final Object payload: payloads ) {
            final EnumSet<TransactionChangeType> changes = (EnumSet<TransactionChangeType>) payload;

            for (final TransactionChangeType change: changes   ) {
                switch (change) {
                    case FEE:
                        txHolder.bindFee(txItem);
                        break;
                    case FIAT:
                        txHolder.bindFiat(txItem);
                        break;
                    case TIME:
                        txHolder.bindTime(txItem);
                        break;
                    case VALUE:
                        txHolder.bindValue(txItem);
                        break;
                    case ADDRESS:
                        txHolder.bindAddress(txItem);
                        break;
                    case MESSAGE:
                        txHolder.bindMessage(txItem);
                        break;
                    case CONFIDENCE:
                        txHolder.bindConfidence(txItem);
                        break;
                    case IS_SELECTED:
                        txHolder.bindIsSelected(txItem);
                        break;
                }
            }
        } // 2-for end
    }

    public void setOnItemClickListener(OnTxItemClickListener itemClickListener) {
        this.itemClickListener = itemClickListener;
    }
}
