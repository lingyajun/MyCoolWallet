package com.bethel.mycoolwallet.interfaces;

import org.bitcoinj.wallet.Wallet;

public interface OnWalletLoadedListener {
    void onWalletLoaded(Wallet wallet);
}
