package com.bethel.mycoolwallet.interfaces;

public interface IWalletBackupCallback {
    void onSuccess();
    void onFailed(Exception e);
}
