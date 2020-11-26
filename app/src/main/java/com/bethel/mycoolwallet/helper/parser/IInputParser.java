package com.bethel.mycoolwallet.helper.parser;

import com.bethel.mycoolwallet.data.payment.PaymentData;

import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.VerificationException;

public interface IInputParser {
    void parse();
    void error(int messageResId, Object... messageArgs);

    void handlePaymentData(PaymentData data);

    void handleDirectTransaction(Transaction transaction) throws VerificationException;

}
