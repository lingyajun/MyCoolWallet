package com.bethel.mycoolwallet.mvvm.view_model;

import android.app.Application;

import androidx.annotation.NonNull;

import com.bethel.mycoolwallet.mvvm.live_data.TransactionsLiveData;

public class BlocksNetworkMonitorViewModel extends BaseViewModel {
    public final TransactionsLiveData transactions;
    public BlocksNetworkMonitorViewModel(@NonNull Application app) {
        super(app);
        transactions = new TransactionsLiveData(getApplication());
    }
}
