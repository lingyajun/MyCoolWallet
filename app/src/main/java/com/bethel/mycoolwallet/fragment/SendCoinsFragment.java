package com.bethel.mycoolwallet.fragment;


import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import android.text.TextUtils;
import android.util.Log;
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

import com.bethel.mycoolwallet.R;
import com.bethel.mycoolwallet.activity.CustomCaptureActivity;
import com.bethel.mycoolwallet.activity.WebActivity;
import com.bethel.mycoolwallet.data.ExchangeRateBean;
import com.bethel.mycoolwallet.helper.PaymentHelper;
import com.bethel.mycoolwallet.helper.parser.InputParserOld;
import com.bethel.mycoolwallet.interfaces.IDeriveKeyCallBack;
import com.bethel.mycoolwallet.interfaces.IQrScan;
import com.bethel.mycoolwallet.interfaces.IRequestPassphrase;
import com.bethel.mycoolwallet.interfaces.ISignPaymentCallback;
import com.bethel.mycoolwallet.mvvm.view_model.SendCoinsViewModel;
import com.bethel.mycoolwallet.service.BlockChainService;
import com.bethel.mycoolwallet.utils.Constants;
import com.bethel.mycoolwallet.utils.CurrencyTools;
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
 */
public class SendCoinsFragment extends BaseFragment implements IQrScan {
    /**
     * 扫描跳转Activity RequestCode
     */
    private static final int REQUEST_QR_SCAN_CODE = 111;
    private static final Logger log = LoggerFactory.getLogger(SendCoinsFragment.class);

    private SendCoinsViewModel viewModel;

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
    protected void onSendClick() {
//        viewModel.toAddress = null;
        final Coin amount = (Coin) coinAmountView.getAmount();
      final String str = receivingAddressView.getText().toString();
       if (null==amount ||Coin.ZERO == amount || TextUtils.isEmpty(str)) {
           return;
       }
        try {
            final Address toAddress = Address.fromString(Constants.NETWORK_PARAMETERS, str);
            viewModel.toAddress = toAddress;
        } catch (AddressFormatException e) {
            XToast.error(getContext(), "please input right address").show();
        }
        if (null == viewModel.toAddress) return;
        /** todo
         * 「0。 生成解密钱包所需要格式的'密钥', 用于钱包支付密码解密」
         * 1. 构造 SendRequest
         * 2。 SendRequest 参数配置
         * 3。 对交易进行签名
         * 4。广播签名后的交易数据
         */

       final Wallet wallet = viewModel.wallet.getValue();
        if (null==wallet) return;
        if (wallet.isEncrypted()) {
            // get decrypt wallet key
            final String password = privateKeyPasswordView.getText().toString();
            if (TextUtils.isEmpty(password)) {
                // todo alert
                return;
            }
            AsyncTask.execute(()->{
                PaymentHelper.deriveKey4DescryptWalet(wallet, password, 0, new IDeriveKeyCallBack() {
                    @Override
                    public void onSuccess(KeyParameter encryptKey, boolean isWalletChanged) {
                        if (isWalletChanged) {
                            // todo restore wallet
                        }
                        runOnUiThread(()-> buildSendRequest(amount, encryptKey));
                    }

                    @Override
                    public void onFailed(String error) {
                        runOnUiThread(()-> XToast.error(getContext(), error).show());
                    }
                });
            });
        } else {
            // build SendRequest
            buildSendRequest(amount, null);
        }
//        XToast.warning(getContext(), "send coins!  " + feeSeekBar.getSelectedNumber()).show();
    }

    private void buildSendRequest(Coin amount, KeyParameter aesKey) {
        Wallet wallet = viewModel.wallet.getValue();
        SendRequest request = PaymentHelper.buildSendRequest(viewModel.toAddress, amount);
        //  SendRequest 参数配置
        // boolean emptyWallet ,Coin feePerKb,String memo , ExchangeRate exchangeRate,  KeyParameter aesKey
        request. emptyWallet = wallet.getBalance(Wallet.BalanceType.AVAILABLE).equals(amount);
        request. feePerKb = Coin.valueOf(feeSeekBar.getSelectedNumber());
        request. memo = null;
        ExchangeRateBean rateBean = viewModel.exchangeRate.getValue();
        request. exchangeRate = null!=rateBean? rateBean.rate: null;
        request.aesKey = aesKey;

        // todo check if  amount > fee

        // 对交易进行签名
        AsyncTask.execute(()-> PaymentHelper.signPayment(wallet, request, iSignPaymentCallback));
    }

    private  final ISignPaymentCallback iSignPaymentCallback = new ISignPaymentCallback() {
        @Override
        public void onSuccess(final Transaction tx) {
            Wallet wallet = viewModel.wallet.getValue();
            log.info("ISignPaymentCallback, fee {},  sent {},  toMe {}", tx.getFee(),
                    tx.getValueSentFromMe(wallet), tx.getValueSentToMe(wallet));

            runOnUiThread(()->{
                viewModel.sentTransaction = tx;
                //  监听 交易的状态变化/事件
                TransactionConfidence confidence = tx.getConfidence();
                confidence.addEventListener(transactionListener);
                // 广播签名后的交易数据
                BlockChainService.broadcastTransaction(getContext(), tx);

                // todo http 发送交易； Bluetooth 发送交易

            });
        }

        @Override
        public void onFailed(String message) {
        }
    };

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
        Log.d("tttttt", "onViewCreated, feeSeekBar: "+ feeSeekBar.getSelectedNumber());
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

//        final MenuItem feeCategoryAction = menu.findItem(R.id.send_coins_options_fee_category);
//        feeCategoryAction.setEnabled(viewModel.state == SendCoinsViewModel.State.INPUT);
//        if (viewModel.feeCategory == FeeCategory.ECONOMIC)
//            menu.findItem(R.id.send_coins_options_fee_category_economic).setChecked(true);
//        else if (viewModel.feeCategory == FeeCategory.NORMAL)
//            menu.findItem(R.id.send_coins_options_fee_category_normal).setChecked(true);
//        else if (viewModel.feeCategory == FeeCategory.PRIORITY)
//            menu.findItem(R.id.send_coins_options_fee_category_priority).setChecked(true);

        super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        switch (itemId) {
            case R.id.send_coins_options_scan:
                    startScan(null);
                break;
            case R.id.send_coins_options_fee_category_economic:
                break;
            case R.id.send_coins_options_fee_category_normal:
                break;
            case R.id.send_coins_options_fee_category_priority:
                break;
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
        //处理二维码扫描结果
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
                    InputParserOld.StringInputParser parser = new InputParserOld.StringInputParser(result) {

                        @Override
                        protected void error(int messageResId, Object... messageArgs) {

                        }

                        @Override
                        protected void handleWebUrl(String url) {
                            WebActivity.start(getActivity(), url);
                        }

                        @Override
                        protected void requestPassphrase(IRequestPassphrase callback) {

                        }

                        @Override
                        protected void handlePaymentAddress(Address address) {

                        }

                        @Override
                        protected void handleDirectTransaction(Transaction transaction) throws VerificationException {

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
