package com.bethel.mycoolwallet.interfaces;

import org.bitcoinj.wallet.Wallet;

public interface IWalletRestoreCallback {
    void onSuccess(Wallet restoredWallet);
    void onFailed(Exception e);
}
