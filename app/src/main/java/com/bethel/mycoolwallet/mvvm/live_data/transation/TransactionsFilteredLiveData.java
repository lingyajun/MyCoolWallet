package com.bethel.mycoolwallet.mvvm.live_data.transation;

import com.bethel.mycoolwallet.CoolApplication;
import com.bethel.mycoolwallet.manager.MyCoolWalletManager;
import com.bethel.mycoolwallet.mvvm.live_data.AbstractWalletLiveData;
import com.bethel.mycoolwallet.utils.Utils;

import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Transaction;
//import org.bitcoinj.core.Utils;
import org.bitcoinj.wallet.Wallet;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class TransactionsFilteredLiveData extends AbstractWalletLiveData<Set<Transaction>> {
    public TransactionsFilteredLiveData(CoolApplication application) {
        super(application);
    }

    @Override
    protected void onWalletActive(Wallet wallet) {
        loadTransactions();
    }

    public void loadTransactions() {
        final Wallet wallet = getWallet();
        if (null==wallet) return;
        executeAsyncTask(()-> {
            MyCoolWalletManager.propagate();
           final Set<Transaction> transactionSet = wallet.getTransactions(false);
            final Set<Transaction> filter = new HashSet<>(Utils.size(transactionSet));

            for (Transaction tx: transactionSet ) {
                final Map<Sha256Hash, Integer> aIn = tx.getAppearsInHashes(); // a map of block [hashes] which contain the transaction
                if (null!=aIn && !aIn.isEmpty()) {
                    filter.add(tx); // TODO filter by updateTime
                }
            }

            postValue(filter);
        });
    }
}
