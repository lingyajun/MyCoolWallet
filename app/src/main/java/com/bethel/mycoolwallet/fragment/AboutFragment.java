package com.bethel.mycoolwallet.fragment;

import android.content.pm.PackageInfo;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;

import androidx.annotation.Nullable;

import com.bethel.mycoolwallet.CoolApplication;
import com.bethel.mycoolwallet.R;
import com.bethel.mycoolwallet.utils.Utils;
import com.bethel.mycoolwallet.utils.WalletUtils;
import com.xuexiang.xui.widget.dialog.materialdialog.MaterialDialog;

import org.bitcoinj.core.VersionMessage;

import java.io.IOException;

public class AboutFragment extends PreferenceFragment {
    private static final String KEY_ABOUT_VERSION = "about_version";
    private static final String KEY_ABOUT_CREDITS_BITCOINJ = "about_credits_bitcoinj";

    private CoolApplication application;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        application = CoolApplication.getApplication();
        addPreferencesFromResource(R.xml.preference_about);
        findPreference(KEY_ABOUT_CREDITS_BITCOINJ)
                .setTitle(getString(R.string.about_credits_bitcoinj_title, VersionMessage.BITCOINJ_VERSION));

        final PackageInfo packageInfo = application.packageInfo();
        final Preference versionPref = findPreference(KEY_ABOUT_VERSION);
        versionPref.setSummary(Utils.versionLine(packageInfo));
        versionPref.setOnPreferenceClickListener(preference -> {
            showApkHashDialog();
            return true;
        });
    }

    private void showApkHashDialog() {
        CharSequence message;
        try {
            final String hash = Utils.apkHash(application).toString();
            message = WalletUtils.formatHash(hash, 4, 0);
        } catch (IOException e) {
            message = "n/a";
        }

        new MaterialDialog.Builder(getActivity())
                .title(R.string.about_version_apk_hash_title)
                .content(message)
                .neutralText(R.string.button_ok)
                .show();
    }
}
