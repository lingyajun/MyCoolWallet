package com.bethel.mycoolwallet.interfaces;

/**
 * 请求输入密码
 *
 * InputParserOld
 * BIP38PrivateKey
 */
public interface IRequestPassphrase {
    void onResult(String passphrase);
}
