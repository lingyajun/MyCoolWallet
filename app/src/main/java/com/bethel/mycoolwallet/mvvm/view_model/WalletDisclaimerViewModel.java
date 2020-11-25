package com.bethel.mycoolwallet.mvvm.view_model;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

import com.bethel.mycoolwallet.mvvm.live_data.BlockChainStateLiveData;
import com.bethel.mycoolwallet.mvvm.live_data.DisclaimerEnabledLiveData;

public class WalletDisclaimerViewModel extends BaseViewModel {
    public final BlockChainStateLiveData blockChainState;
    public final DisclaimerEnabledLiveData disclaimer;
    public WalletDisclaimerViewModel(@NonNull Application app) {
        super(app);
        blockChainState = new BlockChainStateLiveData();
        disclaimer = new DisclaimerEnabledLiveData();
    }
}
