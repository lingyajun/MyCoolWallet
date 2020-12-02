package com.bethel.mycoolwallet.request.bluetooth_server;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.view.SurfaceControl;

import com.bethel.mycoolwallet.manager.MyCoolWalletManager;
import com.bethel.mycoolwallet.utils.BluetoothTools;
import com.bethel.mycoolwallet.utils.Constants;

import org.bitcoinj.core.ProtocolException;
import org.bitcoinj.core.Transaction;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public abstract class AcceptClassicBluetoothTask extends AbsAcceptBluetoothTask {
    public AcceptClassicBluetoothTask(final BluetoothAdapter adapter) throws IOException {
        super( generateServer(adapter));
    }

    @Override
    protected void acceptingLooper() {
        try {
            final  BluetoothSocket socket = listeningSocket.accept();
            final DataInputStream is = new DataInputStream(socket.getInputStream());
            final DataOutputStream os = new DataOutputStream(socket.getOutputStream());
            log.info("accepted classic bluetooth connection");

            boolean ack = true;
            final int num = is.readInt();
            for (int i = 0; i < num; i++) {
                final int size = is.readInt();
                final byte[] msg = new byte[size];
                is.readFully(msg);

                try { //  decode message
                    final Transaction tx = new Transaction(Constants.NETWORK_PARAMETERS, msg);
                    if (!handleTx(tx)) {
                        ack = false;
                    }
                } catch (ProtocolException e) {
                    log.info("cannot decode message received via bluetooth", e);
                    ack = false;
                }
            } // end for()

            os.writeBoolean(ack);

        } catch (IOException e) {
            log.info("exception in bluetooth accept loop", e);
        }
    }

    private static BluetoothServerSocket generateServer(final BluetoothAdapter adapter) throws IOException {
        return  adapter.listenUsingInsecureRfcommWithServiceRecord(
                BluetoothTools.CLASSIC_PAYMENT_PROTOCOL_NAME,
                BluetoothTools.CLASSIC_PAYMENT_PROTOCOL_UUID);
    }
}
