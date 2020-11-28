package com.bethel.mycoolwallet.request.payment;

import androidx.core.util.Preconditions;

import com.bethel.mycoolwallet.manager.MyCoolWalletManager;

import org.bitcoinj.crypto.KeyCrypter;
import org.bitcoinj.crypto.KeyCrypterException;
import org.bitcoinj.crypto.KeyCrypterScrypt;
import org.bitcoinj.wallet.Wallet;
import org.bouncycastle.crypto.params.KeyParameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 导出密钥
 */
public abstract class DeriveKeyTask extends AbsTask {
    private final Wallet wallet;
    private final String password;
    private final int scryptIterationsTarget;

    public DeriveKeyTask(Wallet wallet, String password, int scryptIterationsTarget) {
        super();
        this.wallet = wallet;
        this.password = password;
        this.scryptIterationsTarget = scryptIterationsTarget;
    }

    private static final Logger log = LoggerFactory.getLogger(DeriveKeyTask.class);

    @Override
    public void run() {
        Preconditions.checkState(wallet.isEncrypted());
        final KeyCrypter crypter = Preconditions.checkNotNull(wallet.getKeyCrypter());
        MyCoolWalletManager.propagate();

        KeyParameter keyParameter = null;
        try {
            keyParameter = crypter.deriveKey(password);
        } catch (KeyCrypterException e) {
            log.error("deriveKey error: {}", e.getMessage());
        }

        if (null== keyParameter || !(crypter instanceof KeyCrypterScrypt) || 1> scryptIterationsTarget) {
            onSuccessCallbackThread(keyParameter, false);
            return;
        }

        // crypter instanceof KeyCrypterScrypt
        final long iterations = ((KeyCrypterScrypt) crypter) .getScryptParameters().getN();
        if (iterations == scryptIterationsTarget) {
            onSuccessCallbackThread(keyParameter, false);
            return;
        }

        // iterations != scryptIterationsTarget
        log.info("upgrading scrypt iterations from {} to {}; re-encrypting wallet",
                iterations, scryptIterationsTarget);
        final KeyCrypterScrypt newCrypter = new KeyCrypterScrypt(scryptIterationsTarget);
        final KeyParameter newKey = newCrypter.deriveKey(password);

        boolean wasChanged = false;
        KeyParameter result = keyParameter;
        try {
            wallet.changeEncryptionKey(newCrypter, keyParameter, newKey);
            wasChanged = true;
            result = newKey;
            log.info("scrypt upgrade succeeded");
        } catch (Exception e) {
            log.info("scrypt upgrade failed: {}", e.getMessage());
        }

        onSuccessCallbackThread(result, wasChanged);
    }

    protected abstract void onSuccess(KeyParameter encryptionKey, boolean changed);

    private void onSuccessCallbackThread(final KeyParameter encryptionKey,final boolean changed){
        runOnCallbackThread(() -> onSuccess(encryptionKey, changed));
    }
}
