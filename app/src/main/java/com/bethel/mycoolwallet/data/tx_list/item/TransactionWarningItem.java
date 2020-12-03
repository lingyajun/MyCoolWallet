package com.bethel.mycoolwallet.data.tx_list.item;

import com.bethel.mycoolwallet.data.tx_list.TransactionWarningType;

public class TransactionWarningItem implements IListItem {
    public final TransactionWarningType type;

    public TransactionWarningItem(TransactionWarningType type) {
        this.type = type;
    }
}
