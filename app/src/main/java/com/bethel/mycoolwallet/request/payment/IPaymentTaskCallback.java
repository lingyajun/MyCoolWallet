package com.bethel.mycoolwallet.request.payment;

public interface IPaymentTaskCallback {
    void onResult(boolean ack);

    void onFail(int messageResId, Object... messageArgs);
}
