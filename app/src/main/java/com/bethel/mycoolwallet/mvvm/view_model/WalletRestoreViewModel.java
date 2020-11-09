package com.bethel.mycoolwallet.mvvm.view_model;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.bethel.mycoolwallet.data.Event;

public class WalletRestoreViewModel extends AndroidViewModel {
    public final MutableLiveData<Event<Boolean>> showSuccessDialog = new MutableLiveData<>();
    public final MutableLiveData<Event<String>> showFailureDialog = new MutableLiveData<>();

    public WalletRestoreViewModel(@NonNull Application application) {
        super(application);
    }
}
