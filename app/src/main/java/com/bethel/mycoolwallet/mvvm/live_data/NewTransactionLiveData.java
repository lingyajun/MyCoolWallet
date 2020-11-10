package com.bethel.mycoolwallet.mvvm.live_data;

import androidx.lifecycle.LiveData;

import org.bitcoinj.core.Transaction;
import org.bitcoinj.wallet.Wallet;
import org.bitcoinj.wallet.listeners.WalletCoinsReceivedEventListener;
import org.bitcoinj.wallet.listeners.WalletCoinsSentEventListener;

/**
 * 监听发送货币，收到货币 事件
 */
public class NewTransactionLiveData extends LiveData<Transaction> {
    private final Wallet wallet;
    public NewTransactionLiveData(Wallet wallet) {
        this.wallet = wallet;
    }

    @Override
    protected void onActive() {
        wallet.addCoinsReceivedEventListener(receivedEventListener);
        wallet.addCoinsSentEventListener(sentEventListener);
    }

    @Override
    protected void onInactive() {
        wallet.removeCoinsReceivedEventListener(receivedEventListener);
        wallet.removeCoinsSentEventListener(sentEventListener);
    }

    private final WalletCoinsReceivedEventListener receivedEventListener = (wallet1, tx, prevBalance, newBalance) -> postValue(tx);
    private final WalletCoinsSentEventListener sentEventListener = (wallet1, tx, prevBalance, newBalance) -> postValue(tx);
}
