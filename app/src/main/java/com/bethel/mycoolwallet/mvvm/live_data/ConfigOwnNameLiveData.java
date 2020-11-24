package com.bethel.mycoolwallet.mvvm.live_data;

import android.content.SharedPreferences;

import androidx.lifecycle.LiveData;

import com.bethel.mycoolwallet.CoolApplication;
import com.bethel.mycoolwallet.helper.Configuration;

public class ConfigOwnNameLiveData extends LiveData<String> {
    public ConfigOwnNameLiveData() {
        mConfig = CoolApplication.getApplication().getConfiguration();
    }

    private final Configuration mConfig;

    @Override
    protected void onActive() {
        mConfig.registerOnSharedPreferenceChangeListener(listener);
        setValue(mConfig.getOwnName());
    }

    @Override
    protected void onInactive() {
       mConfig.unregisterOnSharedPreferenceChangeListener(listener);
    }

    private final SharedPreferences.OnSharedPreferenceChangeListener listener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
            if (Configuration.PREFS_KEY_OWN_NAME.equals(s)) {
                setValue(mConfig.getOwnName());
            }
        }
    };
}
