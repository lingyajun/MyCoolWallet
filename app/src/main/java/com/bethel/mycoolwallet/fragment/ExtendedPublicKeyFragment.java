package com.bethel.mycoolwallet.fragment;

import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ShareCompat;
//import androidx.fragment.app.DialogFragment;
//import androidx.fragment.app.FragmentManager;

import com.bethel.mycoolwallet.R;
import com.bethel.mycoolwallet.helper.CoolThreadPool;
import com.bethel.mycoolwallet.utils.Qr;
import com.xuexiang.xui.widget.dialog.materialdialog.DialogAction;
import com.xuexiang.xui.widget.dialog.materialdialog.MaterialDialog;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExtendedPublicKeyFragment extends DialogFragment {
    private static final String TAG = "ExtendedPublicKeyFragme";
    private static final String KEY_EXTENDED_PUBLIC_KEY = "extended_public_key";

    private static final Logger log = LoggerFactory.getLogger(ExtendedPublicKeyFragment.class);

    public static void show(final FragmentManager fm, final CharSequence base58) {
        DialogFragment fragment = instance(base58);
        fragment.show(fm, TAG);
    }

    private static ExtendedPublicKeyFragment instance(final CharSequence base58) {
        final ExtendedPublicKeyFragment fragment = new ExtendedPublicKeyFragment();

        final Bundle args = new Bundle();
        args.putCharSequence(KEY_EXTENDED_PUBLIC_KEY, base58);
        fragment.setArguments(args);

        return fragment;
    }

//    @BindView(R.id.extended_public_key_dialog_image)
    ImageView imageView;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        final String base58 = getArguments().getCharSequence(KEY_EXTENDED_PUBLIC_KEY).toString();
        View view = LayoutInflater.from(getActivity()).inflate(R.layout.extended_public_key_dialog, null);
        imageView = view.findViewById(R.id.extended_public_key_dialog_image);

        MaterialDialog materialDialog = new MaterialDialog.Builder(getActivity())
                .customView(view, true)
                .canceledOnTouchOutside(true)
                .negativeText(R.string.button_dismiss)
//      java.lang.IllegalStateException: You can not set Dialog's OnCancelListener or OnDismissListener
//                .cancelListener((dialogInterface)->dismissAllowingStateLoss())
                .positiveText(R.string.button_share)
                .show();

        materialDialog.getActionButton(DialogAction.NEGATIVE)
                .setOnClickListener(view1 -> dismissAllowingStateLoss());
        materialDialog.getActionButton(DialogAction.POSITIVE)
                .setOnClickListener(view1 -> sharePubKey(base58));

        showQrImage(base58);

        return materialDialog;
    }

    private void showQrImage(String base58) {
//        AsyncTask.execute(()->{
        CoolThreadPool.execute(()->{
            final BitmapDrawable bitmap = new BitmapDrawable(getResources(), Qr.bitmap(base58));
            bitmap.setFilterBitmap(false);
            imageView.post(() -> {
                if (isAdded()) imageView.setImageDrawable(bitmap);
            });
        });
    }

    private void sharePubKey(String base58) {
        final ShareCompat.IntentBuilder builder = ShareCompat.IntentBuilder.from(getActivity());
        builder.setType("text/plain");
        builder.setText(base58);
        builder.setSubject(getString(R.string.extended_public_key_fragment_title));
        builder.setChooserTitle(R.string.extended_public_key_fragment_share);
        builder.startChooser();
        log.info("extended public key shared via intent: {}", base58);
    }
}
