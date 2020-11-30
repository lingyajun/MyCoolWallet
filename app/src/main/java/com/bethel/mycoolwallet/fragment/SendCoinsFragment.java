package com.bethel.mycoolwallet.fragment;


import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import android.os.Handler;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
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
import com.bethel.mycoolwallet.data.AddressBean;
import com.bethel.mycoolwallet.data.payment.PaymentData;
import com.bethel.mycoolwallet.data.payment.PaymentUtil;
import com.bethel.mycoolwallet.db.AddressBookDao;
import com.bethel.mycoolwallet.db.AppDatabase;
import com.bethel.mycoolwallet.helper.Configuration;
import com.bethel.mycoolwallet.helper.SendCoinsHelper;
import com.bethel.mycoolwallet.helper.parser.IntentDataParser;
import com.bethel.mycoolwallet.helper.parser.StringInputParser;
import com.bethel.mycoolwallet.interfaces.IQrScan;
import com.bethel.mycoolwallet.interfaces.ITask;
import com.bethel.mycoolwallet.mvvm.view_model.SendCoinsViewModel;
import com.bethel.mycoolwallet.request.payment.DeriveKeyTask;
import com.bethel.mycoolwallet.request.payment.IPaymentRequestListener;
import com.bethel.mycoolwallet.utils.Constants;
import com.bethel.mycoolwallet.utils.CurrencyTools;
import com.bethel.mycoolwallet.utils.ViewUtil;
import com.bethel.mycoolwallet.utils.WalletUtils;
import com.bethel.mycoolwallet.view.CurrencyAmountView;
import com.bethel.mycoolwallet.view.CurrencyCalculatorLink;
import com.bethel.mycoolwallet.view.FeeSeekBar;
import com.google.common.base.Joiner;
import com.xuexiang.xqrcode.XQRCode;
import com.xuexiang.xui.widget.dialog.materialdialog.DialogAction;
import com.xuexiang.xui.widget.dialog.materialdialog.MaterialDialog;
import com.xuexiang.xui.widget.toast.XToast;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.InsufficientMoneyException;
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
import java.util.LinkedList;
import java.util.List;

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
 * ------------------------------
 * ------------------------------
 * 入口： 1. onCreate(), 解析Intent得到 PaymentData 然后更新到UI；
 *       2. 二维码扫描，解析扫描结果，并响应
 *
 * Menu： 响应响应的操作
 *
 * 支付操作： onSendClick()，获取UI输入，得出PaymentData，然后签名数据，发送出去
 *
 */
public class SendCoinsFragment extends BaseFragment implements IQrScan {
    /**
     * 扫描跳转Activity RequestCode
     */
    private static final int REQUEST_QR_SCAN_CODE = 111;
    /**
     * bluetooth
     */
    private static final int REQUEST_CODE_ENABLE_BLUETOOTH_FOR_PAYMENT_REQUEST = 122;
    private static final int REQUEST_CODE_ENABLE_BLUETOOTH_FOR_DIRECT_PAYMENT = 133;

    private static final Logger log = LoggerFactory.getLogger(SendCoinsFragment.class);

    private SendCoinsViewModel viewModel;
    private ContentResolver contentResolver;
    private BluetoothAdapter bluetoothAdapter;
    private CoolApplication application;
    private CurrencyCalculatorLink amountCalculatorLink;
    private Configuration mConfig;
    private final Handler mHandler = new Handler();
    private AddressBookDao addressBookDao;


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
        validateReceivingAddress();

        if (everythingPlausible()) {
            sendPay();
        } else {
            requestFocusFirst();
        }
        updateView();
    }

    private void sendPay() {
        ViewUtil.setVisibility(privateKeyBadPasswordView, View.INVISIBLE);

        final Wallet wallet = viewModel.wallet.getValue();
        final String password = privateKeyPasswordView.getText().toString().trim();
        if (null!=wallet && wallet.isEncrypted()) {
            new DeriveKeyTask(wallet, password, application.scryptIterationsTarget()) {
                @Override
                protected void onSuccess(KeyParameter encryptionKey, boolean changed) {
                    signAndSendPayment(encryptionKey);

                    if (changed) {
                        SendCoinsFragment.this.executeAsyncTask(() ->
                                WalletUtils.autoBackupWallet(getContext(), wallet));
                    }
                }
            }.executeAsyncTask();

            setState(SendCoinsViewModel.State.DECRYPTING);
            return;
        }
        signAndSendPayment(null);
    }

    private void signAndSendPayment(KeyParameter encryptionKey) {
//    todo    setState(SendCoinsViewModel.State.SIGNING);
    }

    @OnClick(R.id.send_coins_cancel)
    protected void onCancelClick() {
        handleCancel();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        viewModel = getViewModel(SendCoinsViewModel.class);
        contentResolver = getContext().getContentResolver();
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        application = CoolApplication.getApplication();
        mConfig = application.getConfiguration();
        addressBookDao = AppDatabase.getInstance(getContext()).addressBookDao();

        if (null == savedInstanceState) {
            handleIntentData();
        }

        observeData();
    }

    private void observeData() {
        // todo
        if (Constants.ENABLE_EXCHANGE_RATES) {
            viewModel.exchangeRate.observe(this, bean -> {
                if (null == bean || bean.rate == null) return;
                final SendCoinsViewModel.State state = viewModel.state;
                if (null==state || state.ordinal() <= SendCoinsViewModel.State.INPUT.ordinal()) {
                    amountCalculatorLink.setExchangeRate(bean.rate);
                }
            });
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
                SendCoinsHelper.dialog(getContext(), view -> finishActivity(),
                        0, messageResId, messageArgs);
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
        viewModel.paymentData = data;
        viewModel.validatedAddress = null;
        viewModel.directPaymentAck = null;

        updateStateTask.execute();
    }

    /** send request
     *  requestListener
     */
    private void handlePaymentRequest() {
        final String paymentRequestUrl = viewModel.paymentData.paymentRequestUrl;
        setState(SendCoinsViewModel.State.REQUEST_PAYMENT_REQUEST);

        final IPaymentRequestListener requestListener = new IPaymentRequestListener() {
            @Override
            public void onPaymentData(PaymentData data) {
                log.info("PaymentRequest {}", data);
                if (!isAdded()) return;
                viewModel.progress.setValue(null);

                if (viewModel.paymentData.isExtendedBy(data)) {
                    // success
                    setState(SendCoinsViewModel.State.INPUT);
                    updateStateFrom(data);
                    updateView();
                    dryrunRunnable.execute();
                    return;
                }

                // failed
                final List<String> reasons = new LinkedList<>();
                if (!viewModel.paymentData.equalsAddress(data))
                    reasons.add("address");
                if (!viewModel.paymentData.equalsAmount(data))
                    reasons.add("amount");
                if (reasons.isEmpty())
                    reasons.add("unknown");
                //  show failed alert
                final String paymentRequestHost = SendCoinsHelper.getPaymentRequestHost(paymentRequestUrl);
                final String msg = getString(R.string.send_coins_fragment_request_payment_request_failed_message,
                        paymentRequestHost, Joiner.on(", ").join(reasons));
                MaterialDialog dialog = new MaterialDialog.Builder(getContext())
                        .title(R.string.send_coins_fragment_request_payment_request_failed_title)
                        .content(msg)
                        .canceledOnTouchOutside(false)
                        .neutralText(R.string.button_ok)
                        .show();
                dialog.getActionButton(DialogAction.NEUTRAL).setOnClickListener((v)-> handleCancel());
                log.info("BIP72 trust check failed: {}", reasons);
            }

            @Override
            public void onFail(int messageResId, Object... messageArgs) {
                log.error("PaymentRequest, fail: {}", getString(messageResId, messageArgs));
                if (!isAdded()) return;
                viewModel.progress.setValue(null);

                MaterialDialog dialog = new MaterialDialog.Builder(getContext())
                        .title(R.string.send_coins_fragment_request_payment_request_failed_title)
                        .content(getString(messageResId, messageArgs))
                        .positiveText(R.string.button_retry)
                        .negativeText(R.string.button_cancel)
                        .show();

                dialog.getActionButton(DialogAction.POSITIVE)
                        .setOnClickListener(view -> handlePaymentRequest());
                dialog.getActionButton(DialogAction.NEGATIVE).setOnClickListener(view -> {
                    if (viewModel.paymentData.hasOutputs()) {
                        setState(SendCoinsViewModel.State.INPUT);
                    } else {
                        handleCancel();
                    }
                });
            }
        };

        SendCoinsHelper.handlePaymentRequest(paymentRequestUrl, bluetoothAdapter, requestListener);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        //  ui
        coinAmountView.setCurrencySymbol(mConfig.getFormat().code());
        coinAmountView.setInputFormat(mConfig.getMaxPrecisionFormat());
        coinAmountView.setHintFormat(mConfig.getFormat());
        localAmountView.setInputFormat(Constants.LOCAL_FORMAT);
        localAmountView.setHintFormat(Constants.LOCAL_FORMAT);

        amountCalculatorLink = new CurrencyCalculatorLink(coinAmountView, localAmountView);
        amountCalculatorLink.setExchangeDirection(mConfig.getLastExchangeDirection());

        directPaymentEnableView.setOnCheckedChangeListener((compoundButton, b) -> {
            if (b && null!=viewModel.paymentData && viewModel.paymentData.isBluetoothPaymentUrl()
                    && !bluetoothAdapter.isEnabled()) {
                // ask for permission to enable bluetooth
                startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE),
                        REQUEST_CODE_ENABLE_BLUETOOTH_FOR_DIRECT_PAYMENT);
            }
        });

        viewCancel.setText(R.string.button_cancel);
        viewGo.setText(R.string.send_coins_fragment_button_send);

        feeSeekBar.setDefaultValue(2000);
        log.info( "onViewCreated, feeSeekBar: "+ feeSeekBar.getSelectedNumber());
    }

    private void  updateView() {
        // todo
    }

    private void setState(SendCoinsViewModel.State state){
        viewModel.state = state;

        getActivity().invalidateOptionsMenu();
        updateView();
    }

    private void requestFocusFirst() {
        if (!isPayeePlausible())
            receivingAddressView.requestFocus();
        else if (!isAmountPlausible())
            amountCalculatorLink.requestFocus();
        else if (!isPasswordPlausible())
            privateKeyPasswordView.requestFocus();
        else if (everythingPlausible())
            viewGo.requestFocus();
        else
            log.warn("unclear focus");
    }

    private boolean isPayeePlausible() {
        if (null!= viewModel.paymentData &&viewModel.paymentData.hasOutputs()) {
            return true;
        }

        if (viewModel.validatedAddress != null) {
            return true;
        }
        return false;
    }

    private boolean isAmountPlausible() {
        if (viewModel.dryrunTransaction != null)
            return viewModel.dryrunException == null;
        else if (PaymentUtil.mayEditAmount(viewModel.paymentData))
            return amountCalculatorLink.hasAmount();
        else
            return viewModel.paymentData.hasAmount();
    }

    private boolean isPasswordPlausible() {
        final Wallet wallet = viewModel.wallet.getValue();
        if (wallet == null)
            return false;
        if (!wallet.isEncrypted())
            return true;
        final String password = privateKeyPasswordView.getText().toString().trim();
        return !password.isEmpty();
    }

    private boolean everythingPlausible() {
        return viewModel.state == SendCoinsViewModel.State.INPUT && isPayeePlausible()
                && isAmountPlausible() && isPasswordPlausible();
    }

    private void validateReceivingAddress() {
        try {
            final String addressStr = receivingAddressView.getText().toString().trim();
            if (!TextUtils.isEmpty(addressStr)) {
                final Address address = Address.fromString(Constants.NETWORK_PARAMETERS, addressStr);
                final String label = addressBookDao.resolveLabel(address.toString());
                viewModel.validatedAddress = new AddressBean(Constants.NETWORK_PARAMETERS,
                        address.toString() , label);
                receivingAddressView.setText(null);
                log.info("Locked to valid address: {}", viewModel.validatedAddress);
            }
        } catch (final AddressFormatException x) {
            // swallow
        }
    }

    private void handleCancel() {
        if (viewModel.state == null ||
                viewModel.state.ordinal() <= SendCoinsViewModel.State.INPUT.ordinal()) {
            getActivity().setResult(Activity.RESULT_CANCELED);
        }
        log.info("State: {} {}",viewModel.state, null!=viewModel.state? viewModel.state: -12);
        finishActivity();
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

        scanAction.setEnabled(viewModel.state == SendCoinsViewModel.State.INPUT);
//        scanAction.setEnabled(true);

        final MenuItem emptyAction = menu.findItem(R.id.send_coins_options_empty);
        emptyAction.setEnabled(viewModel.state == SendCoinsViewModel.State.INPUT
                && PaymentUtil.mayEditAmount(viewModel.paymentData)
                && viewModel.balance.getValue() != null);

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
                handleEmpty();
                break;
                default:    return super.onOptionsItemSelected(item);
        }
        return true;
    }

    private void handleEmpty() {
        final Coin available = viewModel.balance.getValue();
        amountCalculatorLink.setBtcAmount(available);

        updateView();
        dryrunRunnable.execute();
    }

    @Override
    public void startScan(View v) {
        CustomCaptureActivity.start(this, REQUEST_QR_SCAN_CODE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        mHandler.post(()-> onActivityResultResumed(requestCode, resultCode, data));
    }
    private void onActivityResultResumed(final int requestCode, final int resultCode, final Intent data) {
        switch (requestCode) {
            case REQUEST_QR_SCAN_CODE:
                // 处理二维码扫描结果
                if (resultCode == Activity.RESULT_OK)
                    handleScanResult(data);
                break;
            case REQUEST_CODE_ENABLE_BLUETOOTH_FOR_PAYMENT_REQUEST:
                if (resultCode == Activity.RESULT_OK && null!=viewModel.paymentData
                        && viewModel.paymentData.isBluetoothPaymentRequestUrl()) {
                    handlePaymentRequest();
                }
                break;
            case REQUEST_CODE_ENABLE_BLUETOOTH_FOR_DIRECT_PAYMENT:
                if (null!=viewModel.paymentData && viewModel.paymentData.isBluetoothPaymentUrl()) {
                    directPaymentEnableView.setChecked(resultCode == Activity.RESULT_OK);
                }
                break;
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
                   final String result = bundle.getString(XQRCode.RESULT_DATA);
                    StringInputParser parser = new StringInputParser(result) {
                        @Override
                        protected void handleWebUrl(String link) {
                            WebActivity.start(getContext(), link);
                        }

                        @Override
                        protected void requestBIP38PrivateKeyPassphrase() {
                            // todo need test
                            View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_bip38_password_layout, null);
                            TextView passwordTv = view.findViewById(R.id.dialog_password_bip38_et);
                            MaterialDialog dialog = new MaterialDialog.Builder(getContext())
                                    .title(R.string.sweep_wallet_fragment_password)
                                    .customView(view, true)
                                    .positiveText(R.string.button_ok)
                                    .negativeText(R.string.button_cancel)
                                    .show();
                            dialog.getActionButton(DialogAction.POSITIVE).setOnClickListener(view1 -> {
                                String password = passwordTv.getText().toString().trim();
                                if (!password.isEmpty()) {
                                    responseBIP38PrivateKeyPassphrase(password);
                                    dialog.dismiss();
                                }
                            });

                        }

                        @Override
                        public void handleDirectTransaction(Transaction transaction) throws VerificationException {
                            cannotClassify(result);
                        }

                        @Override
                        public void error(int messageResId, Object... messageArgs) {
                            SendCoinsHelper.dialog(getContext(),null,
                                    R.string.button_scan, messageResId, messageArgs);
                        }

                        @Override
                        public void handlePaymentData(PaymentData data) {
                            setState(null);
                            updateStateFrom(data);
                        }
                    };
                    parser.parse();
                    log.info( " 解析结果: " + result);
                } else if (bundle.getInt(XQRCode.RESULT_TYPE) == XQRCode.RESULT_FAILED) {
                    XToast.error(getContext(), R.string.parse_qr_code_failed).show();
                }
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        amountCalculatorLink.setListener(amountsListener);
        privateKeyPasswordView.addTextChangedListener(privateKeyPasswordListener);
        updateView();
        dryrunRunnable.execute();
    }

    @Override
    public void onPause() {
        super.onPause();
        amountCalculatorLink.setListener(null);
        privateKeyPasswordView.removeTextChangedListener(privateKeyPasswordListener);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mConfig.setLastExchangeDirection(amountCalculatorLink.getExchangeDirection());
    }

    @Override
    public void onDetach() {
        mHandler.removeCallbacksAndMessages(null);
        super.onDetach();
    }

    @Override
    public void onDestroy() {
        if (null!=viewModel.sentTransaction) {
            viewModel.sentTransaction.getConfidence().removeEventListener(transactionListener);
        }
        super.onDestroy();
    }

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_send_coins;
    }

    // ////////////////////////////////////////////////////////
    // ////////////////////////////////////////////////////////
    // ////////////////////// 内部变量，接口实现 ////////////////////////////
    // ////////////////////////////////////////////////////////
    // ////////////////////////////////////////////////////////

    private final TransactionConfidence.Listener transactionListener = new TransactionConfidence.Listener() {
        @Override
        public void onConfidenceChanged(TransactionConfidence confidence, ChangeReason reason) {
            log.info("TransactionConfidence.Listener: {} , {}", reason, confidence.toString());
            //   监听发送交易的状态/事件
            runOnUiThread(()->{
                if (!isResumed()) return;
                //  update UI
                TransactionConfidence.ConfidenceType confidenceType = confidence.getConfidenceType();
                final int numPeers = confidence.numBroadcastPeers();

                if (viewModel.state == SendCoinsViewModel.State.SENDING) {
                    if (confidenceType == TransactionConfidence.ConfidenceType.DEAD) {
                        setState(SendCoinsViewModel.State.FAILED);
                    } else if (numPeers > 1 ||
                            confidenceType == TransactionConfidence.ConfidenceType.BUILDING) {
                        setState(SendCoinsViewModel.State.SENT);

                        // Auto-close the dialog after a short delay
                        if (mConfig.getSendCoinsAutoClose()) {
                            mHandler.postDelayed( ()-> finishActivity(), 500);
                        }
                    }
                }

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

                updateView();
            });
        }
};

    private final ITask updateStateTask = new ITask() {
        @Override
        public void execute() {
            // delay these actions until fragment is resumed
            mHandler.post(this);
        }

        @Override
        public void run() {
            final PaymentData data = viewModel.paymentData;
            if (null == data) return;

            if (data.hasPaymentRequestUrl() && data.isBluetoothPaymentRequestUrl()) {
                // bluetooth
                if (bluetoothAdapter.isEnabled()) {
                    handlePaymentRequest();
                    return;
                }
                //   ask for permission to enable bluetooth
                startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE),
                        REQUEST_CODE_ENABLE_BLUETOOTH_FOR_PAYMENT_REQUEST);
                return;
            }

            if (data.hasPaymentRequestUrl() && data.isHttpPaymentRequestUrl()) {
                // http
                handlePaymentRequest();
                return;
            }

            // ui
            setState(SendCoinsViewModel.State.INPUT);
            receivingAddressView.setText(null);
            amountCalculatorLink.setBtcAmount(data.getAmount());

            if (data.isBluetoothPaymentUrl()) {
                directPaymentEnableView.setChecked(
                        null!=bluetoothAdapter && bluetoothAdapter.isEnabled());
            } else if (data.isHttpPaymentUrl()) {
                directPaymentEnableView.setChecked(true);
            }

            requestFocusFirst();
            updateView();
            dryrunRunnable.execute();
        }
    };

    private final ITask dryrunRunnable = new ITask()  {
        @Override
        public void run() {
            if (SendCoinsViewModel.State.INPUT == viewModel.state) {
                dryrun();
            }
            updateView();
        }

        private void dryrun() {
            viewModel.dryrunTransaction = null;
            viewModel.dryrunException = null;
            final Wallet wallet = viewModel.wallet.getValue();
            final Coin amount = amountCalculatorLink.getAmount();
            final Coin fee = feeSeekBar.getFee();

            if (null==amount || null == fee || null == wallet) return;
            // won't be used, tx is never committed
            final Address dummy = wallet.currentReceiveAddress();

            final PaymentData data =
                    PaymentUtil.mergeWithEditedValues(viewModel.paymentData, amount, dummy);
            final SendRequest sendRequest = PaymentUtil.getSendRequest(data);
            sendRequest.signInputs = false;
            sendRequest.emptyWallet = PaymentUtil.mayEditAmount(viewModel.paymentData)
                    && amount.equals(wallet.getBalance(Wallet.BalanceType.AVAILABLE));
            sendRequest.feePerKb = fee;

            try { // Given a spend request containing an incomplete transaction
                wallet.completeTx(sendRequest); //  包含不完整的交易信息的 SendRequest
                viewModel.dryrunTransaction = sendRequest.tx;
            } catch (final Exception e) {
                viewModel.dryrunException = e;
                log.warn("dryrunException: ", e);
            }
        }

        public void execute() {
            // delay these actions until fragment is resumed
            mHandler.post(this);
        }
    };

    private final TextWatcher privateKeyPasswordListener = new TextWatcher() {
        @Override
        public void onTextChanged(final CharSequence s, final int start, final int before, final int count) {
            ViewUtil.setVisibility(privateKeyBadPasswordView, View.INVISIBLE);
//            privateKeyBadPasswordView.setVisibility(View.INVISIBLE);
            updateView();
        }

        @Override
        public void beforeTextChanged(final CharSequence s, final int start, final int count, final int after) {
        }

        @Override
        public void afterTextChanged(final Editable s) {
        }
    };

    private final CurrencyAmountView.Listener amountsListener = new CurrencyAmountView.Listener() {
        @Override
        public void changed() {
            updateView();
            dryrunRunnable.execute();
        }

        @Override
        public void focusChanged(final boolean hasFocus) {
        }
    };

}
