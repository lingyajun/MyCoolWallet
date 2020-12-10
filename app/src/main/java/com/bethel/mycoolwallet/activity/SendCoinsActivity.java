package com.bethel.mycoolwallet.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.core.app.ActivityCompat;

import com.bethel.mycoolwallet.R;
import com.bethel.mycoolwallet.data.payment.PaymentData;
import com.bethel.mycoolwallet.data.payment.PaymentUtil;
import com.bethel.mycoolwallet.fragment.dialog.HelpDialogFragment;
import com.bethel.mycoolwallet.service.BlockChainService;
import com.bethel.mycoolwallet.utils.Constants;

import org.bitcoinj.core.Coin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SendCoinsActivity extends BaseActivity {
    public static final String INTENT_EXTRA_PAYMENT_INTENT = "payment_data";

    private static final Logger log = LoggerFactory.getLogger(SendCoinsActivity.class);

    public static void start(final Context context, final PaymentData payment,  final int intentFlags) {
        final Intent intent = new Intent(context, SendCoinsActivity.class);
        if (null!= payment)
        intent.putExtra(INTENT_EXTRA_PAYMENT_INTENT, payment);
        if (intentFlags != 0)
            intent.setFlags(intentFlags);
        context.startActivity(intent);
    }

    public static void start(final Context context, final PaymentData payment ) {
        start(context, payment,  0);
    }

    public static void start(final Context context) {
        start(context, null,  0);
    }

    public static void startDonate(final Context context, final Coin amount, final int intentFlags) {
        start(context, PaymentUtil.from(Constants.DONATION_ADDRESS,
                context.getString(R.string.wallet_donate_address_label), amount),  intentFlags);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        log.info("Referrer: {}", ActivityCompat.getReferrer(this));
        setContentView(R.layout.activity_send_coins);

        initTitleBar(R.string.send_coins_activity_title, true);

        BlockChainService.start(this, false);
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.send_coins_activity_options, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        if (item.getItemId() == R.id.send_coins_options_help) {
            HelpDialogFragment.show(getSupportFragmentManager(), R.string.help_send_coins);
//            XToast.info(this,R.string.help_send_coins).show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
