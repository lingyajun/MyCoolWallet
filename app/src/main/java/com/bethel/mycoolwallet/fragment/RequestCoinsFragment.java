package com.bethel.mycoolwallet.fragment;


import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.nfc.NfcAdapter;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.app.ShareCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;

import android.text.SpannableStringBuilder;
import android.text.TextUtils;
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

import com.bethel.mycoolwallet.CoolApplication;
import com.bethel.mycoolwallet.R;
import com.bethel.mycoolwallet.activity.RequestCoinsActivity;
import com.bethel.mycoolwallet.activity.SendCoinsActivity;
import com.bethel.mycoolwallet.data.Event;
import com.bethel.mycoolwallet.helper.Configuration;
import com.bethel.mycoolwallet.mvvm.view_model.RequestCoinsViewModel;
import com.bethel.mycoolwallet.service.AcceptBluetoothService;
import com.bethel.mycoolwallet.utils.BluetoothTools;
import com.bethel.mycoolwallet.utils.Constants;
import com.bethel.mycoolwallet.utils.CurrencyTools;
import com.bethel.mycoolwallet.utils.NfcTools;
import com.bethel.mycoolwallet.utils.Qr;
import com.bethel.mycoolwallet.utils.ViewUtil;
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
 * 收款页.
 */
public class RequestCoinsFragment extends BaseFragment {
    private ClipboardManager clipboardManager;

    private RequestCoinsViewModel viewModel;

    private NfcAdapter nfcAdapter;
    private BluetoothAdapter bluetoothAdapter;
    private Configuration mConfig;

    private static final int REQUEST_CODE_ENABLE_BLUETOOTH = 0;
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
        mConfig = CoolApplication.getApplication().getConfiguration();

        final Intent intent = getActivity().getIntent();
        String scriptName = RequestCoinsActivity.INTENT_EXTRA_OUTPUT_SCRIPT_TYPE;
        if (intent.hasExtra(scriptName)) {
            Script.ScriptType outputScriptType =(Script.ScriptType) intent.getSerializableExtra(scriptName);
            viewModel.freshReceiveAddress.overrideOutputScriptType(outputScriptType);
        }

        if (savedInstanceState != null) {
            restoreInstanceState(savedInstanceState);
        }

        nfcAdapter = NfcAdapter.getDefaultAdapter(getContext());
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModelObserves();

        qrCard.setCardBackgroundColor(Color.WHITE);
        qrCard.setPreventCornerOverlap(false);
        qrCard.setUseCompatPadding(false);
        qrCard.setMaxCardElevation(0); // we're using Lollipop elevation

        //  ui;  SharedPreferences 存储
        coinAmountView.setCurrencySymbol(mConfig.getFormat().code());
        coinAmountView.setInputFormat(mConfig.getMaxPrecisionFormat());
        coinAmountView.setHintFormat(mConfig.getFormat());

        localAmountView.setInputFormat(Constants.LOCAL_FORMAT);
        localAmountView.setHintFormat(Constants.LOCAL_FORMAT);

        amountCalculatorLink = new CurrencyCalculatorLink(coinAmountView, localAmountView);
        amountCalculatorLink.setExchangeDirection(mConfig.getLastExchangeDirection());
        amountCalculatorLink.setExchangeDirection(true);
        amountCalculatorLink.requestFocus();

        final boolean acceptBluetooth = null!=bluetoothAdapter
                && !TextUtils.isEmpty(BluetoothTools.getAddress(bluetoothAdapter));
        ViewUtil.showView(bleCheckBox, acceptBluetooth);
        bleCheckBox.setChecked(null!=bluetoothAdapter && bluetoothAdapter.isEnabled());
        bleCheckBox.setOnCheckedChangeListener((compoundButton, b) -> {
            if (null!=bluetoothAdapter && b) {
                if (bluetoothAdapter.isEnabled()) {
                    maybeStartBluetoothListening();
                } else {
                    // ask for permission to enable bluetooth
                    startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE),
                            REQUEST_CODE_ENABLE_BLUETOOTH);
                }
            }else {
                stopBluetoothListening();
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == REQUEST_CODE_ENABLE_BLUETOOTH) {
            boolean started = false;
            if (resultCode == Activity.RESULT_OK && bluetoothAdapter != null) {
                started = maybeStartBluetoothListening();
            }
            bleCheckBox.setChecked(started);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private boolean maybeStartBluetoothListening() {
        final String address = BluetoothTools.getAddress(bluetoothAdapter);
        if (isAdded() && bleCheckBox.isChecked() && !TextUtils.isEmpty(address)) {
            // AcceptBluetoothService
            final Activity activity = getActivity();
            Intent intent = new Intent(activity, AcceptBluetoothService.class);
            activity.startService(intent);

            viewModel.bluetoothServiceIntent  = intent;
            final String mac = BluetoothTools.compressMac(address);
            viewModel.bluetoothMac.setValue(mac);

            log.debug("Bluetooth: {} , {}", address, mac);
        }
        return false;
    }

    private void stopBluetoothListening() {
        if (viewModel.bluetoothServiceIntent != null) {
            getActivity().stopService(viewModel.bluetoothServiceIntent);
            viewModel.bluetoothServiceIntent = null;
        }
        viewModel.bluetoothMac.setValue(null);
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

        //   Start Bluetooth Listening
        if (null!=bluetoothAdapter && bluetoothAdapter.isEnabled() && bleCheckBox.isChecked()) {
            maybeStartBluetoothListening();
        }
    }

    @Override
    public void onPause() {
        amountCalculatorLink.setListener(null);
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        mConfig.setLastExchangeDirection(amountCalculatorLink.getExchangeDirection());
        super.onDestroyView();
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
            final NfcAdapter nfcAdapter = RequestCoinsFragment.this.nfcAdapter;

            SpannableStringBuilder initiateText = new SpannableStringBuilder(
                    getString(R.string.request_coins_fragment_initiate_request_qr));
            if (null!=nfcAdapter && nfcAdapter.isEnabled()) {
                initiateText.append(' ').append(getString(R.string.request_coins_fragment_initiate_request_nfc));

                nfcAdapter.setNdefPushMessage(NfcTools.createNdefMessage(bytes), getActivity());
            }
            initNfcTv.setText(initiateText);
        });

        viewModel.bitcoinUri.observe(this, uri -> getActivity().invalidateOptionsMenu());
        viewModel.showBitmapDialog.observe(this, bitmapEvent ->
                        BitmapFragment.show(getFragmentManager(), bitmapEvent.getContentIfNotHandled()));

        if (Constants.ENABLE_EXCHANGE_RATES) {
            viewModel.exchangeRate.observe(this,
                    bean -> amountCalculatorLink.setExchangeRate(bean.rate));
        }

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
        //  btc uri
//        final Uri request = Uri.parse("bitcoin:1aasffdvdjsvnjksdnvkjnvkj");
//        clipboardManager.setPrimaryClip(ClipData.newPlainText("btc address", "btc123456uuss"));
        final Uri request = viewModel.bitcoinUri.getValue();
        clipboardManager.setPrimaryClip(ClipData.newRawUri("Bitcoin payment request", request));
        XToast.success(getContext(), R.string.request_coins_clipboard_msg).show();
    }

    private void handleShare() {
        //  btc uri
//        final Uri request = Uri.parse("bitcoin:1aasffdvdjsvnjksdnvkjnvkj");
        final Uri request = viewModel.bitcoinUri.getValue();
        final ShareCompat.IntentBuilder builder = ShareCompat.IntentBuilder.from(getActivity());
        builder.setType("text/plain");
        builder.setText(request.toString());
        builder.setChooserTitle(R.string.request_coins_share_dialog_title);
        builder.startChooser();
    }

    private void handleLocalApp() {
        Context context = getContext();
        ComponentName componentName = new ComponentName(context, SendCoinsActivity.class);
        PackageManager manager = context.getPackageManager();
        Uri btcUri = viewModel.bitcoinUri.getValue();
        Intent intent = new Intent(Intent.ACTION_VIEW, btcUri);

        try {
            manager.setComponentEnabledSetting(componentName,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            XToast.error(context, R.string.request_coins_no_local_app_msg).show();
        } finally {
            manager.setComponentEnabledSetting(componentName,
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
        }
    }

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_request_coins;
    }

}
