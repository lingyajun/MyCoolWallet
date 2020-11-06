package com.bethel.mycoolwallet.data;

import com.bethel.mycoolwallet.R;

public enum PasswordStrength {
    WEAK(R.string.encrypt_keys_dialog_password_strength_weak, R.color.fg_password_strength_weak),
    FAIR(R.string.encrypt_keys_dialog_password_strength_fair, R.color.fg_password_strength_fair),
    GOOD(R.string.encrypt_keys_dialog_password_strength_good, R.color.fg_password_strength_good),
    STRONG(R.string.encrypt_keys_dialog_password_strength_strong, R.color.fg_password_strength_strong);

    PasswordStrength(int text, int colorId) {
        this.text = text;
        this.colorId = colorId;
    }

    private int text, colorId;

    public int getTextId() {
        return text;
    }

    public int getColorId() {
        return colorId;
    }
}
