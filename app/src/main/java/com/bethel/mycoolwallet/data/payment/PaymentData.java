package com.bethel.mycoolwallet.data.payment;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bethel.mycoolwallet.utils.BluetoothTools;
import com.bethel.mycoolwallet.utils.Constants;
import com.bethel.mycoolwallet.utils.Utils;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.Coin;
import org.bitcoinj.script.Script;
import org.bitcoinj.script.ScriptPattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

public class PaymentData implements Parcelable {
    @Nullable
    public final PaymentStandard standard;
    @Nullable
    public final String payeeName;
    @Nullable
    public final String payeeVerifiedBy;
    @Nullable
    public final PaymentOutput[] outputs;
    @Nullable
    public final String memo;
    @Nullable
    public final String paymentUrl;
    @Nullable
    public final byte[] payeeData;
    @Nullable
    public final String paymentRequestUrl;
    @Nullable
    public final byte[] paymentRequestHash;

    private static final Logger log = LoggerFactory.getLogger(PaymentData.class);

    public PaymentData(@Nullable PaymentStandard standard, @Nullable String payeeName,
                       @Nullable String payeeVerifiedBy, @Nullable PaymentOutput[] outputs,
                       @Nullable String memo, @Nullable String paymentUrl, @Nullable byte[] payeeData,
                       @Nullable String paymentRequestUrl, @Nullable byte[] paymentRequestHash) {
        this.standard = standard;
        this.payeeName = payeeName;
        this.payeeVerifiedBy = payeeVerifiedBy;
        this.outputs = outputs;
        this.memo = memo;
        this.paymentUrl = paymentUrl;
        this.payeeData = payeeData;
        this.paymentRequestUrl = paymentRequestUrl;
        this.paymentRequestHash = paymentRequestHash;
    }

    public PaymentData(final Address address, @Nullable final String addressLabel) {
        // memo = addressLabel
        this(null,null, null,
                PaymentUtil.buildSimplePayTo(Coin.ZERO, address), addressLabel, null, null, null, null);
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeSerializable(standard);

        parcel.writeString(payeeName);
        parcel.writeString(payeeVerifiedBy);
        parcel.writeString(paymentRequestUrl);
        parcel.writeString(paymentUrl);
        parcel.writeString(memo);

       final int size = null!= outputs? outputs.length:0;
        parcel.writeInt(size);
        if (size>0) {
            parcel.writeTypedArray(outputs, 0);
        }

        final  int size2 = null!= payeeData? payeeData.length:0;
        parcel.writeInt(size2);
        if (size2>0) {
            parcel.writeByteArray(payeeData);
        }

        final int size3 = null!= paymentRequestHash? paymentRequestHash.length:0;
        parcel.writeInt(size3);
        if (size3>0) {
            parcel.writeByteArray(paymentRequestHash);
        }

    }

    protected PaymentData(Parcel in) {
        this.standard = (PaymentStandard) in.readSerializable();

        this.payeeName = in.readString();
        this.payeeVerifiedBy = in.readString();
        this.paymentRequestUrl = in.readString();
        this.paymentUrl = in.readString();
        this.memo = in.readString();

        final int size = in.readInt();
        if (size > 0) {
            this.outputs = new PaymentOutput[size];
            in.readTypedArray(outputs, PaymentOutput.CREATOR);
        } else {
            this.outputs = null;
        }

        final int size2 = in.readInt();
        if (size2 > 0) {
            this.payeeData = new byte[size2];
            in.readByteArray(payeeData);
        } else {
            this.payeeData = null;
        }

        final int size3 = in.readInt();
        if (size3 > 0) {
            this.paymentRequestHash = new byte[size3];
            in.readByteArray(paymentRequestHash);
        } else {
            this.paymentRequestHash = null;
        }
    }

    public static final Creator<PaymentData> CREATOR = new Creator<PaymentData>() {
        @Override
        public PaymentData createFromParcel(Parcel in) {
            return new PaymentData(in);
        }

        @Override
        public PaymentData[] newArray(int size) {
            return new PaymentData[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    public boolean hasPayee() {
        return payeeName != null;
    }

    public boolean hasOutputs() {
        return outputs != null && outputs.length > 0;
    }

    public boolean hasAddress() {
        if (outputs == null || outputs.length != 1)
            return false;

        final Script script = outputs[0].script;
        return ScriptPattern.isP2PKH(script) || ScriptPattern.isP2SH(script)
                || ScriptPattern.isP2PK(script) || ScriptPattern.isP2WH(script);
    }

    public Address getAddress() {
        if (!hasAddress())
            throw new IllegalStateException();

        final Script script = outputs[0].script;
        return script.getToAddress(Constants.NETWORK_PARAMETERS, true);
    }

    public boolean hasAmount() {
        if (hasOutputs())
            for (final PaymentOutput output : outputs)
                if (output.hasAmount())
                    return true;

        return false;
    }

    public Coin getAmount() {
        Coin amount = Coin.ZERO;

        if (hasOutputs())
            for (final PaymentOutput output : outputs)
                if (output.hasAmount())
                    amount = amount.add(output.amount);

        if (amount.signum() != 0)
            return amount;
        else
            return null;
    }

    public boolean hasPaymentUrl() {
        return paymentUrl != null;
    }

    public boolean hasPaymentRequestUrl() {
        return paymentRequestUrl != null;
    }

    public boolean isHttpPaymentUrl() {
        return paymentUrl != null && (Utils.startsWithIgnoreCase(paymentUrl, "http:")
                || Utils.startsWithIgnoreCase(paymentUrl, "https:"));
    }

    public boolean isHttpPaymentRequestUrl() {
        return paymentRequestUrl != null && (Utils.startsWithIgnoreCase(paymentRequestUrl, "http:")
                || Utils.startsWithIgnoreCase(paymentRequestUrl, "https:"));
    }

    public boolean isBluetoothPaymentRequestUrl() {
        return BluetoothTools.isBluetoothUrl(paymentRequestUrl);
    }

    public boolean isBluetoothPaymentUrl() {
        return BluetoothTools.isBluetoothUrl(paymentUrl);
    }

    public boolean isSupportedPaymentUrl() {
        return isHttpPaymentUrl() || isBluetoothPaymentUrl();
    }

    public boolean isSupportedPaymentRequestUrl() {
        return isHttpPaymentRequestUrl() || isBluetoothPaymentRequestUrl();
    }


    @NonNull
    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();

        builder.append(getClass().getSimpleName());
        builder.append('[');
        builder.append(standard);
        builder.append(',');
        if (hasPayee()) {
            builder.append(payeeName);
            if (payeeVerifiedBy != null)
                builder.append("/").append(payeeVerifiedBy);
            builder.append(',');
        }
        builder.append(hasOutputs() ? Arrays.toString(outputs) : "null");
        builder.append(',');
        builder.append(paymentUrl);
        if (payeeData != null) {
            builder.append(",payeeData=");
            builder.append(Constants.HEX.encode(payeeData));
        }
        if (paymentRequestUrl != null) {
            builder.append(",paymentRequestUrl=");
            builder.append(paymentRequestUrl);
        }
        if (paymentRequestHash != null) {
            builder.append(",paymentRequestHash=");
            builder.append(Constants.HEX.encode(paymentRequestHash));
        }
        builder.append(']');

        return builder.toString();
    }
}
