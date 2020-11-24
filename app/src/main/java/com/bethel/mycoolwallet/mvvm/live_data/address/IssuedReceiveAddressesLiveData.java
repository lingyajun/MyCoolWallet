package com.bethel.mycoolwallet.mvvm.live_data.address;

import com.bethel.mycoolwallet.CoolApplication;
import com.bethel.mycoolwallet.mvvm.live_data.AbstractWalletLiveData;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.utils.Threading;
import org.bitcoinj.wallet.Wallet;
import org.bitcoinj.wallet.listeners.KeyChainEventListener;

import java.util.List;

public class IssuedReceiveAddressesLiveData extends AbstractWalletLiveData<List<Address>> {
    public IssuedReceiveAddressesLiveData(CoolApplication application) {
        super(application);
    }

    @Override
    protected void onWalletActive(Wallet wallet) {
        wallet.addKeyChainEventListener(Threading.SAME_THREAD,listener);
        loadAddress();
    }

    private void loadAddress() {
        final Wallet wallet =getWallet();

        if (null==wallet) return;

        executeAsyncTask(()-> postValue(wallet.getIssuedReceiveAddresses()));
    }

    @Override
    protected void onWalletInactive(Wallet wallet) {
        wallet.removeKeyChainEventListener(listener);
    }

    private final KeyChainEventListener listener = new KeyChainEventListener() {
        @Override
        public void onKeysAdded(List<ECKey> keys) {
            loadAddress();
        }
    };
}
