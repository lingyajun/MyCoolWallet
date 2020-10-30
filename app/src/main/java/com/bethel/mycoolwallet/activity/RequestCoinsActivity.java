package com.bethel.mycoolwallet.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;

import com.bethel.mycoolwallet.R;
import com.xuexiang.xui.widget.toast.XToast;

import org.bitcoinj.script.Script;

public class RequestCoinsActivity extends BaseActivity {
    public static final String INTENT_EXTRA_OUTPUT_SCRIPT_TYPE = "output_script_type";

    public static void start(final Context context) {
        start(context, null);
    }

    public static void start(final Context context, final @Nullable Script.ScriptType outputScriptType) {
        final Intent intent = new Intent(context, RequestCoinsActivity.class);
        if (outputScriptType != null)
            intent.putExtra(INTENT_EXTRA_OUTPUT_SCRIPT_TYPE, outputScriptType);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_request_coins);
        Toolbar toolbar= initTitleBar(R.string.request_coins_activity_title);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);//显示toolbar的左侧返回按钮
        toolbar.setNavigationOnClickListener((v)-> finish());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.request_coins_activity_options, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.request_coins_options_help) {
            // todo
            XToast.info(this, Html.fromHtml(getString(R.string.help_request_coins))).show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
