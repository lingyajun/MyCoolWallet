package com.bethel.mycoolwallet.helper.parser;

import com.bethel.mycoolwallet.data.payment.PaymentData;

public interface IInputParser {
    void parse();
    void error(int messageResId, Object... messageArgs);

    void handlePaymentData(PaymentData data);

}
