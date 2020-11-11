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

import com.bethel.mycoolwallet.CoolApplication;
import com.bethel.mycoolwallet.data.BlockChainState;
import com.bethel.mycoolwallet.service.BlockChainService;

public class BlockChainStateLiveData extends LiveData<BlockChainState> {
    private final Application application;
    private final LocalBroadcastManager broadcastManager;

    public BlockChainStateLiveData() {
        application = CoolApplication.getApplication();
        broadcastManager = LocalBroadcastManager.getInstance(application);
    }

    @Override
    protected void onActive() {
        // bind service
        Intent intent = new Intent(application, BlockChainService.class);
        application.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);

        // register BroadcastReceiver --- BlockChainService::broadcastBlockChainState()
        IntentFilter filter = new IntentFilter(BlockChainService.ACTION_BLOCKCHAIN_STATE);
        broadcastManager.registerReceiver(stateReceiver, filter);
    }

    @Override
    protected void onInactive() {
        // unbind service
        application.unbindService(serviceConnection);
        broadcastManager.unregisterReceiver(stateReceiver);
    }

    private final BroadcastReceiver stateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final BlockChainState state = BlockChainState.fromIntent(intent);
            setValue(state);
        }
    };

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            final BlockChainState state =
                    ((BlockChainService.LocalBinder) iBinder).getServices().getBlockChainState();
            setValue(state);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
        }
    };
}
