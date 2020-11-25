package com.bethel.mycoolwallet.mvvm.live_data.address;

import com.bethel.mycoolwallet.CoolApplication;
import com.bethel.mycoolwallet.mvvm.live_data.AbstractWalletLiveData;
import com.bethel.mycoolwallet.utils.Constants;
import com.bethel.mycoolwallet.utils.Utils;
import com.google.common.collect.Iterables;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.LegacyAddress;
import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.utils.Threading;
import org.bitcoinj.wallet.Wallet;
import org.bitcoinj.wallet.listeners.KeyChainEventListener;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ReceivingAddressesLiveData extends AbstractWalletLiveData<Set<String>> {
    public ReceivingAddressesLiveData(CoolApplication application) {
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

        executeAsyncTask(()-> {
            List<ECKey> imports =  wallet.getImportedKeys();
            List<ECKey> receives = wallet.getIssuedReceiveKeys();
            Collections.sort(receives, DeterministicKey.CHILDNUM_ORDER);

            Set<String> set = new HashSet<>(Utils.size(imports)+ Utils.size(receives));
            for (ECKey key: Iterables.concat(receives,imports)) {
                Address address = LegacyAddress.fromKey(Constants.NETWORK_PARAMETERS, key);
                set.add(address.toString());
            }
            postValue(set);
        });
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
