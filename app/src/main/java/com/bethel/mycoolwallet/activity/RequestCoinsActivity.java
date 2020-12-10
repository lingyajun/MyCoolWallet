package com.bethel.mycoolwallet.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bethel.mycoolwallet.R;
import com.bethel.mycoolwallet.data.Event;
import com.bethel.mycoolwallet.fragment.dialog.HelpDialogFragment;
import com.bethel.mycoolwallet.mvvm.view_model.RequestCoinsActivityViewModel;

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

    private RequestCoinsActivityViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_request_coins);
//        Toolbar toolbar=
        initTitleBar(R.string.request_coins_activity_title, true);
//        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
//        toolbar.setNavigationOnClickListener((v)-> finish());

        viewModel = getViewModel(RequestCoinsActivityViewModel.class);
        viewModel.showHelpDialog.observe(this, new Event.Observer<Integer>() {
            @Override
            public void onEvent(final Integer messageResId) {
                HelpDialogFragment.show(getSupportFragmentManager(), messageResId);
//                XToast.info(getApplication(), messageResId).show();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.request_coins_activity_options, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.request_coins_options_help) {
            viewModel.showHelpDialog.setValue(new Event<>(R.string.help_request_coins));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
