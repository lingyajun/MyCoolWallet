package com.bethel.mycoolwallet.request.payment;

import com.bethel.mycoolwallet.manager.MyCoolWalletManager;

import org.bitcoinj.core.Coin;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.InsufficientMoneyException;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.crypto.KeyCrypterException;
import org.bitcoinj.wallet.SendRequest;
import org.bitcoinj.wallet.Wallet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class SendCoinsOfflineTask extends AbsTask {
    private final Wallet wallet;
    private   final SendRequest sendRequest;

    public SendCoinsOfflineTask(Wallet wallet, SendRequest sendRequest) {
        super();
        this.wallet = wallet;
        this.sendRequest = sendRequest;
    }

    private static final Logger log = LoggerFactory.getLogger(SendCoinsOfflineTask.class);

    @Override
    public void run() {
        MyCoolWalletManager.propagate();
        log.info("sending: {}", sendRequest);

        try {
            final Transaction tx = wallet.sendCoinsOffline(sendRequest);
            runOnCallbackThread(()-> onSuccess(tx));

            log.info("send successful, transaction committed: {}", tx.getTxId());
        } catch (final InsufficientMoneyException e) {
            final Coin missing = e.missing;
            runOnCallbackThread(() -> onInsufficientMoney(missing));

            if (null!=missing) {
                log.info("send failed, {} missing", missing.toFriendlyString());
            } else {
                log.info("send failed, insufficient coins");
            }
        } catch (final ECKey.KeyIsEncryptedException e) {
            runOnCallbackThread(() -> onInvalidEncryptionKey());
            log.info("send failed, key is encrypted: {}", e.getMessage());
        } catch (final KeyCrypterException e) {
            final boolean isEncrypted = wallet.isEncrypted();
            runOnCallbackThread(() -> {
                if (isEncrypted) {
                    onInvalidEncryptionKey();
                } else {
                    onFailure(e);
                }
            });
            log.info("send failed, key crypter exception: {}", e.getMessage());
        } catch (final Wallet.CouldNotAdjustDownwards e) {
            runOnCallbackThread(() -> onFailure(e) );
            log.info("send failed, could not adjust downwards: {}", e.getMessage());
        } catch (final Wallet.CompletionException e) {
            runOnCallbackThread(() -> onFailure(e) );
            log.info("send failed, cannot complete: {}", e.getMessage());
        }
    }

    protected abstract void onSuccess(Transaction transaction);

    protected abstract void onInsufficientMoney(Coin missing);

    protected abstract void onInvalidEncryptionKey();

    protected abstract void onFailure(Exception exception);
}
