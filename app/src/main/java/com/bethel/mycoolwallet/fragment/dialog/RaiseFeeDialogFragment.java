package com.bethel.mycoolwallet.fragment.dialog;


import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bethel.mycoolwallet.R;
import com.bethel.mycoolwallet.utils.WalletUtils;

import org.bitcoinj.core.Coin;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.wallet.Wallet;

/**
 * A simple {@link Fragment} subclass.
 */
public class RaiseFeeDialogFragment extends BaseDialogFragment {


    public RaiseFeeDialogFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_raise_fee_dialog, container, false);
    }

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
