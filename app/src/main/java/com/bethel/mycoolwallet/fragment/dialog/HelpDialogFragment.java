/*
 * Copyright 2013-2015 the original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.bethel.mycoolwallet.fragment.dialog;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.Html;

import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

import com.bethel.mycoolwallet.R;
import com.xuexiang.xui.widget.dialog.materialdialog.MaterialDialog;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 显示一段文字信息
 */
public final class HelpDialogFragment extends DialogFragment {
    private static final String FRAGMENT_TAG = HelpDialogFragment.class.getName();

    private static final String KEY_MESSAGE = "message";

    public static void show(final FragmentManager fm, final int messageResId) {
        final DialogFragment newFragment = HelpDialogFragment.instance(messageResId);
        newFragment.show(fm, FRAGMENT_TAG);
    }

    private static HelpDialogFragment instance(final int messageResId) {
        final HelpDialogFragment fragment = new HelpDialogFragment();

        final Bundle args = new Bundle();
        args.putInt(KEY_MESSAGE, messageResId);
        fragment.setArguments(args);

        return fragment;
    }

//    private Activity activity;

    private static final Logger log = LoggerFactory.getLogger(HelpDialogFragment.class);

    @Override
    public void onAttach(final Context context) {
        super.onAttach(context);
//        this.activity = (AbstractWalletActivity) context;
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        log.info("opening dialog {}", getClass().getName());
    }

    @Override
    public Dialog onCreateDialog(final Bundle savedInstanceState) {
        final Bundle args = getArguments();
        final int messageResId = args.getInt(KEY_MESSAGE);
        MaterialDialog materialDialog = new MaterialDialog.Builder(getContext())
                .canceledOnTouchOutside(false)
                .content(Html.fromHtml(getString(messageResId)))
                .positiveText(R.string.button_ok)
                .show();
        return materialDialog;

//        final DialogBuilder dialog = new DialogBuilder(getActivity());
//        dialog.setMessage(Html.fromHtml(getString(messageResId)));
////        dialog.singleDismissButton(null);
//        dialog.singleConfirmButton(null);
//        return dialog.create();
    }
}
