package com.bethel.mycoolwallet.utils;

import android.widget.TextView;

import androidx.annotation.Nullable;

import org.bitcoinj.core.Monetary;
import org.bitcoinj.utils.MonetaryFormat;

import static androidx.core.util.Preconditions.checkArgument;

public final class CurrencyTools {
    public static void setText(TextView tv, @Nullable final MonetaryFormat format, final boolean signed,
                               @Nullable final Monetary monetary) {
        CharSequence text = format(format, signed, monetary);
        tv.setText(text);
    }

    public static void setText(TextView tv, @Nullable final MonetaryFormat format,
                               @Nullable final Monetary monetary) {
        setText(tv, format, false, monetary);
    }

    private static CharSequence format(@Nullable final MonetaryFormat format, final boolean signed,
                                       final Monetary monetary) {
        if (monetary == null)
            return "";
        if (format == null)
            return monetary.toString();

        checkArgument(monetary.signum() >= 0 || signed);

        if (signed)
            return format.negativeSign(Constants.CURRENCY_MINUS_SIGN).positiveSign(Constants.CURRENCY_PLUS_SIGN)
                    .format(monetary);
        else
            return format.format(monetary);
    }

}
