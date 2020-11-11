package com.bethel.mycoolwallet.mvvm.view_model;

import android.app.Application;

import androidx.annotation.NonNull;

import com.bethel.mycoolwallet.mvvm.live_data.BlockChainStateLiveData;
import com.bethel.mycoolwallet.mvvm.live_data.WalletBalanceLiveData;

public class WalletBalanceViewModel extends BaseViewModel {
    public final WalletBalanceLiveData balanceLiveData;
    public final BlockChainStateLiveData chainStateLiveData;

    /**
     * todo
      SelectedExchangeRateLiveData exchangeRate;
     */
    public WalletBalanceViewModel(@NonNull Application app) {
        super(app);
        balanceLiveData = new WalletBalanceLiveData(getApplication());
        chainStateLiveData = new BlockChainStateLiveData();
    }
}
