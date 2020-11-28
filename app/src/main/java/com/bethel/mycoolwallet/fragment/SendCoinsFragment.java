package com.bethel.mycoolwallet.fragment;


import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import com.bethel.mycoolwallet.CoolApplication;
import com.bethel.mycoolwallet.R;
import com.bethel.mycoolwallet.activity.CustomCaptureActivity;
import com.bethel.mycoolwallet.activity.WebActivity;
import com.bethel.mycoolwallet.data.ExchangeRateBean;
import com.bethel.mycoolwallet.data.payment.PaymentData;
import com.bethel.mycoolwallet.helper.PaymentHelper;
import com.bethel.mycoolwallet.helper.parser.IntentDataParser;
import com.bethel.mycoolwallet.helper.parser.StringInputParser;
import com.bethel.mycoolwallet.interfaces.IDeriveKeyCallBack;
import com.bethel.mycoolwallet.interfaces.IQrScan;
import com.bethel.mycoolwallet.interfaces.ISignPaymentCallback;
import com.bethel.mycoolwallet.mvvm.view_model.SendCoinsViewModel;
import com.bethel.mycoolwallet.request.payment.AbsPaymentRequestTask;
import com.bethel.mycoolwallet.request.payment.BluetoothPaymentRequestTask;
import com.bethel.mycoolwallet.request.payment.DeriveKeyTask;
import com.bethel.mycoolwallet.request.payment.HttpPaymentRequestTask;
import com.bethel.mycoolwallet.request.payment.IPaymentRequestListener;
import com.bethel.mycoolwallet.service.BlockChainService;
import com.bethel.mycoolwallet.utils.BluetoothTools;
import com.bethel.mycoolwallet.utils.Constants;
import com.bethel.mycoolwallet.utils.CurrencyTools;
import com.bethel.mycoolwallet.utils.WalletUtils;
import com.bethel.mycoolwallet.view.CurrencyAmountView;
import com.bethel.mycoolwallet.view.FeeSeekBar;
import com.xuexiang.xqrcode.XQRCode;
import com.xuexiang.xui.widget.toast.XToast;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionConfidence;
import org.bitcoinj.core.VerificationException;
import org.bitcoinj.utils.MonetaryFormat;
import org.bitcoinj.wallet.SendRequest;
import org.bitcoinj.wallet.Wallet;
import org.bouncycastle.crypto.params.KeyParameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.InputStream;

import butterknife.BindView;
import butterknife.OnClick;

/**
 * 支付页面.
 *
 * todo
 * 构造支付数据；
 * 解析扫码结果；
 * 金额输入框联动；
 * 输入地址/标签响应；
 *
 * -----
 * 获取 activity 的 PaymentData 数据
 * 将这数据 初始化,同步到ui
 *
 * 签名，发送交易数据
 */
public class SendCoinsFragment extends BaseFragment implements IQrScan {
    /**
     * 扫描跳转Activity RequestCode
     */
    private static final int REQUEST_QR_SCAN_CODE = 111;
    private static final Logger log = LoggerFactory.getLogger(SendCoinsFragment.class);

    private SendCoinsViewModel viewModel;
    private ContentResolver contentResolver;
    private BluetoothAdapter bluetoothAdapter;
    private CoolApplication application;

    @BindView(R.id.send_coins_payee_group)
     View payeeGroup;
    @BindView(R.id.send_coins_payee_name)
     TextView payeeNameView;
    @BindView(R.id.send_coins_payee_verified_by)
     TextView payeeVerifiedByView;
    @BindView(R.id.send_coins_receiving_address)
     AutoCompleteTextView receivingAddressView;
    @BindView(R.id.send_coins_receiving_static)
     View receivingStaticView;
    @BindView(R.id.send_coins_receiving_static_address)
     TextView receivingStaticAddressView;
    @BindView(R.id.send_coins_receiving_static_label)
     TextView receivingStaticLabelView;
    @BindView(R.id.send_coins_amount_group)
     View amountGroup;

    @BindView(R.id.send_coins_amount_btc)
    CurrencyAmountView coinAmountView;
    @BindView(R.id.send_coins_amount_local)
    CurrencyAmountView localAmountView;

    @BindView(R.id.send_coins_direct_payment_enable)
     CheckBox directPaymentEnableView;
    @BindView(R.id.send_coins_hint)
     TextView hintView;
    @BindView(R.id.send_coins_direct_payment_message)
     TextView directPaymentMessageView;
    @BindView(R.id.transaction_row)
     ViewGroup sentTransactionViewGroup;

    @BindView(R.id.send_coins_private_key_password_group)
     View privateKeyPasswordViewGroup;
    @BindView(R.id.send_coins_private_key_password)
     EditText privateKeyPasswordView;
    @BindView(R.id.send_coins_private_key_bad_password)
     View privateKeyBadPasswordView;

    @BindView(R.id.send_coins_go)
     Button viewGo;
    @BindView(R.id.send_coins_cancel)
     Button viewCancel;

    @BindView(R.id.send_coins_fee_seek_bar)
    FeeSeekBar feeSeekBar;

    @OnClick(R.id.send_coins_go)
    void onSendClick() {
        privateKeyBadPasswordView.setVisibility(View.INVISIBLE);
        final Wallet wallet = viewModel.wallet.getValue();
        if (null!=wallet && wallet.isEncrypted()) {
            new DeriveKeyTask(wallet, privateKeyPasswordView.getText().toString().trim(), application.scryptIterationsTarget()) {
                @Override
                protected void onSuccess(KeyParameter encryptionKey, boolean changed) {
                    signAndSendPayment(encryptionKey);

                    if (changed) {
                        SendCoinsFragment.this.executeAsyncTask(() ->
                                WalletUtils.autoBackupWallet(getContext(), wallet));
                    }
                }
            }.executeAsyncTask();
            // todo   setState(SendCoinsViewModel.State.DECRYPTING);
            return;
        }
        signAndSendPayment(null);
    }

    private void signAndSendPayment(KeyParameter encryptionKey) {
//    todo    setState(SendCoinsViewModel.State.SIGNING);
    }

    private final TransactionConfidence.Listener transactionListener = new TransactionConfidence.Listener() {
        @Override
        public void onConfidenceChanged(TransactionConfidence confidence, ChangeReason reason) {
            log.info("TransactionConfidence.Listener: {} , {}", reason, confidence.toString());
            //   监听发送交易的状态/事件
            runOnUiThread(()->{
                if (!isResumed()) return;
                // todo update UI
                TransactionConfidence.ConfidenceType confidenceType = confidence.getConfidenceType();
                final int numPeers = confidence.numBroadcastPeers();
                switch (confidenceType) {
                    case PENDING:
                        if (reason == ChangeReason.SEEN_PEERS) {
                            // play sound
                            final String sound = String.format("send_coins_broadcast_%d", numPeers);
                            final int soundId = getResources().getIdentifier(sound, "raw", getContext().getPackageName());
                            if (soundId > 0) {
                                String uriString = String.format("android.resource://%s/", getContext().getPackageName(), soundId);
                                RingtoneManager.getRingtone(getContext(), Uri.parse(uriString)).play();
                            }
                        }
                        break;
                }
            });
        }
    };

    @OnClick(R.id.send_coins_cancel)
    protected void onCancelClick() {
        feeSeekBar.setDefaultValue(2020);
        XToast.warning(getContext(), "cancel !").show();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        viewModel = getViewModel(SendCoinsViewModel.class);
        contentResolver = getContext().getContentResolver();
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        application = CoolApplication.getApplication();

        if (null == savedInstanceState) {
            handleIntentData();
        }
    }

    private void handleIntentData() {
        Intent intent = getActivity().getIntent();
        log.debug("Intent  {}", intent);
        if (null == intent) {
            return;
        }

        new IntentDataParser(intent){
            @Override
            public void error(int messageResId, Object... messageArgs) {
                log.error("IntentDataParser {}", getString(messageResId, messageArgs));
            }

            @Override
            public void handlePaymentData(PaymentData data) {
                log.debug("handlePaymentData: {}", data);
                if (isAdded()) {
                    updateStateFrom(data);
                }
            }

            @Override
            protected InputStream openInputStream(Uri bitcoinUri) {
                try {
                    return contentResolver.openInputStream(bitcoinUri);
                } catch (FileNotFoundException e) {
                    throw new RuntimeException(e);
                }
            }
        }.parse();
    }

    private void updateStateFrom(PaymentData data) {
        if (null == data) return;
        // todo
        viewModel.paymentData = data;

        // delay these actions until fragment is resumed
        runOnUiThread(() -> {
            if (data.hasPaymentRequestUrl() && data.isBluetoothPaymentRequestUrl()) {
                // bluetooth
                if (bluetoothAdapter.isEnabled()) {
                    handlePaymentRequest();
                    return;
                }
                // todo  ask for permission to enable bluetooth
                return;
            }

            if (data.hasPaymentRequestUrl() && data.isHttpPaymentRequestUrl()) {
                // http
                handlePaymentRequest();
                return;
            }

            // ui todo
        });
    }

    /** send request
     * todo requestListener
     */
    private void handlePaymentRequest() {
        final String paymentRequestUrl = viewModel.paymentData.paymentRequestUrl;
        final String paymentRequestHost ;
        if (!BluetoothTools.isBluetoothUrl(paymentRequestUrl)) {
            paymentRequestHost = Uri.parse(paymentRequestUrl).getHost();
        } else {
            // Bluetooth
            final String mac = BluetoothTools.getBluetoothMac(paymentRequestUrl);
            paymentRequestHost = BluetoothTools.decompressMac(mac);
        }

        final IPaymentRequestListener requestListener = new IPaymentRequestListener() {
            @Override
            public void onPaymentData(PaymentData data) {
                log.info("PaymentRequest {}", data);
                if (!isAdded()) return;

                // todo
            }

            @Override
            public void onFail(int messageResId, Object... messageArgs) {
                log.error("PaymentRequest, fail: {}", getString(messageResId, messageArgs));
                if (!isAdded()) return;

                // todo
            }
        };

       final AbsPaymentRequestTask paymentTask;
        if (!BluetoothTools.isBluetoothUrl(paymentRequestUrl)) {
            paymentTask = new HttpPaymentRequestTask(CoolApplication.getApplication().httpUserAgent(), paymentRequestUrl, requestListener);
        } else {
            // Bluetooth
            paymentTask = new BluetoothPaymentRequestTask(bluetoothAdapter, paymentRequestUrl, requestListener);
        }

        paymentTask.executeAsyncTask();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // test ui
        coinAmountView.setCurrencySymbol(MonetaryFormat.CODE_MBTC);
        coinAmountView.setInputFormat(CurrencyTools.getMaxPrecisionFormat(3));
        coinAmountView.setHintFormat(CurrencyTools.getFormat(3, 2));

        localAmountView.setInputFormat(Constants.LOCAL_FORMAT);
        localAmountView.setHintFormat(Constants.LOCAL_FORMAT);
        localAmountView.setCurrencySymbol("usd"); // exchangeRate.fiat.currencyCode

        viewCancel.setText(R.string.button_cancel);
        viewGo.setText(R.string.send_coins_fragment_button_send);

        feeSeekBar.setDefaultValue(2000);
        log.info( "onViewCreated, feeSeekBar: "+ feeSeekBar.getSelectedNumber());
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.send_coins_fragment_options, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onPrepareOptionsMenu(final Menu menu) {
        final MenuItem scanAction = menu.findItem(R.id.send_coins_options_scan);
        final PackageManager pm = getActivity().getPackageManager();
        scanAction.setVisible(pm.hasSystemFeature(PackageManager.FEATURE_CAMERA)
                || pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT));
        // todo
//        scanAction.setEnabled(viewModel.state == SendCoinsViewModel.State.INPUT);
        scanAction.setEnabled(true);

        final MenuItem emptyAction = menu.findItem(R.id.send_coins_options_empty);
//        emptyAction.setEnabled(viewModel.state == SendCoinsViewModel.State.INPUT
//                && viewModel.paymentIntent.mayEditAmount() && viewModel.balance.getValue() != null);

        super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        switch (itemId) {
            case R.id.send_coins_options_scan:
                    startScan(null);
                break;
//            case R.id.send_coins_options_fee_category_economic:
//                break;
//            case R.id.send_coins_options_fee_category_normal:
//                break;
//            case R.id.send_coins_options_fee_category_priority:
//                break;
            case R.id.send_coins_options_empty:
                break;
                default:    return super.onOptionsItemSelected(item);
        }
        return true;
    }

    @Override
    public void startScan(View v) {
        CustomCaptureActivity.start(this, REQUEST_QR_SCAN_CODE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // 处理二维码扫描结果
        if (requestCode == REQUEST_QR_SCAN_CODE && resultCode == Activity.RESULT_OK) {
            handleScanResult(data);
        }
    }

    /**
     * 处理二维码扫描结果
     *
     * @param data
     */
    private void handleScanResult(Intent data) {
        if (data != null) {
            Bundle bundle = data.getExtras();
            if (bundle != null) {
                if (bundle.getInt(XQRCode.RESULT_TYPE) == XQRCode.RESULT_SUCCESS) {
                    String result = bundle.getString(XQRCode.RESULT_DATA);
                    // todo handle bitcoin pay
                    StringInputParser parser = new StringInputParser(result) {
                        @Override
                        protected void handleWebUrl(String link) {
                            WebActivity.start(getContext(), link);
                        }

                        @Override
                        protected void requestBIP38PrivateKeyPassphrase() {
                            // todo
                            responseBIP38PrivateKeyPassphrase("todo");
                        }

                        @Override
                        public void handleDirectTransaction(Transaction transaction) throws VerificationException {

                        }

                        @Override
                        public void error(int messageResId, Object... messageArgs) {

                        }

                        @Override
                        public void handlePaymentData(PaymentData data) {

                        }
                    };
                    parser.parse();
//                    XToast.success(getContext(), "解析结果: " + result).show();
                } else if (bundle.getInt(XQRCode.RESULT_TYPE) == XQRCode.RESULT_FAILED) {
                    XToast.error(getContext(), R.string.parse_qr_code_failed).show();
                }
            }
        }
    }

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_send_coins;
    }

}
