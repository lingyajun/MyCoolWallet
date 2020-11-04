package com.bethel.mycoolwallet.mvvm.view_model;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.bethel.mycoolwallet.data.Event;

public class RequestCoinsActivityViewModel extends ViewModel {
    public final MutableLiveData<Event<Integer>> showHelpDialog = new MutableLiveData<>();
}
