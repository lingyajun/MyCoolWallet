package com.bethel.mycoolwallet.request.dns;

import com.bethel.mycoolwallet.request.payment.AbsTask;

import java.net.InetAddress;
import java.net.UnknownHostException;

public abstract class ResolveDnsTask extends AbsTask {
    private final String hostname;

    public ResolveDnsTask(String hostname) {
        super();
        this.hostname = hostname;
    }

    @Override
    public void run() {
        try {
            final InetAddress address = InetAddress.getByName(hostname);
            runOnCallbackThread(()-> onSuccess(address));
        } catch (UnknownHostException e) {
            runOnCallbackThread(()->onUnknownHost());
        }
    }

    protected abstract void onSuccess(InetAddress address);

    protected abstract void onUnknownHost();
}
