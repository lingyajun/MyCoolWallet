package com.bethel.mycoolwallet.request.privkey;

import com.bethel.mycoolwallet.request.payment.AbsTask;

import org.bitcoinj.core.ECKey;
import org.bitcoinj.crypto.BIP38PrivateKey;

public abstract class DecodeBIP38PrivateKeyTask extends AbsTask {
    private final BIP38PrivateKey encryptedKey;
    private final String passphrase;

    public DecodeBIP38PrivateKeyTask(final BIP38PrivateKey encryptedKey, final String passphrase) {
        this.encryptedKey = encryptedKey;
        this.passphrase = passphrase;
    }

    @Override
    public void run() {
        try {
            final ECKey decryptedKey = encryptedKey.decrypt(passphrase); // takes time
            runOnCallbackThread(()-> onSuccess(decryptedKey));
        } catch (final BIP38PrivateKey.BadPassphraseException e) {
            runOnCallbackThread(()-> onBadPassphrase(e.getMessage()));
        }

    }

    protected abstract void onSuccess(ECKey decryptedKey);

    protected abstract void onBadPassphrase(String message);
}
