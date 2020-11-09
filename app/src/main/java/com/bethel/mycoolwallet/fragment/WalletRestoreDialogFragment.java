package com.bethel.mycoolwallet.fragment;


import android.app.Dialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProviders;

import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import com.bethel.mycoolwallet.R;
import com.bethel.mycoolwallet.activity.WalletFilePickerActivity;
import com.bethel.mycoolwallet.data.Event;
import com.bethel.mycoolwallet.interfaces.IWalletRestoreCallback;
import com.bethel.mycoolwallet.manager.RequestPermissionsManager;
import com.bethel.mycoolwallet.mvvm.view_model.WalletRestoreViewModel;
import com.bethel.mycoolwallet.utils.Constants;
import com.bethel.mycoolwallet.utils.Crypto;
import com.bethel.mycoolwallet.utils.WalletUtils;
import com.nbsp.materialfilepicker.MaterialFilePicker;
import com.xuexiang.xui.widget.dialog.materialdialog.DialogAction;
import com.xuexiang.xui.widget.dialog.materialdialog.MaterialDialog;
//import com.xuexiang.xui.widget.textview.BadgeView;
import com.xuexiang.xui.widget.textview.badge.BadgeView;
import com.xuexiang.xui.widget.toast.XToast;

import org.bitcoinj.wallet.Wallet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

import butterknife.BindView;

import static android.app.Activity.RESULT_OK;

/**
 * A simple {@link Fragment} subclass.
 */
public class WalletRestoreDialogFragment extends BaseDialogFragment {
    /**
     * 选择钱包文件 RequestCode
     */
    public static final int WALLET_FILE_PICKER_REQUEST_CODE = 100;

    private static final String TAG = "WalletRestoreDialogFragment";

    private TextView positiveButton, negativeButton;

    @BindView(R.id.restore_wallet_dialog_message)
    TextView messageView;
    @BindView(R.id.import_keys_from_storage_file_tv)
    TextView fileView;
    @BindView(R.id.import_keys_from_storage_password)
    TextView passwordView;
    @BindView(R.id.restore_wallet_from_storage_dialog_replace_warning)
    TextView replaceWarningView;

    private RequestPermissionsManager permissionsManager;
    private File mSelectedFile=null;
    private WalletRestoreViewModel viewModel;

    private static final Logger log = LoggerFactory.getLogger(WalletRestoreDialogFragment.class);

    public static void show(final FragmentManager fm) {
        final DialogFragment newFragment = new WalletRestoreDialogFragment();
        newFragment.show(fm, TAG);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        permissionsManager = new RequestPermissionsManager(getActivity());
        permissionsManager.setPermissionsResult(grant -> {
            if (isAdded() && grant) {
                openWalletFilePicker();
            } else if (isAdded()) {
                XToast.info(getContext(), R.string.restore_wallet_permission_dialog_message).show();
            }
        });

        viewModel = ViewModelProviders.of(this).get(WalletRestoreViewModel.class);
        viewModel.showFailureDialog.observe(this, new Event.Observer<String>(){
            @Override
            public void onEvent(String content) {
                dismiss();
                WalletRestoreErrorDialogFragment.showDialog(getFragmentManager(), content);
            }
        });
        viewModel.showSuccessDialog.observe(this, new Event.Observer<Boolean>() {
            @Override
            public void onEvent(Boolean content) {
                dismiss();
                WalletRestoreSuccessDialogFragment.showDialog(getFragmentManager(), content);
            }
        });
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View view = createAndBindDialogView(R.layout.fragment_restore_wallet_dialog);
        messageView.setVisibility(View.GONE);
        MaterialDialog materialDialog = new MaterialDialog.Builder(getContext())
                .customView(view, true)
                .canceledOnTouchOutside(false)
                .title(R.string.import_keys_dialog_title)
                .negativeText(R.string.button_cancel)
                .positiveText(R.string.import_keys_dialog_button_import)
                .show();
        negativeButton =  materialDialog.getActionButton(DialogAction.NEGATIVE);
        negativeButton.setOnClickListener(view1 -> {
            passwordView.setText(null);
            dismissAllowingStateLoss();
        } );
        positiveButton=materialDialog.getActionButton(DialogAction.POSITIVE);
        positiveButton.setOnClickListener(view1 -> handleGo());

        materialDialog.setOnShowListener(dialogInterface -> {
            new BadgeView(getContext()).bindTarget(fileView).setBadgeText(""); // 加个小红点指示
//            messageView.setText(getString( R.string.import_keys_dialog_message ,""));
            updateView();

            maybeOpenWalletFilePicker();

            // todo WalletBalanceLiveData , replaceWarningView
        });

        fileView.setOnClickListener(view1 -> maybeOpenWalletFilePicker());
        return materialDialog;
    }

    private void handleGo() {
        File file = mSelectedFile;
        if (null == file) return;
        final String password = passwordView.getText().toString().trim();
        passwordView.setText(null); // get rid of it asap

        if (WalletUtils.BACKUP_FILE_FILTER.accept(file)) {
            restoreWalletFromProtobuf(file);
        } else if (isWalletFileEncrypted(file) && !TextUtils.isEmpty(password)) {
            restoreWalletFromEncrypted(file, password);
        }

    }

    private void restoreWalletFromEncrypted(File file, String password) {
        AsyncTask.execute(()->
                WalletUtils.restoreWalletFromEncrypted(file, password, mWalletRestoreCallback)
                );
    }

    private void restoreWalletFromProtobuf(File file) {
        AsyncTask.execute(()->
                WalletUtils.restoreWalletFromProtobuf(file, mWalletRestoreCallback));
    }

    private void updateView() {
        passwordView.setVisibility(isWalletFileEncrypted(mSelectedFile) ? View.VISIBLE : View.GONE);
        passwordView.setText(null);
        if (null!=mSelectedFile)
            fileView.setText(mSelectedFile.getName());
    }

    private boolean isWalletFileEncrypted(File file) {
        return null != file && Crypto.OPENSSL_FILE_FILTER.accept(file);
    }

    @Override
    public void onResume() {
        super.onResume();
        updateView();
    }

    private void maybeOpenWalletFilePicker() {
        if (permissionsManager.checkStoragePermission(getContext())) {
            openWalletFilePicker();
        } else {
            permissionsManager.requestStoragePermissions(WalletRestoreDialogFragment.this);
        }
    }

    private void openWalletFilePicker() {
        File sd = Constants.Files.EXTERNAL_WALLET_BACKUP_DIR;
        new MaterialFilePicker()
                // Pass a source of context. Can be:
                .withSupportFragment(this)
                // With cross icon on the right side of toolbar for closing picker straight away
                .withCloseMenu(true)
                // Entry point path (user will start from it)
                .withPath(sd.getAbsolutePath())
                // Root path (user won't be able to come higher than it)
                .withRootPath(Constants.Files.EXTERNAL_STORAGE_DIR.getAbsolutePath())
                // Showing hidden files
                .withHiddenFiles(false)
                .withFilterDirectories(false)
                .withCustomActivity(WalletFilePickerActivity.class)
                .withTitle(getString(R.string.import_keys_dialog_title))
                .withRequestCode(WALLET_FILE_PICKER_REQUEST_CODE)
                .start();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == WALLET_FILE_PICKER_REQUEST_CODE && resultCode == RESULT_OK) {
            String path = data.getStringExtra(WalletFilePickerActivity.RESULT_FILE_PATH);

            if (!TextUtils.isEmpty(path)) {
                File file = new File(path);
                mSelectedFile = file;
                updateView();
                if (!isWalletFileEncrypted(file)) {
                    handleGo();
                }
                log.info("Path: {}", path);
//                XToast.success(getContext(), "Picked file: "+ path).show();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (!permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults))
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    // todo : 1. alert && 2. store the configs { 3. reset block chain}
    private IWalletRestoreCallback mWalletRestoreCallback = new IWalletRestoreCallback() {
        @Override
        public void onSuccess(Wallet restoredWallet) {
            viewModel.showSuccessDialog.postValue(new Event<>(restoredWallet.isEncrypted()));
        }

        @Override
        public void onFailed(Exception e) {
            viewModel.showFailureDialog.postValue(new Event<>(e.getMessage()));
        }
    };
}
