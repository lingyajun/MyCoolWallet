package com.bethel.mycoolwallet.interfaces;

import org.bitcoinj.core.Transaction;

public interface ISignPaymentCallback {
    void onSuccess(Transaction tx);

    /* todo --

     * @throws InsufficientMoneyException if the request could not be completed due to not enough balance.
     * @throws IllegalArgumentException if you try and complete the same SendRequest twice
     * @throws DustySendRequested if the resultant transaction would violate the dust rules.
     * @throws CouldNotAdjustDownwards if emptying the wallet was requested and the output can't be shrunk for fees without violating a protocol rule.
     * @throws ExceededMaxTransactionSize if the resultant transaction is too big for Bitcoin to process.
     * @throws MultipleOpReturnRequested if there is more than one OP_RETURN output for the resultant transaction.
     */
    void onFailed(String message);
}
