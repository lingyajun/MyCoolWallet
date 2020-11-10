package com.bethel.mycoolwallet.mvvm.live_data;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.text.TextUtils;

import androidx.lifecycle.LiveData;

//import com.bethel.mycoolwallet.CoolApplication;
import com.bethel.mycoolwallet.data.Impediment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumSet;
import java.util.Set;

/**
 * 监听网络连接的变化，和设备存储的变化
 */
public class ImpedimentLiveData extends LiveData<Set<Impediment>> {
    private final Set<Impediment> impedimentSet = EnumSet.noneOf(Impediment.class);
    private final ConnectivityManager connectivityManager;
    private final Application application;

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            handleIntent(intent);
        }
    };

    private static final Logger log = LoggerFactory.getLogger(ImpedimentLiveData.class);

    public ImpedimentLiveData(Application application) {
        this.application = application;
        connectivityManager = (ConnectivityManager) application.getSystemService(Context.CONNECTIVITY_SERVICE);
        setValue(impedimentSet);
    }

    @Override
    protected void onActive() {
        IntentFilter filter = new IntentFilter();
        // net connectivity
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        // storage
        filter.addAction(Intent.ACTION_DEVICE_STORAGE_LOW);
        filter.addAction(Intent.ACTION_DEVICE_STORAGE_OK);

        Intent data = application.registerReceiver(mReceiver, filter);
        handleIntent(data);
    }

    private void handleIntent(Intent intent) {
        final String action = null!= intent? intent.getAction() : null;
        if (TextUtils.isEmpty(action)) return;

        if (ConnectivityManager.CONNECTIVITY_ACTION.equals(action)) {
            // net connectivity changes
            NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
            boolean hasConnectivity = null!=networkInfo && networkInfo.isConnected();
            if (hasConnectivity) {
                impedimentSet.remove(Impediment.NETWORK);
            } else {
                impedimentSet.add(Impediment.NETWORK);
            }
        } else if (Intent.ACTION_DEVICE_STORAGE_LOW.equals(action)) {
            impedimentSet.add(Impediment.STORAGE);
        } else if (Intent.ACTION_DEVICE_STORAGE_OK.equals(action)) {
            impedimentSet.remove(Impediment.STORAGE);
        }

        setValue(impedimentSet);
    }

    @Override
    protected void onInactive() {
        application.unregisterReceiver(mReceiver);
    }
}
