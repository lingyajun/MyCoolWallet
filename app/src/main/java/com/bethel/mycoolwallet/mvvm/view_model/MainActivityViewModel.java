package com.bethel.mycoolwallet.mvvm.view_model;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;

import com.bethel.mycoolwallet.data.Event;
import com.bethel.mycoolwallet.mvvm.live_data.WalletEncryptedLiveData;
import com.bethel.mycoolwallet.mvvm.live_data.WalletLegacyFallbackLiveData;

public class MainActivityViewModel extends BaseViewModel {
    public final WalletEncryptedLiveData walletEncrypted;
    public final WalletLegacyFallbackLiveData legacyFallback;

    public final MutableLiveData<Event<Void>> showEncryptKeysDialog = new MutableLiveData<>();
    public final MutableLiveData<Event<Void>> showBackupWalletDialog = new MutableLiveData<>();
    public final MutableLiveData<Event<Void>> showRestoreWalletDialog = new MutableLiveData<>();

    public final MutableLiveData<Event<Integer>> showHelpDialog = new MutableLiveData<>();
    public final MutableLiveData<Event<Void>> showGuidePage = new MutableLiveData<>();

    public final MutableLiveData<Event<Void>> showReportIssueDialog = new MutableLiveData<>();
    public final MutableLiveData<Event<Void>> showReportCrashDialog = new MutableLiveData<>();

    public MainActivityViewModel(@NonNull Application app) {
        super(app);
        walletEncrypted = new WalletEncryptedLiveData(application);
        legacyFallback = new WalletLegacyFallbackLiveData(getApplication());
    }

    public void transactionsLoadingFinished() {
// todo
    }
    public void balanceLoadingFinished() {
        // todo
    }

    public void addressLoadingFinished() {
        // todo
    }
}
