package com.bethel.mycoolwallet.mvvm.view_model;

import android.app.Application;

import androidx.annotation.NonNull;

import com.bethel.mycoolwallet.mvvm.live_data.WalletLiveData;

public class ReportIssueViewModel extends BaseViewModel {
    public final WalletLiveData wallet;
    public ReportIssueViewModel(@NonNull Application app) {
        super(app);
        wallet = new WalletLiveData(getApplication());
    }
}
