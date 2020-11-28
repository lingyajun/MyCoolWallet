package com.bethel.mycoolwallet.request.payment;

import com.bethel.mycoolwallet.data.payment.PaymentData;

public interface IPaymentRequestListener {
    void onPaymentData(PaymentData data);
    void onFail(int messageResId, Object... messageArgs);
}
