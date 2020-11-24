package com.bethel.mycoolwallet.fragment.dialog;

import android.app.Dialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

import com.bethel.mycoolwallet.R;
import com.bethel.mycoolwallet.service.BlockChainService;
import com.xuexiang.xui.widget.dialog.materialdialog.DialogAction;
import com.xuexiang.xui.widget.dialog.materialdialog.MaterialDialog;

public class WalletRestoreSuccessDialogFragment extends DialogFragment {
    private static final String TAG = "WalletRestoreSuccessDialog";
    private static final String KEY_SHOW_ENCRYPTED_MESSAGE = "show_encrypted_message";
    public static void showDialog(final FragmentManager fm, final  boolean showEncryptedMessage) {
        final DialogFragment newFragment = new WalletRestoreSuccessDialogFragment();
        final Bundle args = new Bundle();
        args.putBoolean(KEY_SHOW_ENCRYPTED_MESSAGE, showEncryptedMessage);
        newFragment.setArguments(args);
        newFragment.show(fm, TAG);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        final boolean showEncryptedMessage = getArguments().getBoolean(KEY_SHOW_ENCRYPTED_MESSAGE);
        final StringBuilder message = new StringBuilder();
//        message.append(getString(R.string.restore_wallet_dialog_success));
//        message.append("\n\n");
        message.append(getString(R.string.restore_wallet_dialog_success_replay));
        if (showEncryptedMessage) {
            message.append("\n\n");
            message.append(getString(R.string.restore_wallet_dialog_success_encrypted));
        }

        MaterialDialog dialog = new MaterialDialog.Builder(getContext())
                .title(R.string.restore_wallet_dialog_success)
                .content(message)
                .positiveText(R.string.button_ok)
                .show();
        dialog.getActionButton(DialogAction.POSITIVE).setOnClickListener(view -> {
            //   block chain reset
              BlockChainService.resetBlockChain(getContext());
//            getActivity().finish();
            dismiss();
                });
        return dialog;
    }
}
