package com.bethel.mycoolwallet.fragment;


import android.content.Intent;
import android.os.Bundle;
import android.os.Process;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import android.os.Handler;
import android.os.HandlerThread;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.bethel.mycoolwallet.R;
import com.bethel.mycoolwallet.activity.SweepWalletActivity;
import com.bethel.mycoolwallet.fragment.dialog.ProgressDialogFragment;
import com.bethel.mycoolwallet.manager.MyCoolWalletManager;
import com.bethel.mycoolwallet.request.payment.SendCoinsOfflineTask;
import com.bethel.mycoolwallet.request.privkey.DecodeBIP38PrivateKeyTask;
import com.bethel.mycoolwallet.mvvm.view_model.SweepWalletViewModel;
import com.bethel.mycoolwallet.request.privkey.RequestWalletBalanceTask;
import com.bethel.mycoolwallet.utils.Constants;
import com.bethel.mycoolwallet.utils.ViewUtil;
import com.xuexiang.xui.widget.dialog.materialdialog.DialogAction;
import com.xuexiang.xui.widget.dialog.materialdialog.MaterialDialog;

import org.bitcoinj.core.Coin;
import org.bitcoinj.core.DumpedPrivateKey;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.PrefixedChecksummedBytes;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.UTXO;
import org.bitcoinj.crypto.BIP38PrivateKey;
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
      // todo  finishActivity();
    }

    private SweepWalletViewModel viewModel ;

    private final Handler handler = new Handler();
    private HandlerThread backgroundThread;
    private Handler backgroundHandler;

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
    private void maybeDecodeKey() {// todo
        if (null == viewModel.privateKeyToSweep) return;
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
                //super.executeAsyncTask();
            }

            @Override
            protected void runOnCallbackThread(Runnable runnable) {
                handler.post(runnable);
               // super.runOnCallbackThread(runnable);
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
    }

    // todo
    private void updateView() {
    }

    private void setState(final SweepWalletViewModel.State state) {
        viewModel.state = state;

        updateView();
    }

    private void handleDecrypt() {
        handler.post(()-> maybeDecodeKey());
    }

    private void handleSweep() {
        // todo  把纸钱包上的btc转到手机钱包的'新'地址上
        final SendRequest sendRequest =
                SendRequest.emptyWallet(MyCoolWalletManager.INSTANCE.getWallet().freshReceiveAddress());
//        sendRequest.feePerKb = fees.get(FeeCategory.NORMAL);

        new SendCoinsOfflineTask(viewModel.walletToSweep, sendRequest){
            @Override
            protected void onSuccess(Transaction transaction) {
                viewModel.sentTransaction = transaction;
// todo
                MyCoolWalletManager.INSTANCE.processDirectTransaction(transaction);
            }

            @Override
            protected void onInsufficientMoney(Coin missing) {

            }

            @Override
            protected void onEmptyWalletFailed(Exception e) {
                super.onEmptyWalletFailed(e);
            }

            @Override
            protected void onInvalidEncryptionKey() {

            }

            @Override
            protected void onFailure(Exception exception) {

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
        super.onDestroy();
    }

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_sweep_wallet;
    }

}
