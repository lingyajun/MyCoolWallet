package com.bethel.mycoolwallet.manager;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.text.TextUtils;
import android.text.format.DateUtils;

import androidx.annotation.Nullable;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Observer;

import com.bethel.mycoolwallet.CoolApplication;
import com.bethel.mycoolwallet.data.BlockChainActiveHistoryEntry;
import com.bethel.mycoolwallet.data.BlockChainState;
import com.bethel.mycoolwallet.data.Impediment;
import com.bethel.mycoolwallet.helper.Configuration;
import com.bethel.mycoolwallet.interfaces.IBlockChainEventsCallback;
import com.bethel.mycoolwallet.mvvm.live_data.ImpedimentLiveData;
import com.bethel.mycoolwallet.mvvm.live_data.NewTransactionLiveData;
import com.bethel.mycoolwallet.mvvm.live_data.TimeLiveData;
import com.bethel.mycoolwallet.mvvm.live_data.WalletLiveData;
import com.bethel.mycoolwallet.utils.Constants;
import com.bethel.mycoolwallet.utils.CrashReporter;
import com.bethel.mycoolwallet.utils.WalletUtils;
import com.google.common.base.Stopwatch;
import com.xuexiang.xui.widget.toast.XToast;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.Block;
import org.bitcoinj.core.BlockChain;
import org.bitcoinj.core.CheckpointManager;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.FilteredBlock;
import org.bitcoinj.core.Peer;
import org.bitcoinj.core.PeerGroup;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.StoredBlock;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionConfidence;
import org.bitcoinj.core.listeners.AbstractPeerDataEventListener;
import org.bitcoinj.core.listeners.PeerConnectedEventListener;
import org.bitcoinj.core.listeners.PeerDisconnectedEventListener;
import org.bitcoinj.net.discovery.MultiplexingDiscovery;
import org.bitcoinj.net.discovery.PeerDiscovery;
import org.bitcoinj.net.discovery.PeerDiscoveryException;
import org.bitcoinj.store.BlockStore;
import org.bitcoinj.store.BlockStoreException;
import org.bitcoinj.store.SPVBlockStore;
import org.bitcoinj.wallet.Wallet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * btc block chain 管理类
 * 对外开放的方法：
 * public void init(...)
 * public void onDestroy()
 *
 * TODO:
 * broadcastBlockChainState()
 * onBlockChainIdling()
 * notifyCoinsReceived(...)
 * onDestroy()
 * startServiceForeground(..)
 *  broadcastPeerState(..)
 */
public class MyCoolBlockChainManager {
    private static final int MIN_COLLECT_HISTORY = 2;
    private static final int IDLE_BLOCK_TIMEOUT_MIN = 1;
    private static final int IDLE_TRANSACTION_TIMEOUT_MIN = 5;
    private static final int MAX_HISTORY_SIZE = Math.max(IDLE_TRANSACTION_TIMEOUT_MIN, IDLE_BLOCK_TIMEOUT_MIN);
    private static final long BLOCKCHAIN_STATE_BROADCAST_THROTTLE_MS = DateUtils.SECOND_IN_MILLIS;

    private CoolApplication application;
    private Handler mainHandler;
    private Handler delayHandler;
    private WalletLiveData walletLiveData;
    private LifecycleOwner lifecycleOwner;
    private Configuration mConfig;
    private PowerManager.WakeLock mWakeLock;
    private ImpedimentLiveData impedimentLiveData;

    private BlockStore blockStore; // A BlockStore is a map of hashes to StoredBlock.
    private File blockChainFile;
    private BlockChain blockChain; // holds a series of {@link Block} objects, links them together, and knows how to verify that the chain.
    @Nullable
    private PeerGroup peerGroup; // Runs a set of connections to the P2P network

    private final AtomicInteger transationReceived = new AtomicInteger();
    private PeerConnectivityListener peerConnectivityListener;

    private IBlockChainEventsCallback mEventsCallback;

    private static final Logger log = LoggerFactory.getLogger(MyCoolBlockChainManager.class);
//    private static final String TAG = "MyCoolBlockChainManager";

    public void init(WalletLiveData walletLiveData, LifecycleOwner owner) {
        this.application = CoolApplication.getApplication();
        this.walletLiveData = walletLiveData;
        this.lifecycleOwner = owner;
        mainHandler = new Handler(Looper.getMainLooper());
        delayHandler = new Handler(Looper.getMainLooper());
        mConfig = application.getConfiguration();

        PowerManager pm = (PowerManager) application.getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, lifecycleOwner.getClass().getName());
        peerConnectivityListener = new PeerConnectivityListener();

        File dir = application.getDir("blockstore", Context.MODE_PRIVATE);
        blockChainFile = new File(dir, Constants.Files.BLOCKCHAIN_FILENAME);

        final boolean isBlockChainFileExists = blockChainFile.exists();
        final Wallet wallet = walletLiveData.getValue();
        if (!isBlockChainFileExists) {
            wallet.reset();
            log.info("blockchain does not exist, resetting wallet");
        }

        // blockStore
        AsyncTask.execute(()->{
            try {
                blockStore = new SPVBlockStore(Constants.NETWORK_PARAMETERS, blockChainFile, Constants.Files.BLOCKCHAIN_STORE_CAPACITY, true);
                blockStore.getChainHead();  // detect corruptions as early as possible

                final long earliestKeyTime = wallet.getEarliestKeyCreationTime();
                if (!isBlockChainFileExists && earliestKeyTime > 0) {
                    // checkpoint, 初始化，同步验证节点
                    checkpoint(earliestKeyTime);
                }
            } catch (BlockStoreException e) {
                blockChainFile.delete();
                final String msg = "blockstore cannot be created";
                log.error(msg, e);
                throw new Error(msg, e);
            }

            try {
                blockChain = new BlockChain(Constants.NETWORK_PARAMETERS, wallet, blockStore);
            } catch (BlockStoreException e) {
                final String msg = "blockstore cannot be created";
                throw new Error(msg, e);
            }

            // observeLiveDatasThatAreDependentOnWalletAndBlockchain()
            mainHandler.post(() -> observeWalletAndBlockChain());
        });

    }

    private void observeWalletAndBlockChain() {
        observeWalletNewTransaction();
        observeTimeTick();
        observeImpediments();
    }

    private void observeImpediments() {
        ImpedimentLiveData liveData = new ImpedimentLiveData(application);
        liveData.observe(lifecycleOwner, impediments -> {
            if (impediments.isEmpty() && peerGroup == null && Constants.ENABLE_BLOCKCHAIN_SYNC) {
                startupPeerGroup();
            } else  if (!impediments.isEmpty() && peerGroup != null) {
                shutdownPeerGroup();
            }

            if (null!=mEventsCallback) mEventsCallback.onNetworkOrStorageChanged();
//            broadcastBlockChainState();
        });
        impedimentLiveData = liveData;
    }

    private void startupPeerGroup() {
        mWakeLock.acquire();
        final Wallet wallet = walletLiveData.getValue();
        //  检查一致性
        // wallet.getLastBlockSeenHeight(): Can be 0 if a wallet is brand new or -1 if the wallet is old and doesn't have that data.
        final int walletlastSeenHeight = wallet.getLastBlockSeenHeight();
        final int chainBestHeight = blockChain.getBestChainHeight();
        if (walletlastSeenHeight >0 && walletlastSeenHeight != chainBestHeight) {
            final String message = "wallet/blockchain out of sync: " + walletlastSeenHeight + "/"
                    + chainBestHeight;
            log.error(message);
            CrashReporter.saveBackgroundTrace(new RuntimeException(message), application.packageInfo());
        }

        // init peerGroup
        peerGroup = new PeerGroup(Constants.NETWORK_PARAMETERS, blockChain);
        peerGroup.addWallet(wallet);
        peerGroup.setDownloadTxDependencies(0);
        peerGroup.setUserAgent(Constants.USER_AGENT, application.packageInfo().versionName);
        peerGroup.addConnectedEventListener(peerConnectivityListener);
        peerGroup.addDisconnectedEventListener(peerConnectivityListener);

        // Peer Discovery
        final boolean connectTrustedPeerOnly = shallConnectTrustedPeerOnly();
        peerGroup.setMaxConnections(connectTrustedPeerOnly ? 1: maxConnectedPeers());
        peerGroup.setConnectTimeoutMillis(Constants.PEER_TIMEOUT_MS);
        peerGroup.setPeerDiscoveryTimeoutMillis(Constants.PEER_DISCOVERY_TIMEOUT_MS);
        peerGroup.addPeerDiscovery(mPeerDiscovery);

        // start
        peerGroup.startAsync();
//        peerGroup.addBlocksDownloadedEventListener(new MBlockChainDownloadListener());
        peerGroup.startBlockChainDownload(mBlockChainDownloadListener);
        log.info("starting {} asynchronously", peerGroup);
    }

    private int maxConnectedPeers() {
        return application.maxConnectedPeers();
    }

    private boolean  hasTrustedPeerHost() {
        return !TextUtils.isEmpty(mConfig.getTrustedPeerHost());
    }
    private boolean  shallConnectTrustedPeerOnly() {
        return mConfig.getTrustedPeerOnly() && hasTrustedPeerHost();
    }

    private void shutdownPeerGroup() {
        releasePeerGroup();
        peerGroup = null;
        //  remove listeners , stop sync
    }

    private void releasePeerGroup() {
        if (null!=peerGroup) {
            Wallet wallet = walletLiveData.getValue();
            peerGroup.removeConnectedEventListener(peerConnectivityListener);
            peerGroup.removeDisconnectedEventListener(peerConnectivityListener);
            peerGroup.removeWallet(wallet);
            log.info("stopping {} asynchronously", peerGroup);
            peerGroup.stopAsync();
        }

        log.debug("releasing wakelock");
        if (mWakeLock.isHeld()) {
            mWakeLock.release();
        }
    }

    private void observeTimeTick() {
        TimeLiveData timeLiveData = new TimeLiveData(application);
        timeLiveData.observe(lifecycleOwner, new Observer<Date>() {
            private int lastChainHeight =0;
            private final List<BlockChainActiveHistoryEntry> activeHistoryEntries = new LinkedList<>();
            @Override
            public void onChanged(Date date) {
                final int chainHeight = blockChain.getBestChainHeight();
                if (lastChainHeight >0) {
                    final int numBlocksDownloaded = chainHeight - lastChainHeight;
                    final int numTransactionsReceived = transationReceived.getAndSet(0);

                    activeHistoryEntries.add(0, new BlockChainActiveHistoryEntry(numTransactionsReceived, numBlocksDownloaded));
                    final int size =activeHistoryEntries.size();
                    if (MAX_HISTORY_SIZE < size) {
                        activeHistoryEntries.remove(size -1);
                    }
                    // determine if block and transaction activity is idling ;
                    // 确定是否空闲，如果闲就结束 service
                    boolean isIdle = false;
                    final int length =activeHistoryEntries.size();
                    if (MIN_COLLECT_HISTORY <= length) {
                        isIdle = true;
                        for (int i = 0; i < length; i++) {
                            BlockChainActiveHistoryEntry entry = activeHistoryEntries.get(i);
                            boolean isblocksActive = entry.numBlocksDownloaded>0 && i <= IDLE_BLOCK_TIMEOUT_MIN;
                            boolean isTxActive = entry.numTransactionsReceived>0 && i <= IDLE_TRANSACTION_TIMEOUT_MIN;
                            if (isblocksActive || isTxActive) {
                                isIdle = false;
                                break;
                            }
                        }
                    }

                    if (isIdle) {
                        log.info("idling detected, stopping service");
//                        stopSelf();
                        if (null!=mEventsCallback)  mEventsCallback.onBlockChainIdling();
                    }
                }

                lastChainHeight = chainHeight;
            }
        });
    }

    private void observeWalletNewTransaction() {
        NewTransactionLiveData newTransactionLiveData = new NewTransactionLiveData(this.walletLiveData.getValue());
        newTransactionLiveData.observe(lifecycleOwner, transaction -> {
            // Coins Received
            final Wallet wallet = walletLiveData.getValue();
            transationReceived.incrementAndGet();
            Coin amount = transaction.getValue(wallet);
            if (amount.isPositive()) {
                Address address = WalletUtils.getWalletAddressOfReceived(transaction, wallet);
                TransactionConfidence.ConfidenceType confidenceType = transaction.getConfidence().getConfidenceType();

                // check replay tx
                boolean isReplay = blockChain.getBestChainHeight() < mConfig.getBestChainHeightEver();
                boolean isReplayedTx = isReplay && confidenceType == TransactionConfidence.ConfidenceType.BUILDING;
                // notify coins received
                if (!isReplayedTx) {
                    if (null!= mEventsCallback) mEventsCallback.onCoinsReceived(address, amount, transaction.getTxId());
//                    notifyCoinsReceived(address, amount, transaction.getTxId());
                }
            }
        });
    }

    private void checkpoint(long earliestTime) throws BlockStoreException {
        try {
            Stopwatch watch = Stopwatch.createStarted();
            InputStream is = application.getAssets().open(Constants.Files.CHECKPOINTS_ASSET);
            CheckpointManager.checkpoint(Constants.NETWORK_PARAMETERS, is, blockStore, earliestTime);
            watch.stop();
            log.info("checkpoints loaded from '{}', took {}", Constants.Files.CHECKPOINTS_ASSET,
                    watch);
        } catch (IOException e) {
            log.error("problem reading checkpoints, continuing without", e);
        }
    }

    //  BlockchainService ::  onDestroy()
    public void onDestroy() {
        log.debug(".onDestroy()");

        releasePeerGroup();
        peerConnectivityListener.stop();
        delayHandler.removeCallbacksAndMessages(null);

        if (blockStore != null) {
            try {
                blockStore.close();
            } catch (final BlockStoreException x) {
                throw new RuntimeException(x);
            }
        }

    }

    public void removeBlockChainFile() {
        log.info("removing block chain");
        if (null!= blockChainFile) blockChainFile.delete();
    }

    public void broadcastTransaction(Sha256Hash hash) {
        if (null!=peerGroup && null!= application) {
            Transaction tx = application.getWallet().getTransaction(hash);
            if (null!=tx) {
                log.info("broadcasting transaction {}", tx.getTxId());
                peerGroup.broadcastTransaction(tx);
            }
        } else {
            log.info("PeerGroup not available, not broadcasting transaction {}", hash);
        }
    }

    private final class PeerConnectivityListener
            implements PeerConnectedEventListener, PeerDisconnectedEventListener {
        private AtomicBoolean stopped = new AtomicBoolean(false);

        @Override
        public void onPeerConnected(Peer peer, int peerCount) {
            changed(peerCount);
        }

        private void changed(final int peerCount) {
            if (stopped.get()) return;
            mainHandler.post(()-> {
                if (null!= mEventsCallback) mEventsCallback.onPeerConnectionChanged(peerCount);
//                startServiceForeground(peerCount);
//                broadcastPeerState(peerCount);
            });
        }

        @Override
        public void onPeerDisconnected(Peer peer, int peerCount) {
            changed(peerCount);
        }

        public void stop() {
            stopped.set(true);
        }
    }

    private final MPeerDiscovery mPeerDiscovery = new MPeerDiscovery();
    private class MPeerDiscovery implements PeerDiscovery {
        // Builds a suitable set of peer discoveries.
        private final PeerDiscovery normalPeerDiscovery =
                MultiplexingDiscovery.forServices(Constants.NETWORK_PARAMETERS, 0);

        @Override
        public InetSocketAddress[] getPeers(long services, long timeoutValue, TimeUnit timeoutUnit) throws PeerDiscoveryException {
            List<InetSocketAddress> peers = new LinkedList<>();
            boolean needTrimPeers = false;

            final String trustedPeerHost = mConfig.getTrustedPeerHost();
            if (!TextUtils.isEmpty(trustedPeerHost)) {
                final InetSocketAddress address =
                        new InetSocketAddress(trustedPeerHost, Constants.NETWORK_PARAMETERS.getPort());
                if (null!=address) {
                    peers.add(address);
                    needTrimPeers = true;
                }
                log.info( "trusted peer : " + trustedPeerHost );
            }

            if (!shallConnectTrustedPeerOnly()) {
                peers.addAll(Arrays.asList(normalPeerDiscovery.getPeers(services, timeoutValue, timeoutUnit)));
            }

            if (needTrimPeers) {
                while (peers.size()>= maxConnectedPeers()) {
                    peers.remove(peers.size() -1);
                }
            }

            InetSocketAddress[] arr = new InetSocketAddress[0];
            return peers.toArray(arr);
        }

        @Override
        public void shutdown() {
            normalPeerDiscovery.shutdown();
        }
    }

    private final MBlockChainDownloadListener mBlockChainDownloadListener = new MBlockChainDownloadListener();
    private class MBlockChainDownloadListener extends AbstractPeerDataEventListener {
        private final AtomicLong lastTime = new AtomicLong(0);

        @Override
        public void onBlocksDownloaded(Peer peer, Block block, @javax.annotation.Nullable FilteredBlock filteredBlock, int blocksLeft) {
            final long now = System.currentTimeMillis();
            delayHandler.removeCallbacksAndMessages(null);

            if (now-lastTime.get() > BLOCKCHAIN_STATE_BROADCAST_THROTTLE_MS) {
                delayHandler.post(task);
            } else {
                delayHandler.postDelayed(task, BLOCKCHAIN_STATE_BROADCAST_THROTTLE_MS);
            }
//            log.info("onBlocksDownloaded  {} , peer {}", block.getTime(), peer.getAddress());
        }

        Runnable task = () ->{
            lastTime.set(System.currentTimeMillis());
            mConfig.maybeIncrementBestChainHeightEver(blockChain.getChainHead().getHeight());
//            broadcastBlockChainState();
            if (null!= mEventsCallback) mEventsCallback.onBlocksDownloaded();
        };
    }

    public void setBlockChainEventsCallback(IBlockChainEventsCallback callback) {
        this.mEventsCallback = callback;
    }


    public BlockChainState getBlockChainState() {
        if (null == blockChain) return null;
        StoredBlock head = blockChain.getChainHead(); // the block at the head of the current best chain.
        Date time = head.getHeader().getTime();
        int height = head.getHeight();
        boolean isReplay = head.getHeight() < mConfig.getBestChainHeightEver();
        Set<Impediment> impediments = null!= impedimentLiveData?
                impedimentLiveData.getValue() : EnumSet.noneOf(Impediment.class);
        return new BlockChainState(time, height ,isReplay, impediments);
    }

    public List<Peer> getConnectedPeers() {
        return null!=peerGroup ? peerGroup.getConnectedPeers() : null;
    }
    public List<StoredBlock> getRecentBlocks(final int maxBlocks) {
        if (null==blockChain || null == blockStore) return null;
        List<StoredBlock> list = new ArrayList<>();
        StoredBlock head = blockChain.getChainHead();

        try {
            while (null!= head) {
                list.add(head);
                if (list.size() >= maxBlocks) break;

                head = head.getPrev(blockStore);
            }

            return list;
        } catch (BlockStoreException e) {
            throw new RuntimeException(e);
        }
    }

}
