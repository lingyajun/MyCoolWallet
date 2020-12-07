package com.bethel.mycoolwallet.helper.parser;

import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.os.Parcelable;

import com.bethel.mycoolwallet.R;
import com.bethel.mycoolwallet.data.payment.PaymentData;
import com.bethel.mycoolwallet.data.payment.PaymentUtil;
import com.bethel.mycoolwallet.utils.NfcTools;

import org.bitcoinj.protocols.payments.PaymentProtocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class NfcIntentDataParser implements IInputParser {
    protected static final Logger log = LoggerFactory.getLogger(NfcIntentDataParser.class);
    private final Intent intent;

    public NfcIntentDataParser(Intent intent) {
        this.intent = intent;
    }

    @Override
    public void parse() {
        final String action = null != intent ? intent.getAction() : null;
        if (!NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {
            cannotClassify(action);
            log.error("not Nfc action  {}", action);
            return;
        }

        final String mimeType = intent.getType();
        final Parcelable[]  datas = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
        if (datas == null || datas.length <1) {
            log.error("NfcPaymentRequestData  is null !");
            if (PaymentProtocol.MIMETYPE_PAYMENTREQUEST.equals(mimeType)) {
                cannotClassify(mimeType);
               // handlePaymentData(PaymentUtil.blank());
            }
            return;
        }

        final NdefMessage msg = (NdefMessage) datas[0];
        final byte[] payload = NfcTools.extractMimePayload(PaymentProtocol.MIMETYPE_PAYMENTREQUEST, msg);
        parseAndHandlePaymentRequest(mimeType, payload);
    }

    private void parseAndHandlePaymentRequest(String mimeType, byte[] payload) {
        new BinaryInputParser(mimeType, payload) {
            @Override
            public void error(int messageResId, Object... messageArgs) {
                NfcIntentDataParser.this.error(messageResId, messageArgs);
            }

            @Override
            public void handlePaymentData(PaymentData data) {
                NfcIntentDataParser.this.handlePaymentData(data);
            }
        }.parse();
    }


    protected void cannotClassify(final String input) {
        log.info("cannot classify: '{}'", input);

        error(R.string.input_parser_cannot_classify, input);
    }

}
