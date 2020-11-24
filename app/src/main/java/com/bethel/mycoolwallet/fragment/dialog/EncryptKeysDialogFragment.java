package com.bethel.mycoolwallet.fragment.dialog;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Process;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProviders;

import com.bethel.mycoolwallet.CoolApplication;
import com.bethel.mycoolwallet.R;
import com.bethel.mycoolwallet.data.PasswordStrength;
import com.bethel.mycoolwallet.helper.Configuration;
import com.bethel.mycoolwallet.mvvm.view_model.MainActivityViewModel;
import com.bethel.mycoolwallet.utils.Constants;
import com.bethel.mycoolwallet.utils.Utils;
import com.bethel.mycoolwallet.utils.WalletUtils;
import com.google.common.base.Strings;
import com.xuexiang.xui.widget.dialog.materialdialog.DialogAction;
import com.xuexiang.xui.widget.dialog.materialdialog.MaterialDialog;

import org.bitcoinj.crypto.KeyCrypterException;
import org.bitcoinj.crypto.KeyCrypterScrypt;
import org.bitcoinj.wallet.Wallet;
import org.bouncycastle.crypto.params.KeyParameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import butterknife.BindView;

public class EncryptKeysDialogFragment extends BaseDialogFragment {
    private static final String TAG = "EncryptKeysDialogFragme";

    public static void show(final FragmentManager fm) {
        final DialogFragment newFragment = new EncryptKeysDialogFragment();
        newFragment.show(fm,  TAG);
    }

    private static final Logger log = LoggerFactory.getLogger(EncryptKeysDialogFragment.class);

    @BindView(R.id.encrypt_keys_dialog_password_old_group)
     View oldPasswordGroup;
    @BindView(R.id.encrypt_keys_dialog_password_old)
     EditText oldPasswordView;
    @BindView(R.id.encrypt_keys_dialog_password_new)
     EditText newPasswordView;
    @BindView(R.id.encrypt_keys_dialog_bad_password)
     View badPasswordView;
    @BindView(R.id.encrypt_keys_dialog_password_strength)
     TextView passwordStrengthView;

    private TextView positiveButton, negativeButton;

    private  DialogInterface myDialog;

    private HandlerThread backgroundThread;
    private Handler backgroundHandler;

    private enum State {
        INPUT, CRYPTING, DONE
    }
    private State state = State.INPUT;

    private Wallet wallet;
    private MainActivityViewModel mainActivityViewModel;
    private Configuration mConfig;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        wallet = CoolApplication.getApplication().getWallet();
        mainActivityViewModel = ViewModelProviders.of(getActivity()).get(MainActivityViewModel.class);
        mConfig = CoolApplication.getApplication().getConfiguration();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        backgroundThread = new HandlerThread("backgroundThread", Process.THREAD_PRIORITY_BACKGROUND);
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View view = createAndBindDialogView(R.layout.encrypt_keys_dialog);
        MaterialDialog materialDialog = new MaterialDialog.Builder(getContext())
                .customView(view, true)
                .canceledOnTouchOutside(false)
                .title(R.string.encrypt_keys_dialog_title)
                .positiveText(R.string.button_ok)
                .negativeText(R.string.button_cancel)
                .show();

        materialDialog.setOnShowListener((dialogInterface)->{
            positiveButton = materialDialog.getActionButton(DialogAction.POSITIVE);
            negativeButton = materialDialog.getActionButton(DialogAction.NEGATIVE);

            positiveButton.setTypeface(Typeface.DEFAULT_BOLD);
            positiveButton.setOnClickListener(view1 -> handleGo());
            negativeButton.setOnClickListener(view1 -> dismissAllowingStateLoss());

            oldPasswordView.addTextChangedListener(textWatcher);
            newPasswordView.addTextChangedListener(textWatcher);

            myDialog = dialogInterface;
            updateView();
        });
        return materialDialog;
    }

    @Override
    public void onResume() {
        super.onResume();
        updateView();
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        myDialog = null;

        oldPasswordView.removeTextChangedListener(textWatcher);
        newPasswordView.removeTextChangedListener(textWatcher);
        wipePasswords();
        super.onDismiss(dialog);
    }

    private void wipePasswords() {
        oldPasswordView.setText(null);
        newPasswordView.setText(null);
    }

    @Override
    public void onDestroy() {
        backgroundThread.getLooper().quit();
        super.onDestroy();
    }

    // todo
    private void handleGo() {
        final String oldPassword = Strings.emptyToNull(oldPasswordView.getText().toString().trim());
        final String newPassword = Strings.emptyToNull(newPasswordView.getText().toString().trim());

        if (oldPassword != null && newPassword != null)
            log.info("changing spending password");
        else if (newPassword != null)
            log.info("setting spending password");
        else if (oldPassword != null)
            log.info("removing spending password");
        else
            throw new IllegalStateException();

        state = State.CRYPTING;
        updateView();

        backgroundHandler.post(() ->{
            final KeyParameter oldKey = oldPassword != null ? wallet.getKeyCrypter().deriveKey(oldPassword) : null;

            // 新密码, 创建一个新的 key crypter （加密器）.
            final KeyCrypterScrypt keyCrypter =
                    new KeyCrypterScrypt(CoolApplication.getApplication().scryptIterationsTarget());
            final KeyParameter newKey = newPassword != null ? keyCrypter.deriveKey(newPassword) : null;

            runOnUIthread(()->{
                // 钱包已加密了,需要先解密
                if (wallet.isEncrypted()) {
                    if (null!= oldKey) {   // 使用旧密码 解密
                        try {
                            wallet.decrypt(oldKey);
                            state = State.DONE;
                            log.info("wallet successfully decrypted");
                        } catch (KeyCrypterException e){
                            badPasswordView.setVisibility(View.VISIBLE);
                            state = State.INPUT;
                            oldPasswordView.requestFocus();
                            log.info("wallet decryption failed: " + e.getMessage());
                        }

                    } else {
                        state = State.INPUT;
                        oldPasswordView.requestFocus();
                    }
                }

                // Returns true if the wallet contains random keys and no HD chains.
                // P2WPKH ( pay to witness pubkey hash)
                if (wallet.isDeterministicUpgradeRequired(Constants.UPGRADE_OUTPUT_SCRIPT_TYPE)
                        && !wallet.isEncrypted()) {
                    wallet.upgradeToDeterministic(Constants.UPGRADE_OUTPUT_SCRIPT_TYPE, null);
                }

                // 使用新密码加密钱包
                if (null!=newKey && !wallet.isEncrypted()) {
                    wallet.encrypt(keyCrypter, newKey);
                    // todo : save to preference ---  mConfig.updateLastEncryptKeysTime();
                    state = State.DONE;
                    log.info(
                            "wallet successfully encrypted, using key derived by new spending password ({} scrypt iterations)",
                            keyCrypter.getScryptParameters().getN());
                }

                updateView();
                if (state == State.DONE) {
                    // 备份钱包文件
                    WalletUtils.autoBackupWallet(getContext(), wallet);
                    // 加密操作完成，通知外部～
                    mainActivityViewModel.walletEncrypted.load();
                    // dialog 消失
                    runOnUIthread(()-> dismiss() , 2000);
//                    XToast.success(getContext(),"wallet successfully encrypted !").show();
                }
            });
        });
    }

    private void updateView() {
        if (null == myDialog || null == wallet) return;
        final boolean hasOldPassword = !oldPasswordView.getText().toString().trim().isEmpty();
        final boolean hasPassword = !newPasswordView.getText().toString().trim().isEmpty();

        oldPasswordGroup.setVisibility(wallet.isEncrypted() ? View.VISIBLE : View.GONE);
        oldPasswordView.setEnabled(state == State.INPUT);

        newPasswordView.setEnabled(state == State.INPUT);

        Context activity = getContext();
        final int passwordLength = newPasswordView.getText().length();
        passwordStrengthView.setVisibility(state == State.INPUT && passwordLength > 0 ? View.VISIBLE : View.INVISIBLE);

        PasswordStrength strength = Utils.getPinPasswordStrength(newPasswordView.getText());
        passwordStrengthView.setText(strength.getTextId());
        passwordStrengthView.setTextColor(ContextCompat.getColor(activity, strength.getColorId()));

        if (state == State.INPUT) {
            if (wallet.isEncrypted()) {
                positiveButton.setText(hasPassword ? R.string.button_edit : R.string.button_remove);
                positiveButton.setEnabled(hasOldPassword);
            } else {
                positiveButton.setText(R.string.button_set);
                positiveButton.setEnabled(hasPassword);
            }

            negativeButton.setEnabled(true);
        } else if (state == State.CRYPTING) {
            positiveButton.setText(newPasswordView.getText().toString().trim().isEmpty()
                    ? R.string.encrypt_keys_dialog_state_decrypting : R.string.encrypt_keys_dialog_state_encrypting);
            positiveButton.setEnabled(false);
            negativeButton.setEnabled(false);
        } else if (state == State.DONE) {
            positiveButton.setText(R.string.encrypt_keys_dialog_state_done);
            positiveButton.setEnabled(false);
            negativeButton.setEnabled(false);
        }
    }

    private final TextWatcher textWatcher = new TextWatcher() {
        @Override
        public void onTextChanged(final CharSequence s, final int start, final int before, final int count) {
            badPasswordView.setVisibility(View.INVISIBLE);
            updateView();
        }

        @Override
        public void beforeTextChanged(final CharSequence s, final int start, final int count, final int after) {
        }

        @Override
        public void afterTextChanged(final Editable s) {
        }
    };

}
