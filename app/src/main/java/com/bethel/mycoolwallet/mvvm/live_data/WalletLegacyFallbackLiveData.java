package com.bethel.mycoolwallet.mvvm.live_data;

import com.bethel.mycoolwallet.CoolApplication;
import com.bethel.mycoolwallet.manager.MyCoolWalletManager;

import org.bitcoinj.script.Script;
import org.bitcoinj.wallet.Wallet;

/**
 * 判断钱包是不是接收老式的地址
 */
public class WalletLegacyFallbackLiveData extends AbstractWalletLiveData<Boolean> {
    public WalletLegacyFallbackLiveData(CoolApplication application) {
        super(application);
    }

    @Override
    protected void onWalletActive(Wallet wallet) {
        load();
    }

    @Override
    protected void load() {
        final Wallet wallet = getWallet();
        executeAsyncTask(()->{
            MyCoolWalletManager.propagate();
            boolean isLegacy = wallet.getActiveKeyChain().getOutputScriptType()== Script.ScriptType.P2WPKH
                    && wallet.getActiveKeyChains().get(0).getOutputScriptType() != Script.ScriptType.P2WPKH;
            // pay to pubkey hash
            // [not] pay to witness pubkey hash
            postValue(isLegacy);
        });
    }
}
