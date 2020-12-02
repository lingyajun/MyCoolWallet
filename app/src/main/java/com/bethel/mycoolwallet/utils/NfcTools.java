package com.bethel.mycoolwallet.utils;

import android.nfc.NdefMessage;
import android.nfc.NdefRecord;

import org.bitcoinj.protocols.payments.PaymentProtocol;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public final class NfcTools {
    public static byte[] extractMimePayload(final String mimeType, final NdefMessage message) {
        final byte[] mimeBytes = mimeType.getBytes(Commons.US_ASCII);

        for (final NdefRecord record : message.getRecords()) {
            if (record.getTnf() == NdefRecord.TNF_MIME_MEDIA && Arrays.equals(record.getType(), mimeBytes))
                return record.getPayload();
        }

        return null;
    }

    public static NdefMessage createNdefMessage(final byte[] paymentRequest) {
        if (paymentRequest != null)
            return new NdefMessage(
                    new NdefRecord[] { createMime(PaymentProtocol.MIMETYPE_PAYMENTREQUEST, paymentRequest) });
        else
            return null;
    }

    public static NdefRecord createMime(final String mimeType, final byte[] payload) {
        final byte[] mimeBytes = mimeType.getBytes(Commons.US_ASCII);
        return new NdefRecord(NdefRecord.TNF_MIME_MEDIA, mimeBytes, new byte[0], payload);
    }

}
