package com.bethel.mycoolwallet.fragment;

import android.app.Dialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

import com.bethel.mycoolwallet.R;
import com.xuexiang.xui.widget.dialog.materialdialog.DialogAction;
import com.xuexiang.xui.widget.dialog.materialdialog.MaterialDialog;

public class WalletBackupErrorDialogFragment extends DialogFragment {
    private static final String TAG = "WalletBackupErrorDialog";
    private static final String KEY_EXCEPTION_MESSAGE = "exception_message";

    public static void showDialog(final FragmentManager fm, final String exceptionMessage) {
        final DialogFragment newFragment = new WalletBackupErrorDialogFragment();
        final Bundle args = new Bundle();
        args.putString(KEY_EXCEPTION_MESSAGE, exceptionMessage);
        newFragment.setArguments(args);
        newFragment.show(fm, TAG);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        final String exceptionMessage = getArguments().getString(KEY_EXCEPTION_MESSAGE);
        MaterialDialog dialog = new MaterialDialog.Builder(getContext())
                .iconRes(R.drawable.ic_warning_grey600_24dp)
                .title(R.string.import_export_keys_dialog_failure_title)
                .content(getString(R.string.export_keys_dialog_failure, exceptionMessage))
                .positiveText(R.string.button_ok)
                .show();
        dialog.getActionButton(DialogAction.POSITIVE).setOnClickListener(view -> getActivity().finish());
        return dialog;
    }
}
