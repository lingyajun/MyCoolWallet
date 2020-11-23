package com.bethel.mycoolwallet.mvvm.live_data;

import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.text.TextUtils;

import androidx.lifecycle.LiveData;

import com.bethel.mycoolwallet.data.ExchangeRateBean;
import com.bethel.mycoolwallet.helper.Configuration;

public class ExchangeRateSelectedLiveData extends ExchangeRateLiveData {
    public ExchangeRateSelectedLiveData(String currencyCode) {
        super(currencyCode);
    }

    public ExchangeRateSelectedLiveData() {
        this(null);
    }

    @Override
    protected void onActive() {
        mConfig.registerOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);
        super.onActive();
    }

    @Override
    protected void onInactive() {
        mConfig.unregisterOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);
    }

    @Override
    protected void postValue(ExchangeRateBean value) {
        super.postValue(value);
    }

    private final SharedPreferences.OnSharedPreferenceChangeListener sharedPreferenceChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
            // todo
            if (!TextUtils.isEmpty(s) && Configuration.PREFS_KEY_CURRENCY_CODE.equals(s)) {
//                AsyncTask.execute(()-> load(mConfig.getMyCurrencyCode()));
                load(mConfig.getMyCurrencyCode());
            }
        }
    };
}
