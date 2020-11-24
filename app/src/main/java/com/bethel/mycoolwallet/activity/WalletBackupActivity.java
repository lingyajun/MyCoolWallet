package com.bethel.mycoolwallet.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.bethel.mycoolwallet.fragment.dialog.WalletBackupDialogFragment;

public class WalletBackupActivity extends BaseActivity {

    public static void start(final Context context) {
        context.startActivity(new Intent(context, WalletBackupActivity.class));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WalletBackupDialogFragment.show(getSupportFragmentManager());
    }
}
