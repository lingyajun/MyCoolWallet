package com.bethel.mycoolwallet.mvvm.live_data;

import android.content.SharedPreferences;

import androidx.lifecycle.LiveData;

import com.bethel.mycoolwallet.CoolApplication;
import com.bethel.mycoolwallet.helper.Configuration;

import org.bitcoinj.utils.MonetaryFormat;

public class ConfigFormatLiveData extends LiveData<MonetaryFormat> {
    private final Configuration mConfig;
    public ConfigFormatLiveData() {
        mConfig = CoolApplication.getApplication().getConfiguration();
    }

    @Override
    protected void onActive() {
        mConfig.registerOnSharedPreferenceChangeListener(listener);
        setValue(mConfig.getFormat());
    }

    @Override
    protected void onInactive() {
       mConfig.unregisterOnSharedPreferenceChangeListener(listener);
    }


    private final SharedPreferences.OnSharedPreferenceChangeListener listener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
            if (Configuration.PREFS_KEY_BTC_PRECISION.equals(s)) {
                setValue(mConfig.getFormat());
            }
        }
    };
}
