package com.bethel.mycoolwallet;

import com.bethel.mycoolwallet.data.payment.PaymentData;
import com.bethel.mycoolwallet.helper.parser.BinaryInputParser;
import com.bethel.mycoolwallet.helper.parser.StringInputParser;
import com.bethel.mycoolwallet.http.HttpUtil;

import org.bitcoinj.core.DumpedPrivateKey;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.VerificationException;
import org.bitcoinj.crypto.BIP38PrivateKey;
import org.bitcoinj.params.MainNetParams;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void addition_isCorrect() {
        assertEquals(4, 2 + 2);
    }

//    @Test
//    public void requestExchangeRate() {
//        HttpUtil.requestExchangeRate("CNY", null);
//    }

    @Test
    public void bip38_noCompression_noEcMultiply_test() throws Exception {
        //如果你看到一个6P开头的的密钥，这就意味着该密钥是被加密过；输入生成一个加密私钥对象
        BIP38PrivateKey encryptedKey = BIP38PrivateKey.fromBase58( MainNetParams.get(),
                "6PRVWUbkzzsbcVac2qwfssoUJAN1Xhrg6bNk8J7Nzm5H7kxEbn2Nh2ZoGg");
        //用密码解密,长密码作为口令，通常由多个单词或一段复杂的数字字母字符串组成
        ECKey key = encryptedKey.decrypt("TestingOneTwoThree");
        //生成转储私钥对象
        DumpedPrivateKey dumpedPrivateKey=key.getPrivateKeyEncoded( MainNetParams.get());
        String privateKey= dumpedPrivateKey.toString();
        //该密钥回到可被用在任何钱包WIF格式的私钥（前缀为5）
        assertEquals("5KN7MzqK5wt2TP1fQCYyHBtDrXdJuXbUzm4A9rKAteGu3Qi5CVR",
                privateKey);
    }

    @Test
    public void inputParser_test() {
        new StringInputParser("5KN7MzqK5wt2TP1fQCYyHBtDrXdJuXbUzm4A9rKAteGu3Qi5CVR") {
            @Override
            public void error(int messageResId, Object... messageArgs) {

            }

            @Override
            public void handlePaymentData(PaymentData data) {

            }

            @Override
            public void handleDirectTransaction(Transaction transaction) throws VerificationException {

            }

            @Override
            public void requestPassphrase(BIP38PrivateKey encryptKey) {

            }
        };

        new BinaryInputParser("abc", new byte[]{'a', 'c'}) {
            @Override
            public void error(int messageResId, Object... messageArgs) {

            }

            @Override
            public void handlePaymentData(PaymentData data) {

            }
        };
    }
}