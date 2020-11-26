package com.bethel.mycoolwallet.helper.parser;

import androidx.core.util.Preconditions;

import com.bethel.mycoolwallet.R;
import com.bethel.mycoolwallet.data.payment.PaymentData;
import com.google.common.io.ByteStreams;

import org.bitcoinj.protocols.payments.PaymentProtocol;
import org.bitcoinj.protocols.payments.PaymentProtocolException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public abstract class StreamInputParser extends AbsInputParser { //implements IInputParser
//    private static final Logger log = LoggerFactory.getLogger(StreamInputParser.class);

    private final String inputType;
    private final InputStream is;

    public StreamInputParser(String inputType, InputStream is) {
        this.inputType = inputType;
        this.is = is;
    }

    @Override
    public void parse() {
        Preconditions.checkArgument(null!= inputType && null != is);
        if (PaymentProtocol.MIMETYPE_PAYMENTREQUEST.equals(inputType)) {
            try {
                final ByteArrayOutputStream stream = new ByteArrayOutputStream();
                ByteStreams.copy(is, stream);

                final byte[] payload = stream.toByteArray();
                baseParseAndHandlePaymentRequestBytes(payload);
//                final PaymentData paymentData = InputParserTool.parsePaymentRequest(payload);
//                handlePaymentData(paymentData);
            } catch (IOException x) {
                log.info("i/o error while fetching payment request", x);

                error(R.string.input_parser_io_error, x.getMessage());
//            } catch (final PaymentProtocolException.PkiVerificationException x) {
//                log.info("got unverifyable payment request", x);
//
//                error(R.string.input_parser_unverifyable_paymentrequest, x.getMessage());
//            } catch (final PaymentProtocolException x) {
//                log.info("got invalid payment request", x);
//
//                error(R.string.input_parser_invalid_paymentrequest, x.getMessage());
            }
            finally {
                try {
                    is.close();
                } catch (IOException e) {
                    log.error("close InputStream: ", e);
                }
            }

            return;
        }

        cannotClassify(inputType);
    }

}
