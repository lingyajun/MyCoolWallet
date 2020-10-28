package com.bethel.mycoolwallet.adapter;

import android.content.Context;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bethel.mycoolwallet.R;

public class TestTransactionsAdapter extends ListAdapter<TestTransactionsAdapter.ListItem, RecyclerView.ViewHolder> {

    protected final Context context;
    private final LayoutInflater inflater;
//    private final TransactionsAdapter.OnClickListener onClickListener;

    private static final int VIEW_TYPE_TRANSACTION = 0;
    private static final int VIEW_TYPE_WARNING = 1;

    public TestTransactionsAdapter(Context context) {
        super(new DiffUtil.ItemCallback<ListItem>() {
            @Override
            public boolean areItemsTheSame(@NonNull ListItem oldItem, @NonNull ListItem newItem) {
                return false;
            }

            @Override
            public boolean areContentsTheSame(@NonNull ListItem oldItem, @NonNull ListItem newItem) {
                return false;
            }
        });
        this.context = context;
        this.inflater = LayoutInflater.from(context);
    }

    @Override
    public int getItemViewType(int position) {
        final ListItem listItem = getItem(position);
        if (listItem instanceof ListItem.WarningItem)
            return VIEW_TYPE_WARNING;
        else if (listItem instanceof ListItem.TransactionItem)
            return VIEW_TYPE_TRANSACTION;
        else
            throw new IllegalStateException();
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
        } else if (viewType == VIEW_TYPE_WARNING) {
            return new WarningViewHolder(inflater.inflate(R.layout.transaction_row_warning, parent, false));
        } else {
            throw new IllegalStateException("unknown type: " + viewType);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        final TestTransactionsAdapter.ListItem listItem = getItem(position);
        if (holder instanceof TransactionViewHolder) {
            final TransactionViewHolder transactionHolder = (TransactionViewHolder) holder;
            final ListItem.TransactionItem transactionItem = (ListItem.TransactionItem) listItem;
            transactionHolder.itemView.setActivated(transactionItem.isSelected);
            transactionHolder.bind(transactionItem);
        } else if (holder instanceof WarningViewHolder) {
            final WarningViewHolder warningHolder = (WarningViewHolder) holder;
            final ListItem.WarningItem warningItem = (ListItem.WarningItem) listItem;
            if (warningItem.type == WarningType.BACKUP) {
                if (getItemCount() == 2 /* 1 transaction, 1 warning */) {
                    warningHolder.messageView.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
                    warningHolder.messageView
                            .setText(Html.fromHtml(context.getString(R.string.wallet_transactions_row_warning_backup)));
                } else {
                    warningHolder.messageView
                            .setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_warning_grey600_24dp, 0, 0, 0);
                    warningHolder.messageView.setText(
                            Html.fromHtml(context.getString(R.string.wallet_disclaimer_fragment_remind_backup)));
                }
            } else if (warningItem.type == WarningType.STORAGE_ENCRYPTION) {
                warningHolder.messageView.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
                warningHolder.messageView.setText(
                        Html.fromHtml(context.getString(R.string.wallet_transactions_row_warning_storage_encryption)));
            } else if (warningItem.type == WarningType.CHAIN_FORKING) {
                warningHolder.messageView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_warning_grey600_24dp, 0,
                        0, 0);
                warningHolder.messageView.setText(
                        Html.fromHtml(context.getString(R.string.wallet_transactions_row_warning_chain_forking)));
            }
        }
    }

    public static class WarningViewHolder extends RecyclerView.ViewHolder {
        private final TextView messageView;

        private WarningViewHolder(final View itemView) {
            super(itemView);
            messageView = (TextView) itemView.findViewById(R.id.transaction_row_warning_message);
        }
    }

    public static class TransactionViewHolder extends RecyclerView.ViewHolder {
        public TransactionViewHolder(@NonNull View itemView) {
            super(itemView);
        }

        public void bind(ListItem.TransactionItem transactionItem) {}
    }

    public static class ListItem {
        public static class WarningItem extends ListItem {
            public final WarningType type;

            public WarningItem(final WarningType type) {
                this.type = type;
            }
        }

        public static class TransactionItem extends ListItem {
            public boolean isSelected;
        }
    }

    public enum WarningType {
        BACKUP, STORAGE_ENCRYPTION, CHAIN_FORKING
    }

}
