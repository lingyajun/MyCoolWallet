package com.bethel.mycoolwallet.mvvm.live_data;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.IBinder;

import androidx.lifecycle.LiveData;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.bethel.mycoolwallet.service.BlockChainService;
import com.bethel.mycoolwallet.utils.Utils;

import org.bitcoinj.core.Peer;
//import org.bitcoinj.core.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class PeersLiveData extends LiveData<List<Peer>> {
    private final Application application;
    private LocalBroadcastManager broadcastManager;
    private BlockChainService mService;

    private static final Logger log = LoggerFactory.getLogger(PeersLiveData.class);

    public PeersLiveData(Application application) {
        this.application = application;
        broadcastManager = LocalBroadcastManager.getInstance(application);
    }

    @Override
    protected void onActive() {
        // 1. bind service
        Intent intent = new Intent(application, BlockChainService.class);
        application.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
        // 2. register broadcast
        IntentFilter filter = new IntentFilter(BlockChainService.ACTION_PEER_STATE);
        broadcastManager.registerReceiver(receiver, filter);
    }

    @Override
    protected void onInactive() {
        application.unbindService(serviceConnection);
        broadcastManager.unregisterReceiver(receiver);
    }

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (null!=mService) {
                List<Peer> list = mService.getConnectedPeers();
                setValue(list);
                int count = intent.getIntExtra(BlockChainService.ACTION_PEER_STATE_NUM_PEERS, -1);
                log.info("peers: {}, peerCount: {}", Utils.size(list), count);
            }
        }
    };

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            mService = ((BlockChainService.LocalBinder) iBinder).getServices();
            setValue(mService.getConnectedPeers());
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mService = null;
        }
    };
}
