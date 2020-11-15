package com.bethel.mycoolwallet.interfaces;

import org.bouncycastle.crypto.params.KeyParameter;

public interface IDeriveKeyCallBack {
    void onSuccess(KeyParameter encryptKey, boolean isWalletChanged);
    void onFailed (String error);
}
