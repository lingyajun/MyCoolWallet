package com.bethel.mycoolwallet.mvvm.view_model;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;

import com.bethel.mycoolwallet.data.Event;
import com.bethel.mycoolwallet.mvvm.live_data.WalletEncryptedLiveData;

public class MainActivityViewModel extends BaseViewModel {
    public final WalletEncryptedLiveData walletEncrypted;

    public final MutableLiveData<Event<Void>> showEncryptKeysDialog = new MutableLiveData<>();
    public final MutableLiveData<Event<Void>> showBackupWalletDialog = new MutableLiveData<>();
    public final MutableLiveData<Event<Void>> showRestoreWalletDialog = new MutableLiveData<>();

    public final MutableLiveData<Event<Integer>> showHelpDialog = new MutableLiveData<>();

    public MainActivityViewModel(@NonNull Application app) {
        super(app);
        walletEncrypted = new WalletEncryptedLiveData(application);
    }

    public void transactionsLoadingFinished() {
// todo
    }
    public void balanceLoadingFinished() {
        // todo
    }

}
