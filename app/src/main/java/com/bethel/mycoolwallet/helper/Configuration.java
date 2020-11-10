package com.bethel.mycoolwallet.helper;

import android.content.Context;

import com.google.common.base.Strings;

public class Configuration {
    private  static  Configuration config;

    private PreferenceWraper mPreference;

    private static final String PREFS_KEY_BEST_CHAIN_HEIGHT_EVER = "best_chain_height_ever";
    public static final String PREFS_KEY_TRUSTED_PEER = "trusted_peer";
    public static final String PREFS_KEY_TRUSTED_PEER_ONLY = "trusted_peer_only";
    public static final String PREFS_KEY_BTC_PRECISION = "btc_precision";

    private static final int PREFS_DEFAULT_BTC_SHIFT = 3;
    private static final int PREFS_DEFAULT_BTC_PRECISION = 2;

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

    public String getTrustedPeerHost() {
        return Strings.emptyToNull(mPreference.getString(PREFS_KEY_TRUSTED_PEER, "").trim());
    }

    public boolean getTrustedPeerOnly() {
        return mPreference.getBoolean(PREFS_KEY_TRUSTED_PEER_ONLY, false);
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

}
