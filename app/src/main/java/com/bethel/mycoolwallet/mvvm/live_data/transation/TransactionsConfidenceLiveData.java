package com.bethel.mycoolwallet.mvvm.live_data.transation;

import com.bethel.mycoolwallet.CoolApplication;
import com.bethel.mycoolwallet.mvvm.live_data.AbstractWalletLiveData;

import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.listeners.TransactionConfidenceEventListener;
import org.bitcoinj.utils.Threading;
import org.bitcoinj.wallet.Wallet;

public class TransactionsConfidenceLiveData extends AbstractWalletLiveData<Void> {
    public TransactionsConfidenceLiveData(CoolApplication application) {
        super(application);
    }

    @Override
    protected void onWalletActive(Wallet wallet) {
        wallet.addTransactionConfidenceEventListener(Threading.SAME_THREAD, confidenceEventListener);
    }

    @Override
    protected void onWalletInactive(Wallet wallet) {
        wallet.removeTransactionConfidenceEventListener(confidenceEventListener);
    }

    @Override
    protected void load() {
        postValue(null);
    }

    private final TransactionConfidenceEventListener confidenceEventListener = new TransactionConfidenceEventListener() {
        @Override
        public void onTransactionConfidenceChanged(Wallet wallet, Transaction tx) {
            triggerLoad();
        }
    };
}
