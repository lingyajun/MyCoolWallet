package com.bethel.mycoolwallet.fragment.preference;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.text.TextUtils;

import androidx.annotation.Nullable;

import com.bethel.mycoolwallet.R;
import com.bethel.mycoolwallet.helper.Configuration;
import com.bethel.mycoolwallet.request.dns.ResolveDnsTask;
import com.bethel.mycoolwallet.service.BlockChainService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;

public class SettingsFragment extends PreferenceFragment {
    private final Handler handler = new Handler();

    private Preference trustedPeerPreference;
    private Preference trustedPeerOnlyPreference;

    private static final Logger log = LoggerFactory.getLogger(SettingsFragment.class);

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preference_settings);

        trustedPeerPreference = findPreference(Configuration.PREFS_KEY_TRUSTED_PEER);
        ((EditTextPreference) trustedPeerPreference).getEditText().setSingleLine();
        trustedPeerPreference.setOnPreferenceChangeListener(changeListener);

        trustedPeerOnlyPreference = findPreference(Configuration.PREFS_KEY_TRUSTED_PEER_ONLY);
        trustedPeerOnlyPreference.setOnPreferenceChangeListener(changeListener);

        /* <intent
            android:targetClass="com.android.settings.Settings$DataUsageSummaryActivity"
            android:targetPackage="com.android.settings" />
         */
        final Preference dataUsagePreference = findPreference(Configuration.PREFS_KEY_DATA_USAGE);
        final PackageManager pm = getActivity().getPackageManager();
        final boolean dataUsage = pm.resolveActivity(dataUsagePreference.getIntent(), 0) !=null;
        dataUsagePreference.setEnabled(dataUsage);
    }

    private final Preference.OnPreferenceChangeListener changeListener = (preference, o) -> {
// preference isn't persisted until after this method returns
        handler.post(() ->{
            if (preference.equals(trustedPeerPreference) ){
                BlockChainService.stop(getActivity());

                updateTrustedPeer();
            } else if (preference.equals(trustedPeerOnlyPreference) ) {
                BlockChainService.stop(getActivity());
            }
        });
        return true;
    };

    private void updateTrustedPeer() {
        final String trustedPeer = Configuration.INSTANCE.getTrustedPeerHost();
        if (TextUtils.isEmpty(trustedPeer)) {
            trustedPeerPreference.setSummary(R.string.preferences_trusted_peer_summary);
            trustedPeerOnlyPreference.setEnabled(false);
            return;
        }
        // not empty
        final String summary = String.format("%s\n[%s]", trustedPeer,
                getString(R.string.preferences_trusted_peer_resolve_progress)); //正在解析...
        trustedPeerPreference.setSummary(summary);
        trustedPeerOnlyPreference.setEnabled(true);

        //解析 dns
        new ResolveDnsTask(trustedPeer){
            @Override
            protected void onSuccess(InetAddress address) {
                if (!isAdded()) return;
                trustedPeerPreference.setSummary(trustedPeer);
                log.info("trusted peer '{}' resolved to {}", trustedPeer, address);
            }

            @Override
            protected void onUnknownHost() {
                if (!isAdded()) return;
                final String summary = String.format("%s\n[%s]", trustedPeer,
                        getString(R.string.preferences_trusted_peer_resolve_unknown_host)); //Unknown Host
                trustedPeerPreference.setSummary(summary);
            }
        }.executeAsyncTask();
    }

    @Override
    public void onDestroy() {
        trustedPeerPreference.setOnPreferenceChangeListener(null);
        trustedPeerOnlyPreference.setOnPreferenceChangeListener(null);
        super.onDestroy();
    }
}
