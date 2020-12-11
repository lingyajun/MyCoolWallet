package com.bethel.mycoolwallet.fragment.dialog;


import android.app.Dialog;
import android.graphics.Typeface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.bethel.mycoolwallet.CoolApplication;
import com.bethel.mycoolwallet.R;
import com.bethel.mycoolwallet.helper.Configuration;
import com.bethel.mycoolwallet.helper.CoolThreadPool;
import com.bethel.mycoolwallet.manager.MyCoolWalletManager;
import com.bethel.mycoolwallet.request.payment.DeriveKeyTask;
import com.bethel.mycoolwallet.service.BlockChainService;
import com.bethel.mycoolwallet.utils.Constants;
import com.bethel.mycoolwallet.utils.ViewUtil;
import com.bethel.mycoolwallet.utils.WalletUtils;
import com.bethel.mycoolwallet.view.FeeSeekBar;
import com.xuexiang.xui.widget.dialog.materialdialog.DialogAction;
import com.xuexiang.xui.widget.dialog.materialdialog.MaterialDialog;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.core.VerificationException;
import org.bitcoinj.crypto.KeyCrypterException;
import org.bitcoinj.wallet.KeyChain;
import org.bitcoinj.wallet.SendRequest;
import org.bitcoinj.wallet.Wallet;
import org.bouncycastle.crypto.params.KeyParameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import butterknife.BindView;

import static androidx.core.util.Preconditions.checkNotNull;

/**
 * 增加手续费.
 */
public class RaiseFeeDialogFragment extends BaseDialogFragment {
    private static final String TAG = "RaiseFeeDialogFragment";
    private static final String KEY_TRANSACTION = "transaction";

    public static void show(final FragmentManager fm, final Transaction tx) {
        final DialogFragment newFragment = instance(tx);
        newFragment.show(fm,  TAG);
    }

    private static RaiseFeeDialogFragment instance(final Transaction tx) {
        final RaiseFeeDialogFragment fragment = new RaiseFeeDialogFragment();

        final Bundle args = new Bundle();
        args.putByteArray(KEY_TRANSACTION, tx.getTxId().getBytes());
        fragment.setArguments(args);

        return fragment;
    }

    @BindView(R.id.raise_fee_dialog_message)
     TextView messageView;
    @BindView(R.id.raise_fee_dialog_password_group)
    View passwordGroup;
    @BindView(R.id.raise_fee_dialog_password)
    EditText passwordView;
    @BindView(R.id.raise_fee_dialog_bad_password)
    View badPasswordView;
    @BindView(R.id.raise_fee_dialog_fee_seek_bar)
    FeeSeekBar feeSeekBar;

    private TextView positiveButton, negativeButton;
    private Dialog mDialog;

//    private Coin feeRaise = null;
    private Transaction transaction;
    private Wallet wallet;

    private enum State {
        INPUT, DECRYPTING, DONE
    }
    private State state = State.INPUT;

    private static final Logger log = LoggerFactory.getLogger(RaiseFeeDialogFragment.class);

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        wallet = MyCoolWalletManager.INSTANCE.getWallet();

        final Bundle args = getArguments();
        final byte[] txBytes = args.getByteArray(KEY_TRANSACTION);
        final Sha256Hash txHash = Sha256Hash.wrap(txBytes);
        transaction = checkNotNull(wallet.getTransaction( txHash ));

    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        final View view = createAndBindDialogView(R.layout.fragment_raise_fee_dialog);
        final MaterialDialog dialog = new MaterialDialog.Builder(getContext())
                .title(R.string.raise_fee_dialog_title)
                .customView(view, true)
                .positiveText(R.string.raise_fee_dialog_button_raise)
                .negativeText(R.string.button_cancel)
                .cancelable(false)
                .canceledOnTouchOutside(false)
                .show();

        dialog.setOnShowListener(dialogInterface -> {
            positiveButton = dialog.getActionButton(DialogAction.POSITIVE);
            negativeButton = dialog.getActionButton(DialogAction.NEGATIVE);
            positiveButton.setTypeface(Typeface.DEFAULT_BOLD);
            positiveButton.setOnClickListener(view1 -> handleGo());
            negativeButton.setOnClickListener(view1 -> dismissAllowingStateLoss());

            mDialog = dialog;
            passwordView.addTextChangedListener(textWatcher);
            updateView();
        });
        log.info("showing raise fee dialog");
        return dialog;
    }

    private void handleGo() {
        state = State.DECRYPTING;
        updateView();

        if (!wallet.isEncrypted()) {
            doRaiseFee(null);
            return;
        }

        // decrypt
        final String password = passwordView.getText().toString().trim();
        final int scryptIterations = CoolApplication.getApplication().scryptIterationsTarget();
        new DeriveKeyTask(wallet, password, scryptIterations){
            @Override
            protected void onSuccess(KeyParameter encryptionKey, boolean changed) {
                doRaiseFee(encryptionKey);
                if (changed) {
                    CoolThreadPool.execute(()-> WalletUtils.autoBackupWallet(getContext(), wallet));
                }
            }
        }.executeAsyncTask();
    }

    private Coin getFeeRaise() {
        // child-pays-for-parent
        // pay fee for two transactions:
        // The transaction to raise the fee of and the CPFP transaction we're about to create.
        final int size = transaction.getMessageSize() + 192;
        final Coin feePkb = feeSeekBar.getFee();
        final Coin feeRaise = feePkb.multiply(size).divide(1000);
        log.info("feePkb {}, size {} ; feeRaise {}", feePkb, size, feeRaise);

        return feeRaise;
    }

    private void doRaiseFee(final KeyParameter encryptionKey) {
        final Coin feeRaise = getFeeRaise();
        // tx: child-pays-for-parent
        final TransactionOutput outputToSpend = checkNotNull(findSpendableOutput(wallet, transaction, feeRaise));
        final Transaction txToSend = new Transaction(Constants.NETWORK_PARAMETERS);
        final Coin outValue = outputToSpend.getValue().subtract(feeRaise);
        final Address outAddress= wallet.freshAddress(KeyChain.KeyPurpose.CHANGE);
        txToSend.addOutput(outValue, outAddress);
        txToSend.addInput(outputToSpend);
        txToSend.setPurpose(Transaction.Purpose.RAISE_FEE);

        // SendRequest
        final SendRequest request = SendRequest.forTx(txToSend);
        request.aesKey = encryptionKey;

        try {
            // sign
            wallet.signTransaction(request);
            log.info("raise fee: child-pays-for-parent  {}", txToSend);

            // puts it into the pending pool, sets the spent flags
            // and runs the onCoinsSent/onCoinsReceived event listener.
            wallet.commitTx(txToSend);

            BlockChainService.broadcastTransaction(getContext(), txToSend);
            state = State.DONE;
            updateView();
            log.info("raise fee tx has been broadcast~");
            dismiss();
        } catch (Exception e) {
//        } catch (VerificationException | KeyCrypterException e) {
            ViewUtil.setVisibility(badPasswordView, View.VISIBLE);
            state = State.INPUT;
            updateView();

            passwordView.requestFocus();
            log.warn("sign and broadcast error", e);
        }
    }

    private void updateView() {
        if (null==mDialog || !mDialog.isShowing()) return;
        final Coin feeRaise = getFeeRaise();

        final TransactionOutput outputToSpend = null!=feeRaise ?
                findSpendableOutput(wallet, transaction, feeRaise) : null;

        if (null==feeRaise) {
            messageView.setText(R.string.raise_fee_dialog_determining_fee);
            ViewUtil.showView(passwordGroup, false);
        } else if (null == outputToSpend) {
            messageView.setText(R.string.raise_fee_dialog_cant_raise);
            ViewUtil.showView(passwordGroup, false);
        } else {
            messageView.setText(getString(R.string.raise_fee_dialog_message,
                    Configuration.INSTANCE.getFormat().format(feeRaise)));
            ViewUtil.showView(passwordGroup, wallet.isEncrypted());
        }

        switch (state) {
            case INPUT:
                positiveButton.setText(R.string.raise_fee_dialog_button_raise);
                final String password = passwordView.getText().toString().trim();
                final boolean positive = (!wallet.isEncrypted() || !password.isEmpty()) && null!=outputToSpend;
                positiveButton.setEnabled(positive);
                negativeButton.setEnabled(true);
                break;
            case DECRYPTING:
                positiveButton.setText(R.string.raise_fee_dialog_state_decrypting);
                positiveButton.setEnabled(false);
                negativeButton.setEnabled(false);
                break;
            case DONE:
                positiveButton.setText(R.string.raise_fee_dialog_state_done);
                positiveButton.setEnabled(false);
                negativeButton.setEnabled(false);
                break;
        }
    }

    @Override
    public void dismiss() {
        passwordView.setText(null);
        super.dismiss();
    }

    private final TextWatcher textWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            updateView();
            ViewUtil.setVisibility(badPasswordView, View.INVISIBLE);
        }

        @Override
        public void afterTextChanged(Editable editable) {

        }
    };

    public static boolean feeCanLikelyBeRaised(final Wallet wallet, final Transaction transaction) {
        if (transaction.getConfidence().getDepthInBlocks() > 0)
            return false;

        if (WalletUtils.isPayToManyTransaction(transaction))
            return false;

        // We don't know dynamic fees here, so we need to guess.
        if (findSpendableOutput(wallet, transaction, Transaction.DEFAULT_TX_FEE) == null)
            return false;

        return true;
    }

    private static @Nullable
    TransactionOutput findSpendableOutput(final Wallet wallet, final Transaction transaction,
                                          final Coin minimumOutputValue) {
        for (final TransactionOutput output : transaction.getOutputs()) {
            if (output.isMine(wallet) && output.isAvailableForSpending()
                    && output.getValue().isGreaterThan(minimumOutputValue))
                return output;
        }

        return null;
    }
}
