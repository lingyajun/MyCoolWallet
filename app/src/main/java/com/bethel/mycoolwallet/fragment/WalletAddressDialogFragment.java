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
import com.bethel.mycoolwallet.utils.Constants;
import com.bethel.mycoolwallet.utils.Qr;
import com.bethel.mycoolwallet.utils.WalletUtils;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.LegacyAddress;
import org.bitcoinj.uri.BitcoinURI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;

import butterknife.ButterKnife;
import butterknife.Unbinder;

/**
 * btc地址 二维码
 */
public  class WalletAddressDialogFragment extends DialogFragment {
    private static final String TAG = "WalletAddressDialogFragment";
    private static final String KEY_ADDRESS = "address";
    private static final String KEY_ADDRESS_LABEL = "address_label";
    private Handler mHandler;

    private static final Logger log = LoggerFactory.getLogger(WalletAddressDialogFragment.class);

    public static void show(final FragmentManager fm, final Address address, @Nullable final String addressLabel) {
        instance(address, addressLabel).show(fm, TAG);
    }

    private static WalletAddressDialogFragment instance(final Address address, @Nullable final String addressLabel) {
        final WalletAddressDialogFragment fragment = new WalletAddressDialogFragment();

        final Bundle args = new Bundle();
        args.putString(KEY_ADDRESS, address.toString());
        if (addressLabel != null)
            args.putString(KEY_ADDRESS_LABEL, addressLabel);
        fragment.setArguments(args);

        return fragment;
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
        final Bundle args = getArguments();
        final Address address = Address.fromString(Constants.NETWORK_PARAMETERS, args.getString(KEY_ADDRESS));
        final String addressStr = address.toString();
        final String addressLabel = args.getString(KEY_ADDRESS_LABEL);

        final Dialog dialog = new Dialog(getActivity());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.wallet_address_dialog);
        dialog.setCanceledOnTouchOutside(true);

        final String addressUri;
        if (address instanceof LegacyAddress || addressLabel != null)
            addressUri = BitcoinURI.convertToBitcoinURI(address, null, addressLabel, null);
        else
            addressUri = address.toString().toUpperCase(Locale.US);

        final ImageView imageView = dialog.findViewById(R.id.wallet_address_dialog_image);
        showQrBmp(imageView, addressUri);

        final View labelButtonView = dialog.findViewById(R.id.wallet_address_dialog_label_button);
        final TextView labelView =  dialog.findViewById(R.id.wallet_address_dialog_label);
        final CharSequence label = WalletUtils.formatHash(addressStr,
                        Constants.ADDRESS_FORMAT_GROUP_SIZE, Constants.ADDRESS_FORMAT_LINE_SIZE);
        labelView.setText(label);
        labelButtonView.setVisibility(View.VISIBLE);
        labelButtonView.setOnClickListener(( v)-> {
                final ShareCompat.IntentBuilder builder = ShareCompat.IntentBuilder.from(getActivity());
                builder.setType("text/plain");
                builder.setText(addressStr);
                builder.setChooserTitle(R.string.bitmap_fragment_share);
                builder.startChooser();
                log.info("wallet address shared via intent: {}", addressStr);
        });

        final View hintView = dialog.findViewById(R.id.wallet_address_dialog_hint);
        int visible = getResources().getBoolean(R.bool.show_wallet_address_dialog_hint) ? View.VISIBLE : View.GONE;
        hintView.setVisibility(visible);

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
