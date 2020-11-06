package com.bethel.mycoolwallet.mvvm.live_data;

import com.bethel.mycoolwallet.CoolApplication;

import org.bitcoinj.wallet.Wallet;

public class WalletLiveData extends AbstractWalletLiveData<Wallet> {
    public WalletLiveData(CoolApplication application) {
        super(application, 0);
    }

    @Override
    protected void onWalletActive(Wallet wallet) {
        postValue(wallet);
    }
}
