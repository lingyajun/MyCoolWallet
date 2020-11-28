package com.bethel.mycoolwallet.request.payment;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

import androidx.core.util.Preconditions;

import com.bethel.mycoolwallet.R;
import com.bethel.mycoolwallet.utils.BluetoothTools;

import org.bitcoin.protocols.payments.Protos;
import org.bitcoinj.protocols.payments.PaymentProtocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * 通过 BLE 发送支付[交易]信息
 */
public class BluetoothPaymentTask extends AbsPaymentTask {
    private final BluetoothAdapter adapter;
    private final String bluetoothMac;
    private final Protos.Payment payment;

    public BluetoothPaymentTask(BluetoothAdapter adapter, String bluetoothMac, Protos.Payment payment, IPaymentTaskCallback callback) {
        super(callback);
        this.adapter = adapter;
        this.bluetoothMac = bluetoothMac;
        this.payment = payment;
    }

    private static final Logger log = LoggerFactory.getLogger(BluetoothPaymentTask.class);

    @Override
    public void run() {
        log.info("trying to send tx via bluetooth {}", bluetoothMac);

        final int count = payment.getTransactionsCount();
        Preconditions.checkArgument( count != 1, "wrong transactions count: "+count);
        final String address = BluetoothTools.compressMac(bluetoothMac);
        final BluetoothDevice device = adapter.getRemoteDevice(address);

        try {
            final BluetoothSocket socket = device.createInsecureRfcommSocketToServiceRecord(BluetoothTools.BIP70_PAYMENT_PROTOCOL_UUID);
            final DataOutputStream os = new DataOutputStream(socket.getOutputStream());
            final DataInputStream is = new DataInputStream(socket.getInputStream());
            socket.connect();
            log.info("connected to payment protocol {}", bluetoothMac);

            payment.writeDelimitedTo(os);
            os.flush();
            log.info("tx sent via bluetooth");

            // parse ack
            final Protos.PaymentACK paymentACK = Protos.PaymentACK.parseDelimitedFrom(is);
            final PaymentProtocol.Ack proAck = PaymentProtocol.parsePaymentAck(paymentACK);
            log.info("received {} via bluetooth", proAck.getMemo());

            onResult("ack".equals(proAck.getMemo()));
        } catch (IOException x) {
            log.info("problem sending", x);

            onFail(R.string.error_io, x.getMessage());
        }
    }
}
