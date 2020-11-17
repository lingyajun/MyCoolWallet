package com.bethel.mycoolwallet.activity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.bethel.mycoolwallet.R;
import com.bethel.mycoolwallet.fragment.WebFragment;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web);
    }
}
