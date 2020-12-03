package com.bethel.mycoolwallet.data.tx_list.holder;

import android.content.Context;
import android.text.Html;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bethel.mycoolwallet.R;
import com.bethel.mycoolwallet.data.tx_list.item.TransactionWarningItem;

public class WarningViewHolder extends RecyclerView.ViewHolder {
    private final TextView messageView;

    public WarningViewHolder(@NonNull View itemView) {
        super(itemView);
        messageView = itemView.findViewById(R.id.transaction_row_warning_message);
    }

    public void bind(final TransactionWarningItem warningItem, final int listCount) {
        final Context context = itemView.getContext();

        switch (warningItem.type) {
            case BACKUP:
                if (2 == listCount) { //  1 transaction, 1 warning
                    messageView.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
                    messageView.setText(
                            Html.fromHtml(context.getString(R.string.wallet_transactions_row_warning_backup)));
                    break;
                }
                messageView
                        .setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_warning_grey600_24dp, 0, 0, 0);
                messageView.setText(
                        Html.fromHtml(context.getString(R.string.wallet_disclaimer_fragment_remind_backup)));
                break;
            case CHAIN_FORKING:
                messageView.setCompoundDrawablesWithIntrinsicBounds(
                        R.drawable.ic_warning_grey600_24dp, 0, 0, 0);
                messageView.setText(
                        Html.fromHtml(context.getString(R.string.wallet_transactions_row_warning_chain_forking)));
                break;
            case STORAGE_ENCRYPTION:
                messageView.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
                messageView.setText(
                        Html.fromHtml(context.getString(R.string.wallet_transactions_row_warning_storage_encryption)));
                break;
        }

    }
}
