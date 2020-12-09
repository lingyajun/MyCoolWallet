package com.bethel.mycoolwallet.mvvm.view_model;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.MutableLiveData;

import com.bethel.mycoolwallet.utils.Constants;
import com.bethel.mycoolwallet.utils.Utils;
import com.google.common.collect.ComparisonChain;

import org.bitcoinj.core.Coin;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.PrefixedChecksummedBytes;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionConfidence;
import org.bitcoinj.core.TransactionInput;
import org.bitcoinj.core.TransactionOutPoint;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.core.UTXO;
import org.bitcoinj.wallet.Wallet;
import org.bitcoinj.wallet.WalletTransaction;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class SweepWalletViewModel extends BaseViewModel {
    public final MutableLiveData<String> progress = new MutableLiveData<>();

    public State state = null;

    public @Nullable
    PrefixedChecksummedBytes privateKeyToSweep = null;

    public @Nullable
    Wallet walletToSweep = null;

    public @Nullable
    Transaction sentTransaction = null;

    public SweepWalletViewModel(@NonNull Application app) {
        super(app);
    }


    public void mergeUTXOsResponse(final Set<UTXO> utxos) {
        if (utxos == null|| this.walletToSweep ==null) {
            return;
        }
        final Set<Transaction> txSet = application.getWallet().getTransactions(false);
        final Set<UTXO> sortedUtxos = new TreeSet<>(UTXO_COMPARATOR);

        for (UTXO utxo: utxos ) {
            if (!spendBy(txSet, utxo)) {
                sortedUtxos.add(utxo);
            }
        }

        // Fake transaction funding the wallet to sweep.
        final Map<Sha256Hash, Transaction> fakeTxMap = new HashMap<>();
        for (UTXO utxo: sortedUtxos ) {
            Transaction fakeTx = fakeTxMap.get(utxo.getHash());
            if (null==fakeTx) {
                fakeTx = FakeTransaction.generateFakeTransaction(utxo);
                fakeTxMap.put(fakeTx.getTxId(), fakeTx);
            }

            final TransactionOutput fakeOutput = new TransactionOutput(Constants.NETWORK_PARAMETERS,
                    fakeTx, utxo.getValue(), utxo.getScript().getProgram());

            while (fakeTx.getOutputs().size() < utxo.getIndex()) {
                final TransactionOutput output = new TransactionOutput(Constants.NETWORK_PARAMETERS,
                        fakeTx, Coin.NEGATIVE_SATOSHI, new byte[]{});
                fakeTx.getOutputs().add(output);
            } // end while

            fakeTx.getOutputs().add(fakeOutput); // we will spend later.
        } // end for

        this.walletToSweep.clearTransactions(0);
        for (Transaction tx: fakeTxMap.values()  ) {
            final WalletTransaction wTx = new WalletTransaction(WalletTransaction.Pool.UNSPENT, tx);
            this.walletToSweep.addWalletTransaction(wTx);
        }
    }

    private boolean spendBy(final Set<Transaction> txSet, final UTXO utxo) {
        for (Transaction tx: txSet   ) {
            final List<TransactionInput> inputList = tx.getInputs();
            for (TransactionInput input: inputList   ) {
                final TransactionOutPoint outPoint = input.getOutpoint();
                if (outPoint.getIndex()==utxo.getIndex() &&
                        Utils.equals(outPoint.getHash(), utxo.getHash())) {
                    return true;
                }
            }
        }
        return false;
    }

    private static final Comparator<UTXO> UTXO_COMPARATOR = new Comparator<UTXO>() {
        @Override
        public int compare(final UTXO lhs, final UTXO rhs) {
            return ComparisonChain.start().compare(lhs.getHash(), rhs.getHash()).compare(lhs.getIndex(), rhs.getIndex())
                    .result();
        }
    };

    private static class FakeTransaction extends Transaction {
        private final Sha256Hash txId, wTxId;

        private static FakeTransaction generateFakeTransaction(UTXO utxo) {
            FakeTransaction fakeTx = new FakeTransaction(Constants.NETWORK_PARAMETERS,
                    utxo.getHash(), utxo.getHash());
            fakeTx.getConfidence().setConfidenceType(TransactionConfidence.ConfidenceType.BUILDING);
            return fakeTx;
        }

        private FakeTransaction(final NetworkParameters params, final Sha256Hash txId, final Sha256Hash wTxId) {
            super(params);
            this.txId = txId;
            this.wTxId = wTxId;
        }

        @Override
        public Sha256Hash getTxId() {
            return txId;
        }

        @Override
        public Sha256Hash getWTxId() {
            return wTxId;
        }
    }


    public enum State {
        DECODE_KEY, // ask for password
        CONFIRM_SWEEP, // displays balance and asks for confirmation
        PREPARATION, SENDING, SENT, FAILED // sending states
    }

}
