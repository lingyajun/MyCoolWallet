package com.bethel.mycoolwallet.data.tx_list;

import com.bethel.mycoolwallet.R;

public enum TransactionDirection {
    RECEIVED(R.drawable.transactions_list_filter_received),
    SENT(R.drawable.transactions_list_filter_sent),
    ALL(R.drawable.ic_filter_list_white_24dp);

    private int iconId;

    TransactionDirection(int icon) {
        this.iconId = icon;
    }

    public int getIconId() {
        return iconId;
    }
}
