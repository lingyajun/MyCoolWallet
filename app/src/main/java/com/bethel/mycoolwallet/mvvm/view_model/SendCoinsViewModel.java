package com.bethel.mycoolwallet.mvvm.view_model;

import android.app.Application;

import androidx.annotation.NonNull;

import com.bethel.mycoolwallet.mvvm.live_data.WalletLiveData;

import org.bitcoinj.core.Address;

public class SendCoinsViewModel extends BaseViewModel {
    private final WalletLiveData wallet;

    public Address toAddress;

    public SendCoinsViewModel(@NonNull Application app) {
        super(app);
        wallet = new WalletLiveData(getApplication());
    }
}
