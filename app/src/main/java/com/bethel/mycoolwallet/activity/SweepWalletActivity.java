package com.bethel.mycoolwallet.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.bethel.mycoolwallet.R;
import com.bethel.mycoolwallet.service.BlockChainService;

import org.bitcoinj.core.PrefixedChecksummedBytes;

public class SweepWalletActivity extends BaseActivity {

    public static final String INTENT_EXTRA_KEY = "sweep_key";

    public static void start(final Context context) {
        context.startActivity(new Intent(context, SweepWalletActivity.class));
    }

    public static void start(final Context context, final PrefixedChecksummedBytes key) {
        final Intent intent = new Intent(context, SweepWalletActivity.class);
        intent.putExtra(INTENT_EXTRA_KEY, key);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sweep_wallet);
        initTitleBar(R.string.sweep_wallet_activity_title , true);
        BlockChainService.start(this, false);
    }
}
