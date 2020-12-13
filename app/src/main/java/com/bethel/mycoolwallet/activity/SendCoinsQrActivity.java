package com.bethel.mycoolwallet.activity;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import com.bethel.mycoolwallet.CoolApplication;
import com.bethel.mycoolwallet.R;
import com.bethel.mycoolwallet.data.payment.PaymentData;
import com.bethel.mycoolwallet.helper.SendCoinsHelper;
import com.bethel.mycoolwallet.helper.parser.StringInputParser;
import com.bethel.mycoolwallet.utils.Constants;
import com.xuexiang.xqrcode.XQRCode;
import com.xuexiang.xui.widget.toast.XToast;

import org.bitcoinj.core.PrefixedChecksummedBytes;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.VerificationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SendCoinsQrActivity extends AppCompatActivity {
    private static final int REQUEST_QR_SCAN_CODE = 0;
    private static final Logger log = LoggerFactory.getLogger(SendCoinsQrActivity.class);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (null == savedInstanceState) {
            // scan qr code
            CustomCaptureActivity.start(this, null, REQUEST_QR_SCAN_CODE, R.style.XQRCodeTheme_Custom);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //处理二维码扫描结果
        if (requestCode == REQUEST_QR_SCAN_CODE && resultCode == RESULT_OK) {
            handleScanResult(data);
        }
    }

    /**
     * 处理二维码扫描结果
     *
     * @param data
     */
    private void handleScanResult(Intent data) {
        Bundle bundle = null != data ? data.getExtras() : null;
        if (bundle == null) {
            SendCoinsQrActivity.this.finish();
            return;
        }

        if (bundle.getInt(XQRCode.RESULT_TYPE) == XQRCode.RESULT_SUCCESS) {
            final String result = bundle.getString(XQRCode.RESULT_DATA);
            if (TextUtils.isEmpty(result)) return;
            //  handle bitcoin pay
            final StringInputParser parser = new StringInputParser(result) {
                @Override
                protected void handleWebUrl(String link) {
                    WebActivity.start(SendCoinsQrActivity.this, link);
                }

                @Override
                protected void handlePrivateKey(PrefixedChecksummedBytes key) {
                    if (Constants.ENABLE_SWEEP_WALLET) {
                        SweepWalletActivity.start(SendCoinsQrActivity.this, key);
                        SendCoinsQrActivity.this.finish();
                    } else {
                        super.handlePrivateKey(key);
                    }
                }

                @Override
                public void handleDirectTransaction(Transaction transaction) throws VerificationException {
                    CoolApplication.getApplication().processDirectTransaction(transaction);
                    SendCoinsQrActivity.this.finish();
                }

                @Override
                public void error(int messageResId, Object... messageArgs) {
                    SendCoinsHelper.dialog(SendCoinsQrActivity.this,
                            view -> SendCoinsQrActivity.this.finish(),
                            R.string.button_scan, messageResId, messageArgs);
                    log.error("IntentDataParser {}", getString(messageResId, messageArgs));
                }

                @Override
                public void handlePaymentData(PaymentData data) {
                    SendCoinsActivity.start(SendCoinsQrActivity.this, data);
                    SendCoinsQrActivity.this.finish();
                }
            };
            parser.parse();
            log.info(" 解析结果: {} ", result);
        } else if (bundle.getInt(XQRCode.RESULT_TYPE) == XQRCode.RESULT_FAILED) {
            XToast.error(this, R.string.parse_qr_code_failed, Toast.LENGTH_LONG).show();
        }
    }

}
