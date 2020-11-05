package com.bethel.mycoolwallet.fragment;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;

import com.bethel.mycoolwallet.R;
import com.xuexiang.xui.widget.toast.XToast;

public class DiagnosticsFragment extends PreferenceFragment {
    private static final String PREFS_KEY_INITIATE_RESET = "initiate_reset";
    private static final String PREFS_KEY_EXTENDED_PUBLIC_KEY = "extended_public_key";


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preference_diagnostics);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
//        View view = inflater.inflate(R.layout.preference_layout, container, false);
//        return view;
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        String key = preference.getKey();
        if (PREFS_KEY_EXTENDED_PUBLIC_KEY.equals(key)) {
            handleExtendedPubkey();
//            XToast.info(getActivity(), PREFS_KEY_EXTENDED_PUBLIC_KEY).show();
            return true;
        } else if (PREFS_KEY_INITIATE_RESET.equals(key)) {
            XToast.info(getActivity(), PREFS_KEY_INITIATE_RESET).show();
            return true;
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    private void handleExtendedPubkey() {
    }
}
