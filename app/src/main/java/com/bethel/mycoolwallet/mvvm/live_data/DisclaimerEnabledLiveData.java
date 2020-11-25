package com.bethel.mycoolwallet.mvvm.live_data;

import android.content.SharedPreferences;

import androidx.lifecycle.LiveData;

import com.bethel.mycoolwallet.CoolApplication;
import com.bethel.mycoolwallet.helper.Configuration;

/**
 * 免责声明
 */
public class DisclaimerEnabledLiveData extends LiveData<Boolean> {
    private final Configuration mConfig;

    public DisclaimerEnabledLiveData() {
        mConfig = CoolApplication.getApplication().getConfiguration();
    }

    @Override
    protected void onActive() {
        mConfig.registerOnSharedPreferenceChangeListener(listener);
        setValue(mConfig.getDisclaimerEnabled());
    }

    @Override
    protected void onInactive() {
       mConfig.unregisterOnSharedPreferenceChangeListener(listener);
    }

    private final SharedPreferences.OnSharedPreferenceChangeListener listener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
            if (Configuration.PREFS_KEY_DISCLAIMER.equals(s)) {
                setValue(mConfig.getDisclaimerEnabled());
            }
        }
    };
}
