package com.bethel.mycoolwallet.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.menu.MenuBuilder;
import androidx.appcompat.widget.Toolbar;

import android.content.res.Resources;
import android.os.Bundle;
import android.os.Environment;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.bethel.mycoolwallet.R;
import com.bethel.mycoolwallet.utils.Constants;
import com.xuexiang.xui.widget.toast.XToast;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.wallet_content);

        initTitleBar();
    }

    private void initTitleBar() {       //隐藏默认actionbar
        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null){
            actionBar.hide();
        }

        //获取toolbar
       Toolbar toolBar = findViewById(R.id.toolbar);
        //主标题，必须在setSupportActionBar之前设置，否则无效，如果放在其他位置，则直接setTitle即可
        toolBar.setTitle(getString(R.string.app_name));
        //用toolbar替换actionbar
        setSupportActionBar(toolBar);
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
//  todo      final Boolean isEncrypted = viewModel.walletEncrypted.getValue();
//        if (isEncrypted != null) {
//            final MenuItem encryptKeysOption = menu.findItem(R.id.wallet_options_encrypt_keys);
//            encryptKeysOption.setTitle(isEncrypted ? R.string.wallet_options_encrypt_keys_change
//                    : R.string.wallet_options_encrypt_keys_set);
//            encryptKeysOption.setVisible(true);
//        }
//        final Boolean isLegacyFallback = viewModel.walletLegacyFallback.getValue();
//        if (isLegacyFallback != null) {
//            final MenuItem requestLegacyOption = menu.findItem(R.id.wallet_options_request_legacy);
//            requestLegacyOption.setVisible(isLegacyFallback);
//        }

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
                break;
            case R.id.wallet_options_send :
                handleSendCoins();
                break;
            case R.id.wallet_options_scan :
                handleScan(null);
                break;
            case R.id.wallet_options_address_book :
                break;
            case R.id.wallet_options_exchange_rates :
                break;
            case R.id.wallet_options_sweep_wallet :
                break;
            case R.id.wallet_options_network_monitor :
                break;
            case R.id.wallet_options_restore_wallet :
                break;
            case R.id.wallet_options_backup_wallet :
                break;
            case R.id.wallet_options_encrypt_keys :
                break;
            case R.id.wallet_options_preferences :
                break;
            case R.id.wallet_options_safety :
                XToast.info(this, "safety").show();
                break;
            case R.id.wallet_options_technical_notes :
                break;
            case R.id.wallet_options_report_issue :
                XToast.info(this, "report issue").show();
                break;
            case R.id.wallet_options_help :
                XToast.info(this, "help doc").show();
                break;
            default: return super.onOptionsItemSelected(item);
        }
        return true;
//        return super.onOptionsItemSelected(item);
    }

    private void   handleRequestCoins() {
        XToast.info(this, "handleRequestCoins").show();
    }

    private void handleSendCoins() {
        XToast.info(this, "handleSendCoins").show();
    }

    private void handleScan(View o) {
        XToast.info(this, "handleScan").show();
    }
}
