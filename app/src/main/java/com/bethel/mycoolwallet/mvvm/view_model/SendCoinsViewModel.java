package com.bethel.mycoolwallet.mvvm.view_model;

import android.app.Application;

import androidx.annotation.NonNull;

import com.bethel.mycoolwallet.mvvm.live_data.exchange_rate.ExchangeRateLiveData;
import com.bethel.mycoolwallet.mvvm.live_data.WalletLiveData;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.Transaction;

public class SendCoinsViewModel extends BaseViewModel {
    public final WalletLiveData wallet;
    public final ExchangeRateLiveData exchangeRate;

    public Address toAddress = null;
    public Transaction sentTransaction = null;

    public SendCoinsViewModel(@NonNull Application app) {
        super(app);
        wallet = new WalletLiveData(getApplication());
        exchangeRate = new ExchangeRateLiveData(null);
    }
}
