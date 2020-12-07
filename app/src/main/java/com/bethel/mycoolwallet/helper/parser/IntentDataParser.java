package com.bethel.mycoolwallet.helper.parser;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.os.Parcelable;

import androidx.core.util.Preconditions;

import com.bethel.integration_android.BitcoinIntegration;
import com.bethel.mycoolwallet.activity.SendCoinsActivity;
import com.bethel.mycoolwallet.data.payment.PaymentData;
import com.bethel.mycoolwallet.data.payment.PaymentUtil;
import com.bethel.mycoolwallet.utils.NfcTools;

import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.VerificationException;
import org.bitcoinj.protocols.payments.PaymentProtocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.InputStream;

/** 解析 Intent 传递的数据
 * { SendCoinsFragment :: onCreate() }
 */
public abstract class IntentDataParser implements IInputParser {
    private final Intent intent;

    private static final Logger log = LoggerFactory.getLogger(IntentDataParser.class);

    public IntentDataParser(Intent input) {
        this.intent = input;
    }

    @Override
    public void parse() {
        Preconditions.checkArgument(null != intent);
        final String action = intent.getAction();
        final Uri intentUri = intent.getData();
        final String scheme = intentUri != null ? intentUri.getScheme() : null;
        final String mimeType = intent.getType();

        if ((Intent.ACTION_VIEW.equals(action) || NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action))
                && intentUri != null && "bitcoin".equals(scheme)) {
            // BitcoinUri
            parseAndHandleBitcoinUri(intentUri);
            return;
        }

        if ((NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action))
                && PaymentProtocol.MIMETYPE_PAYMENTREQUEST.equals(mimeType)) {
            // nfc , PaymentRequest
            parseAndHandleNfcPaymentRequestData();
            return;
        }

        if (Intent.ACTION_VIEW.equals(action)
                && PaymentProtocol.MIMETYPE_PAYMENTREQUEST.equals(mimeType)) {
            if (null!= intentUri) {
                parseAndHandleIntentUri(mimeType, intentUri);
                return;
            }
            // byte[] payload // BitcoinIntegration.paymentRequestFromIntent
            final byte[] payload = BitcoinIntegration.paymentRequestFromIntent(intent);
            if (null!= payload) {
                parseAndHandlePaymentRequest(mimeType, payload);
                return;
            }
            throw new IllegalArgumentException();
//            return;
        }

        if (intent.hasExtra(SendCoinsActivity.INTENT_EXTRA_PAYMENT_INTENT)) {
            // PaymentData
            final PaymentData payment = intent.getParcelableExtra(SendCoinsActivity.INTENT_EXTRA_PAYMENT_INTENT);
            handlePaymentData(payment);
            log.info("PaymentData: {}", payment);
            return;
        }

        // else
        handlePaymentData(PaymentUtil.blank());
    }

    private void parseAndHandleIntentUri(String mimeType, Uri bitcoinUri) {
        try {
            final ContentResolver resolver = getContext().getContentResolver();
            final InputStream is = resolver.openInputStream(bitcoinUri);
            new StreamInputParser(mimeType, is) {
                @Override
                public void error(int messageResId, Object... messageArgs) {
                    IntentDataParser.this.error(messageResId, messageArgs);
                }

                @Override
                public void handlePaymentData(PaymentData data) {
                    IntentDataParser.this.handlePaymentData(data);
                }
            }.parse();
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

//    protected abstract InputStream openInputStream(Uri bitcoinUri);
    protected abstract Context getContext();


    private void parseAndHandleNfcPaymentRequestData() {
        Parcelable[]  datas = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
        if (datas == null || datas.length <1) {
            log.error("NfcPaymentRequestData  is null !");
            handlePaymentData(PaymentUtil.blank());
            return;
        }

        final NdefMessage msg = (NdefMessage) datas[0];
        final byte[] payload = NfcTools.extractMimePayload(PaymentProtocol.MIMETYPE_PAYMENTREQUEST, msg);
        final String mimeType = intent.getType();
        parseAndHandlePaymentRequest(mimeType, payload);
    }

    private void parseAndHandlePaymentRequest(String mimeType, byte[] payload) {
        new BinaryInputParser(mimeType, payload) {
            @Override
            public void error(int messageResId, Object... messageArgs) {
                IntentDataParser.this.error(messageResId, messageArgs);
            }

            @Override
            public void handlePaymentData(PaymentData data) {
                IntentDataParser.this.handlePaymentData(data);
            }
        }.parse();
    }

    private void parseAndHandleBitcoinUri(Uri bitcoinUri) {
        final String input = bitcoinUri.toString();
        log.info("bitcoinUri {}", input);
        new  StringInputParser(input) {
            @Override
            public void error(int messageResId, Object... messageArgs) {
                IntentDataParser.this.error(messageResId, messageArgs);
            }

            @Override
            public void handlePaymentData(PaymentData data) {
                IntentDataParser.this.handlePaymentData(data);
            }

            @Override
            protected void handleWebUrl(String link) {
                log.info("UnsupportedOperationException: web url");
                throw new UnsupportedOperationException();
            }

//            @Override
//            protected void requestBIP38PrivateKeyPassphrase() {
//                log.info("UnsupportedOperationException: request passphrase");
//                throw new UnsupportedOperationException();
//            }

            @Override
            public void handleDirectTransaction(Transaction transaction) throws VerificationException {
                log.info("UnsupportedOperationException: transaction");
                throw new UnsupportedOperationException();
            }
        }.parse();
    }
}
