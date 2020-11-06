package com.bethel.mycoolwallet.fragment;

import android.app.Dialog;
import android.os.Bundle;
import android.text.Html;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

import com.bethel.mycoolwallet.R;
import com.xuexiang.xui.widget.dialog.materialdialog.DialogAction;
import com.xuexiang.xui.widget.dialog.materialdialog.MaterialDialog;

public class WalletBackupSuccessDialogFragment extends DialogFragment {
    private static final String TAG = "WalletBackupSuccessDialog";
    private static final String KEY_TARGET = "target";

    public static void showDialog(final FragmentManager fm, final String message) {
        final DialogFragment newFragment = new WalletBackupSuccessDialogFragment();
        final Bundle args = new Bundle();
        args.putString(KEY_TARGET, message);
        newFragment.setArguments(args);
        newFragment.show(fm, TAG);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        final String target = getArguments().getString(KEY_TARGET);
        MaterialDialog dialog = new MaterialDialog.Builder(getContext())
//                .iconRes(R.drawable.ic_warning_grey600_24dp)
                .title(R.string.export_keys_dialog_title)
                .content(Html.fromHtml(getString(R.string.export_keys_dialog_success, target)))
                .positiveText(R.string.button_ok)
                .show();
        dialog.getActionButton(DialogAction.POSITIVE).setOnClickListener(view -> getActivity().finish());
        return dialog;
    }
}
