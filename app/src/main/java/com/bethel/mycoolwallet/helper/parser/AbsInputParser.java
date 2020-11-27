package com.bethel.mycoolwallet.helper.parser;

import com.bethel.mycoolwallet.R;
import com.bethel.mycoolwallet.data.payment.PaymentData;

import org.bitcoinj.protocols.payments.PaymentProtocolException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *  StringInputParser, StreamInputParser, BinaryInputParser
 */
public abstract class AbsInputParser implements IInputParser {
    protected static final Logger log = LoggerFactory.getLogger(AbsInputParser.class);

    /**
     * 将 {payload} 解析成 Protos.PaymentRequest, 构造 PaymentData
     */
    protected void baseParseAndHandlePaymentRequestBytes(final byte[] payload) {
        try {
            final PaymentData paymentData = InputParserTool.parsePaymentRequest(payload);
            handlePaymentData(paymentData);
        } catch (final PaymentProtocolException.PkiVerificationException x) {
            log.info("got unverifyable payment request", x);

            error(R.string.input_parser_unverifyable_paymentrequest, x.getMessage());
        }  catch (PaymentProtocolException x) {
            log.info("got invalid payment request", x);

            error(R.string.input_parser_invalid_paymentrequest, x.getMessage());
        }
    }

    protected void cannotClassify(final String input) {
        log.info("cannot classify: '{}'", input);

        error(R.string.input_parser_cannot_classify, input);
    }

}
