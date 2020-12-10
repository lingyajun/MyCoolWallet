package com.bethel.mycoolwallet.mvvm.live_data;

import android.content.SharedPreferences;

import androidx.lifecycle.LiveData;

import com.bethel.mycoolwallet.CoolApplication;
import com.bethel.mycoolwallet.helper.Configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigOwnNameLiveData extends LiveData<String> {
    public ConfigOwnNameLiveData() {
        mConfig = CoolApplication.getApplication().getConfiguration();
    }

    private final Configuration mConfig;
    private static final Logger log = LoggerFactory.getLogger(ConfigOwnNameLiveData.class);

    @Override
    protected void onActive() {
        mConfig.registerOnSharedPreferenceChangeListener(listener);
//        setValue(mConfig.getOwnName());
        final String name = mConfig.getOwnName();
        setValue(name);
        log.info("name {}", name);
    }

    @Override
    protected void onInactive() {
       mConfig.unregisterOnSharedPreferenceChangeListener(listener);
    }

    private final SharedPreferences.OnSharedPreferenceChangeListener listener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
            if (Configuration.PREFS_KEY_OWN_NAME.equals(s)) {
//                setValue(mConfig.getOwnName());
                setValue(Configuration.INSTANCE.getOwnName());
            }
        }
    };
}
