package com.bethel.mycoolwallet.helper.parser;

import androidx.core.util.Preconditions;

import com.bethel.mycoolwallet.R;
import com.bethel.mycoolwallet.data.payment.PaymentData;
import com.bethel.mycoolwallet.utils.Constants;

import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.VerificationException;
import org.bitcoinj.protocols.payments.PaymentProtocol;
import org.bitcoinj.protocols.payments.PaymentProtocolException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class BinaryInputParser extends AbsInputParser { //implements IInputParser
    private final String inputType;
    private final byte[] input;

    public BinaryInputParser(String inputType, byte[] input) {
        this.inputType = inputType;
        this.input = input;
    }

//    private static final Logger log = LoggerFactory.getLogger(BinaryInputParser.class);
    @Override
    public void parse() {
        Preconditions.checkArgument(null!= inputType && null != input);

        if (Constants.MIMETYPE_TRANSACTION.equals(inputType)) {
            try {
                final Transaction tx = new Transaction(Constants.NETWORK_PARAMETERS, input);
                handleDirectTransaction(tx);
            } catch (VerificationException x) {
                log.info("got invalid transaction", x);

                error(R.string.input_parser_invalid_transaction, x.getMessage());
            }

            return;
        }

        if (PaymentProtocol.MIMETYPE_PAYMENTREQUEST.equals(inputType)) {
            baseParseAndHandlePaymentRequestBytes(input);
            return;
        }

        cannotClassify(inputType);
    }

    @Override
    public void handleDirectTransaction(Transaction transaction) throws VerificationException {
        throw new UnsupportedOperationException();
    }

}
