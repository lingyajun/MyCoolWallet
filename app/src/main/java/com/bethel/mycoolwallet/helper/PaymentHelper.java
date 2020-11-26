package com.bethel.mycoolwallet.helper;

import android.text.TextUtils;

import com.bethel.mycoolwallet.interfaces.IDeriveKeyCallBack;
import com.bethel.mycoolwallet.interfaces.ISignPaymentCallback;
import com.bethel.mycoolwallet.manager.MyCoolWalletManager;
import com.bethel.mycoolwallet.utils.Constants;

import org.bitcoin.protocols.payments.Protos;
import org.bitcoinj.core.Address;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.InsufficientMoneyException;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.crypto.KeyCrypter;
import org.bitcoinj.crypto.KeyCrypterException;
import org.bitcoinj.crypto.KeyCrypterScrypt;
import org.bitcoinj.script.Script;
import org.bitcoinj.script.ScriptBuilder;
import org.bitcoinj.utils.ExchangeRate;
import org.bitcoinj.wallet.SendRequest;
import org.bitcoinj.wallet.Wallet;
import org.bouncycastle.crypto.params.KeyParameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;

import static androidx.core.util.Preconditions.checkNotNull;

/**
 * 「0。 生成解密钱包所需要格式的'密钥', 用于钱包支付密码解密」
 * 1. 构造 SendRequest
 * 2。 SendRequest 参数配置
 * 3。 对交易进行签名
 * 4。广播签名后的交易数据
 */
public class PaymentHelper {
    private static final Logger log = LoggerFactory.getLogger(PaymentHelper.class);

    private void sendCoins(Address toAddress, Coin amount, Wallet wallet) throws InsufficientMoneyException {
        Script outputScript = ScriptBuilder.createOutputScript(toAddress);
        Transaction transaction = new Transaction(Constants.NETWORK_PARAMETERS);
        transaction.addOutput(amount, outputScript);

        SendRequest sendRequest = SendRequest.forTx(transaction);
        SendRequest sendRequest1 = SendRequest.to(toAddress, amount);
        // 参数配置
        sendRequest.emptyWallet = false; // all coins selected by the coin selector are sent to the first output in tx.
        sendRequest.feePerKb = Coin.MICROCOIN;
        sendRequest.memo = "hi"; // this memo is recorded with the transaction during completion.
        sendRequest.exchangeRate = null; // this exchange rate is recorded with the transaction during completion.
        sendRequest.aesKey = null; // The AES key to use to decrypt the private keys before signing.

        // sign and send
        Transaction tx = wallet.sendCoinsOffline(sendRequest);
    }

    /** 0。 生成解密钱包所需要格式的'密钥', 用于钱包支付密码解密。
     *
     * @param wallet
     * @param password
     */
    public static void deriveKey4DescryptWalet(Wallet wallet, final String password, IDeriveKeyCallBack callBack)  {
        deriveKey4DescryptWalet(wallet, password, 0, callBack);
    }
    public static void deriveKey4DescryptWalet(Wallet wallet, final String password,
                                        final int appScryptIterations, IDeriveKeyCallBack callBack)  {
        if (!wallet.isEncrypted() || TextUtils.isEmpty(password)) return;
        final KeyCrypter keyCrypter = checkNotNull(wallet.getKeyCrypter());
        MyCoolWalletManager.propagate();
        try {
            // 获取解密钱包所需要格式的 密钥
            final KeyParameter keyParameter = keyCrypter.deriveKey(password);

            KeyParameter result = keyParameter;
            boolean changed = false;
            if (keyCrypter instanceof KeyCrypterScrypt && 0 < appScryptIterations) {
                //  是否更换钱包的密钥「AES加密算法迭代次数变更」
                final long scrypterN = ((KeyCrypterScrypt) keyCrypter).getScryptParameters().getN();
                if (scrypterN != appScryptIterations) {
                    // changeEncryptionKey and re-encrypting wallet
                    KeyCrypterScrypt keyCrypter2 = new KeyCrypterScrypt(appScryptIterations);
                    KeyParameter keyParameter2 = keyCrypter2.deriveKey(password);

                    wallet.changeEncryptionKey(keyCrypter2, keyParameter, keyParameter2);
                    result = keyParameter2;
                    changed = true;
                }
            }
            log.info("derive Key success");
            if (null!=callBack) callBack.onSuccess(result, changed);
        } catch (KeyCrypterException e) {
            log.error("derive Key failed", e);
            if (null != callBack) callBack.onFailed(e.getMessage());
        }
    }

    /** 1. 构造 SendRequest
     *
     * @param toAddress
     * @param amount
     */
    public static SendRequest buildSendRequest(Address toAddress, Coin amount) {
        return SendRequest.to(toAddress, amount);
    }

    /** 2. SendRequest 参数配置
     *
     */
    public static void paramSendRequest(SendRequest req, boolean emptyWallet ,Coin feePerKb,String memo ,
                                 ExchangeRate exchangeRate,  KeyParameter aesKey) {}

    /** 3. 对交易进行签名
     *
     * @param req
     * todo handle  Exceptions
     */
    public static void signPayment(Wallet wallet, SendRequest req, ISignPaymentCallback callback) {
        MyCoolWalletManager.propagate();
        try {
            final Transaction transaction =  wallet.sendCoinsOffline(req); // can take a long time
            if (null != callback) callback.onSuccess(transaction);
        } catch (InsufficientMoneyException e){
            // you don't have enough money available to perform the requested operation.
        }
        catch ( ECKey.KeyIsEncryptedException e){
            // there's something wrong with aesKey.
        }
        catch (KeyCrypterException e) {
            // encryption / decryption exception
        }
        catch (Wallet.CouldNotAdjustDownwards e) {
//            Thrown when we were trying to empty the wallet, and
//            the fee was smaller than the min payment.
        }
        catch (Wallet.CompletionException e) {
            //  thrown in {@link Wallet#completeTx(SendRequest)}.
        }

    }

    /** 4。广播签名后的交易数据
     *
     */
    public void broadcastPayment(Transaction signedTx) {
        // a. BlockchainService.broadcastTransaction()
//   todo     Protos.Payment PaymentProtocol.createPaymentMessage(List<Transaction> transactions,
//                @Nullable Coin refundAmount, @Nullable Address refundAddress, @Nullable String memo,
//        @Nullable byte[] merchantData) ;
        // http

        // bluetooth
    }
}
