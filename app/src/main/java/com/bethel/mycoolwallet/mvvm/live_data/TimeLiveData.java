package com.bethel.mycoolwallet.mvvm.live_data;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import androidx.lifecycle.LiveData;

import java.util.Date;

public class TimeLiveData extends LiveData<Date> {
    private final Application application;

    public TimeLiveData(Application application) {
        this.application = application;
    }

    @Override
    protected void onActive() {
        // Intent.ACTION_TIME_TICK : 这个广播动作是以每分钟一次的形式发送。
        // 每分钟收到一次事件
        IntentFilter filter = new IntentFilter(Intent.ACTION_TIME_TICK);
        application.registerReceiver(tickReceiver, filter);
        setValue(new Date());
    }

    @Override
    protected void onInactive() {
        application.unregisterReceiver(tickReceiver);
    }

    private final BroadcastReceiver tickReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            setValue(new Date());
        }
    };
}
