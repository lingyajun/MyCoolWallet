package com.bethel.mycoolwallet.helper;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.format.DateUtils;

import com.google.common.base.Strings;

import org.bitcoinj.utils.MonetaryFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Configuration {
    private  static  Configuration config;

    private PreferenceWraper mPreference;

    private static final String PREFS_KEY_BEST_CHAIN_HEIGHT_EVER = "best_chain_height_ever";
    public static final String PREFS_KEY_TRUSTED_PEER = "trusted_peer";
    public static final String PREFS_KEY_TRUSTED_PEER_ONLY = "trusted_peer_only";
    public static final String PREFS_KEY_BTC_PRECISION = "btc_precision";
    private static final String PREFS_KEY_LAST_USED = "last_used";
    private static final String PREFS_KEY_CURRENCY_CODE = "currency_code";

    private final static String KEY_CACHE_EXCHANGE_RATE_SINGLE_CURRENCY_FORMATE ="exchange_rate_%s";
    private final static String KEY_CACHE_EXCHANGE_RATE_LIST ="exchange_rate_list";

    private static final int PREFS_DEFAULT_BTC_SHIFT = 3;
    private static final int PREFS_DEFAULT_BTC_PRECISION = 2;

    private static final Logger log = LoggerFactory.getLogger(Configuration.class);

    private Configuration(Context context) {
        mPreference = PreferenceWraper.getInstance(context);
    }

    public static Configuration instance(Context context) {
        if (null == config) {
            config = new Configuration(context);
        }
        return config;
    }

    public int getBestChainHeightEver() {
        return mPreference.getInt(PREFS_KEY_BEST_CHAIN_HEIGHT_EVER, 0);
    }

    public void maybeIncrementBestChainHeightEver(final int bestChainHeightEver) {
        if (bestChainHeightEver > getBestChainHeightEver())
            mPreference.putInt(PREFS_KEY_BEST_CHAIN_HEIGHT_EVER, bestChainHeightEver);
    }

    public String getMyCurrencyCode() {
        return Strings.emptyToNull(mPreference.getString(PREFS_KEY_CURRENCY_CODE, "").trim());
    }

    public void setMyCurrencyCode(String currencyCode) {
        mPreference.putString(PREFS_KEY_CURRENCY_CODE, currencyCode);
    }

    public String getTrustedPeerHost() {
        return Strings.emptyToNull(mPreference.getString(PREFS_KEY_TRUSTED_PEER, "").trim());
    }

    public boolean getTrustedPeerOnly() {
        return mPreference.getBoolean(PREFS_KEY_TRUSTED_PEER_ONLY, false);
    }

    public void cacheExchangeRateRequest(String currencyCode, String json) {
        String key = String.format(KEY_CACHE_EXCHANGE_RATE_SINGLE_CURRENCY_FORMATE, currencyCode);
        mPreference.putString(key, json);
    }

    public String getCacheExchangeRateRequest(String currencyCode) {
        String key = String.format(KEY_CACHE_EXCHANGE_RATE_SINGLE_CURRENCY_FORMATE, currencyCode);
        return mPreference.getString(key, null);
    }

    public void cacheExchangeRateListRequest(String json) {
        mPreference.putString(KEY_CACHE_EXCHANGE_RATE_LIST, json);
    }

    public String getCacheExchangeRateListRequest() {
        return mPreference.getString(KEY_CACHE_EXCHANGE_RATE_LIST, null);
    }

    public MonetaryFormat getFormat() {
        return getFormat(getBtcShift(), getBtcPrecision());
    }

    private MonetaryFormat getFormat(int shift, int precision) {
        final int minPrecision = shift <= 3 ? 2 : 0;
        final int decimalRepetitions = (precision - minPrecision) / 2;
        return new MonetaryFormat().shift(shift).minDecimals(minPrecision).repeatOptionalDecimals(2,
                decimalRepetitions);
    }

    public int getBtcShift() {
        final String precision = mPreference.getString(PREFS_KEY_BTC_PRECISION, null);
        if (precision != null)
            return precision.length() == 3 ? precision.charAt(2) - '0' : 0;
        else
            return PREFS_DEFAULT_BTC_SHIFT;
    }

    public int getBtcPrecision() {
        final String precision = mPreference.getString(PREFS_KEY_BTC_PRECISION, null);
        if (precision != null)
            return precision.charAt(0) - '0';
        else
            return PREFS_DEFAULT_BTC_PRECISION;
    }

    public boolean hasBeenUsed() {
        return mPreference.getSharedPreferences().contains(PREFS_KEY_LAST_USED);
    }

    public long getLastUsedAgo() {
        final long now = System.currentTimeMillis();

        return now - mPreference.getLong(PREFS_KEY_LAST_USED, 0);
    }

    public void touchLastUsed() {
        final long prefsLastUsed = mPreference.getLong(PREFS_KEY_LAST_USED, 0);
        final long now = System.currentTimeMillis();
        mPreference.putLong(PREFS_KEY_LAST_USED, now);

        log.info("just being used - last used {} minutes ago", (now - prefsLastUsed) / DateUtils.MINUTE_IN_MILLIS);
    }


    public void registerOnSharedPreferenceChangeListener(final SharedPreferences.OnSharedPreferenceChangeListener listener) {
        mPreference.registerOnSharedPreferenceChangeListener(listener);
    }

    public void unregisterOnSharedPreferenceChangeListener(final SharedPreferences.OnSharedPreferenceChangeListener listener) {
        mPreference.unregisterOnSharedPreferenceChangeListener(listener);
    }
}
