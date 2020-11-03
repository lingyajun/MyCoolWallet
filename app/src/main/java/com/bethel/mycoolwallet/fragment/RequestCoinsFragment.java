package com.bethel.mycoolwallet.fragment;


import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.app.ShareCompat;
import androidx.fragment.app.Fragment;

import android.text.SpannableStringBuilder;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.bethel.mycoolwallet.R;
import com.bethel.mycoolwallet.utils.Constants;
import com.bethel.mycoolwallet.utils.CurrencyTools;
import com.bethel.mycoolwallet.utils.Qr;
import com.bethel.mycoolwallet.view.CurrencyAmountView;
import com.xuexiang.xui.widget.toast.XToast;

import org.bitcoinj.utils.MonetaryFormat;

import butterknife.BindView;
import butterknife.OnClick;

/**
 * A simple {@link Fragment} subclass.
 */
public class RequestCoinsFragment extends BaseFragment {
    private ClipboardManager clipboardManager;

    @BindView(R.id.request_coins_amount_btc)
    CurrencyAmountView coinAmountView;
    @BindView(R.id.request_coins_amount_local)
    CurrencyAmountView localAmountView;

    @BindView(R.id.request_coins_amount_btc_edittext)
    EditText coinEt;
    @BindView(R.id.request_coins_amount_local_edittext)
    EditText localEt;

    @BindView(R.id.request_coins_accept_bluetooth_payment)
    CheckBox bleCheckBox;

    @BindView(R.id.request_coins_qr_card)
    CardView qrCard;
    @BindView(R.id.request_coins_qr)
    ImageView qrImg;

    @BindView(R.id.request_coins_fragment_initiate_request)
    TextView initNfcTv;


    @OnClick(R.id.request_coins_qr_card)
    public void onCardClick() {
//    todo    WalletAddressDialogFragment.show(getFragmentManager());
        XToast.info(getActivity(), " my request uri").show();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.clipboardManager = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
        setHasOptionsMenu(true);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        qrCard.setCardBackgroundColor(Color.WHITE);
        qrCard.setPreventCornerOverlap(false);
        qrCard.setUseCompatPadding(false);
        qrCard.setMaxCardElevation(0); // we're using Lollipop elevation

        // test ui
        coinAmountView.setCurrencySymbol(MonetaryFormat.CODE_MBTC);
        coinAmountView.setInputFormat(CurrencyTools.getMaxPrecisionFormat(3));
        coinAmountView.setHintFormat(CurrencyTools.getFormat(3, 2));

        localAmountView.setInputFormat(Constants.LOCAL_FORMAT);
        localAmountView.setHintFormat(Constants.LOCAL_FORMAT);
        localAmountView.setCurrencySymbol("usd"); // exchangeRate.fiat.currencyCode

        // test qr
        showQrBmp("test-1hbvsffgukllllllkjjhvsk-8743799");

        // test nfc
        SpannableStringBuilder initiateText = new SpannableStringBuilder(
                getString(R.string.request_coins_fragment_initiate_request_qr));
        initiateText.append(' ').append(getString(R.string.request_coins_fragment_initiate_request_nfc));
        initNfcTv.setText(initiateText);
    }

    private void showQrBmp(final String content) {
        new Thread(() -> {
            final Bitmap bitmap = Qr.bitmap(content);
            runOnUIthread( () -> {
                        if (isAdded()) {
                            qrImg.setImageBitmap(bitmap);
                        }
                    }
            );
        }).start();
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.request_coins_fragment_options, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onPrepareOptionsMenu(final Menu menu) {
        // todo
        final boolean hasBitcoinUri = true;
        menu.findItem(R.id.request_coins_options_copy).setEnabled(hasBitcoinUri);
        menu.findItem(R.id.request_coins_options_share).setEnabled(hasBitcoinUri);
        menu.findItem(R.id.request_coins_options_local_app).setEnabled(hasBitcoinUri);
        super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        switch (itemId) {
            case R.id.request_coins_options_copy:
                handleCopy();
                break;
            case R.id.request_coins_options_share:
                handleShare();
                break;
            case R.id.request_coins_options_local_app:
                handleLocalApp();
                break;
                default: return super.onOptionsItemSelected(item);
        }
        return true;
    }

    private void handleCopy() {
        // todo btc uri
        final Uri request = Uri.parse("bitcoin:1aasffdvdjsvnjksdnvkjnvkj");
        clipboardManager.setPrimaryClip(ClipData.newPlainText("btc address", "btc123456uuss"));
//        final Uri request = viewModel.bitcoinUri.getValue();
//        clipboardManager.setPrimaryClip(ClipData.newRawUri("Bitcoin payment request", request));
        XToast.success(getContext(), R.string.request_coins_clipboard_msg).show();
    }

    private void handleShare() {
        // todo btc uri
        final Uri request = Uri.parse("bitcoin:1aasffdvdjsvnjksdnvkjnvkj");
//        final Uri request = viewModel.bitcoinUri.getValue();
        final ShareCompat.IntentBuilder builder = ShareCompat.IntentBuilder.from(getActivity());
        builder.setType("text/plain");
        builder.setText(request.toString());
        builder.setChooserTitle(R.string.request_coins_share_dialog_title);
        builder.startChooser();
    }

    private void handleLocalApp() {
        XToast.warning(getContext(), "handleLocalApp-ComponentName-SendCoinsActivity").show();
    }

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_request_coins;
    }

}
