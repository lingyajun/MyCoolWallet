package com.bethel.mycoolwallet.fragment.preference;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;

import androidx.annotation.Nullable;

import com.bethel.mycoolwallet.CoolApplication;
import com.bethel.mycoolwallet.R;
import com.bethel.mycoolwallet.helper.Configuration;
import com.bethel.mycoolwallet.service.BlockChainService;
import com.bethel.mycoolwallet.utils.Constants;
import com.xuexiang.xui.widget.dialog.materialdialog.DialogAction;
import com.xuexiang.xui.widget.dialog.materialdialog.MaterialDialog;
import com.xuexiang.xui.widget.toast.XToast;

import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.script.Script;
import org.bitcoinj.wallet.DeterministicKeyChain;

import java.util.Locale;

public class DiagnosticsFragment extends PreferenceFragment {
    private static final String PREFS_KEY_INITIATE_RESET = "initiate_reset";
    private static final String PREFS_KEY_EXTENDED_PUBLIC_KEY = "extended_public_key";


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preference_diagnostics);
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        String key = preference.getKey();
        if (PREFS_KEY_EXTENDED_PUBLIC_KEY.equals(key)) {
            handleExtendedPubkey();
            return true;
        } else if (PREFS_KEY_INITIATE_RESET.equals(key)) {
            handleInitiateReset();
//            XToast.info(getActivity(), PREFS_KEY_INITIATE_RESET).show();
            return true;
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    private void handleInitiateReset() {
        final MaterialDialog dialog = new MaterialDialog.Builder(getActivity())
                .title(R.string.preferences_initiate_reset_title)
                .content(R.string.preferences_initiate_reset_dialog_message)
                .positiveText(R.string.preferences_initiate_reset_dialog_positive)
                .negativeText(R.string.button_dismiss)
                .show();
        dialog.getActionButton(DialogAction.POSITIVE).setOnClickListener(view -> {
            resetBlockChain();
            getActivity().finish();
        });
    }

    private void resetBlockChain() {
        BlockChainService.resetBlockChain(getActivity());
        Configuration.INSTANCE.updateLastBlockchainResetTime();
    }

    /**
     * DeterministicKeyChain :
     * 确定性钥匙链是{ KeyChain}，它使用  BIP 32标准 ，
     * 由{  DeterministicHierarchy}，用于从主种子派生钥匙串中的所有钥匙。
     *
     * DeterministicKey:
     * 确定性键是{ DeterministicHierarchy}中的一个节点。
     */
    private void handleExtendedPubkey() {
        DeterministicKeyChain keyChain = CoolApplication.getApplication().getWallet().getActiveKeyChain();
        DeterministicKey myKey = keyChain.getWatchingKey();

        Script.ScriptType outputScriptType = keyChain.getOutputScriptType();
        long creation = myKey.getCreationTimeSeconds();

        String pubBase58 = myKey.serializePubB58(Constants.NETWORK_PARAMETERS, outputScriptType);
        String data = String.format(Locale.US, "%s?c=%d&h=bip32", pubBase58, creation);
        ExtendedPublicKeyFragment.show(getFragmentManager(),  (CharSequence) data);
    }
}
