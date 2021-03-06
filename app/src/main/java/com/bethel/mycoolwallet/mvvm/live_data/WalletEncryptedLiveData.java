package com.bethel.mycoolwallet.mvvm.live_data;

import com.bethel.mycoolwallet.CoolApplication;
import com.bethel.mycoolwallet.manager.MyCoolWalletManager;

import org.bitcoinj.wallet.Wallet;

public class WalletEncryptedLiveData extends AbstractWalletLiveData<Boolean> {
    public WalletEncryptedLiveData(CoolApplication application) {
        super(application);
    }

    @Override
    protected void onWalletActive(Wallet wallet) {
        load();
    }

    @Override
    public void load() {
        Wallet wallet = getWallet();
        executeAsyncTask(()->{
            MyCoolWalletManager.propagate();
            postValue(wallet.isEncrypted());
        });
    }
}
