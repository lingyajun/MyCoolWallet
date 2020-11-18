package com.bethel.mycoolwallet.activity;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.bethel.mycoolwallet.R;

public class ExchangeRatesActivity extends AppCompatActivity {

    public static void start(Context context) {
        context.startActivity(new Intent(context, ExchangeRatesActivity.class));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_exchange_rates);
    }
}
