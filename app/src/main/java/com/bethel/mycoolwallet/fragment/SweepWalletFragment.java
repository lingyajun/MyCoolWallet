package com.bethel.mycoolwallet.fragment;


import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Process;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import android.os.Handler;
import android.os.HandlerThread;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.bethel.mycoolwallet.CoolApplication;
import com.bethel.mycoolwallet.R;
import com.bethel.mycoolwallet.activity.CustomCaptureActivity;
import com.bethel.mycoolwallet.activity.SweepWalletActivity;
import com.bethel.mycoolwallet.activity.WebActivity;
import com.bethel.mycoolwallet.data.BlockChainState;
import com.bethel.mycoolwallet.data.payment.PaymentData;
import com.bethel.mycoolwallet.data.tx_list.holder.TransactionViewHolder;
import com.bethel.mycoolwallet.data.tx_list.item.TransactionListItem;
import com.bethel.mycoolwallet.fragment.dialog.ProgressDialogFragment;
import com.bethel.mycoolwallet.helper.Configuration;
import com.bethel.mycoolwallet.helper.SendCoinsHelper;
import com.bethel.mycoolwallet.helper.parser.StringInputParser;
import com.bethel.mycoolwallet.manager.MyCoolWalletManager;
import com.bethel.mycoolwallet.mvvm.view_model.SendCoinsViewModel;
import com.bethel.mycoolwallet.request.payment.SendCoinsOfflineTask;
import com.bethel.mycoolwallet.request.privkey.DecodeBIP38PrivateKeyTask;
import com.bethel.mycoolwallet.mvvm.view_model.SweepWalletViewModel;
import com.bethel.mycoolwallet.request.privkey.RequestWalletBalanceTask;
import com.bethel.mycoolwallet.utils.Constants;
import com.bethel.mycoolwallet.utils.CurrencyTools;
import com.bethel.mycoolwallet.utils.ViewUtil;
import com.bethel.mycoolwallet.view.FeeSeekBar;
import com.xuexiang.xqrcode.XQRCode;
import com.xuexiang.xui.widget.dialog.materialdialog.DialogAction;
import com.xuexiang.xui.widget.dialog.materialdialog.MaterialDialog;

import org.bitcoinj.core.Coin;
import org.bitcoinj.core.DumpedPrivateKey;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.PrefixedChecksummedBytes;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionConfidence;
import org.bitcoinj.core.UTXO;
import org.bitcoinj.core.VerificationException;
import org.bitcoinj.crypto.BIP38PrivateKey;
import org.bitcoinj.utils.MonetaryFormat;
import org.bitcoinj.wallet.SendRequest;
import org.bitcoinj.wallet.Wallet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

import butterknife.BindView;
import butterknife.OnClick;

/**
 * 交换纸钱包上的比特币
 */
public class SweepWalletFragment extends BaseFragment {

    @BindView(R.id.sweep_wallet_fragment_message)
    TextView messageView;
    @BindView(R.id.sweep_wallet_fragment_password_group)
    View passwordViewGroup;
    @BindView(R.id.sweep_wallet_fragment_password)
    EditText passwordView;
    @BindView(R.id.sweep_wallet_fragment_bad_password)
    View badPasswordView;
    @BindView(R.id.sweep_wallet_fragment_balance)
    TextView balanceView;
    @BindView(R.id.sweep_wallet_fragment_hint)
    View hintView;
    @BindView(R.id.transaction_row)
    ViewGroup sweepTransactionViewGroup;
    private TransactionViewHolder sweepTransactionViewHolder;

    @BindView(R.id.sweep_wallet_fragment_fee_seek_bar)
    FeeSeekBar feeSeekBar;

    @BindView(R.id.send_coins_go)
    Button viewGo;
    @BindView(R.id.send_coins_cancel)
    Button viewCancel;

    @OnClick(R.id.send_coins_cancel)
    void onCancleClick() {
        finishActivity();
    }
    @OnClick(R.id.send_coins_go)
    void onConfirmClick() {
      if (null == viewModel.state) return;
      switch (viewModel.state) {
          case DECODE_KEY:
              handleDecrypt();
              break;
          case CONFIRM_SWEEP:
              handleSweep();
              break;
          default: log.info(" unhandled {}", viewModel.state);
      }
    }

    private SweepWalletViewModel viewModel ;

    private final Handler handler = new Handler();
    private HandlerThread backgroundThread;
    private Handler backgroundHandler;

    private MenuItem reloadAction;
    private MenuItem scanAction;

    // 扫描
    private static final int REQUEST_QR_SCAN_CODE = 111;
    private static final Logger log = LoggerFactory.getLogger(SweepWalletFragment.class);

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        viewModel = getViewModel(SweepWalletViewModel.class);
        viewModel.progress.observe(this, new ProgressDialogFragment.Observer(getFragmentManager()));

        initThread();

        if (null == savedInstanceState) {
            checkSweepKey();
        }
    }

    private void initThread() {
        backgroundThread = new HandlerThread("backgroundThread", Process.THREAD_PRIORITY_BACKGROUND);
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
    }

    private void checkSweepKey() {
        final Intent intent = getActivity().getIntent();
        if (null!=intent && intent.hasExtra(SweepWalletActivity.INTENT_EXTRA_KEY)) {
            viewModel.privateKeyToSweep = (PrefixedChecksummedBytes) intent.getSerializableExtra(SweepWalletActivity.INTENT_EXTRA_KEY);
            //  decode key
            handler.post(()-> maybeDecodeKey());
        }
    }
    private void maybeDecodeKey() {
        if (null == viewModel.privateKeyToSweep) return;
        if (SweepWalletViewModel.State.DECODE_KEY != viewModel.state) return;

        final PrefixedChecksummedBytes privKey = viewModel.privateKeyToSweep;
        if (privKey instanceof DumpedPrivateKey) {
            final ECKey key = ((DumpedPrivateKey) privKey).getKey();
              askConfirmSweep(key);
            return;
        }

        if (privKey instanceof BIP38PrivateKey) {
            ViewUtil.setVisibility(badPasswordView, View.INVISIBLE);
            final String password = passwordView.getText().toString().trim();
            passwordView.setText(null);
            if (password.isEmpty()) return;

            decrypt( (BIP38PrivateKey) privKey, password);
            return;
        }

        log.error("cannot handle type: {}" , viewModel.privateKeyToSweep.getClass().getName());
    }

    private void decrypt(final BIP38PrivateKey encryptedKey, final String passphrase) {
        new DecodeBIP38PrivateKeyTask(encryptedKey, passphrase){
            @Override
            protected void onSuccess(ECKey decryptedKey) {
                log.info("successfully decoded BIP38 private key");

                viewModel.progress.setValue(null);

                askConfirmSweep(decryptedKey);
            }

            @Override
            protected void onBadPassphrase(String message) {
                log.info("failed decoding BIP38 private key (bad password)");

                viewModel.progress.setValue(null);

                ViewUtil.setVisibility(badPasswordView, View.VISIBLE);
                passwordView.requestFocus();
            }

            @Override
            public void executeAsyncTask() {
                backgroundHandler.post(this);
            }

            @Override
            protected void runOnCallbackThread(Runnable runnable) {
                handler.post(runnable);
            }
        }.executeAsyncTask();
    }

    private void askConfirmSweep(final ECKey key) {
        viewModel.walletToSweep = Wallet.createBasic(Constants.NETWORK_PARAMETERS);
        viewModel.walletToSweep.importKey(key);

        setState(SweepWalletViewModel.State.CONFIRM_SWEEP);

        // delay until fragment is resumed
        handler.post(()-> requestWalletBalance());
    }

    private void requestWalletBalance() {
        if (null == viewModel.walletToSweep) return;
        viewModel.progress.setValue(getString(R.string.sweep_wallet_fragment_request_wallet_balance_progress));
        // request Electrum Server
        final ECKey key = viewModel.walletToSweep.getImportedKeys().iterator().next();
        new RequestWalletBalanceTask(getContext().getAssets(), key) {
            @Override
            protected void onResult(Set<UTXO> utxos) {
                viewModel.progress.setValue(null);
                viewModel.mergeUTXOsResponse(utxos);

                log.info("built wallet to sweep:\n{}",
                        viewModel.walletToSweep.toString(false, false, null, true, false, null));

                updateView();
            }

            @Override
            protected void onFail(int messageResId, Object... messageArgs) {
                viewModel.progress.setValue(null);
                //  alert retry
                MaterialDialog dialog = new MaterialDialog.Builder(getContext())
                        .title(R.string.sweep_wallet_fragment_request_wallet_balance_failed_title)
                        .content(getString(messageResId, messageArgs))
                        .positiveText(R.string.button_retry)
                        .negativeText(R.string.button_dismiss)
                        .show();
                dialog.getActionButton(DialogAction.POSITIVE).setOnClickListener(
                        view -> requestWalletBalance());

                log.warn("RequestWalletBalance fail: {}", getString(messageResId, messageArgs));
            }

            @Override
            public void executeAsyncTask() {
                backgroundHandler.post(this);
            }

            @Override
            protected void runOnCallbackThread(Runnable runnable) {
                handler.post(runnable);
            }
        }.executeAsyncTask();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        feeSeekBar.setDefaultValue(2020);
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.sweep_wallet_fragment_options, menu);

        reloadAction = menu.findItem(R.id.sweep_wallet_options_reload);
        scanAction = menu.findItem(R.id.sweep_wallet_options_scan);

        final PackageManager pm = getContext().getPackageManager();
        scanAction.setVisible(pm.hasSystemFeature(PackageManager.FEATURE_CAMERA)
                || pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT));
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        switch (itemId) {
            case R.id.sweep_wallet_options_reload:
                handleReload();
                break;
            case R.id.sweep_wallet_options_scan:
                CustomCaptureActivity.start(this, REQUEST_QR_SCAN_CODE);
                break;
                default: return  super.onOptionsItemSelected(item);
        }
        return true;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        handler.post(()-> onActivityResultResumed(requestCode, resultCode, data));
        super.onActivityResult(requestCode, resultCode, data);
    }
    private void onActivityResultResumed(final int requestCode, final int resultCode, final Intent data) {
        if (REQUEST_QR_SCAN_CODE == requestCode && Activity.RESULT_OK == resultCode) {
            final Bundle bundle = data.getExtras();
            final String result = null!=bundle? bundle.getString(XQRCode.RESULT_DATA):null;
            if (TextUtils.isEmpty(result)) return;
            new StringInputParser(result) {
                @Override
                public void error(int messageResId, Object... messageArgs) {
                    SendCoinsHelper.dialog(getContext(),null,
                            R.string.button_scan, messageResId, messageArgs);
                }

                @Override
                protected void handlePrivateKey(PrefixedChecksummedBytes key) {
                    // super.handlePrivateKey(key);
                    viewModel.privateKeyToSweep = key;
                    setState(SweepWalletViewModel.State.DECODE_KEY);
                    maybeDecodeKey();
                }

                @Override
                public void handlePaymentData(PaymentData data) {
                    cannotClassify(result);
                }

                @Override
                protected void handleWebUrl(String link) {
                    WebActivity.start(getContext(), link);
                }

                @Override
                public void handleDirectTransaction(Transaction transaction) throws VerificationException {
                    cannotClassify(result);
                }
            }.parse();
        }
    }

    private void handleReload() {
        if (viewModel.walletToSweep == null) return;

        requestWalletBalance();
    }

    private void updateView() {
        final MonetaryFormat btcFormat = Configuration.INSTANCE.getFormat();
        // balanceView
        if (null!=viewModel.walletToSweep) {
            final CharSequence seq = CurrencyTools.format(btcFormat, false,
                    viewModel.walletToSweep.getBalance(Wallet.BalanceType.ESTIMATED));

            final SpannableStringBuilder balance = new SpannableStringBuilder(seq);
            balance.insert(0, ": ");
            balance.insert(0, getString(R.string.sweep_wallet_fragment_balance));

            balanceView.setText(balance);
            ViewUtil.showView(balanceView, true);
        } else {
            ViewUtil.showView(balanceView, false);
        }

        // sweepTransactionViewHolder
        if (null!=viewModel.sentTransaction) {
            final TransactionListItem item = new TransactionListItem(getContext(), viewModel.sentTransaction,
                    MyCoolWalletManager.INSTANCE.getWallet(), null, btcFormat,
                    CoolApplication.getApplication().maxConnectedPeers(), false);
            if (null==sweepTransactionViewHolder) {
                sweepTransactionViewHolder = new TransactionViewHolder(sweepTransactionViewGroup);
            }
            sweepTransactionViewHolder.bind(item);
            ViewUtil.showView(sweepTransactionViewGroup, true);
        } else {
            ViewUtil.showView(sweepTransactionViewGroup, false);
        }

        // edit
        final boolean showEdit =  viewModel.state == SweepWalletViewModel.State.DECODE_KEY
                && viewModel.privateKeyToSweep != null;
        ViewUtil.showView(passwordViewGroup, showEdit);
        final boolean showFee =  viewModel.state == SweepWalletViewModel.State.DECODE_KEY
                ||  viewModel.state == SweepWalletViewModel.State.CONFIRM_SWEEP;
        ViewUtil.showView(feeSeekBar, showFee);
        final boolean showHint =viewModel.state == SweepWalletViewModel.State.DECODE_KEY
                && viewModel.privateKeyToSweep == null;
        ViewUtil.showView(hintView, showHint);

        // messageView
        if (viewModel.state == SweepWalletViewModel.State.DECODE_KEY
                && viewModel.privateKeyToSweep == null) {
            ViewUtil.showView(messageView, true);
            messageView.setText(R.string.sweep_wallet_fragment_wallet_unknown);
        } else if (viewModel.state == SweepWalletViewModel.State.DECODE_KEY) {
            // viewModel.privateKeyToSweep != null
            ViewUtil.showView(messageView, true);
            messageView.setText(R.string.sweep_wallet_fragment_encrypted);
        } else {
            ViewUtil.showView(messageView, false);
        }

            //  enable actions
        if (null!=reloadAction) {
            boolean reload =  viewModel.state == SweepWalletViewModel.State.CONFIRM_SWEEP
                    && viewModel.walletToSweep != null;
            reloadAction.setEnabled(reload);
        }
        if (null!=scanAction) {
            boolean scan = viewModel.state == SweepWalletViewModel.State.DECODE_KEY
                    || viewModel.state == SweepWalletViewModel.State.CONFIRM_SWEEP;
            scanAction.setEnabled(scan);
        }

        updateBottomActionsView();
    }

    private void updateBottomActionsView() {
        viewCancel.setEnabled(viewModel.state != SweepWalletViewModel.State.PREPARATION);

        if (null == viewModel.state) return;
        switch (viewModel.state) {
            case DECODE_KEY:
                viewCancel.setText(R.string.button_cancel);
                viewGo.setText(R.string.sweep_wallet_fragment_button_decrypt);
                viewGo.setEnabled(viewModel.privateKeyToSweep != null);
                break;
            case CONFIRM_SWEEP:
                viewCancel.setText(R.string.button_cancel);
                viewGo.setText(R.string.sweep_wallet_fragment_button_sweep);
                final Coin fee = feeSeekBar.getFee();
                final boolean enable = viewModel.walletToSweep != null && null!=fee
                        && viewModel.walletToSweep.getBalance(Wallet.BalanceType.ESTIMATED).signum() > 0;
                viewGo.setEnabled(enable);
                break;
            case PREPARATION:
                viewCancel.setText(R.string.button_cancel);
                viewGo.setText(R.string.send_coins_preparation_msg);
                viewGo.setEnabled(false);
                break;
            case SENDING:
                viewCancel.setText(R.string.send_coins_fragment_button_back);
                viewGo.setText(R.string.send_coins_sending_msg);
                viewGo.setEnabled(false);
                break;
            case SENT:
                viewCancel.setText(R.string.send_coins_fragment_button_back);
                viewGo.setText(R.string.send_coins_sent_msg);
                viewGo.setEnabled(false);
                break;
            case FAILED:
                viewCancel.setText(R.string.send_coins_fragment_button_back);
                viewGo.setText(R.string.send_coins_failed_msg);
                viewGo.setEnabled(false);
                break;
        }
    }

    private void setState(final SweepWalletViewModel.State state) {
        viewModel.state = state;

        updateView();
    }

    private void handleDecrypt() {
        handler.post(()-> maybeDecodeKey());
    }

    private void handleSweep() {
        setState(SweepWalletViewModel.State.PREPARATION);
        //   把纸钱包上的btc转到手机钱包的'新'地址上
        final SendRequest sendRequest =
                SendRequest.emptyWallet(MyCoolWalletManager.INSTANCE.getWallet().freshReceiveAddress());
        sendRequest.feePerKb = feeSeekBar.getFee();

        new SendCoinsOfflineTask(viewModel.walletToSweep, sendRequest){
            @Override
            protected void onSuccess(Transaction transaction) {
                viewModel.sentTransaction = transaction;
                viewModel.sentTransaction.getConfidence().addEventListener(sweepTxConfidenceListener);

                setState(SweepWalletViewModel.State.SENDING);
                MyCoolWalletManager.INSTANCE.processDirectTransaction(transaction);
            }

            @Override
            protected void onInsufficientMoney(Coin missing) {
                setState(SweepWalletViewModel.State.FAILED);
                SendCoinsHelper.dialog(getContext(), null,
                        R.string.sweep_wallet_fragment_insufficient_money_title,
                        R.string.sweep_wallet_fragment_insufficient_money_msg);
            }

            @Override
            protected void onEmptyWalletFailed(Exception e) {
                setState(SweepWalletViewModel.State.FAILED);
                SendCoinsHelper.dialog(getContext(), null,
                        R.string.sweep_wallet_fragment_insufficient_money_title,
                        R.string.sweep_wallet_fragment_insufficient_money_msg);
            }

            @Override
            protected void onInvalidEncryptionKey() {
                log.error(" onInvalidEncryptionKey ");
            }

            @Override
            protected void onFailure(Exception exception) {
                setState(SweepWalletViewModel.State.FAILED);
                SendCoinsHelper.dialogWarn(getContext(), R.string.send_coins_error_msg , exception.toString());
            }

            @Override
            public void executeAsyncTask() {
                backgroundHandler.post(this);
            }

            @Override
            protected void runOnCallbackThread(Runnable runnable) {
                handler.post(runnable);
            }
        }.executeAsyncTask();
    }

    @Override
    public void onDestroy() {
        backgroundThread.getLooper().quit();
        handler.removeCallbacksAndMessages(null);

        if (null != viewModel.sentTransaction) {
            viewModel.sentTransaction.getConfidence().removeEventListener(sweepTxConfidenceListener);
        }
        super.onDestroy();
    }

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_sweep_wallet;
    }

    private final TransactionConfidence.Listener sweepTxConfidenceListener = new TransactionConfidence.Listener() {
        @Override
        public void onConfidenceChanged(final TransactionConfidence confidence, final ChangeReason reason) {
            runOnUiThread(()-> {
                if (!isResumed()) return;
                log.info(" check confidence {}, {}", confidence.getConfidenceType(), viewModel.sentTransaction.getConfidence().getConfidenceType());

                final TransactionConfidence.ConfidenceType confidenceType = confidence.getConfidenceType();
                final int numBroadcastPeers = confidence.numBroadcastPeers();

                if (SweepWalletViewModel.State.SENDING == viewModel.state) {
                    // setState
                    if (TransactionConfidence.ConfidenceType.DEAD== confidenceType) {
                        setState(SweepWalletViewModel.State.FAILED);
                    } else if ( TransactionConfidence.ConfidenceType.BUILDING ==confidenceType
                                 || 1<numBroadcastPeers) {
                        setState(SweepWalletViewModel.State.SENT);
                    }
                }

                if (TransactionConfidence.ConfidenceType.PENDING == confidenceType
                     && ChangeReason.SEEN_PEERS == reason) {
                    // play sound effect
                    SendCoinsHelper.playSoundEffect(getContext(), numBroadcastPeers);
                }
            });
        }
    };

}
