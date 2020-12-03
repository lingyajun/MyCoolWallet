package com.bethel.mycoolwallet.data.tx_list;

import android.content.Context;

import androidx.core.content.ContextCompat;

import com.bethel.mycoolwallet.R;

public enum ColorType {
    //  重要的 ,值得关注的
    Significant(R.color.fg_significant),
    LessSignificant(R.color.fg_less_significant),

    // 无足轻重的
    Insignificant(R.color.fg_insignificant),
    ValuePositve(R.color.fg_value_positive),
    ValueNegative(R.color.fg_value_negative),
    Error(R.color.fg_error)
;
    private int colorId;

    ColorType(int colorId) {
        this.colorId = colorId;
    }

    public int getColor(Context context) {
        return ContextCompat.getColor(context, colorId);
    }
}
