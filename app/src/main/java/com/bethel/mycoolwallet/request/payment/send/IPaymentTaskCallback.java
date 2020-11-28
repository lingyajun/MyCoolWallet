package com.bethel.mycoolwallet.request.payment.send;

public interface IPaymentTaskCallback {
    void onResult(boolean ack);

    void onFail(int messageResId, Object... messageArgs);
}
