package com.bethel.mycoolwallet.request.payment;

import com.bethel.mycoolwallet.R;
import com.bethel.mycoolwallet.data.payment.PaymentData;
import com.bethel.mycoolwallet.helper.parser.StreamInputParser;
import com.bethel.mycoolwallet.utils.Constants;

import org.bitcoinj.protocols.payments.PaymentProtocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;

import okhttp3.CacheControl;
import okhttp3.Call;
import okhttp3.Request;
import okhttp3.Response;

public  class HttpPaymentRequestTask extends AbsPaymentRequestTask {
    private final String userAgent;
    private final String url;

    public HttpPaymentRequestTask(String userAgent, String url, IPaymentRequestListener listener) {
        super(listener);
        this.userAgent = userAgent;
        this.url = url;
    }

    private static final Logger log = LoggerFactory.getLogger(HttpPaymentRequestTask.class);

    @Override
    public void run() {
        log.info("trying to request payment request from {} , {}", url, userAgent);
        // request
        final Request.Builder builder = new Request.Builder();
        builder.url(url);
        builder.cacheControl(new CacheControl.Builder().noCache().build());
        builder.header("Accept", PaymentProtocol.MIMETYPE_PAYMENTREQUEST);
        if (null!=userAgent) {
            builder.header("User-Agent", userAgent);
        }

        final Call call = Constants.HTTP_CLIENT.newCall(builder.build());
        try {
            Response response = call.execute();
            if (!response.isSuccessful()) {
                // failed
                final int responseCode = response.code();
                final String responseMessage = response.message();

                log.info("got http error {}: {}", responseCode, responseMessage);
                onFail(R.string.error_http, responseCode, responseMessage);
                return;
            }

            // Success
            final String contentType = response.header("Content-Type");
            final InputStream is = response.body().byteStream();

            new StreamInputParser(contentType, is) {
                @Override
                public void error(int messageResId, Object... messageArgs) {
                    onFail(messageResId, messageArgs);
                }

                @Override
                public void handlePaymentData(PaymentData data) {
                    log.info("received ' {} ' via http", data);
                    onPaymentData(data);
                }
            }.parse();

        } catch (IOException e) {
            log.info("problem sending", e);

            onFail(R.string.error_io, e.getMessage());
        }
    }

}
