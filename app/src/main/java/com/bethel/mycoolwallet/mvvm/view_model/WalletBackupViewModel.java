package com.bethel.mycoolwallet.mvvm.view_model;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;

import com.bethel.mycoolwallet.mvvm.live_data.WalletLiveData;

public class WalletBackupViewModel extends BaseViewModel {
    public final MutableLiveData<String> password = new MutableLiveData<>();
    public final WalletLiveData walletLiveData;

    public WalletBackupViewModel(@NonNull Application app) {
        super(app);
        walletLiveData = new WalletLiveData(getApplication());
    }
}
