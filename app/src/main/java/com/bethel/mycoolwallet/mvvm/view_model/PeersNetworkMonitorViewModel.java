package com.bethel.mycoolwallet.mvvm.view_model;

import android.app.Application;

import androidx.annotation.NonNull;

import com.bethel.mycoolwallet.mvvm.live_data.HostNameLiveData;
import com.bethel.mycoolwallet.mvvm.live_data.PeersLiveData;

public class PeersNetworkMonitorViewModel extends BaseViewModel {
    public final PeersLiveData peers;
    public final  HostNameLiveData hostName;
    public PeersNetworkMonitorViewModel(@NonNull Application app) {
        super(app);
        peers = new PeersLiveData(app);
        hostName = new HostNameLiveData();
    }

}
