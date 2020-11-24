package com.bethel.mycoolwallet.fragment.dialog;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
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
import com.bethel.mycoolwallet.interfaces.IWalletBackupCallback;
import com.bethel.mycoolwallet.mvvm.view_model.WalletBackupViewModel;
import com.bethel.mycoolwallet.utils.Constants;
import com.bethel.mycoolwallet.utils.Iso8601Format;
import com.bethel.mycoolwallet.utils.Utils;
import com.bethel.mycoolwallet.utils.WalletUtils;
import com.xuexiang.xui.widget.dialog.materialdialog.DialogAction;
import com.xuexiang.xui.widget.dialog.materialdialog.MaterialDialog;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DateFormat;
import java.util.Date;
import java.util.TimeZone;

import butterknife.BindView;

import static androidx.core.util.Preconditions.checkNotNull;
import static androidx.core.util.Preconditions.checkState;

public class WalletBackupDialogFragment extends BaseDialogFragment {
    private static final String TAG = "WalletBackupDialogFragment";

    @BindView(R.id.backup_wallet_dialog_password)
    EditText passwordView;
    @BindView(R.id.backup_wallet_dialog_password_again)
    EditText passwordAgainView;
    @BindView(R.id.backup_wallet_dialog_password_strength)
    TextView passwordStrengthView;
    @BindView(R.id.backup_wallet_dialog_password_mismatch)
    View passwordMismatchView;
    @BindView(R.id.backup_wallet_dialog_warning_encrypted)
    TextView warningView;

    private TextView positiveButton, negativeButton;
    private WalletBackupViewModel viewModel;

    private static final int REQUEST_CODE_CREATE_DOCUMENT = 0;

    private static final Logger log = LoggerFactory.getLogger(WalletBackupDialogFragment.class);

    public static void show(final FragmentManager fm) {
        final DialogFragment newFragment = new WalletBackupDialogFragment();
        newFragment.show(fm, TAG);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = ViewModelProviders.of(this).get(WalletBackupViewModel.class);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View view = createAndBindDialogView(R.layout.fragment_backup_wallet_dialog);
        MaterialDialog materialDialog = new MaterialDialog.Builder(getContext())
                .customView(view, true)
                .canceledOnTouchOutside(false)
                .title(R.string.export_keys_dialog_title)
                .negativeText(R.string.button_cancel)
                .positiveText(R.string.button_ok)
                .show();
        negativeButton =  materialDialog.getActionButton(DialogAction.NEGATIVE);
        negativeButton.setOnClickListener(view1 -> {
            dismissAllowingStateLoss();
            getActivity().finish();
        } );
        positiveButton=materialDialog.getActionButton(DialogAction.POSITIVE);
        positiveButton.setOnClickListener(view1 -> handleGo());

        materialDialog.setOnShowListener(dialogInterface -> {
            positiveButton.setEnabled(false);
            positiveButton.setTypeface(Typeface.DEFAULT_BOLD);

            viewModel.walletLiveData.observe(this, wallet ->
                    warningView.setVisibility(wallet.isEncrypted() ?  View.VISIBLE : View.GONE));

            passwordAgainView.addTextChangedListener(textWatcher);
            passwordView.addTextChangedListener(textWatcher);

            viewModel.password.observe(this, s -> {
                passwordMismatchView.setVisibility(View.INVISIBLE);

                final int passwordLength = passwordView.getText().length();
                passwordStrengthView.setVisibility(passwordLength > 0 ? View.VISIBLE : View.INVISIBLE);
                PasswordStrength strength = Utils.getBackupWalletPasswordStrength(passwordView.getText());
                passwordStrengthView.setText(strength.getTextId());
                passwordStrengthView
                        .setTextColor(ContextCompat.getColor(getContext(), strength.getColorId()));

                final boolean hasPassword = passwordLength>0;
                final boolean hasPasswordAgain = !passwordAgainView.getText().toString().trim().isEmpty();
                final boolean hasWallet = viewModel.walletLiveData.getValue() != null;
                if (null!=positiveButton) {
                    positiveButton.setEnabled(hasPassword && hasPasswordAgain && hasWallet);
                }
                log.info("positiveButton: hasPassword- '{}',hasPasswordAgain- {}, hasWallet- {} ",
                        hasPassword, hasPasswordAgain, hasWallet);
            });
        });
        return materialDialog;
    }

    private void handleGo() {
        final String password = passwordView.getText().toString().trim();
        final String passwordAgain = passwordAgainView.getText().toString().trim();

        if (passwordAgain.equals(password)) {
            backupWallet();
        } else {
            passwordMismatchView.setVisibility(View.VISIBLE);
        }
    }

    private void backupWallet() {
        passwordView.setEnabled(false);
        passwordAgainView.setEnabled(false);

        final DateFormat dateFormat = new Iso8601Format("yyyy-MM-dd-HH-mm");
        dateFormat.setTimeZone(TimeZone.getDefault());

        // filename = bitcoin-wallet-backup-testnet-2020-11-05-09-52
        final StringBuilder filename = new StringBuilder(Constants.Files.EXTERNAL_WALLET_BACKUP);
        filename.append('-');
        filename.append(dateFormat.format(new Date()));

        // 请求创建文件
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType(Constants.MIMETYPE_WALLET_BACKUP);
        intent.putExtra(Intent.EXTRA_TITLE, filename.toString());
        startActivityForResult(intent, REQUEST_CODE_CREATE_DOCUMENT);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (REQUEST_CODE_CREATE_DOCUMENT != requestCode) {
            super.onActivityResult(requestCode, resultCode, data);
            return;
        }

        // 请求创建备份文件 返回结果
        if (resultCode == Activity.RESULT_OK) {
            viewModel.walletLiveData.observe(this, wallet -> {
                viewModel.walletLiveData.removeObservers(this);
                final Uri targetUri = checkNotNull(data.getData());
                final String password = passwordView.getText().toString().trim();
                checkState(!password.isEmpty());
                AsyncTask.execute(() -> {
                    WalletUtils.backupWallet2FileSystem(getActivity().getContentResolver(),
                            viewModel.walletLiveData.getValue(),password, targetUri,
                            new IWalletBackupCallback() {
                        @Override
                        public void onSuccess() {
//   handle success
//  application.getConfiguration().disarmBackupReminder();
                          runOnUIthread(()->{
                              CoolApplication.getApplication().getConfiguration().disarmBackupReminder();
                              dismiss();
                              String target = WalletUtils.getFileStorageName(targetUri);
                              WalletBackupSuccessDialogFragment.showDialog(getFragmentManager(),
                                      target != null ? target : targetUri.toString());
//                              XToast.success(getContext(), "success !").show();
                          });
                        }

                        @Override
                        public void onFailed(Exception e) {
                            runOnUIthread(()->{
                                dismiss();
                                WalletBackupErrorDialogFragment.showDialog(getFragmentManager(), e.getMessage());
                                //  handle failed
//                                XToast.error(getContext(), "failed : " + e.getMessage()).show();
                            });
                        }
                    });
                }); // ==== end AsyncTask ====
            });

        } else  if (resultCode == Activity.RESULT_CANCELED) {
            log.info("cancelled backing up wallet");
            dismiss();
            getActivity().finish();
        }
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        passwordView.removeTextChangedListener(textWatcher);
        passwordAgainView.removeTextChangedListener(textWatcher);

        wipePasswords();
        super.onDismiss(dialog);
//        getActivity().finish();
    }

    private void wipePasswords() {
        passwordView.setText(null);
        passwordAgainView.setText(null);
    }

    private final TextWatcher textWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            viewModel.password.postValue(charSequence.toString().trim());
        }

        @Override
        public void afterTextChanged(Editable editable) {

        }
    };
}
