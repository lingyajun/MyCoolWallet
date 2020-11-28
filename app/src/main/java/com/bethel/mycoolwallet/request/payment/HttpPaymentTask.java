package com.bethel.mycoolwallet.request.payment;

import androidx.annotation.Nullable;

import com.bethel.mycoolwallet.R;
import com.bethel.mycoolwallet.utils.Constants;

import org.bitcoin.protocols.payments.Protos;
import org.bitcoinj.protocols.payments.PaymentProtocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;

import okhttp3.CacheControl;
import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.BufferedSink;

/**
 * 通过http发送支付[交易]信息
 */
public  class HttpPaymentTask extends AbsPaymentTask {
    private final String url;
    @Nullable
    private final String userAgent;
    private final Protos.Payment payment;

    public HttpPaymentTask(String url, @Nullable String userAgent, Protos.Payment payment,
                           IPaymentTaskCallback callback) {
        super(callback);
        this.url = url;
        this.userAgent = userAgent;
        this.payment = payment;
    }

    private static final Logger log = LoggerFactory.getLogger(HttpPaymentTask.class);

    @Override
    public void run() {
        log.info("trying to send tx to {}", url);
        final Request.Builder request = new Request.Builder();
        request.url(url);
        request.cacheControl(new CacheControl.Builder().noCache().build());
        request.header("Accept", PaymentProtocol.MIMETYPE_PAYMENTACK);
        if (userAgent != null) {
            request.header("User-Agent", userAgent);
        }

        request.post(getBodyData());

        final Call call = Constants.HTTP_CLIENT.newCall(request.build());

        try {
            Response response = call.execute();
            if (!response.isSuccessful()) {
                final int responseCode = response.code();
                final String responseMessage = response.message();

                log.info("got http error {}: {}", responseCode, responseMessage);
                onFail(R.string.error_http, responseCode, responseMessage);
                return;
            }

            // success
            log.info("tx sent via http");

            final InputStream is = response.body().byteStream();
            final Protos.PaymentACK paymentACK = Protos.PaymentACK.parseFrom(is);
            is.close();

            final PaymentProtocol.Ack proAck = PaymentProtocol.parsePaymentAck(paymentACK);
            log.info("received {} via http", proAck.getMemo());

            onResult(!"nack".equals(proAck.getMemo()));
        } catch (IOException e) {
            log.info("problem sending", e);
            onFail(R.string.error_io, e.getMessage());
        }
    }

    private  RequestBody getBodyData() {
        return  new RequestBody() {
            @javax.annotation.Nullable
            @Override
            public MediaType contentType() {
                return MediaType.parse(PaymentProtocol.MIMETYPE_PAYMENT);
            }

            @Override
            public void writeTo(BufferedSink sink) throws IOException {
                payment.writeTo(sink.outputStream());
            }

            @Override
            public long contentLength() throws IOException {
                return payment.getSerializedSize();
            }
        };
    }
}
