package com.bethel.mycoolwallet.data.payment;

import androidx.annotation.Nullable;
import androidx.core.util.Preconditions;

import com.bethel.mycoolwallet.utils.BluetoothTools;
import com.bethel.mycoolwallet.utils.Constants;
import com.google.common.io.BaseEncoding;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.script.ScriptBuilder;
import org.bitcoinj.uri.BitcoinURI;
import org.bitcoinj.wallet.SendRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 构造 PaymentData ，PaymentOutput ，SendRequest
 */
public final class PaymentUtil {
    private static final Logger log = LoggerFactory.getLogger(PaymentUtil.class);

    public static PaymentData blank() {
        return new PaymentData(null, null, null, null, null, null, null, null, null);
    }

    public static PaymentOutput[] buildSimplePayTo(final Coin amount, final Address address) {
        return new PaymentOutput[] { new PaymentOutput(amount, ScriptBuilder.createOutputScript(address)) };
    }

    public static SendRequest getSendRequest(PaymentOutput[] outputs) {
        final Transaction transaction = new Transaction(Constants.NETWORK_PARAMETERS);
        if (null!= outputs) {
            for (PaymentOutput out: outputs) {
                transaction.addOutput(out.amount, out.script);
            }
        }
        return SendRequest.forTx(transaction);
    }

    public static SendRequest getSendRequest(PaymentData data) {
        Preconditions.checkArgument(data !=null);
        return getSendRequest(data.outputs);
    }

    public static PaymentData from(final String address, @Nullable final String addressLabel,
                                   @Nullable final Coin amount)   throws AddressFormatException {
        final Address account = Address.fromString(Constants.NETWORK_PARAMETERS, address);
//        return new PaymentData(account, addressLabel);
       return new PaymentData(null,null, null,
                PaymentUtil.buildSimplePayTo(amount, account), addressLabel, null, null, null, null);

    }

    public static PaymentData fromAddress(final Address address, @Nullable final String addressLabel) {
        return  new PaymentData(address, addressLabel);
    }

    public static PaymentData fromAddress(final String address, @Nullable final String addressLabel)
            throws AddressFormatException {
        final Address account = Address.fromString(Constants.NETWORK_PARAMETERS, address);
        return fromAddress(account, addressLabel);
    }

    public static PaymentData  fromBitcoinUri(final BitcoinURI bitcoinUri) {
        final Address address = bitcoinUri.getAddress();
        final PaymentOutput[] outputs = null!=address ?
                buildSimplePayTo(bitcoinUri.getAmount(), address): null;
        final String bluetoothMac = (String) bitcoinUri.getParameterByName(BluetoothTools.MAC_URI_PARAM);
        final String paymentUrl = null!=bluetoothMac? "bt:" + bluetoothMac : null;

        final String hashStr = (String) bitcoinUri.getParameterByName("h");
        final byte[] paymentRequestHash = null!=hashStr? base64UrlDecode(hashStr) : null;
        final String paymentRequestUrl = bitcoinUri.getPaymentRequestUrl();
        final String memo = bitcoinUri.getLabel();

        return new PaymentData(PaymentStandard.BIP21, null, null,
                outputs, memo, paymentUrl, null, paymentRequestUrl, paymentRequestHash);
    }

    public static PaymentData mergeWithEditedValues(PaymentData source, @Nullable final Coin editedAmount,
                                                    @Nullable final Address editedAddress) {
        Preconditions.checkArgument(source !=null);
        final PaymentOutput[] outputs;
        if (source.hasOutputs()) {
            // merge
            if (mayEditAmount(source)) {
                Preconditions.checkArgument(editedAmount !=null);
                outputs = new PaymentOutput[]{new PaymentOutput(editedAmount, source.outputs[0].script)};
            } else {
                outputs = source.outputs;
            }

        } else {
            Preconditions.checkArgument(editedAddress !=null);
            Preconditions.checkArgument(editedAmount !=null);
            outputs = buildSimplePayTo(editedAmount, editedAddress);
        }

//        if (!source.hasOutputs() || (source.hasOutputs() && mayEditAmount(source))){
//            Preconditions.checkArgument(editedAddress !=null);
//            Preconditions.checkArgument(editedAmount !=null);
//            outputs = buildSimplePayTo(editedAmount, editedAddress);
//        } else {
//            outputs = source.outputs;
//        }

        return new PaymentData(source.standard, source.payeeName, source.payeeVerifiedBy, outputs,
                source.memo, source.paymentUrl, source.payeeData, source.paymentRequestUrl, source.paymentRequestHash);
    }

    public static boolean mayEditAmount(PaymentData source) {
        return null!=source &&
                !(source.standard == PaymentStandard.BIP70 && source.hasAmount());
    }

    private static final BaseEncoding BASE64URL = BaseEncoding.base64Url().omitPadding();

    private static byte[] base64UrlDecode(final String encoded) {
        try {
            return BASE64URL.decode(encoded);
        } catch (final IllegalArgumentException x) {
            log.info("cannot base64url-decode: {}" , encoded);
            return null;
        }
    }

}
