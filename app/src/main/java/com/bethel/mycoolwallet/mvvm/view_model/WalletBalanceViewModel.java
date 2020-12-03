package com.bethel.mycoolwallet.mvvm.view_model;

import android.app.Application;

import androidx.annotation.NonNull;

import com.bethel.mycoolwallet.mvvm.live_data.BlockChainStateLiveData;
import com.bethel.mycoolwallet.mvvm.live_data.exchange_rate.ExchangeRateLiveData;
import com.bethel.mycoolwallet.mvvm.live_data.WalletBalanceLiveData;
import com.bethel.mycoolwallet.mvvm.live_data.exchange_rate.ExchangeRateSelectedLiveData;

public class WalletBalanceViewModel extends BaseViewModel {
    public final WalletBalanceLiveData balanceLiveData;
    public final BlockChainStateLiveData chainStateLiveData;
    public final ExchangeRateLiveData rateLiveData;

    public WalletBalanceViewModel(@NonNull Application app) {
        super(app);
        balanceLiveData = new WalletBalanceLiveData(getApplication());
        chainStateLiveData = new BlockChainStateLiveData();
        rateLiveData = new ExchangeRateSelectedLiveData();
    }
}
