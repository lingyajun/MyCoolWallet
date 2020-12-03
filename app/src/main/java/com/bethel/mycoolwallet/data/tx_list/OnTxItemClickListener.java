package com.bethel.mycoolwallet.data.tx_list;

import android.view.View;

import org.bitcoinj.core.Sha256Hash;

public interface OnTxItemClickListener {
    void onTransactionClick(View view, Sha256Hash transactionHash);

    void onTransactionMenuClick(View view, Sha256Hash transactionHash);

    void onWarningClick(View view);
}
