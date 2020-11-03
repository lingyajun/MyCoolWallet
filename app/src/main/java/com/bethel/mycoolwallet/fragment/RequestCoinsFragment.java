package com.bethel.mycoolwallet.fragment;


import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
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
import com.bethel.mycoolwallet.activity.RequestCoinsActivity;
import com.bethel.mycoolwallet.data.Event;
import com.bethel.mycoolwallet.mvvm.view_model.RequestCoinsViewModel;
import com.bethel.mycoolwallet.utils.Constants;
import com.bethel.mycoolwallet.utils.CurrencyTools;
import com.bethel.mycoolwallet.utils.Qr;
import com.bethel.mycoolwallet.view.CurrencyAmountView;
import com.bethel.mycoolwallet.view.CurrencyCalculatorLink;
import com.xuexiang.xui.widget.toast.XToast;

import org.bitcoinj.core.Address;
import org.bitcoinj.script.Script;
import org.bitcoinj.utils.MonetaryFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import butterknife.BindView;
import butterknife.OnClick;

/**
 * A simple {@link Fragment} subclass.
 */
public class RequestCoinsFragment extends BaseFragment {
    private ClipboardManager clipboardManager;

    private RequestCoinsViewModel viewModel;

    private static final String KEY_RECEIVE_ADDRESS = "receive_address";
    private static final Logger log = LoggerFactory.getLogger(RequestCoinsFragment.class);

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

    private CurrencyCalculatorLink amountCalculatorLink;

    @OnClick(R.id.request_coins_qr_card)
    public void onCardClick() {
//        WalletAddressDialogFragment.show(getFragmentManager());
//        XToast.info(getActivity(), " my request uri").show();
        Bitmap bitmap = viewModel.qrCode.getValue();
        if (null!=bitmap)
            viewModel.showBitmapDialog.setValue(new Event<>(bitmap));
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.clipboardManager = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
        setHasOptionsMenu(true);
        viewModel = getViewModel(RequestCoinsViewModel.class);

        final Intent intent = getActivity().getIntent();
        String scriptName = RequestCoinsActivity.INTENT_EXTRA_OUTPUT_SCRIPT_TYPE;
        if (intent.hasExtra(scriptName)) {
            Script.ScriptType outputScriptType =(Script.ScriptType) intent.getSerializableExtra(scriptName);
            viewModel.freshReceiveAddress.overrideOutputScriptType(outputScriptType);
        }

        if (savedInstanceState != null) {
            restoreInstanceState(savedInstanceState);
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModelObserves();

        qrCard.setCardBackgroundColor(Color.WHITE);
        qrCard.setPreventCornerOverlap(false);
        qrCard.setUseCompatPadding(false);
        qrCard.setMaxCardElevation(0); // we're using Lollipop elevation

        //  test ui; todo SharedPreferences 存储
        coinAmountView.setCurrencySymbol(MonetaryFormat.CODE_MBTC);
        coinAmountView.setInputFormat(CurrencyTools.getMaxPrecisionFormat(3));
        coinAmountView.setHintFormat(CurrencyTools.getFormat(3, 2));

        localAmountView.setInputFormat(Constants.LOCAL_FORMAT);
        localAmountView.setHintFormat(Constants.LOCAL_FORMAT);
        localAmountView.setCurrencySymbol("usd"); // exchangeRate.fiat.currencyCode

        amountCalculatorLink = new CurrencyCalculatorLink(coinAmountView, localAmountView);

        // todo amountCalculatorLink.setExchangeDirection(config.getLastExchangeDirection());
        amountCalculatorLink.setExchangeDirection(true);
        amountCalculatorLink.requestFocus();
    }

    @Override
    public void onResume() {
        super.onResume();
        amountCalculatorLink.setListener(new CurrencyAmountView.Listener() {
            @Override
            public void changed() {
                viewModel.amount.setValue(amountCalculatorLink.getAmount());
            }

            @Override
            public void focusChanged(final boolean hasFocus) {
                // focus linking
                final int activeAmountViewId = amountCalculatorLink.activeTextView().getId();
                bleCheckBox.setNextFocusUpId(activeAmountViewId);
            }
        });

        // todo  Start Bluetooth Listening
    }

    @Override
    public void onPause() {
        amountCalculatorLink.setListener(null);
        super.onPause();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        final Address receiveAddress = viewModel.freshReceiveAddress.getValue();
        if (null != receiveAddress) {
            outState.putString(KEY_RECEIVE_ADDRESS, receiveAddress.toString());
        }
    }

    private void restoreInstanceState(Bundle savedInstanceState) {
        if (savedInstanceState.containsKey(KEY_RECEIVE_ADDRESS)) {
            Address address = Address.fromString(Constants.NETWORK_PARAMETERS,
                    savedInstanceState.getString(KEY_RECEIVE_ADDRESS));
            viewModel.freshReceiveAddress.setValue(address);
        }
    }

    private void viewModelObserves() {
        viewModel.freshReceiveAddress.observe(this, address -> log.info("request coins address: {}", address));
        viewModel.qrCode.observe(this, bitmap -> showQrBmp(bitmap));
        viewModel.paymentRequest.observe(this, bytes -> {
            // todo test nfc
            SpannableStringBuilder initiateText = new SpannableStringBuilder(
                    getString(R.string.request_coins_fragment_initiate_request_qr));
            initiateText.append(' ').append(getString(R.string.request_coins_fragment_initiate_request_nfc));
            initNfcTv.setText(initiateText);
        });

        viewModel.bitcoinUri.observe(this, uri -> getActivity().invalidateOptionsMenu());
        viewModel.showBitmapDialog.observe(this, bitmapEvent ->
                        BitmapFragment.show(getFragmentManager(), bitmapEvent.getContentIfNotHandled()));


        // todo exchangeRate
    }

    private void showQrBmp(Bitmap bitmap) {
        if (isAdded()) {
            final BitmapDrawable qrDrawable = new BitmapDrawable(getResources(), bitmap);
            qrDrawable.setFilterBitmap(false);
            qrImg.setImageDrawable(qrDrawable);
        }
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.request_coins_fragment_options, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onPrepareOptionsMenu(final Menu menu) {
        final boolean hasBitcoinUri = viewModel.bitcoinUri.getValue() != null;
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
