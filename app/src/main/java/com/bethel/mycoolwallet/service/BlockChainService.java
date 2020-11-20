package com.bethel.mycoolwallet.service;

import android.content.ComponentCallbacks2;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleService;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.bethel.mycoolwallet.CoolApplication;
import com.bethel.mycoolwallet.data.BlockChainState;
import com.bethel.mycoolwallet.interfaces.IBlockChainEventsCallback;
import com.bethel.mycoolwallet.manager.MyCoolBlockChainManager;
import com.bethel.mycoolwallet.manager.MyCoolNotificationManager;
import com.bethel.mycoolwallet.mvvm.live_data.WalletBalanceLiveData;
import com.bethel.mycoolwallet.mvvm.live_data.WalletLiveData;
import com.bethel.mycoolwallet.utils.Constants;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.Peer;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.StoredBlock;
import org.bitcoinj.core.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class BlockChainService extends LifecycleService {
    private static final String MyPackageName = BlockChainService.class.getPackage().getName();
    public static final String ACTION_PEER_STATE =  MyPackageName+ ".peer_state";
    public static final String ACTION_PEER_STATE_NUM_PEERS = "num_peers";
    public static final String ACTION_BLOCKCHAIN_STATE = MyPackageName + ".blockchain_state";

    private static final String ACTION_CANCEL_COINS_RECEIVED = MyPackageName + ".cancel_coins_received";
    private static final String ACTION_RESET_BLOCKCHAIN = MyPackageName  + ".reset_blockchain";
    private static final String ACTION_BROADCAST_TRANSACTION = MyPackageName + ".broadcast_transaction";
    private static final String ACTION_BROADCAST_TRANSACTION_HASH = "hash";


    private final MyCoolBlockChainManager mBlockChainManager = new MyCoolBlockChainManager();
    private final MyCoolNotificationManager mNotificationManager = new MyCoolNotificationManager();
    private WalletLiveData wallet;
    private long serviceCreatedAt;
    private boolean resetBlockChainOnShutdown = false;
//    private BlockChainServiceViewModel viewModel;

    private final IBinder mBinder = new LocalBinder();

    private static final Logger log = LoggerFactory.getLogger(BlockChainService.class);

    public static void start(final Context context, final boolean cancelCoinsReceived) {
        Intent intent ;
        if (cancelCoinsReceived) {
            intent = new Intent(ACTION_CANCEL_COINS_RECEIVED, null, context, BlockChainService.class);
        } else {
            intent = new Intent(context, BlockChainService.class);
        }
        ContextCompat.startForegroundService(context, intent);
    }

    public static void broadcastTransaction(final Context context, final Transaction tx) {
        Intent intent = new Intent(context, BlockChainService.class);
        intent.setAction(ACTION_BROADCAST_TRANSACTION);
        intent.putExtra(ACTION_BROADCAST_TRANSACTION_HASH, tx.getTxId().getBytes());
        ContextCompat.startForegroundService(context, intent);
    }
    @Nullable
    @Override
    public IBinder onBind(@NonNull Intent intent) {
         super.onBind(intent);
        log.debug(".onBind()");
         return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        log.debug(".onUnbind()");
        return super.onUnbind(intent);
    }

    @Override
    public void onCreate() {
        serviceCreatedAt = System.currentTimeMillis();
        log.debug(".onCreate()");
        super.onCreate();
//        viewModel = ViewModelProviders.of()

        mNotificationManager.init(this);

        // todo : WalletBalanceLiveData,SelectedExchangeRateLiveData, WalletLiveData
        wallet = new WalletLiveData(CoolApplication.getApplication());
        wallet.observe(this, wallet1 -> {
            wallet.removeObservers(BlockChainService.this);
            mBlockChainManager.init(wallet, BlockChainService.this);
            mBlockChainManager.setBlockChainEventsCallback(mEventsCallback);
        });

        WalletBalanceLiveData walletBalance = new WalletBalanceLiveData(CoolApplication.getApplication());
        walletBalance.observe(this, coin -> {
            // todo: update Widgets
        });

        startForeground(0);
        broadcastPeerState(0);

    }

    @Override
    public int onStartCommand(@NonNull Intent intent, int flags, int startId) {
         super.onStartCommand(intent, flags, startId);
         final String action = null!=intent ? intent.getAction() : null;
        log.info("service start command: " + intent);

        if (ACTION_CANCEL_COINS_RECEIVED.equals(action)) {
            mNotificationManager.cancelCoinsReceivedNotification();
        } else if (ACTION_BROADCAST_TRANSACTION.equals(action)) {
            byte[] hashBytes = intent.getByteArrayExtra(ACTION_BROADCAST_TRANSACTION_HASH);
            if (null!=hashBytes && hashBytes.length >0) {
                Sha256Hash hash = Sha256Hash.wrap(hashBytes);
                mBlockChainManager.broadcastTransaction(hash);
            }
        } else if (ACTION_RESET_BLOCKCHAIN.equals(action)) {
            log.info("will remove blockChain on service shutdown");
            resetBlockChainOnShutdown = true;
            stopSelf();
        }
        return START_NOT_STICKY;
    }

    private void startForeground(final int numPeers) {
        startForeground(Constants.NOTIFICATION_ID_CONNECTED,
                mNotificationManager.buildPeersCountNotification(numPeers));

//        mNotificationManager.testNotifyCoinsReceived();
    }

    private void notifyCoinsReceived(@Nullable final Address address, final Coin amount,
                                     final Sha256Hash transactionHash) {
        mNotificationManager.notifyCoinsReceived(address, amount, transactionHash);
    }

    @Override
    public void onDestroy() {
        // todo restart service , auto saveWallet
        mBlockChainManager.onDestroy();

        if (resetBlockChainOnShutdown) {
            mBlockChainManager.removeBlockChainFile();
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            MyJobService.startUp();
        }
        stopForeground(true);
        super.onDestroy();
        long duration = System.currentTimeMillis() - serviceCreatedAt;
        log.info(String.format("service was up for  %s  minutes", duration / 1000 / 60));
    }

    @Override
    public void onTrimMemory(int level) {
        log.info("onTrimMemory({}) called", level);

        if (level >= ComponentCallbacks2.TRIM_MEMORY_BACKGROUND) {
            log.warn("low memory detected, stopping service");
            stopSelf();
        }
    }

    private final IBlockChainEventsCallback mEventsCallback = new IBlockChainEventsCallback() {
        @Override
        public void onPeerConnectionChanged(int peerCount) {
            startForeground(peerCount);
            broadcastPeerState(peerCount);
        }

        @Override
        public void onNetworkOrStorageChanged() {
            broadcastBlockChainState();
        }

        @Override
        public void onBlockChainIdling() {
            BlockChainService.this.stopSelf();
        }

        @Override
        public void onBlocksDownloaded() {
            broadcastBlockChainState();
        }

        @Override
        public void onCoinsReceived(Address address, Coin amount, Sha256Hash txId) {
            notifyCoinsReceived(address, amount, txId);
        }
    };

    private void broadcastBlockChainState() {
        Intent intent = new Intent(ACTION_BLOCKCHAIN_STATE);
        BlockChainState state = getBlockChainState();
        if (null!=state) state.putExtras(intent);

        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void broadcastPeerState(int peerCount) {
        Intent intent = new Intent(ACTION_PEER_STATE);
        intent.putExtra(ACTION_PEER_STATE_NUM_PEERS, peerCount);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    public BlockChainState getBlockChainState() {
        return mBlockChainManager.getBlockChainState();
    }
    public List<Peer> getConnectedPeers() {
        return mBlockChainManager.getConnectedPeers();
    }
    public List<StoredBlock> getRecentBlocks(final int maxBlocks) {
        return mBlockChainManager.getRecentBlocks(maxBlocks);
    }

    public class LocalBinder extends Binder {
        public BlockChainService  getServices() {
            return BlockChainService.this;
        }
    }
}
