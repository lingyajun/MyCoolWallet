package com.bethel.mycoolwallet.helper.parser;

import androidx.core.util.Preconditions;

import com.bethel.mycoolwallet.data.payment.PaymentData;
import com.bethel.mycoolwallet.data.payment.PaymentOutput;
import com.bethel.mycoolwallet.data.payment.PaymentStandard;
import com.bethel.mycoolwallet.data.payment.PaymentUtil;
import com.bethel.mycoolwallet.utils.Constants;
import com.google.common.hash.Hashing;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.UninitializedMessageException;

import org.bitcoin.protocols.payments.Protos;
import org.bitcoinj.crypto.TrustStoreLoader;
import org.bitcoinj.protocols.payments.PaymentProtocol;
import org.bitcoinj.protocols.payments.PaymentProtocolException;
import org.bitcoinj.protocols.payments.PaymentSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.util.Date;
import java.util.LinkedList;

public final class InputParserUtil {
    private static final Logger log = LoggerFactory.getLogger(InputParserUtil.class);

    public static PaymentData parsePaymentRequest(final byte[] paymentRequestBytes) throws PaymentProtocolException {
        final  int size = null!= paymentRequestBytes? paymentRequestBytes.length : 0;
        if (size<1 || size> 50000) {
            throw new PaymentProtocolException("can not parse payment request : "+size);
        }

        try {
            Protos.PaymentRequest paymentRequest =  Protos.PaymentRequest.parseFrom(paymentRequestBytes);

            final String payeeName;
            final String payeeVerifiedBy;
            if (!"none".equals(paymentRequest.getPkiType())) {
                final KeyStore keystore = new TrustStoreLoader.DefaultTrustStoreLoader().getKeyStore();
                // find the corresponding public key and verify the provided signature.
                final PaymentProtocol.PkiVerificationData verificationData =
                        PaymentProtocol.verifyPaymentRequestPki(paymentRequest, keystore);
                if (null != verificationData) {
                    payeeName = verificationData.displayName;
                    payeeVerifiedBy = verificationData.rootAuthorityName;
                } else {
                    payeeName = null;
                    payeeVerifiedBy = null;
                }
            } else {
                payeeName = null;
                payeeVerifiedBy = null;
            }
            log.info("pki  {}, {}", paymentRequest.getPkiType(), paymentRequest.getPkiData());

            final PaymentSession paymentSession = PaymentProtocol.parsePaymentRequest(paymentRequest);
            if (paymentSession.isExpired()) {
                log.error("parsePaymentRequest paymentSession isExpired  {}",paymentSession.getExpires());
                throw new PaymentProtocolException.Expired("payment details expired: current time " + new Date()
                        + " after expiry time " + paymentSession.getExpires());
            }
            if (!Constants.NETWORK_PARAMETERS.equals(paymentSession.getNetworkParameters())) {
                log.error("parsePaymentRequest InvalidNetwork  {}",paymentSession.getNetworkParameters());
                throw new PaymentProtocolException.InvalidNetwork(
                        "cannot handle payment request network: " + paymentSession.getNetworkParameters());
            }

            final LinkedList<PaymentOutput> list = new LinkedList<>();
            for (PaymentProtocol.Output out: paymentSession.getOutputs() ) {
                list.add(PaymentOutput.valueOf(out));
            }
            final  PaymentOutput[] paymentOutputs = new PaymentOutput[list.size()];
            list.toArray(paymentOutputs);

            final String memo = paymentSession.getMemo();
            final String paymentUrl = paymentSession.getPaymentUrl();
            final byte[] payeeData = paymentSession.getMerchantData(); // 贸易\商家 数据
            final byte[] paymentRequestHash = Hashing.sha256().hashBytes(paymentRequestBytes).asBytes();

            final PaymentData data = new PaymentData(PaymentStandard.BIP70, payeeName, payeeVerifiedBy,
                    paymentOutputs, memo, paymentUrl, payeeData, null, paymentRequestHash);

            if (data.hasPaymentUrl() && !data.isSupportedPaymentUrl()) {
                log.error("parsePaymentRequest InvalidPaymentURL  {}", data.paymentUrl);
                throw new PaymentProtocolException.InvalidPaymentURL(
                        "cannot handle payment url: " + data.paymentUrl);
            }

            return data;
        } catch (InvalidProtocolBufferException| UninitializedMessageException e) {
            log.error("parsePaymentRequest Exception",e);
            throw new PaymentProtocolException(e);
        } catch (FileNotFoundException | KeyStoreException e) {
            log.error("parsePaymentRequest Exception",e);
            throw new RuntimeException(e);
        }
//        return PaymentUtil.blank();
    }
}
