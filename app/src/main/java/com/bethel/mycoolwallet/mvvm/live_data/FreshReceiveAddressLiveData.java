package com.bethel.mycoolwallet.mvvm.live_data;

import com.bethel.mycoolwallet.CoolApplication;
import com.bethel.mycoolwallet.manager.MyCoolWalletManager;

import org.bitcoinj.core.Address;
import org.bitcoinj.script.Script;
import org.bitcoinj.wallet.Wallet;

public class FreshReceiveAddressLiveData extends AbstractWalletLiveData<Address> {
    private Script.ScriptType outputScriptType = null;

    public FreshReceiveAddressLiveData(CoolApplication application) {
        super(application);
    }

    public void overrideOutputScriptType(final Script.ScriptType outputScriptType) {
        this.outputScriptType = outputScriptType;
    }

    @Override
    public void setValue(Address value) {
        super.setValue(value);
    }

    @Override
    protected void onWalletActive(Wallet wallet) {
        maybeLoad();
    }

    private void maybeLoad() {
        if (null != getValue()) return;

        final Wallet wallet = getWallet();
        final Script.ScriptType oScriptType = this.outputScriptType;
        executeAsyncTask(()->{
            MyCoolWalletManager.propagate();
            Address address = null != oScriptType ?
                    wallet.freshReceiveAddress(oScriptType) : wallet.freshReceiveAddress();
            postValue(address);
        });
    }
}
