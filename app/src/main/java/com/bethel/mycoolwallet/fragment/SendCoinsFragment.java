package com.bethel.mycoolwallet.fragment;


import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;

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
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.TextView;

import com.bethel.integration_android.BitcoinIntegration;
import com.bethel.mycoolwallet.CoolApplication;
import com.bethel.mycoolwallet.R;
import com.bethel.mycoolwallet.activity.CustomCaptureActivity;
import com.bethel.mycoolwallet.activity.WebActivity;
import com.bethel.mycoolwallet.adapter.AddressLabelListAdapter;
import com.bethel.mycoolwallet.data.AddressBean;
import com.bethel.mycoolwallet.data.BlockChainState;
import com.bethel.mycoolwallet.data.payment.PaymentData;
import com.bethel.mycoolwallet.data.payment.PaymentStandard;
import com.bethel.mycoolwallet.data.payment.PaymentUtil;
import com.bethel.mycoolwallet.db.AddressBook;
import com.bethel.mycoolwallet.db.AddressBookDao;
import com.bethel.mycoolwallet.db.AppDatabase;
import com.bethel.mycoolwallet.fragment.dialog.ProgressDialogFragment;
import com.bethel.mycoolwallet.helper.Configuration;
import com.bethel.mycoolwallet.helper.SendCoinsHelper;
import com.bethel.mycoolwallet.helper.parser.IntentDataParser;
import com.bethel.mycoolwallet.helper.parser.StringInputParser;
import com.bethel.mycoolwallet.interfaces.IQrScan;
import com.bethel.mycoolwallet.interfaces.ITask;
import com.bethel.mycoolwallet.mvvm.view_model.SendCoinsViewModel;
import com.bethel.mycoolwallet.request.payment.DeriveKeyTask;
import com.bethel.mycoolwallet.request.payment.IPaymentRequestListener;
import com.bethel.mycoolwallet.request.payment.SendCoinsOfflineTask;
import com.bethel.mycoolwallet.request.payment.send.IPaymentTaskCallback;
import com.bethel.mycoolwallet.service.BlockChainService;
import com.bethel.mycoolwallet.utils.Constants;
import com.bethel.mycoolwallet.utils.CurrencyTools;
import com.bethel.mycoolwallet.utils.Utils;
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

import org.bitcoin.protocols.payments.Protos;
import org.bitcoinj.core.Address;
import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.InsufficientMoneyException;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionConfidence;
import org.bitcoinj.core.VerificationException;
import org.bitcoinj.protocols.payments.PaymentProtocol;
import org.bitcoinj.utils.MonetaryFormat;
import org.bitcoinj.wallet.KeyChain;
import org.bitcoinj.wallet.SendRequest;
import org.bitcoinj.wallet.Wallet;
import org.bouncycastle.crypto.params.KeyParameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.OnClick;

/**
 * 支付页面.
 *
 *
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
//    private ContentResolver contentResolver;
    private BluetoothAdapter bluetoothAdapter;
    private CoolApplication application;
    private CurrencyCalculatorLink amountCalculatorLink;
    private Configuration mConfig;
    private final Handler mHandler = new Handler();
    private AddressBookDao addressBookDao;
    private AddressLabelListAdapter addressLabelListAdapter;


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
        setState(SendCoinsViewModel.State.SIGNING);
        final PaymentData paymentData = PaymentUtil.mergeWithEditedValues(viewModel.paymentData,
                amountCalculatorLink.getAmount(),
                null!=viewModel.validatedAddress? viewModel.validatedAddress.address:null);
        final Coin finalAmount = paymentData.getAmount();
        final Wallet wallet = viewModel.wallet.getValue();

        final SendRequest sendRequest = PaymentUtil.getSendRequest(paymentData);
        sendRequest.emptyWallet = PaymentUtil.mayEditAmount(viewModel.paymentData)
                && finalAmount.equals(wallet.getBalance(Wallet.BalanceType.AVAILABLE));
        sendRequest.feePerKb = feeSeekBar.getFee();
        sendRequest.memo = viewModel.paymentData.memo;
        sendRequest.exchangeRate = amountCalculatorLink.getExchangeRate();
        sendRequest.aesKey = encryptionKey;

        final Coin fee = viewModel.dryrunTransaction.getFee();
        if (fee.isGreaterThan(finalAmount)) {
            setState(SendCoinsViewModel.State.INPUT);

            final MonetaryFormat btcFormat = mConfig.getFormat();
            MaterialDialog dialog = new MaterialDialog.Builder(getContext())
                    .title(R.string.send_coins_fragment_significant_fee_title)
                    .content(getString(R.string.send_coins_fragment_significant_fee_message,
                            btcFormat.format(fee), btcFormat.format(finalAmount)))
                    .positiveText(R.string.send_coins_fragment_button_send)
                    .negativeText(R.string.button_cancel)
                    .show();
            dialog.getActionButton(DialogAction.POSITIVE).setOnClickListener(
                    view -> sendPayment(sendRequest, finalAmount) );
        } else {
            sendPayment(sendRequest, finalAmount);
        }
    }

    private void sendPayment(final SendRequest sendRequest, final Coin finalAmount) {
        final Wallet wallet = viewModel.wallet.getValue();
        new SendCoinsOfflineTask(wallet, sendRequest){
            @Override
            protected void onSuccess(Transaction transaction) {
                viewModel.sentTransaction = transaction;
                setState(SendCoinsViewModel.State.SENDING);
                transaction.getConfidence().addEventListener(transactionListener);

                final Address refundAddress = viewModel.paymentData.standard == PaymentStandard.BIP70
                        ? wallet.freshAddress(KeyChain.KeyPurpose.REFUND) : null;
                LinkedList<Transaction> txList = new LinkedList<>();
                txList.add(transaction);
                final Protos.Payment payment = PaymentProtocol.createPaymentMessage(
                        txList, finalAmount, refundAddress, null, viewModel.paymentData.payeeData);
                //  broadcastTransaction
                if (directPaymentEnableView.isChecked()) {
                    directPay(payment);
                }
                BlockChainService.broadcastTransaction(getContext(), viewModel.sentTransaction);

                // callback
                final ComponentName callingActivity = getActivity().getCallingActivity();
                if (callingActivity != null) {
                    log.info("returning result to calling activity: {}", callingActivity.flattenToString());

                    final Intent result = new Intent();
                    BitcoinIntegration.transactionHashToResult(result,
                            viewModel.sentTransaction.getTxId().toString());
                    if (viewModel.paymentData.standard == PaymentStandard.BIP70){
                        BitcoinIntegration.paymentToResult(result, payment.toByteArray());
                    }
                    getActivity().setResult(Activity.RESULT_OK, result);
                }
            }

            private void directPay(final Protos.Payment payment) {
                // http, bluetooth
                final IPaymentTaskCallback callback = new IPaymentTaskCallback() {
                    @Override
                    public void onResult(boolean ack) {
                        viewModel.directPaymentAck = ack;

                        if (viewModel.state == SendCoinsViewModel.State.SENDING) {
                            setState(SendCoinsViewModel.State.SENT);
                        }
                        updateView();
                    }

                    @Override
                    public void onFail(int messageResId, Object... messageArgs) {
                        final StringBuilder msg = new StringBuilder();
                        msg.append(viewModel.paymentData.paymentUrl).append( "\n" )
                                .append(getString(messageResId, messageArgs)).append( "\n\n" )
                                .append(getString(R.string.send_coins_fragment_direct_payment_failed_msg));

                        MaterialDialog dialog = new MaterialDialog.Builder(getContext())
                                .title(R.string.send_coins_fragment_direct_payment_failed_title)
                                .content(msg.toString())
                                .positiveText(R.string.button_retry)
                                .negativeText(R.string.button_dismiss)
                                .show();
                        dialog.getActionButton(DialogAction.POSITIVE)
                                .setOnClickListener(view -> directPay(payment));
                    }
                };

                SendCoinsHelper.sendPayment(viewModel.paymentData.paymentUrl, payment,
                        bluetoothAdapter, callback);
            }

            @Override
            protected void onInsufficientMoney(Coin missing) {
//    not enough coin
                setState(SendCoinsViewModel.State.INPUT);

                final Coin estimated = wallet.getBalance(Wallet.BalanceType.ESTIMATED);
                final Coin available = wallet.getBalance(Wallet.BalanceType.AVAILABLE);
                final Coin pending = estimated.subtract(available);

                final MonetaryFormat btcFormat = mConfig.getFormat();
                final StringBuilder msg = new StringBuilder();
                msg.append(getString(R.string.send_coins_fragment_insufficient_money_msg1,
                        btcFormat.format(missing)));

                if (pending.signum() > 0) {
                    msg.append("\n\n").append(getString(R.string.send_coins_fragment_pending,
                                    btcFormat.format(pending)));
                }
                if (PaymentUtil.mayEditAmount(viewModel.paymentData)) {
                    msg.append("\n\n")
                            .append(getString(R.string.send_coins_fragment_insufficient_money_msg2));
                }

                // dialog
                MaterialDialog.Builder builder = new MaterialDialog.Builder(getContext());
                builder.title( R.string.send_coins_fragment_insufficient_money_title)
                        .iconRes(R.drawable.ic_warning_grey600_24dp).content(msg);
                if (PaymentUtil.mayEditAmount(viewModel.paymentData)) {
                    builder.positiveText(R.string.send_coins_options_empty)
                            .negativeText(R.string.button_cancel);
                } else {
                    builder.neutralText(R.string.button_ok);
                }

                MaterialDialog dialog = builder.show();

                if (PaymentUtil.mayEditAmount(viewModel.paymentData)) {
                    dialog.getActionButton(DialogAction.POSITIVE)
                            .setOnClickListener(view -> handleEmpty());
                }
            }

            @Override
            protected void onInvalidEncryptionKey() {
                // error password
                setState(SendCoinsViewModel.State.INPUT);

                ViewUtil.showView(privateKeyBadPasswordView, true);
                privateKeyPasswordView.requestFocus();
            }

            @Override
            protected void onEmptyWalletFailed(Exception e) {
                //super.onEmptyWalletFailed(e);
                setState(SendCoinsViewModel.State.INPUT);
                SendCoinsHelper.dialogWarn(getContext(),
                        R.string.send_coins_fragment_empty_wallet_failed_title,
                        R.string.send_coins_fragment_hint_empty_wallet_failed);
            }

            @Override
            protected void onFailure(Exception exception) {
                setState(SendCoinsViewModel.State.FAILED);

                SendCoinsHelper.dialogWarn(getContext(),
                        R.string.send_coins_error_msg, exception.toString());
            }
        }.executeAsyncTask();
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
        if (Constants.ENABLE_EXCHANGE_RATES) {
            viewModel.exchangeRate.observe(this, bean -> {
                if (null == bean || bean.rate == null) return;
                final SendCoinsViewModel.State state = viewModel.state;
                if (null==state || state.ordinal() <= SendCoinsViewModel.State.INPUT.ordinal()) {
                    amountCalculatorLink.setExchangeRate(bean.rate);
                }
            });
        }

        viewModel.wallet.observe(this, wallet -> updateView());
        viewModel.addressBook.observe(this, list -> updateView());
        viewModel.blockChain.observe(this, blockChainState -> updateView());
        viewModel.balance.observe(this, coin -> getActivity().invalidateOptionsMenu());

        final FragmentManager fragmentManager = getFragmentManager();
        viewModel.progress.observe(this, new  ProgressDialogFragment.Observer(fragmentManager));
    }

    private void handleIntentData() {
        Intent intent = getActivity().getIntent();
        log.debug("Intent  {}", intent);
        if (null == intent) {
            updateStateFrom(PaymentUtil.blank());
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
            protected Context getContext() {
                return SendCoinsFragment.this.getContext();
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
        final String paymentRequestHost = SendCoinsHelper.getPaymentRequestHost(paymentRequestUrl);
        viewModel.progress.setValue(
                getString(R.string.send_coins_fragment_request_payment_request_progress, paymentRequestHost));
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

        // address input
        addressLabelListAdapter = new AddressLabelListAdapter(getContext());
        addressLabelListAdapter.setAddressLabelListFilter(mAddressLabelListFilter);
        receivingAddressView.setAdapter(addressLabelListAdapter);
        receivingAddressView.setOnFocusChangeListener(receivingAddressListener);
        receivingAddressView.addTextChangedListener(receivingAddressListener);
        receivingAddressView.setOnItemClickListener(receivingAddressListener);

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

        feeSeekBar.setDefaultValue(2020);
        log.info( "onViewCreated, feeSeekBar: "+ feeSeekBar.getSelectedNumber());
    }

    private void  updateView() {
        if (null==viewModel.paymentData) {
            ViewUtil.showView(getView(), false);
            return;
        }

        ViewUtil.showView(getView(), true);

//        final Coin fee = feeSeekBar.getFee();
//        final BlockChainState chainState = viewModel.blockChain.getValue();
//        final Map<String, AddressBook> addressBookMap = AddressBook.asMap(viewModel.addressBook.getValue());
//        final MonetaryFormat btcFormat = mConfig.getFormat();

        if (viewModel.paymentData.hasPayee()) {
            payeeNameView.setText(viewModel.paymentData.payeeName);

            final String verifiedBy = viewModel.paymentData.payeeVerifiedBy != null
                    ? viewModel.paymentData.payeeVerifiedBy
                    : getString(R.string.send_coins_fragment_payee_verified_by_unknown);
            final String verifiedByText =Constants.CHAR_CHECKMARK
                    + String.format(getString(R.string.send_coins_fragment_payee_verified_by), verifiedBy);
            payeeVerifiedByView.setText(verifiedByText);

            ViewUtil.showView(payeeNameView, true);
            ViewUtil.showView(payeeVerifiedByView, true);
        } else {
            ViewUtil.showView(payeeNameView, false);
            ViewUtil.showView(payeeVerifiedByView, false);
        }

        if (viewModel.paymentData.hasOutputs()) {
            ViewUtil.showView(payeeGroup, true);
            ViewUtil.showView(receivingAddressView, false);
            boolean receiveStatic = !viewModel.paymentData.hasPayee() || viewModel.paymentData.payeeVerifiedBy == null;
            ViewUtil.showView(receivingStaticView, receiveStatic);

            receivingStaticLabelView.setText(viewModel.paymentData.memo);

            if (viewModel.paymentData.hasAddress()) {
                final CharSequence address = WalletUtils.formatAddress(viewModel.paymentData.getAddress(),
                        Constants.ADDRESS_FORMAT_GROUP_SIZE, Constants.ADDRESS_FORMAT_LINE_SIZE);
                receivingStaticAddressView.setText(address);
            } else {
                receivingStaticAddressView.setText(R.string.send_coins_fragment_receiving_address_complex);
            }
        } else if (null!=viewModel.validatedAddress) {
            ViewUtil.showView(payeeGroup, true);
            ViewUtil.showView(receivingAddressView, false);
            ViewUtil.showView(receivingStaticView, true);

            final CharSequence address = WalletUtils.formatAddress(viewModel.validatedAddress.address,
                    Constants.ADDRESS_FORMAT_GROUP_SIZE, Constants.ADDRESS_FORMAT_LINE_SIZE);
            receivingStaticAddressView.setText(address);

            final String label = addressBookDao.resolveLabel(viewModel.validatedAddress.getAddressStr());
            final String staticLabel;
            if (!TextUtils.isEmpty(label)) {
                staticLabel = label;
            } else if (!TextUtils.isEmpty(viewModel.validatedAddress.label)) {
                staticLabel = viewModel.validatedAddress.label;
            } else {
                staticLabel = getString(R.string.address_unlabeled);
            }
            receivingStaticLabelView.setText(staticLabel);

            int colorResId = !TextUtils.isEmpty(viewModel.validatedAddress.label)?
                    R.color.fg_significant : R.color.fg_insignificant;
            receivingStaticLabelView.setTextColor(ContextCompat.getColor(getContext(), colorResId));
        } else if (null==viewModel.paymentData.standard) {
            ViewUtil.showView(payeeGroup, true);
            ViewUtil.showView(receivingAddressView, true);
            ViewUtil.showView(receivingStaticView, false);
        } else {
            ViewUtil.showView(payeeGroup, false);
        }

        receivingAddressView.setEnabled(viewModel.state == SendCoinsViewModel.State.INPUT);
        final boolean amountVisible = viewModel.paymentData.hasAmount()
                || (null!=viewModel.state && viewModel.state.ordinal() >= SendCoinsViewModel.State.INPUT.ordinal());
        ViewUtil.showView(amountGroup, amountVisible);
        final boolean amountLink = viewModel.state == SendCoinsViewModel.State.INPUT
                && PaymentUtil.mayEditAmount(viewModel.paymentData);
        amountCalculatorLink.setEnabled(amountLink);

        // directPaymentEnableView
        updateDirectPaymentView();

        // hintView
        updateHintView();

        // sentTransactionViewGroup
        updateTransactionView();

        // directPaymentMessageView
        if (null!=viewModel.directPaymentAck) {
            ViewUtil.showView(directPaymentMessageView, true);
            directPaymentMessageView.setText(viewModel.directPaymentAck ?
                            R.string.send_coins_fragment_direct_payment_ack
                            : R.string.send_coins_fragment_direct_payment_nack);
        } else {
            ViewUtil.showView(directPaymentMessageView, false);
        }

        // viewCancel, viewGo
        updateBottomActionsView();

        // privateKeyPassword
        final Wallet wallet = viewModel.wallet.getValue();
        final boolean privateKeyPasswordViewVisible = (viewModel.state == SendCoinsViewModel.State.INPUT
                || viewModel.state == SendCoinsViewModel.State.DECRYPTING) && wallet != null
                && wallet.isEncrypted();
        ViewUtil.showView(privateKeyPasswordViewGroup, privateKeyPasswordViewVisible);
        privateKeyPasswordView.setEnabled(viewModel.state == SendCoinsViewModel.State.INPUT);

        // NextFocus id
        updateFocusLinking();
    }

    private void updateDirectPaymentView() {
        final boolean directPaymentVisible;
        if (viewModel.paymentData.hasPaymentUrl()) {
            if (viewModel.paymentData.isBluetoothPaymentUrl())
                directPaymentVisible = bluetoothAdapter != null;
            else
                directPaymentVisible = true;
        } else {
            directPaymentVisible = false;
        }
        ViewUtil.showView(directPaymentEnableView, directPaymentVisible);
        directPaymentEnableView.setEnabled(viewModel.state == SendCoinsViewModel.State.INPUT);
    }

    private void updateFocusLinking() {
        final boolean privateKeyPasswordViewVisible = privateKeyPasswordViewGroup.getVisibility() == View.VISIBLE;
        final int activeAmountViewId = amountCalculatorLink.activeTextView().getId();
        receivingAddressView.setNextFocusDownId(activeAmountViewId);
        receivingAddressView.setNextFocusForwardId(activeAmountViewId);

        amountCalculatorLink.setNextFocusId(privateKeyPasswordViewVisible ?
                R.id.send_coins_private_key_password : R.id.send_coins_go);

        privateKeyPasswordView.setNextFocusUpId(activeAmountViewId);
        privateKeyPasswordView.setNextFocusDownId(R.id.send_coins_go);
        privateKeyPasswordView.setNextFocusForwardId(R.id.send_coins_go);

        viewGo.setNextFocusUpId(privateKeyPasswordViewVisible ?
                        R.id.send_coins_private_key_password : activeAmountViewId);
    }

    private void updateBottomActionsView() {
        final boolean cancelable = SendCoinsViewModel.State.REQUEST_PAYMENT_REQUEST != viewModel.state
                && SendCoinsViewModel.State.DECRYPTING != viewModel.state
                && SendCoinsViewModel.State.SIGNING != viewModel.state;
        viewCancel.setEnabled(cancelable);

        final Wallet wallet = viewModel.wallet.getValue();
        final Coin fee = feeSeekBar.getFee();
        final   BlockChainState blockChainState = viewModel.blockChain.getValue();
        final boolean payable = everythingPlausible() && null!=viewModel.dryrunTransaction
                && null!= wallet && null!=fee && (null==blockChainState || !blockChainState.replaying);
        viewGo.setEnabled(payable);

        if (viewModel.state == null ||
                viewModel.state == SendCoinsViewModel.State.REQUEST_PAYMENT_REQUEST) {
            viewCancel.setText(R.string.button_cancel);
            viewGo.setText(null);
        } else if (viewModel.state == SendCoinsViewModel.State.INPUT) {
            viewCancel.setText(R.string.button_cancel);
            viewGo.setText(R.string.send_coins_fragment_button_send);
        } else if (viewModel.state == SendCoinsViewModel.State.DECRYPTING) {
            viewCancel.setText(R.string.button_cancel);
            viewGo.setText(R.string.send_coins_fragment_state_decrypting);
        } else if (viewModel.state == SendCoinsViewModel.State.SIGNING) {
            viewCancel.setText(R.string.button_cancel);
            viewGo.setText(R.string.send_coins_preparation_msg);
        } else if (viewModel.state == SendCoinsViewModel.State.SENDING) {
            viewCancel.setText(R.string.send_coins_fragment_button_back);
            viewGo.setText(R.string.send_coins_sending_msg);
        } else if (viewModel.state == SendCoinsViewModel.State.SENT) {
            viewCancel.setText(R.string.send_coins_fragment_button_back);
            viewGo.setText(R.string.send_coins_sent_msg);
        } else if (viewModel.state == SendCoinsViewModel.State.FAILED) {
            viewCancel.setText(R.string.send_coins_fragment_button_back);
            viewGo.setText(R.string.send_coins_failed_msg);
        }

    }

    private void updateTransactionView() {
        // todo  TransactionsAdapter.ListItem.TransactionItem()
        //  if (viewModel.sentTransaction != null && wallet != null) {
        ViewUtil.showView(sentTransactionViewGroup, false);
    }

    private void updateHintView() {
        ViewUtil.showView(hintView, false);
        if (SendCoinsViewModel.State.INPUT == viewModel.state) {
            final   BlockChainState blockChainState = viewModel.blockChain.getValue();
            final MonetaryFormat btcFormat = mConfig.getFormat();
            final Wallet wallet = viewModel.wallet.getValue();

            if (null!=blockChainState && blockChainState.replaying) {
                ViewUtil.showView(hintView, true);
                hintView.setTextColor(ContextCompat.getColor(getContext(), R.color.fg_error));
                hintView.setText(R.string.send_coins_fragment_hint_replaying);
            } else if (null==viewModel.validatedAddress
                    && PaymentUtil.mayEditAmount(viewModel.paymentData)
                    && !TextUtils.isEmpty(receivingAddressView.getText())) {
                ViewUtil.showView(hintView, true);
                hintView.setTextColor(ContextCompat.getColor(getContext(), R.color.fg_error));
                hintView.setText(R.string.send_coins_fragment_receiving_address_error);
            } else if (null!=viewModel.dryrunException) {
                ViewUtil.showView(hintView, true);
                hintView.setTextColor(ContextCompat.getColor(getContext(), R.color.fg_error));

                final String hintText;
                Exception e = viewModel.dryrunException;
                if (e instanceof Wallet.DustySendRequested) {
                    hintText= getString(R.string.send_coins_fragment_hint_dusty_send);
                } else if (e instanceof InsufficientMoneyException) {
                    hintText = getString(R.string.send_coins_fragment_hint_insufficient_money,
                            btcFormat.format(((InsufficientMoneyException) e).missing));
                } else if (e instanceof Wallet.CouldNotAdjustDownwards) {
                    hintText = getString(R.string.send_coins_fragment_hint_empty_wallet_failed);
                } else {
                    hintText = e.toString();
                }
                hintView.setText(hintText);
            } else if (null!=viewModel.dryrunTransaction &&
                    null!=viewModel.dryrunTransaction.getFee()) {
                ViewUtil.showView(hintView, true);
                final int hintResId= R.string.send_coins_fragment_hint_fee;
                final int colorResId = R.color.fg_insignificant;
                hintView.setTextColor(ContextCompat.getColor(getContext(), colorResId));
                hintView.setText(getString(hintResId,
                        btcFormat.format(viewModel.dryrunTransaction.getFee())));
            } else if (null!= viewModel.validatedAddress && null!= wallet
                    && PaymentUtil.mayEditAmount(viewModel.paymentData)
                    && wallet.isAddressMine(viewModel.validatedAddress.address)) {
                hintView.setTextColor(ContextCompat.getColor(getContext(), R.color.fg_insignificant));
                ViewUtil.showView(hintView, true);
                hintView.setText(R.string.send_coins_fragment_receiving_address_own);
            }
        }
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
        mHandler.post(()-> onActivityResultResumed(requestCode, resultCode, data));
        super.onActivityResult(requestCode, resultCode, data);
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

    private final ReceivingAddressListener receivingAddressListener = new ReceivingAddressListener();
    private class ReceivingAddressListener implements View.OnFocusChangeListener,
            TextWatcher, AdapterView.OnItemClickListener {
        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
        }

        @Override
        public void afterTextChanged(Editable editable) {
            final String content = editable.toString().trim();
            if (content.isEmpty()) {
                updateView();
            } else {
                validateReceivingAddress();
            }
        }

        @Override
        public void onFocusChange(View view, boolean b) {
            if (!b) {
                validateReceivingAddress();
                updateView();
            }
        }

        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
            try {
                final AddressBook book = addressLabelListAdapter.getItem(position);
                viewModel.validatedAddress = new AddressBean(Constants.NETWORK_PARAMETERS,
                        book.getAddress(), book.getLabel());
                receivingAddressView.setText(null);
                log.info("Picked valid address from suggestions: {}", viewModel.validatedAddress);
            } catch (Exception e) {
                log.info("address suggestions onItemClick:", e);
            }
        }
    }

    private final Filter mAddressLabelListFilter = new Filter() {
        @Override
        protected FilterResults performFiltering(CharSequence charSequence) {
            final String trimmedConstraint = charSequence.toString().trim();
            final FilterResults results = new FilterResults();
            if (null==viewModel.validatedAddress && !trimmedConstraint.isEmpty()) {
                List<AddressBook> list = addressBookDao.get(trimmedConstraint);
                results.count = Utils.size(list);
                results.values = list;
            } else {
                results.count =0;
                results.values = Collections.emptyList();
            }
            return results;
        }

        @Override
        protected void publishResults(CharSequence charSequence, FilterResults filterResults) {

            addressLabelListAdapter.setNotifyOnChange(false);
            addressLabelListAdapter.clear();
            if (filterResults.count >0) {
                addressLabelListAdapter.addAll( (List<AddressBook>) filterResults.values);
            }
            addressLabelListAdapter.notifyDataSetChanged();
        }
    };

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
