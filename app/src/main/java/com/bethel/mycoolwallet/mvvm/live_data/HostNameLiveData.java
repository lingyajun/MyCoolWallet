package com.bethel.mycoolwallet.mvvm.live_data;

import android.os.AsyncTask;

import androidx.lifecycle.LiveData;

import com.bethel.mycoolwallet.helper.CoolThreadPool;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

public class HostNameLiveData extends LiveData<Map<InetAddress, String>> {
    public HostNameLiveData() {
        setValue(new HashMap<>());
    }

    public void reverseLookup(final InetAddress address) {
        final Map<InetAddress, String> map = getValue();
        if (!map.containsKey(address)) {
//            AsyncTask.execute(() -> {
            CoolThreadPool.execute(() -> {
                final String hostname = address.getCanonicalHostName();
                map.put(address, hostname);
                postValue(map);
            });
        }
    }
}
