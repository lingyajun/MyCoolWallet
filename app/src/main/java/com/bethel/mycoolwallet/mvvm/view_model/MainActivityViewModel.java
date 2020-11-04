package com.bethel.mycoolwallet.mvvm.view_model;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;

import com.bethel.mycoolwallet.data.Event;
import com.bethel.mycoolwallet.mvvm.live_data.WalletEncryptedLiveData;

public class MainActivityViewModel extends BaseViewModel {
    public final WalletEncryptedLiveData walletEncrypted;

    public final MutableLiveData<Event<Void>> showEncryptKeysDialog = new MutableLiveData<>();

    public MainActivityViewModel(@NonNull Application app) {
        super(app);
        walletEncrypted = new WalletEncryptedLiveData(application);
    }
}
