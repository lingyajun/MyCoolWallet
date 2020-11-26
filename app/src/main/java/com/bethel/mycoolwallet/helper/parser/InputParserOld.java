package com.bethel.mycoolwallet.helper.parser;

import android.text.TextUtils;

import com.bethel.mycoolwallet.R;
import com.bethel.mycoolwallet.interfaces.IRequestPassphrase;
import com.bethel.mycoolwallet.utils.BluetoothTools;
import com.bethel.mycoolwallet.utils.Constants;
import com.bethel.mycoolwallet.utils.Qr;
import com.google.common.hash.Hashing;
import com.google.common.io.BaseEncoding;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.UninitializedMessageException;

import org.bitcoin.protocols.payments.Protos;
import org.bitcoinj.core.Address;
import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.DumpedPrivateKey;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.LegacyAddress;
import org.bitcoinj.core.PrefixedChecksummedBytes;
import org.bitcoinj.core.ProtocolException;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.VerificationException;
import org.bitcoinj.crypto.BIP38PrivateKey;
import org.bitcoinj.crypto.TrustStoreLoader;
import org.bitcoinj.protocols.payments.PaymentProtocol;
import org.bitcoinj.protocols.payments.PaymentProtocolException;
import org.bitcoinj.protocols.payments.PaymentSession;
import org.bitcoinj.uri.BitcoinURI;
import org.bitcoinj.uri.BitcoinURIParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.util.Date;
import java.util.regex.Pattern;

/**
 * @author Andreas Schildbach
 */
public abstract class InputParserOld {
    private static final Logger log = LoggerFactory.getLogger(InputParserOld.class);

    public abstract void parse();

    protected abstract void error(int messageResId, Object... messageArgs);

    public abstract static class StringInputParser extends InputParserOld {
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
                }  catch (final IOException x) {
                    log.info("i/o error while fetching payment request", x);

                    error(R.string.input_parser_io_error, x.getMessage());
                } catch (final PaymentProtocolException.PkiVerificationException x) {
                    log.info("got unverifyable payment request", x);

                    error(R.string.input_parser_unverifyable_paymentrequest, x.getMessage());
                } catch (final PaymentProtocolException x) {
                    log.info("got invalid payment request", x);

                    error(R.string.input_parser_invalid_paymentrequest, x.getMessage());
                }
            } else if (input.startsWith("bitcoin:") || input.startsWith("BITCOIN:")) {
                try {
                    final BitcoinURI bitcoinURI = new BitcoinURI(null,"bitcoin:" + input.substring(8));
                    final Address address = bitcoinURI.getAddress();
                    if (address != null && !Constants.NETWORK_PARAMETERS.equals(address.getParameters()))
                        throw new BitcoinURIParseException("mismatched network");
                    handleBitcoinURI(bitcoinURI);
                } catch (BitcoinURIParseException e) {
                    log.info("got invalid bitcoin uri: '{}' \n {}", input, e);

                    error(R.string.input_parser_invalid_bitcoin_uri, input);
                }
            } else if (PATTERN_TRANSACTION.matcher(input).matches()) {
                try {
                    final byte[] payload = Qr.decodeDecompressBinary(input);
                    final Transaction tx = new Transaction(Constants.NETWORK_PARAMETERS, payload);
                    handleDirectTransaction(tx);
                } catch (IOException x) {
                    log.info("i/o error while fetching transaction", x);

                    error(R.string.input_parser_invalid_transaction, x.getMessage());
                } catch (ProtocolException x) {
                    log.info("got invalid transaction", x);

                    error(R.string.input_parser_invalid_transaction, x.getMessage());
                }
            } else if (input.startsWith("http://") || input.startsWith("https://")) {
                //  web
                handleWebUrl(input);
            } else {
                // handlePrivateKey
                try {
                    PrefixedChecksummedBytes privateKey = DumpedPrivateKey.fromBase58(Constants.NETWORK_PARAMETERS, input);
                    handlePrivateKey(privateKey);
                } catch (AddressFormatException e) {
                    try {
                        PrefixedChecksummedBytes privateKey = BIP38PrivateKey.fromBase58(Constants.NETWORK_PARAMETERS, input);
                        handlePrivateKey(privateKey);
                    } catch (AddressFormatException ex) {
                        try {
                            Address address = Address.fromString(Constants.NETWORK_PARAMETERS, input);
                            handlePaymentAddress(address);
                        } catch (AddressFormatException.WrongNetwork exc) {
                            log.info("detected address, but wrong network", exc);
                            error(R.string.input_parser_invalid_address);
                        } catch (AddressFormatException exc) {
                            cannotClassify(input);
                        }
                    }
                }

            }
        }


        protected void handlePrivateKey(final PrefixedChecksummedBytes key) {
            Address address = null;
            if (key instanceof DumpedPrivateKey) {
                address = LegacyAddress.fromKey(Constants.NETWORK_PARAMETERS, ((DumpedPrivateKey) key).getKey());
            } else if (key instanceof BIP38PrivateKey) {
                //  https://www.jianshu.com/p/b5d6ba5cca24 ; BIP38：加密私钥, 还需要一个长密码作为口令
                requestPassphrase(passphrase -> {
                    try {
                        ECKey  ecKey = ((BIP38PrivateKey) key).decrypt(null);
                        Address  address2 = LegacyAddress.fromKey(Constants.NETWORK_PARAMETERS, ecKey);
                        handlePaymentAddress(address2);
                    } catch (BIP38PrivateKey.BadPassphraseException e) {
                        e.printStackTrace();
                    }
                });
            }
            if (null != address) {
                //  address 作为对方账户的地址，需要被填入发送coin页的地址栏中.
                handlePaymentAddress(address);
            }
        }

    }


    //--------- InputParserOld -----------/

    private static void handleBitcoinURI(BitcoinURI bitcoinUri) {
        final Address address = bitcoinUri.getAddress();
        final Coin amount = bitcoinUri.getAmount();
//        final Output[] outputs = address != null ? buildSimplePayTo(bitcoinUri.getAmount(), address) : null;
        final String bluetoothMac = (String) bitcoinUri.getParameterByName(BluetoothTools.MAC_URI_PARAM);
        final String paymentRequestHashStr = (String) bitcoinUri.getParameterByName("h");
        final byte[] paymentRequestHash = paymentRequestHashStr != null ? base64UrlDecode(paymentRequestHashStr) : null;

//   todo     return new PaymentIntent(PaymentIntent.Standard.BIP21, null, null, outputs, bitcoinUri.getLabel(),
//                bluetoothMac != null ? "bt:" + bluetoothMac : null, null, bitcoinUri.getPaymentRequestUrl(),
//                paymentRequestHash);
    }

    private static void parsePaymentRequestBytes(byte[] paymentRequestBytes) throws PaymentProtocolException {
            try {
                if (paymentRequestBytes.length > 50000)
                    throw new PaymentProtocolException("payment request too big: " + paymentRequestBytes.length);

                final Protos.PaymentRequest paymentRequest = Protos.PaymentRequest.parseFrom(paymentRequestBytes);

                final String pkiName;
                final String pkiCaName;
                if (!"none".equals(paymentRequest.getPkiType())) {
                    final KeyStore keystore = new TrustStoreLoader.DefaultTrustStoreLoader().getKeyStore();
                    final PaymentProtocol.PkiVerificationData verificationData = PaymentProtocol.verifyPaymentRequestPki(paymentRequest,
                            keystore);
                    pkiName = verificationData.displayName;
                    pkiCaName = verificationData.rootAuthorityName;
                } else {
                    pkiName = null;
                    pkiCaName = null;
                }

                final PaymentSession paymentSession = PaymentProtocol.parsePaymentRequest(paymentRequest);

                if (paymentSession.isExpired())
                    throw new PaymentProtocolException.Expired("payment details expired: current time " + new Date()
                            + " after expiry time " + paymentSession.getExpires());

                if (!paymentSession.getNetworkParameters().equals(Constants.NETWORK_PARAMETERS))
                    throw new PaymentProtocolException.InvalidNetwork(
                            "cannot handle payment request network: " + paymentSession.getNetworkParameters());

                final String memo = paymentSession.getMemo();

                final String paymentUrl = paymentSession.getPaymentUrl();

                final byte[] merchantData = paymentSession.getMerchantData();

                final byte[] paymentRequestHash = Hashing.sha256().hashBytes(paymentRequestBytes).asBytes();

//     todo           final PaymentIntent paymentIntent = new PaymentIntent(PaymentIntent.Standard.BIP70, pkiName, pkiCaName,
//                        outputs.toArray(new PaymentIntent.Output[0]), memo, paymentUrl, merchantData, null,
//                        paymentRequestHash);

            }  catch (final InvalidProtocolBufferException | UninitializedMessageException x) {
                throw new PaymentProtocolException(x);
            } catch (final FileNotFoundException | KeyStoreException x) {
                throw new RuntimeException(x);
            }
//            catch (PaymentProtocolException e) {
//                e.printStackTrace();
//            } catch (InvalidProtocolBufferException e) {
//                e.printStackTrace();
//            } catch (FileNotFoundException e) {
//                e.printStackTrace();
//            } catch (KeyStoreException e) {
//                e.printStackTrace();
//            }

    }

    protected void cannotClassify(final String input) {
        log.info("cannot classify: '{}'", input);

        error(R.string.input_parser_cannot_classify, input);
    }

    protected abstract  void handleWebUrl(String url) ;

    protected abstract void requestPassphrase(IRequestPassphrase callback);

    protected abstract void handlePaymentAddress(Address address);

    protected abstract void handleDirectTransaction(Transaction transaction) throws VerificationException;

    private static final Pattern PATTERN_TRANSACTION = Pattern
            .compile("[0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ$\\*\\+\\-\\.\\/\\:]{100,}");


    private static final BaseEncoding BASE64URL = BaseEncoding.base64Url().omitPadding();

    private static byte[] base64UrlDecode(final String encoded) {
        try {
            return BASE64URL.decode(encoded);
        } catch (final IllegalArgumentException x) {
            log.info("cannot base64url-decode: " + encoded);
            return null;
        }
    }

}
