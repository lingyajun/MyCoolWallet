package com.bethel.mycoolwallet.utils;

public final class Utils {
    public static boolean startsWithIgnoreCase(final String string, final String prefix) {
        return string.regionMatches(true, 0, prefix, 0, prefix.length());
    }

}
