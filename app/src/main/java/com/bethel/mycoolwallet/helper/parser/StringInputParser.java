package com.bethel.mycoolwallet.helper.parser;

import android.text.TextUtils;

import com.bethel.mycoolwallet.R;
import com.bethel.mycoolwallet.utils.Constants;
import com.bethel.mycoolwallet.utils.Qr;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.ProtocolException;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.uri.BitcoinURI;
import org.bitcoinj.uri.BitcoinURIParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.regex.Pattern;

public abstract class StringInputParser extends InputParser {
    private static final Logger log = LoggerFactory.getLogger(StringInputParser.class);
    private final String input;

    public StringInputParser(String input) {
        this.input = input;
    }

    @Override
    public void parse() {
        if (TextUtils.isEmpty(input)) return;
        if (input.startsWith("BITCOIN:-")) {
            try {
                final byte[] paymentRequestBytes = Qr.decodeBinary(input.substring(9));
                parsePaymentRequestBytes(paymentRequestBytes);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (input.startsWith("bitcoin:") || input.startsWith("BITCOIN:")) {
            try {
                final BitcoinURI bitcoinURI = new BitcoinURI(null,"bitcoin:" + input.substring(8));
                final Address address = bitcoinURI.getAddress();
                if (address != null && !Constants.NETWORK_PARAMETERS.equals(address.getParameters()))
                    throw new BitcoinURIParseException("mismatched network");
                // todo
            } catch (BitcoinURIParseException e) {
                log.info("got invalid bitcoin uri: '{}' \n {}", input, e);

                error(R.string.input_parser_invalid_bitcoin_uri, input);
            }
        } else if (PATTERN_TRANSACTION.matcher(input).matches()) {
            try {
                final byte[] payload = Qr.decodeDecompressBinary(input);
                final Transaction tx = new Transaction(Constants.NETWORK_PARAMETERS, payload);

                // todo
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ProtocolException e) {
                e.printStackTrace();
            }
        } else if (input.startsWith("http://") || input.startsWith("https://")) {
            // todo web
        } else {
            // handlePrivateKey
        }
    }

    private void parsePaymentRequestBytes(byte[] paymentRequestBytes) {
        // todo
    }

    @Override
    protected void error(int messageResId, Object... messageArgs) {

    }


    private static final Pattern PATTERN_TRANSACTION = Pattern
            .compile("[0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ$\\*\\+\\-\\.\\/\\:]{100,}");
}
