package com.bethel.mycoolwallet.data.payment;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import com.bethel.mycoolwallet.utils.Constants;

import org.bitcoinj.core.Coin;
import org.bitcoinj.protocols.payments.PaymentProtocol;
import org.bitcoinj.protocols.payments.PaymentProtocolException;
import org.bitcoinj.script.Script;
import org.bitcoinj.script.ScriptException;
import org.bitcoinj.script.ScriptPattern;

/**
 * PaymentIntent.Output
 * link {PaymentProtocol.Output}
 */
public class PaymentOutput implements Parcelable {
    public final Coin amount;
    public final Script script;

    public PaymentOutput(Coin amount, Script script) {
        this.amount = amount;
        this.script = script;
    }

    protected PaymentOutput(Parcel in) {
        amount =(Coin) in.readSerializable();

        int length = in.readInt();
        final byte[] program = new byte[length];
        in.readByteArray(program);
        script = new Script(program);
    }

    public boolean hasAmount() {
        return null!=amount && 0!=amount.signum();
    }

    public static PaymentOutput valueOf(PaymentProtocol.Output output) throws PaymentProtocolException.InvalidOutputs {
        final Coin amount = output.amount;
        final byte[] program = output.scriptData;
        try {
            final Script script = new Script(program);
            return new PaymentOutput(amount, script);
        } catch (ScriptException e) {
            String msg = "unparseable script in output: " + Constants.HEX.encode(program);
            throw  new PaymentProtocolException.InvalidOutputs(msg);
        }
    }

    public static final Creator<PaymentOutput> CREATOR = new Creator<PaymentOutput>() {
        @Override
        public PaymentOutput createFromParcel(Parcel in) {
            return new PaymentOutput(in);
        }

        @Override
        public PaymentOutput[] newArray(int size) {
            return new PaymentOutput[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeSerializable(amount);

        //  serialized program as a newly created byte array.
        final byte[] program = script.getProgram();
        parcel.writeByteArray(program);
        parcel.writeInt(program.length);
    }

    @NonNull
    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();

        builder.append(getClass().getSimpleName());
        builder.append('[');
        builder.append(hasAmount() ? amount.toPlainString() : "null");
        builder.append(',');
        if (ScriptPattern.isP2PKH(script) || ScriptPattern.isP2SH(script)
                || ScriptPattern.isP2WH(script)) {
            builder.append(script.getToAddress(Constants.NETWORK_PARAMETERS));
        }
        else if (ScriptPattern.isP2PK(script)) {
            builder.append(Constants.HEX.encode(ScriptPattern.extractKeyFromP2PK(script)));
        }
        else if (ScriptPattern.isSentToMultisig(script)) {
            builder.append("multisig");
        }
        else {
            builder.append("unknown");
        }
        builder.append(']');
        return builder.toString();
    }
}
