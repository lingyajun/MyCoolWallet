package com.bethel.mycoolwallet.request.payment;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

import com.bethel.mycoolwallet.R;
import com.bethel.mycoolwallet.data.payment.PaymentData;
import com.bethel.mycoolwallet.helper.parser.BinaryInputParser;
import com.bethel.mycoolwallet.utils.BluetoothTools;
import com.google.protobuf.CodedInputStream;
import com.google.protobuf.CodedOutputStream;

import org.bitcoinj.protocols.payments.PaymentProtocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * 通过 BLE 接收对方的支付[交易]请求
 */
public class BluetoothPaymentRequestTask extends AbsPaymentRequestTask {
    private final BluetoothAdapter adapter;
    private final String url;

    public BluetoothPaymentRequestTask(BluetoothAdapter adapter, String url, IPaymentRequestListener listener) {
       super(listener);
        this.adapter = adapter;
        this.url = url;
    }

    private static final Logger log = LoggerFactory.getLogger(BluetoothPaymentRequestTask.class);

    @Override
    public void run() {
        log.info("trying to request payment request from {}", url);
        // ble request
        final String mac = BluetoothTools.getBluetoothMac(url);
        final String address = BluetoothTools.compressMac(mac);
        final BluetoothDevice device = adapter.getRemoteDevice(address);
        log.info("Bluetooth Remote Device, mac {} , address {}", mac, address);
        log.info("Bluetooth Remote Device  {}", device);

        try {
            BluetoothSocket socket =
                    device.createInsecureRfcommSocketToServiceRecord(BluetoothTools.PAYMENT_REQUESTS_UUID);
            final OutputStream os = socket.getOutputStream();
            final InputStream is = socket.getInputStream();

            socket.connect();
            log.info("connected to {}", url);

            final CodedOutputStream cos = CodedOutputStream.newInstance(os);
            final CodedInputStream cis = CodedInputStream.newInstance(is);

            cos.writeInt32NoTag(0);
            final String query = BluetoothTools.getBluetoothQuery(url);
            cos.writeStringNoTag(query);
            cos.flush();

            final int responseCode = cis.readInt32();
            if (200 != responseCode) {
                // failed
                log.info("got bluetooth error {}", responseCode);
                onFail(R.string.error_bluetooth, responseCode);

                return;
            }

            // 200
            final byte[] payload = cis.readBytes().toByteArray();
            new BinaryInputParser(PaymentProtocol.MIMETYPE_PAYMENTREQUEST, payload) {
                @Override
                public void error(int messageResId, Object... messageArgs) {
                    onFail(messageResId, messageArgs);
                }

                @Override
                public void handlePaymentData(PaymentData data) {
                    log.info("received {} via bluetooth", data);
                    onPaymentData(data);
                }
            }.parse();
        } catch (IOException e) {
            log.info("problem sending", e);

            onFail(R.string.error_io, e.getMessage());
        }
    }

}
