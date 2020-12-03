package com.bethel.mycoolwallet.utils;

import androidx.annotation.Nullable;

import org.bitcoinj.params.MainNetParams;

import java.nio.charset.Charset;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Bethel
 */
public final class Commons {
    public static final Charset UTF_8 = Charset.forName("UTF-8");
    public static final Charset US_ASCII = Charset.forName("US-ASCII");

    // https://www.blockchain.com/btc/block/000000000000000000029aa834ab9127c23fc66e4b2268a9584fc88a48d6b288
    public static final String MAIN_NET_VIEW = "https://www.blockchain.com/btc/";
    // https://www.blockchain.com/btc-testnet/block/00000000000000493226636e9139fb52bc22cbdb294bb19b60a735ea1ce7cf17
    public static final String TEST_NET_VIEW = "https://www.blockchain.com/btc-testnet/";
    public static final String BLOCK_CHAIN_VIEW = MainNetParams.get().equals(Constants.NETWORK_PARAMETERS)?
            MAIN_NET_VIEW: TEST_NET_VIEW;


    public static class Formats {
        public static final Pattern PATTERN_MONETARY_SPANNABLE = Pattern.compile("(?:([\\p{Alpha}\\p{Sc}]++)\\s?+)?" // prefix
                + "([\\+\\-" + Constants.CURRENCY_PLUS_SIGN + Constants.CURRENCY_MINUS_SIGN
                + "]?+(?:\\d*+\\.\\d{0,2}+|\\d++))" // significant
                + "(\\d++)?"); // insignificant

        public static int PATTERN_GROUP_PREFIX = 1; // optional
        public static int PATTERN_GROUP_SIGNIFICANT = 2; // mandatory
        public static int PATTERN_GROUP_INSIGNIFICANT = 3; // optional

        private static final Pattern PATTERN_MEMO = Pattern.compile(
                "(?:Payment request for Coinbase order code: (.+)|Payment request for BitPay invoice (.+) for merchant (.+))",
                Pattern.CASE_INSENSITIVE);

        @Nullable
        public static String[] sanitizeMemo(final @Nullable String memo) {
            if (memo == null)
                return null;

            final Matcher m = PATTERN_MEMO.matcher(memo);
            if (m.matches() && m.group(1) != null)
                return new String[] { m.group(1) + " (via Coinbase)" };
            else if (m.matches() && m.group(2) != null)
                return new String[] { m.group(2) + " (via BitPay)", m.group(3) };
            else
                return new String[] { memo };
        }
    }
}
