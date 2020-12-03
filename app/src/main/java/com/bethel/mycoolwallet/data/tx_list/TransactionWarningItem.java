package com.bethel.mycoolwallet.data.tx_list;

public class TransactionWarningItem implements IListItem {
    public final TransactionWarningType type;

    public TransactionWarningItem(TransactionWarningType type) {
        this.type = type;
    }
}
