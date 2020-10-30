package com.bethel.mycoolwallet.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;

import com.bethel.mycoolwallet.R;
import com.bethel.mycoolwallet.data.FeeCategory;
import com.bethel.mycoolwallet.data.PaymentIntent;
import com.bethel.mycoolwallet.utils.Constants;
import com.xuexiang.xui.widget.toast.XToast;

import org.bitcoinj.core.Coin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SendCoinsActivity extends BaseActivity {
    public static final String INTENT_EXTRA_PAYMENT_INTENT = "payment_intent";
    public static final String INTENT_EXTRA_FEE_CATEGORY = "fee_category";

    private static final Logger log = LoggerFactory.getLogger(SendCoinsActivity.class);

    public static void start(final Context context, final PaymentIntent paymentIntent,
                             final @Nullable FeeCategory feeCategory, final int intentFlags) {
        final Intent intent = new Intent(context, SendCoinsActivity.class);
        if (null!= paymentIntent)
        intent.putExtra(INTENT_EXTRA_PAYMENT_INTENT, paymentIntent);
        if (feeCategory != null)
            intent.putExtra(INTENT_EXTRA_FEE_CATEGORY, feeCategory);
        if (intentFlags != 0)
            intent.setFlags(intentFlags);
        context.startActivity(intent);
    }

    public static void start(final Context context, final PaymentIntent paymentIntent) {
        start(context, paymentIntent, null, 0);
    }

    public static void start(final Context context) {
        start(context, null, null, 0);
    }

    public static void startDonate(final Context context, final Coin amount, final @Nullable FeeCategory feeCategory,
                                   final int intentFlags) {
        // todo: change donation address for me.
        start(context, PaymentIntent.from(Constants.DONATION_ADDRESS,
                context.getString(R.string.wallet_donate_address_label), amount), feeCategory, intentFlags);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        log.info("Referrer: {}", ActivityCompat.getReferrer(this));
        setContentView(R.layout.activity_send_coins);
        Toolbar toolbar= initTitleBar(R.string.send_coins_activity_title);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);//toolbar的左侧返回按钮
        toolbar.setNavigationOnClickListener((v)-> finish());
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.send_coins_activity_options, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        if (item.getItemId() == R.id.send_coins_options_help) {
            // todo
            XToast.info(this,R.string.help_send_coins).show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
