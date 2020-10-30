package com.bethel.mycoolwallet.utils;

import android.widget.TextView;

import androidx.annotation.Nullable;

import org.bitcoinj.core.Monetary;
import org.bitcoinj.utils.MonetaryFormat;

import java.util.Currency;

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

    public static CharSequence format(@Nullable final MonetaryFormat format, final boolean signed,
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

    public static String currencySymbol(final String currencyCode) {
        try {
            final Currency currency = Currency.getInstance(currencyCode);
            return currency.getSymbol();
        } catch (final IllegalArgumentException x) {
            return currencyCode;
        }
    }


    public static MonetaryFormat getMaxPrecisionFormat(int shift) {
//        final int shift = getBtcShift();
        if (shift == 0)
            return new MonetaryFormat().shift(0).minDecimals(2).optionalDecimals(2, 2, 2);
        else if (shift == 3)
            return new MonetaryFormat().shift(3).minDecimals(2).optionalDecimals(2, 1);
        else
            return new MonetaryFormat().shift(6).minDecimals(0).optionalDecimals(2);
    }

    public static MonetaryFormat getFormat(int shift, int precision) {
//        final int shift = getBtcShift();
        final int minPrecision = shift <= 3 ? 2 : 0;
        final int decimalRepetitions = (precision - minPrecision) / 2;
//        final int decimalRepetitions = (getBtcPrecision() - minPrecision) / 2;
        return new MonetaryFormat().shift(shift).minDecimals(minPrecision).repeatOptionalDecimals(2,
                decimalRepetitions);
    }

}
