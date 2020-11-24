package com.bethel.mycoolwallet.mvvm.live_data.address;

import com.bethel.mycoolwallet.CoolApplication;
import com.bethel.mycoolwallet.mvvm.live_data.AbstractWalletLiveData;
import com.bethel.mycoolwallet.utils.Constants;
import com.bethel.mycoolwallet.utils.Utils;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.LegacyAddress;
import org.bitcoinj.utils.Threading;
import org.bitcoinj.wallet.Wallet;
import org.bitcoinj.wallet.listeners.KeyChainEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ImportedAddressesLiveData extends  AbstractWalletLiveData<List<Address>>  {
    public ImportedAddressesLiveData(CoolApplication application ) {
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
             final List<ECKey> keyList = wallet.getImportedKeys();
             final List<Address> importedAddresses = new ArrayList<>();
             if (Utils.isEmpty(keyList)) {
                 postValue(importedAddresses);
                 return;
             }
             sortKeys(wallet, keyList);
             for (ECKey key: keyList  ) {
                 // A standard address is built by taking the RIPE-MD160 hash of the public key bytes.
                 Address address = LegacyAddress.fromKey(Constants.NETWORK_PARAMETERS, key);
                 importedAddresses.add(address);
             }
             postValue(importedAddresses);
         });
    }

    private static void sortKeys(final Wallet wallet,final List<ECKey> keyList) {
        if (Utils.isEmpty(keyList)) return;
        if (null==wallet) return;

        Collections.sort(keyList, (ecKey1, ecKey2) -> {
            final boolean isRotating1 = wallet.isKeyRotating(ecKey1);
            final boolean isRotating2 = wallet.isKeyRotating(ecKey2);

            if (isRotating1 != isRotating2) {
                return isRotating1 ? 1: -1;
            }

            final long time1 =ecKey1.getCreationTimeSeconds();
            final long time2 =ecKey2.getCreationTimeSeconds();

            if (time1 != time2) {
                return time1 > time2 ? 1: -1;
            }

            return 0;
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
