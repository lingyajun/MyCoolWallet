package com.bethel.mycoolwallet.data.tx_list;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.RecyclerView;

import java.util.EnumSet;
import java.util.List;

public class TransactionListItemAnimator extends DefaultItemAnimator {
    @Override
    public boolean canReuseUpdatedViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, @NonNull List<Object> payloads) {
        for (final Object payload : payloads) {
            final EnumSet<TransactionChangeType > changes = (EnumSet<TransactionChangeType>) payload;
            if (changes.contains(TransactionChangeType.IS_SELECTED))
                return false;
        }
        return super.canReuseUpdatedViewHolder(viewHolder, payloads);
    }
}
