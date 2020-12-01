package com.bethel.mycoolwallet.utils;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.bethel.mycoolwallet.data.PasswordStrength;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public final class Utils {
    public static boolean startsWithIgnoreCase(final String string, final String prefix) {
        return string.regionMatches(true, 0, prefix, 0, prefix.length());
    }

    public static boolean isHttpUrl(String url) {
        return url != null && (Utils.startsWithIgnoreCase(url, "http:")
                || Utils.startsWithIgnoreCase(url, "https:"));
    }

    /**
     * 将本地资源图片大小缩放
     * @param resId
     * @param w
     * @param h
     * @return
     */
    public static Drawable zoomImage( Resources res ,int resId, int w, int h){
//        Resources res = mContext.getResources();
        Bitmap oldBmp = BitmapFactory.decodeResource(res, resId);
        Bitmap newBmp = Bitmap.createScaledBitmap(oldBmp,w, h, true);
        Drawable drawable = new BitmapDrawable(res, newBmp);
        return drawable;
    }

    /**
     * 根据手机的分辨率从 dp 的单位 转成为 px(像素)
     */
    public static int dip2px(Context context, float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    /**
     * 根据手机的分辨率从 px(像素) 的单位 转成为 dp
     */
    public static int px2dip(Context context, float pxValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (pxValue / scale + 0.5f);
    }

    public static PasswordStrength getPinPasswordStrength(@NotNull CharSequence password) {
        int length = password.length();
        if (length < 4) {
            return PasswordStrength.WEAK;
        } else if (length < 6) {
            return PasswordStrength.FAIR;
        } else if (length < 8) {
            return PasswordStrength.GOOD;
        } else {
            return PasswordStrength.STRONG;
        }
    }

    public static PasswordStrength getBackupWalletPasswordStrength(@NotNull CharSequence password) {
        int length = password.length();
        if (length < 6) {
            return PasswordStrength.WEAK;
        } else if (length < 8) {
            return PasswordStrength.FAIR;
        } else if (length < 10) {
            return PasswordStrength.GOOD;
        } else {
            return PasswordStrength.STRONG;
        }
    }

    public static boolean isNetworkConnected(Context context) {
        try {
            ConnectivityManager  manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = manager.getActiveNetworkInfo();
            return null!=networkInfo && networkInfo.isConnected();
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean equals(Object a, Object b) {
        return (a == b) || (a != null && a.equals(b));
    }

    public static boolean isEmpty(Collection  collection) {
        return null==collection || collection.isEmpty();
    }

    public static int size(Collection  collection) {
        return null!=collection ? collection.size() : 0;
    }
}
