package com.bethel.mycoolwallet.request.bluetooth_server;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;

import com.bethel.mycoolwallet.utils.BluetoothTools;
import com.bethel.mycoolwallet.utils.Constants;
import com.bethel.mycoolwallet.utils.Utils;

import org.bitcoin.protocols.payments.Protos;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.protocols.payments.PaymentProtocol;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * server : AcceptPaymentProtocolTask
 * client : BluetoothPaymentTask
 */
public abstract class AcceptPaymentProtocolTask extends AbsAcceptBluetoothTask {
    public AcceptPaymentProtocolTask(final BluetoothAdapter adapter) throws IOException {
        super(generateServer(adapter));
    }

    @Override
    protected void acceptingLooper() {
        try {
            final BluetoothSocket socket = listeningSocket.accept();
            final DataInputStream is = new DataInputStream(socket.getInputStream());
            final DataOutputStream os = new DataOutputStream(socket.getOutputStream());
            log.info("accepted payment protocol bluetooth connection");

            boolean ack = true;
            final Protos.Payment payment = Protos.Payment.parseDelimitedFrom(is);

            final List<Transaction> list =
                    PaymentProtocol.parseTransactionsFromPaymentMessage(Constants.NETWORK_PARAMETERS, payment);
            log.debug("got payment message: {}" , Utils.size(list));

            for (Transaction tx: list ) {
                if (!handleTx(tx)) {
                    ack = false;
                }
            }

            final String memo = ack ? "ack" : "nack";
            log.info("sending {} via bluetooth", memo);

            final Protos.PaymentACK paymentACK = PaymentProtocol.createPaymentAck(payment, memo);
            paymentACK.writeDelimitedTo(os);
        } catch (IOException e) {
            log.info("exception in bluetooth accept loop", e);
        }

    }

    private static BluetoothServerSocket generateServer(final BluetoothAdapter adapter) throws IOException {
        return  adapter.listenUsingInsecureRfcommWithServiceRecord(
                BluetoothTools.BIP70_PAYMENT_PROTOCOL_NAME,
                BluetoothTools.BIP70_PAYMENT_PROTOCOL_UUID);
    }

}
