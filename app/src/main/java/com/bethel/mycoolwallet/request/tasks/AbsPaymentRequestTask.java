package com.bethel.mycoolwallet.request.tasks;

import com.bethel.mycoolwallet.data.payment.PaymentData;

public abstract class AbsPaymentRequestTask implements IPaymentRequestListener, Runnable {
    protected final IPaymentRequestListener listener;

    public AbsPaymentRequestTask(IPaymentRequestListener listener) {
        this.listener = listener;
    }

    @Override
    public void onFail(int messageResId, Object... messageArgs) {
        if (null!= listener) listener.onFail(messageResId, messageArgs);
    }

    @Override
    public void onPaymentData(PaymentData data) {
        if (null!= listener) listener.onPaymentData(data);
    }
}
