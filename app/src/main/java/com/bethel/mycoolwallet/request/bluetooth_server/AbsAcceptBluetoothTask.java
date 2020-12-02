package com.bethel.mycoolwallet.request.bluetooth_server;

import android.bluetooth.BluetoothServerSocket;

import com.bethel.mycoolwallet.manager.MyCoolWalletManager;
import com.bethel.mycoolwallet.request.payment.AbsTask;

import org.bitcoinj.core.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class AbsAcceptBluetoothTask extends AbsTask {
    protected static final Logger log = LoggerFactory.getLogger(AbsAcceptBluetoothTask.class);

    protected final BluetoothServerSocket listeningSocket;
    protected final AtomicBoolean running = new AtomicBoolean(true);

    public AbsAcceptBluetoothTask(final BluetoothServerSocket listeningSocket) {
//        super();
        this.listeningSocket = listeningSocket;
    }

    @Override
    public void run() {
        MyCoolWalletManager.propagate();
        while (running.get()) {
            acceptingLooper();
        }
    }

    // server listening
    protected abstract void acceptingLooper();

    public void stopAccepting() {
        running.set(false);

        try {
            listeningSocket.close();
        } catch (final IOException x) {
            // swallow
        }
    }
    protected abstract boolean handleTx(Transaction tx);

    @Override
    public void executeAsyncTask() {
//        super.executeAsyncTask();
        new Thread(this).start();
    }
}
