package com.bethel.mycoolwallet.mvvm.live_data;

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

//import org.bitcoinj.core.Context;
import org.bitcoinj.core.StoredBlock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class BlocksLiveData extends LiveData<List<StoredBlock>> {
    private final Context context;
    private LocalBroadcastManager broadcastManager;
    private BlockChainService mService;

    private static final Logger log = LoggerFactory.getLogger(BlocksLiveData.class);

    private static final int MAX_BLOCKS = 120;

    public BlocksLiveData(Context context) {
        this.context = context;
        broadcastManager = LocalBroadcastManager.getInstance(context);
    }

    @Override
    protected void onActive() {
        // 1. bind service
        Intent intent = new Intent(context, BlockChainService.class);
        context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
        // 2. register broadcast
        IntentFilter filter = new IntentFilter(BlockChainService.ACTION_BLOCKCHAIN_STATE);
        broadcastManager.registerReceiver(receiver, filter);
    }

    @Override
    protected void onInactive() {
        context.unbindService(serviceConnection);
        broadcastManager.unregisterReceiver(receiver);
    }

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (null!=mService) {
                List<StoredBlock>  list = mService.getRecentBlocks(MAX_BLOCKS);
                setValue(list);
//                int count = intent.getIntExtra(BlockChainService.ACTION_PEER_STATE_NUM_PEERS, -1);
                log.info("StoredBlock: {} -- 120", Utils.size(list));
            }
        }
    };

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            mService = ((BlockChainService.LocalBinder) iBinder).getServices();
            setValue(mService.getRecentBlocks(MAX_BLOCKS));
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mService = null;
        }
    };
}
