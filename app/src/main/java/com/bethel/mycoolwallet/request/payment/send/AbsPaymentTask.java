package com.bethel.mycoolwallet.request.payment.send;

import com.bethel.mycoolwallet.request.payment.AbsTask;

public abstract class AbsPaymentTask extends AbsTask {
    protected final IPaymentTaskCallback callback;
    public AbsPaymentTask(IPaymentTaskCallback callback) {
        super();
        this.callback = callback;
    }

    public void onFail(int messageResId, Object... messageArgs) {
        if (null!=callback) runOnCallbackThread(()-> callback.onFail(messageResId, messageArgs));
    }

    public void onResult(boolean ack) {
        if (null!=callback) runOnCallbackThread(()-> callback.onResult(ack));
    }
}
