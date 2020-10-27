package com.bethel.mycoolwallet.fragment;


import android.app.Dialog;
import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ShareCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;

import com.bethel.mycoolwallet.R;
import com.bethel.mycoolwallet.utils.Qr;

import butterknife.ButterKnife;
import butterknife.Unbinder;

/**
 * btc地址 二维码
 */
public  class WalletAddressDialogFragment extends DialogFragment {
    private static final String TAG = "WalletAddressDialogFragment";
    private Handler mHandler;

    public static void show(FragmentManager manager) {
        DialogFragment fragment = new WalletAddressDialogFragment();
        fragment.show(manager, TAG);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mHandler = new Handler();
    }

    // todo btc address.
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        final Dialog dialog = new Dialog(getActivity());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.wallet_address_dialog);
        dialog.setCanceledOnTouchOutside(true);

        final String addressStr = "15nsnvguw8439jovnv";
                // address.toString();
        final String addressUri = "test-btc-uri:15nsnvguw8439jovnv";

        final ImageView imageView = (ImageView) dialog.findViewById(R.id.wallet_address_dialog_image);
        showQrBmp(imageView, addressUri);

        final View labelButtonView = dialog.findViewById(R.id.wallet_address_dialog_label_button);
        final TextView labelView = (TextView) dialog.findViewById(R.id.wallet_address_dialog_label);
        final CharSequence label = "test lable";
//                WalletUtils.formatHash(addressStr, Constants.ADDRESS_FORMAT_GROUP_SIZE,
//                Constants.ADDRESS_FORMAT_LINE_SIZE);
        labelView.setText(label);
        labelButtonView.setVisibility(View.VISIBLE);
        labelButtonView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                final ShareCompat.IntentBuilder builder = ShareCompat.IntentBuilder.from(getActivity());
                builder.setType("text/plain");
                builder.setText(addressStr);
                builder.setChooserTitle(R.string.bitmap_fragment_share);
                builder.startChooser();
//                log.info("wallet address shared via intent: {}", addressStr);
            }
        });

        final View hintView = dialog.findViewById(R.id.wallet_address_dialog_hint);
        hintView.setVisibility(View.VISIBLE);
//                getResources().getBoolean(R.bool.show_wallet_address_dialog_hint) ? View.VISIBLE : View.GONE);

        final View dialogView = dialog.findViewById(R.id.wallet_address_dialog_group);
        dialogView.setOnClickListener((v)->  dismissAllowingStateLoss());

        return dialog;
    }

    private void showQrBmp(ImageView imageView, String addressUri) {
        new Thread(() -> {
            final BitmapDrawable bitmap = new BitmapDrawable(getResources(), Qr.bitmap(addressUri));
            bitmap.setFilterBitmap(false);
            mHandler.post(() -> {
                if (isAdded())
                imageView.setImageDrawable(bitmap);
            });
        }).start();
    }
}
