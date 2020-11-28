package com.bethel.mycoolwallet.request.payment;

import com.bethel.mycoolwallet.data.payment.PaymentData;

public abstract class AbsPaymentRequestTask extends AbsTask {
    protected final IPaymentRequestListener listener;

    public AbsPaymentRequestTask(IPaymentRequestListener listener) {
        super();
        this.listener = listener;
    }


    protected void onFail(int messageResId, Object... messageArgs) {
        if (null!= listener)
            runOnCallbackThread(() -> listener.onFail(messageResId, messageArgs));
    }


    protected void onPaymentData(PaymentData data) {
        if (null!= listener)
            runOnCallbackThread(() -> listener.onPaymentData(data));
    }
}
