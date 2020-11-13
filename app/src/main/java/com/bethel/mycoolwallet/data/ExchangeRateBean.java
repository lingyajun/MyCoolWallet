package com.bethel.mycoolwallet.data;

import androidx.annotation.NonNull;

import org.bitcoinj.utils.ExchangeRate;

public class ExchangeRateBean {
    public final ExchangeRate rate;
    public final String source;

    public ExchangeRateBean(ExchangeRate rate, String source) {
        checkcheckNotNull(rate.fiat.currencyCode);
        this.rate = rate;
        this.source = source;
    }

    public String getCurrencyCode() {
        return rate.fiat.currencyCode;
    }

    private static void checkcheckNotNull(Object reference) {
        if (reference == null) {
            throw new NullPointerException();
        }
    }

    @NonNull
    @Override
    public String toString() {
        return String.format("ExchangeRateBean: [%s, %s, %s]", rate.fiat.currencyCode, rate.fiat.value, source);
    }
}
