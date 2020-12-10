package com.bethel.mycoolwallet.activity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;

import com.bethel.mycoolwallet.R;
import com.bethel.mycoolwallet.fragment.WebFragment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebActivity extends AppCompatActivity {
    public static void start(Context context, String url) {
        start(context, url, null);
    }
    public static void start(Context context, String url, String title) {
        Bundle data = new Bundle();
        data.putString(WebFragment.EXTRA_URL, url);
        data.putString(WebFragment.EXTRA_TITLE, title);
        Intent intent = new Intent(context, WebActivity.class);
        intent.putExtras(data);
        context.startActivity(intent);
//        ContextCompat.startActivity(context, intent, data);
    }
    private static final Logger log = LoggerFactory.getLogger(WebActivity.class);

    public static final String ACTION_WEB_VIEW = "bethel.intent.action.VIEW";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (null==savedInstanceState) {
            Intent intent = getIntent();
            final String action = intent.getAction();
            final Uri uri = intent.getData();
            log.info("action {}, uri  {}", action, uri);

            Bundle data = intent.getExtras();
            final String mUrl = null!=data? data.getString(WebFragment.EXTRA_URL): null;
            final String mTitle = null!=data? data.getString(WebFragment.EXTRA_TITLE): null;
            log.info("Bundle {}, {}", mTitle, mUrl);

            if (TextUtils.isEmpty(mUrl) && ACTION_WEB_VIEW.equals(action) && null!= uri) {
                if (null == data) data = new Bundle();

                data.putString(WebFragment.EXTRA_URL, uri.toString());
                intent.putExtras(data);
            }
        }

        setContentView(R.layout.activity_web);
    }
}
