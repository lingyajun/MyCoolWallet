package com.bethel.mycoolwallet.helper.parser;

import android.text.TextUtils;

import androidx.core.util.Preconditions;

import com.bethel.mycoolwallet.R;
import com.bethel.mycoolwallet.data.payment.PaymentData;
import com.bethel.mycoolwallet.data.payment.PaymentUtil;
import com.bethel.mycoolwallet.utils.Constants;
import com.bethel.mycoolwallet.utils.Qr;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.DumpedPrivateKey;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.LegacyAddress;
import org.bitcoinj.core.PrefixedChecksummedBytes;
import org.bitcoinj.core.ProtocolException;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.VerificationException;
import org.bitcoinj.crypto.BIP38PrivateKey;
import org.bitcoinj.protocols.payments.PaymentProtocolException;
import org.bitcoinj.uri.BitcoinURI;
import org.bitcoinj.uri.BitcoinURIParseException;

import java.io.IOException;
import java.util.regex.Pattern;

public abstract class StringInputParser  extends AbsInputParser { //implements IInputParser
    private final static String Prefix_PaymentRequest = "BITCOIN:-";
    private final static String Prefix_BitcoinURI = "BITCOIN:";
    private final static String Prefix_BitcoinURI_2 = "bitcoin:";

    private static final Pattern PATTERN_TRANSACTION = Pattern
            .compile("[0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ$\\*\\+\\-\\.\\/\\:]{100,}");

    private final String input;

    public StringInputParser(String input) {
        this.input = input;
    }

    @Override
    public void parse() {
//        log.info("input {}", input);
        Preconditions.checkArgument(!TextUtils.isEmpty(input));
        if (input.startsWith(Prefix_PaymentRequest)) {
            parseAndHandlePaymentRequest();
            return;
        }
        if (input.startsWith(Prefix_BitcoinURI) || input.startsWith(Prefix_BitcoinURI_2)) {
            parseAndHandlePaymentUri();
            return;
        }
        if (PATTERN_TRANSACTION.matcher(input).matches()) {
            parseAndHandleTransaction();
            return;
        }

        if (input.startsWith("http://") || input.startsWith("https://")) {
            //  web
            handleWebUrl(input);
            return;
        }
        // handlePrivateKey
        // handlePayment
        parseAndHandleAddress();
    }

    protected abstract void handleWebUrl(String link);

    /**
     * 将 {input} 解析成 Address
     */
    private void parseAndHandleAddress() {
        try {
            final DumpedPrivateKey dumpedPrivateKey = DumpedPrivateKey.fromBase58(Constants.NETWORK_PARAMETERS, input);
            handlePrivateKey(dumpedPrivateKey);
        } catch (AddressFormatException e) {
            log.info(" DumpedPrivateKey ", e);
            try {
                final BIP38PrivateKey bip38PrivateKey = BIP38PrivateKey.fromBase58(Constants.NETWORK_PARAMETERS, input);
//            bip38PrivateKey.decrypt(String passphrase); // todo need a passphrase
//                requestBIP38PrivateKeyPassphrase(bip38PrivateKey);
//                responseBIP38PrivateKeyPassphrase( String passphrase);
                handlePrivateKey(bip38PrivateKey);
            } catch ( Exception ex) {
                log.warn(" BIP38PrivateKey ", ex);
                parseAndHandleNormalAddress();
            }
        }
    }

    protected void handlePrivateKey(final PrefixedChecksummedBytes key) {
        final Address address = LegacyAddress.fromKey(Constants.NETWORK_PARAMETERS,
                ((DumpedPrivateKey) key).getKey());
        handlePaymentData(PaymentUtil.fromAddress(address, null));
    }

//    protected abstract void requestBIP38PrivateKeyPassphrase( final BIP38PrivateKey key);

    /**
     * 将 {input} 解析成 Address
     */
    private void parseAndHandleNormalAddress() {
        if (TextUtils.isEmpty(input)) return;

        try {
            final Address address = Address.fromString(Constants.NETWORK_PARAMETERS, input);
            handlePaymentData(PaymentUtil.fromAddress(address, null));
        } catch (AddressFormatException.WrongNetwork exc) {
            log.info("detected address, but wrong network", exc);
            error(R.string.input_parser_invalid_address);
        } catch (AddressFormatException exce) {
            cannotClassify(input);
            log.info(" Address ", exce);
        }
    }

    public abstract  void handleDirectTransaction(Transaction transaction) throws VerificationException;

    /**
     * 外部调用
     * 用户输入完密码，调用此方法回应
     * 与 requestPassphrase() 成对使用
     *
     * @param passphrase
     */
    public void responseBIP38PrivateKeyPassphrase(String passphrase) {
        if (TextUtils.isEmpty(input)) return;

        try {
            final BIP38PrivateKey encryptKey = BIP38PrivateKey.fromBase58(Constants.NETWORK_PARAMETERS, input);
            ECKey key = encryptKey.decrypt(passphrase);
            final Address address = LegacyAddress.fromKey(Constants.NETWORK_PARAMETERS, key);
            handlePaymentData(PaymentUtil.fromAddress(address, null));
        } catch (BIP38PrivateKey.BadPassphraseException e) {
            log.info(" BadPassphraseException ", e);
            error(R.string.bad_password, input);
        } catch (AddressFormatException ex) {
            log.info(" BIP38PrivateKey ", ex);
            parseAndHandleNormalAddress();
        }
    }

    /**
     * 将 {input} 解析成 Transaction
     */
    private void parseAndHandleTransaction() {
        try {
            final Transaction tx = new Transaction(Constants.NETWORK_PARAMETERS,
                    Qr.decodeDecompressBinary(input));
            handleDirectTransaction(tx);
        } catch (IOException x) {
            log.info("i/o error while fetching transaction", x);

            error(R.string.input_parser_invalid_transaction, x.getMessage());
        } catch (ProtocolException x) {
            log.info("got invalid transaction", x);

            error(R.string.input_parser_invalid_transaction, x.getMessage());
        }
    }

    /**
     * 将 {input} 解析成 BitcoinURI
     */
    private void parseAndHandlePaymentUri() {
        final String uri = input.startsWith(Prefix_BitcoinURI) ?
                Prefix_BitcoinURI_2 + input.substring(Prefix_BitcoinURI.length()): input;
        log.info("bitcoin uri: '{}' \n {}" , input , uri);
        try {
            final BitcoinURI bitcoinURI = new BitcoinURI(null, uri);

            final Address address = bitcoinURI.getAddress();
            if (null!= address && !Constants.NETWORK_PARAMETERS.equals(address.getParameters())) {
                throw new BitcoinURIParseException("mismatched network");
            }

            final PaymentData data = PaymentUtil.fromBitcoinUri(bitcoinURI);
            handlePaymentData(data);
        } catch (BitcoinURIParseException e) {
            log.info("got invalid bitcoin uri: '{}'  {}" , input , e);

            error(R.string.input_parser_invalid_bitcoin_uri, input);
        }
    }

    /**
     * 将 {input} 解析成 Protos.PaymentRequest
     */
    private void parseAndHandlePaymentRequest() {
        try {
            final byte[] payload = Qr.decodeBinary(input.substring(Prefix_PaymentRequest.length()));
            baseParseAndHandlePaymentRequestBytes(payload);
        } catch (IOException x) {
            log.info("i/o error while fetching payment request", x);

            error(R.string.input_parser_io_error, x.getMessage());
        }
    }

}
