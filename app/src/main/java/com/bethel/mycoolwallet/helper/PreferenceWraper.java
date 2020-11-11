package com.bethel.mycoolwallet.helper;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Build;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * SharedPreference包装类
 * 
 * @author PeterZhang
 * 
 */
public class PreferenceWraper {
    protected SharedPreferences mSharedPref;

    protected static Map<String, PreferenceWraper> sInstanceMap = new HashMap<String, PreferenceWraper>();

    public synchronized static PreferenceWraper getInstance(Context context) {
        return getInstance(context, null);
    }
    public synchronized static PreferenceWraper getInstance(Context context, final String prefFileName) {
       final String key = !TextUtils.isEmpty(prefFileName) ? prefFileName : "_____defau1t_____";
        if (sInstanceMap.containsKey(key)) {
            return sInstanceMap.get(key);
        }
        PreferenceWraper prefWraper = new PreferenceWraper(context, prefFileName);
        sInstanceMap.put(key, prefWraper);
        return prefWraper;
    }

    protected PreferenceWraper(Context context, String prefFileName) {
        mSharedPref = !TextUtils.isEmpty(prefFileName) ?
                context.getSharedPreferences(prefFileName, Context.MODE_PRIVATE):
                PreferenceManager.getDefaultSharedPreferences(context);
    }

    public void putString(String key, String value) {
        Editor editor = mSharedPref.edit().putString(key, value);
        commit(editor);
    }

    public String getString(String key, String defValue) {
        return mSharedPref.getString(key, defValue);
    }

    public void putLong(String key, long value) {
        Editor editor = mSharedPref.edit().putLong(key, value);
        commit(editor);
    }

    public long getLong(String key, long defaultValue) {
        return mSharedPref.getLong(key, defaultValue);
    }

    public void putInt(String key, int value) {
        Editor editor = mSharedPref.edit().putInt(key, value);
        commit(editor);
    }

    public int getInt(String key, int defaultValue) {
        return mSharedPref.getInt(key, defaultValue);
    }

    public void putBoolean(String key, boolean value) {
        Editor editor = mSharedPref.edit().putBoolean(key, value);
        commit(editor);
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        return mSharedPref.getBoolean(key, defaultValue);
    }

    public void remove(String key) {
        Editor editor = mSharedPref.edit().remove(key);
        commit(editor);
    }

    @SuppressLint("NewApi")
    public static boolean commit(SharedPreferences.Editor editor) {
        if (Build.VERSION.SDK_INT < 9) {
            return editor.commit();
        }
        editor.apply();
        return true;
    }

    public SharedPreferences getSharedPreferences() {
        return mSharedPref;
    }

    public void registerOnSharedPreferenceChangeListener(final SharedPreferences.OnSharedPreferenceChangeListener listener) {
        mSharedPref.registerOnSharedPreferenceChangeListener(listener);
    }

    public void unregisterOnSharedPreferenceChangeListener(final SharedPreferences.OnSharedPreferenceChangeListener listener) {
        mSharedPref.unregisterOnSharedPreferenceChangeListener(listener);
    }
}
