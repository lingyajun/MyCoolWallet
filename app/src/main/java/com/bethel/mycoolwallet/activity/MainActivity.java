package com.bethel.mycoolwallet.activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.view.menu.MenuBuilder;

import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.bethel.mycoolwallet.R;
import com.bethel.mycoolwallet.data.Event;
import com.bethel.mycoolwallet.fragment.dialog.EncryptKeysDialogFragment;
import com.bethel.mycoolwallet.fragment.HelpDialogFragment;
import com.bethel.mycoolwallet.fragment.dialog.WalletRestoreDialogFragment;
import com.bethel.mycoolwallet.interfaces.IQrScan;
import com.bethel.mycoolwallet.interfaces.IRequestCoins;
import com.bethel.mycoolwallet.interfaces.ISendCoins;
import com.bethel.mycoolwallet.mvvm.view_model.MainActivityViewModel;
import com.bethel.mycoolwallet.service.BlockChainService;
import com.bethel.mycoolwallet.utils.Constants;
import com.xuexiang.xqrcode.XQRCode;
import com.xuexiang.xui.widget.toast.XToast;

import org.bitcoinj.script.Script;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MainActivity extends BaseActivity implements IQrScan, IRequestCoins, ISendCoins {
    /**
     * 扫描跳转Activity RequestCode
     */
    public static final int REQUEST_QR_SCAN_CODE = 111;

    private  MainActivityViewModel viewModel;

    private final Handler mHandler = new Handler();

    private static final Logger log = LoggerFactory.getLogger(MainActivity.class);
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.wallet_content);

        initTitleBar(R.string.app_name);

        viewModel = getViewModel(MainActivityViewModel.class);
        viewModel.walletEncrypted.observe(this, result -> invalidateOptionsMenu());
        viewModel.showEncryptKeysDialog.observe(this,
                (v) -> EncryptKeysDialogFragment.show(getSupportFragmentManager()));
        viewModel.showRestoreWalletDialog.observe(this, e -> {
            WalletRestoreDialogFragment.show(getSupportFragmentManager());
        });
        viewModel.showBackupWalletDialog.observe(this, e -> WalletBackupActivity.start(this));
        viewModel.showHelpDialog.observe(this, new Event.Observer< Integer >(){
            @Override
            public void onEvent( Integer content) {
                HelpDialogFragment.show(getSupportFragmentManager(), content);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        mHandler.postDelayed(()-> BlockChainService.start(this, true),1000);
    }

    @Override
    protected void onPause() {
        mHandler.removeCallbacksAndMessages(null);
        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.wallet_options, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(final Menu menu) {
        super.onPrepareOptionsMenu(menu);
        MenuBuilder m;
        final Resources res = getResources();

        final boolean showExchangeRatesOption = Constants.ENABLE_EXCHANGE_RATES
                && res.getBoolean(R.bool.show_exchange_rates_option);
        menu.findItem(R.id.wallet_options_exchange_rates).setVisible(showExchangeRatesOption);
        menu.findItem(R.id.wallet_options_sweep_wallet).setVisible(Constants.ENABLE_SWEEP_WALLET);
        final String externalStorageState = Environment.getExternalStorageState();
        final boolean enableRestoreWalletOption = Environment.MEDIA_MOUNTED.equals(externalStorageState)
                || Environment.MEDIA_MOUNTED_READ_ONLY.equals(externalStorageState);
        menu.findItem(R.id.wallet_options_restore_wallet).setEnabled(enableRestoreWalletOption);

        final Boolean isEncrypted = viewModel.walletEncrypted.getValue();
        if (isEncrypted != null) {
            final MenuItem encryptKeysOption = menu.findItem(R.id.wallet_options_encrypt_keys);
            encryptKeysOption.setTitle(isEncrypted ? R.string.wallet_options_encrypt_keys_change
                    : R.string.wallet_options_encrypt_keys_set);
            encryptKeysOption.setVisible(true);
        }

//   todo     final Boolean isLegacyFallback = viewModel.walletLegacyFallback.getValue();
//        if (isLegacyFallback != null) {
//            final MenuItem requestLegacyOption = menu.findItem(R.id.wallet_options_request_legacy);
//            requestLegacyOption.setVisible(isLegacyFallback);
//        }

        boolean debug = res.getBoolean(R.bool.show_debug_option);
        menu.findItem(R.id.wallet_options_debug).setEnabled(debug);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.wallet_options_request:
                handleRequestCoins();
                break;

            case R.id.wallet_options_request_legacy:
                // 用旧式地址索取比特币 (pay to pubkey hash)
                RequestCoinsActivity.start(this, Script.ScriptType.P2PKH);
                break;
            case R.id.wallet_options_send :
                handleSendCoins();
                break;
            case R.id.wallet_options_scan :
                handleScan(null);
                break;
            case R.id.wallet_options_address_book :
                AddressBookActivity.start(this);
                break;
            case R.id.wallet_options_exchange_rates :
                ExchangeRatesActivity.start(this);
                break;
            case R.id.wallet_options_sweep_wallet :
                break;
            case R.id.wallet_options_network_monitor :
                BlockChainNetworkMonitorActivity.start(this);
                break;
            case R.id.wallet_options_restore_wallet :
                viewModel.showRestoreWalletDialog.setValue(Event.simple());
                break;
            case R.id.wallet_options_backup_wallet :
                viewModel.showBackupWalletDialog.setValue(Event.simple());
                break;
            case R.id.wallet_options_encrypt_keys :
                viewModel.showEncryptKeysDialog.setValue(Event.simple());
                break;
            case R.id.wallet_options_preferences :
                SettingsActivity.start(this);
                break;
            case R.id.wallet_options_safety :
                viewModel.showHelpDialog.setValue(new Event<>(R.string.help_safety));
                break;
            case R.id.wallet_options_technical_notes :
                viewModel.showHelpDialog.setValue(new Event<>(R.string.help_technical_notes));
                break;
            case R.id.wallet_options_report_issue :
                XToast.info(this, "report issue").show();
                break;
            case R.id.wallet_options_help :
                viewModel.showHelpDialog.setValue(new Event<>(R.string.help_wallet));
                break;
            case R.id.wallet_options_debug :
                DebugActivity.start(this);
                break;
            default: return super.onOptionsItemSelected(item);
        }
        return true;
//        return super.onOptionsItemSelected(item);
    }

    private void   handleRequestCoins() {
        requestCoins();
       // XToast.info(this, "handleRequestCoins").show();
    }

    private void handleSendCoins() {
        sendCoins();
//        XToast.info(this, "handleSendCoins").show();
    }

    private void handleScan(View o) {
        startScan(o);
    }

    /**
     * 打开扫码页面
     * */
    @Override
    public void startScan(View o) {
//        maybeOpenCamera();
//        requestCameraPermissionsIfNotGranted();
        if (!maybeOpenCamera(o)) {
            requestCameraPermissions();
       }
    }

    @Override
    public void onCameraPermissionsResult(boolean grant) {
        if (grant) {
            maybeOpenCamera(null);
        } else {
            XToast.warning(this, R.string.open_camera_permissions).show();
        }
    }

    private boolean maybeOpenCamera(View o) {
        boolean can = checkCameraPermission();
        if (can)
            CustomCaptureActivity.start(this, o, REQUEST_QR_SCAN_CODE, R.style.XQRCodeTheme_Custom);
        return can;
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
        if (data != null) {
            Bundle bundle = data.getExtras();
            if (bundle != null) {
                if (bundle.getInt(XQRCode.RESULT_TYPE) == XQRCode.RESULT_SUCCESS) {
                    String result = bundle.getString(XQRCode.RESULT_DATA);
                    // todo handle bitcoin pay
                    XToast.success(this, "解析结果: " + result, Toast.LENGTH_LONG).show();
                } else if (bundle.getInt(XQRCode.RESULT_TYPE) == XQRCode.RESULT_FAILED) {
                    XToast.error(this,R.string.parse_qr_code_failed, Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    /**
     * 打开收钱页面
     * */
    @Override
    public void requestCoins() {
//         XToast.info(this, "requestCoins").show();
        RequestCoinsActivity.start(this);
    }

    /**
     * 打开支付页面
     * */
    @Override
    public void sendCoins() {
        SendCoinsActivity.start(this);
    }
}
