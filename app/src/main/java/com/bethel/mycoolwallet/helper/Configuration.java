package com.bethel.mycoolwallet.helper;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.format.DateUtils;

import com.bethel.mycoolwallet.data.ExchangeRateBean;
import com.google.common.base.Strings;

import org.bitcoinj.core.Coin;
import org.bitcoinj.utils.Fiat;
import org.bitcoinj.utils.MonetaryFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public enum  Configuration {
    /**
     * 枚举方法 --  单例模式
     * (1)自由序列化。
     * (2)保证只有一个实例。
     * (3)线程安全。
     */
    INSTANCE;
//public class Configuration {
//    private  static  Configuration config;

    private PreferenceWraper mPreference;

    private static final String PREFS_KEY_BEST_CHAIN_HEIGHT_EVER = "best_chain_height_ever";
    public static final String PREFS_KEY_TRUSTED_PEER = "trusted_peer";
    public static final String PREFS_KEY_TRUSTED_PEER_ONLY = "trusted_peer_only";
    public static final String PREFS_KEY_BTC_PRECISION = "btc_precision";
    private static final String PREFS_KEY_LAST_USED = "last_used";
    public static final String PREFS_KEY_CURRENCY_CODE = "currency_code";
    public static final String PREFS_KEY_EXCHANGE_CURRENCY = "exchange_currency";

    private final static String KEY_CACHE_EXCHANGE_RATE_SINGLE_CURRENCY_FORMATE ="exchange_rate_%s";
    private final static String KEY_CACHE_EXCHANGE_RATE_LIST ="exchange_rate_list";
    public static final String PREFS_KEY_REMIND_BACKUP = "remind_backup";
    private static final String PREFS_KEY_LAST_BACKUP = "last_backup";
    private static final String PREFS_KEY_LAST_RESTORE = "last_restore";
    public static final String PREFS_KEY_OWN_NAME = "own_name";

    public static final String PREFS_KEY_DISCLAIMER = "disclaimer";
    private static final String PREFS_KEY_LAST_EXCHANGE_DIRECTION = "last_exchange_direction";
    public static final String PREFS_KEY_SEND_COINS_AUTOCLOSE = "send_coins_autoclose";
    private static final String PREFS_KEY_LAST_BLOCKCHAIN_RESET = "last_blockchain_reset";
    public static final String PREFS_KEY_DATA_USAGE = "data_usage";

    public static final String PREFS_KEY_GUIDE_USER = "guide_user_main";

    private static final String PREFS_KEY_CACHED_EXCHANGE_CURRENCY = "cached_exchange_currency";
    private static final String PREFS_KEY_CACHED_EXCHANGE_RATE_COIN = "cached_exchange_rate_coin";
    private static final String PREFS_KEY_CACHED_EXCHANGE_RATE_FIAT = "cached_exchange_rate_fiat";

    private static final int PREFS_DEFAULT_BTC_SHIFT = 3;
    private static final int PREFS_DEFAULT_BTC_PRECISION = 2;

    private static final Logger log = LoggerFactory.getLogger(Configuration.class);

//    private Configuration(Context context) {
//        mPreference = PreferenceWraper.getInstance(context);
//    }

//    public static Configuration instance(Context context) {
//        if (null == config) {
//            config = new Configuration(context);
//        }
//        return config;
//    }
    public void init(Context context) {
        if (null == mPreference)   mPreference = PreferenceWraper.getInstance(context);
    }

    public int getBestChainHeightEver() {
        return mPreference.getInt(PREFS_KEY_BEST_CHAIN_HEIGHT_EVER, 0);
    }

    public void maybeIncrementBestChainHeightEver(final int bestChainHeightEver) {
        if (bestChainHeightEver > getBestChainHeightEver()) {
            mPreference.putInt(PREFS_KEY_BEST_CHAIN_HEIGHT_EVER, bestChainHeightEver);
        }
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

    /**
     *         <item>8</item>
     *         <item>6</item>
     *         <item>4</item>
     *         <item>2/3</item>
     *         <item>0/6</item>
     * @return
     */
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

    public Coin getBtcBase() {
        final int shift = getBtcShift();
        if (shift == 0)
            return Coin.COIN;
        else if (shift == 3)
            return Coin.MILLICOIN;
        else if (shift == 6)
            return Coin.MICROCOIN;
        else
            throw new IllegalStateException("cannot handle shift: " + shift);
    }

    public MonetaryFormat getMaxPrecisionFormat() {
        final int shift = getBtcShift();
        if (shift == 0)
            return new MonetaryFormat().shift(0).minDecimals(2).optionalDecimals(2, 2, 2);
        else if (shift == 3)
            return new MonetaryFormat().shift(3).minDecimals(2).optionalDecimals(2, 1);
        else
            return new MonetaryFormat().shift(6).minDecimals(0).optionalDecimals(2);
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

    public boolean remindBackup() {
        return mPreference.getBoolean(PREFS_KEY_REMIND_BACKUP, true);
    }

    public long getLastBackupTime() {
        return mPreference.getLong(PREFS_KEY_LAST_BACKUP, 0);
    }

    public void armBackupReminder() {
        mPreference.putBoolean(PREFS_KEY_REMIND_BACKUP, true);
    }

    public void disarmBackupReminder() {
        mPreference.putBoolean(PREFS_KEY_REMIND_BACKUP, false);
           mPreference.putLong(PREFS_KEY_LAST_BACKUP, System.currentTimeMillis());
    }

    public void updateLastRestoreTime() {
        mPreference.putLong(PREFS_KEY_LAST_RESTORE, System.currentTimeMillis());
    }

    public long getLastRestoreTime() {
        return mPreference.getLong(PREFS_KEY_LAST_RESTORE, 0);
    }

    public String getOwnName() {
        return Strings.emptyToNull(mPreference.getString(PREFS_KEY_OWN_NAME, "").trim());
    }

    public boolean getDisclaimerEnabled() {
        return mPreference.getBoolean(PREFS_KEY_DISCLAIMER, true);
    }

    public boolean getLastExchangeDirection() {
        return mPreference.getBoolean(PREFS_KEY_LAST_EXCHANGE_DIRECTION, true);
    }

    public void setLastExchangeDirection(final boolean exchangeDirection) {
        mPreference.putBoolean(PREFS_KEY_LAST_EXCHANGE_DIRECTION, exchangeDirection) ;
    }

    public boolean hasGuideUser() {
        return mPreference.getBoolean(PREFS_KEY_GUIDE_USER, false);
    }

    public void guideUser() {
        mPreference.putBoolean(PREFS_KEY_GUIDE_USER, true) ;
    }

    public boolean getSendCoinsAutoClose() {
        return mPreference.getBoolean(PREFS_KEY_SEND_COINS_AUTOCLOSE, true);
    }

    public long getLastBlockchainResetTime() {
        return mPreference.getLong(PREFS_KEY_LAST_BLOCKCHAIN_RESET, 0);
    }

    public void updateLastBlockchainResetTime() {
        mPreference.putLong(PREFS_KEY_LAST_BLOCKCHAIN_RESET, System.currentTimeMillis());
    }


    public ExchangeRateBean getCachedExchangeRate() {
        final  SharedPreferences prefs = mPreference.mSharedPref;
        if (prefs.contains(PREFS_KEY_CACHED_EXCHANGE_CURRENCY) && prefs.contains(PREFS_KEY_CACHED_EXCHANGE_RATE_COIN)
                && prefs.contains(PREFS_KEY_CACHED_EXCHANGE_RATE_FIAT)) {
            final String cachedExchangeCurrency = prefs.getString(PREFS_KEY_CACHED_EXCHANGE_CURRENCY, null);
            final Coin cachedExchangeRateCoin = Coin.valueOf(prefs.getLong(PREFS_KEY_CACHED_EXCHANGE_RATE_COIN, 0));
            final Fiat cachedExchangeRateFiat = Fiat.valueOf(cachedExchangeCurrency,
                    prefs.getLong(PREFS_KEY_CACHED_EXCHANGE_RATE_FIAT, 0));
            return new ExchangeRateBean(new org.bitcoinj.utils.ExchangeRate(cachedExchangeRateCoin, cachedExchangeRateFiat),
                    null);
        }

        return null;
    }

    public void setCachedExchangeRate(final ExchangeRateBean cachedExchangeRate) {
        mPreference.putString(PREFS_KEY_CACHED_EXCHANGE_CURRENCY, cachedExchangeRate.getCurrencyCode());
        mPreference.putLong(PREFS_KEY_CACHED_EXCHANGE_RATE_COIN, cachedExchangeRate.rate.coin.value);
        mPreference.putLong(PREFS_KEY_CACHED_EXCHANGE_RATE_FIAT, cachedExchangeRate.rate.fiat.value);
    }

    public void registerOnSharedPreferenceChangeListener(final SharedPreferences.OnSharedPreferenceChangeListener listener) {
        mPreference.registerOnSharedPreferenceChangeListener(listener);
    }

    public void unregisterOnSharedPreferenceChangeListener(final SharedPreferences.OnSharedPreferenceChangeListener listener) {
        mPreference.unregisterOnSharedPreferenceChangeListener(listener);
    }
}
