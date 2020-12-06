package com.bethel.mycoolwallet.manager;

import android.content.Intent;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.MainThread;
import androidx.annotation.WorkerThread;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.bethel.mycoolwallet.CoolApplication;
import com.bethel.mycoolwallet.R;
import com.bethel.mycoolwallet.helper.Configuration;
import com.bethel.mycoolwallet.interfaces.OnWalletLoadedListener;
import com.bethel.mycoolwallet.service.BlockChainService;
import com.bethel.mycoolwallet.utils.Constants;
import com.bethel.mycoolwallet.utils.CrashReporter;
import com.bethel.mycoolwallet.utils.WalletUtils;
import com.google.common.base.Stopwatch;
import com.google.common.util.concurrent.SettableFuture;
import com.xuexiang.xui.widget.toast.XToast;

import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.VerificationException;
import org.bitcoinj.crypto.MnemonicCode;
import org.bitcoinj.utils.Threading;
import org.bitcoinj.wallet.UnreadableWalletException;
import org.bitcoinj.wallet.Wallet;
import org.bitcoinj.wallet.WalletFiles;
import org.bitcoinj.wallet.WalletProtobufSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

//public class MyCoolWalletManager {
public enum  MyCoolWalletManager {
    /**
     * 枚举方法 --  单例模式
     * (1)自由序列化。
     * (2)保证只有一个实例。
     * (3)线程安全。
     */
    INSTANCE;

    public static final String ACTION_WALLET_REFERENCE_CHANGED =
            MyCoolWalletManager.class.getPackage().getName() + ".wallet_reference_changed";
    private static final String BIP39_WORDLIST_FILENAME = "bip39-wordlist.txt";

    private CoolApplication application;
    private File walletFile;
    private WalletFiles walletFiles;
    private Configuration mConfig;

    private Handler mHandler;

    private static final Logger log = LoggerFactory.getLogger(MyCoolWalletManager.class);

    public void init(CoolApplication app) {
        Threading.throwOnLockCycles();
        org.bitcoinj.core.Context.enableStrictMode();
        propagate();
//        org.bitcoinj.core.Context.propagate(Constants.CONTEXT);

        log.info("=== starting wallet  network: {}",  Constants.NETWORK_PARAMETERS.getId());

        application = app;

        initWalletExceptionHandler();

        mConfig = app.getConfiguration();
        walletFile = application.getFileStreamPath(Constants.Files.WALLET_FILENAME_PROTOBUF);
        mHandler = new Handler(Looper.getMainLooper());
        cleanupFiles();
    }

    private void initWalletExceptionHandler() {
        Threading.uncaughtExceptionHandler = (thread, throwable) -> {
            log.info("bitcoinj uncaught exception", throwable);
            CrashReporter.saveBackgroundTrace(throwable, application.packageInfo());
        };
    }

    private void cleanupFiles() {
        String[] fileList = application.fileList();
        for (final String filename : fileList) {
            if (filename.startsWith(Constants.Files.WALLET_KEY_BACKUP_BASE58)
                    || filename.startsWith(Constants.Files.WALLET_KEY_BACKUP_PROTOBUF + '.')
                    || filename.endsWith(".tmp")) {
                final File file = new File(application.getFilesDir(), filename);
                log.info("removing obsolete file: '{}'", file);
                file.delete();
            }
        }
    }


    @MainThread
    public Wallet getWallet() {
        final Stopwatch watch = Stopwatch.createStarted();
        final SettableFuture<Wallet> future = SettableFuture.create();
        getWalletAsync((wallet) -> future.set(wallet));
        try {
            return future.get();
        } catch (final InterruptedException | ExecutionException x) {
            throw new RuntimeException(x);
        } finally {
            watch.stop();
            if (Looper.myLooper() == Looper.getMainLooper())
                log.warn("UI thread blocked for " + watch + " when using getWallet()", new RuntimeException());
        }
    }

    /* Executors.newSingleThreadExecutor() :
        只有一个核心线程，并没有超时机制.
        统一所有任务到一个线程中， 这使得在这些任务之间不需要处理线程同步的问题.
     */
    private final Executor getWalletExecutor = Executors.newSingleThreadExecutor();
    private final Object getWalletLock = new Object();

    @MainThread
    public void getWalletAsync(final OnWalletLoadedListener listener) {
        getWalletExecutor.execute(new Runnable() {
            @Override
            public void run() {
                propagate();
                synchronized (getWalletLock) {
                    initMnemonicCode();
                    if (walletFiles == null)
                        loadWalletFromProtobuf();
                }
                listener.onWalletLoaded(walletFiles.getWallet());
//                showThreadToast(R.string.wallet_low_storage_dialog_button_apps); // test
            }

            @WorkerThread
            private void loadWalletFromProtobuf() {
                Wallet wallet;
                if (walletFile.exists()) {
                    try (final FileInputStream walletStream = new FileInputStream(walletFile)) {
                        final Stopwatch watch = Stopwatch.createStarted();
                        wallet = new WalletProtobufSerializer().readWallet(walletStream);
                        watch.stop();

                        if (!wallet.getParams().equals(Constants.NETWORK_PARAMETERS))
                            throw new UnreadableWalletException(
                                    "bad wallet network parameters: " + wallet.getParams().getId());

                        log.info("wallet loaded from: '{}', took {}", walletFile, watch);
                    } catch (final IOException | UnreadableWalletException x) {
                        log.warn("problem loading wallet, auto-restoring: " + walletFile, x);
                        wallet = restoreWalletFromAutoBackup();
                        if (wallet != null) {
                            showToast(R.string.toast_wallet_reset);
//                            XToast.info(application, R.string.toast_wallet_reset);
                        }
                    }
                    if (!wallet.isConsistent()) {
                        log.warn("inconsistent wallet, auto-restoring: " + walletFile);
                        wallet = restoreWalletFromAutoBackup();
                        if (wallet != null) {
                            showToast(R.string.toast_wallet_reset);
                        }
                    }

                    if (!wallet.getParams().equals(Constants.NETWORK_PARAMETERS))
                        throw new Error("bad wallet network parameters: " + wallet.getParams().getId());

                    wallet.cleanup();
                    walletFiles = wallet.autosaveToFile(walletFile, Constants.Files.WALLET_AUTOSAVE_DELAY_MS,
                            TimeUnit.MILLISECONDS, null);
                } else {
                    final Stopwatch watch = Stopwatch.createStarted();
                    wallet = Wallet.createDeterministic(Constants.NETWORK_PARAMETERS,
                            Constants.DEFAULT_OUTPUT_SCRIPT_TYPE);
                    walletFiles = wallet.autosaveToFile(walletFile, Constants.Files.WALLET_AUTOSAVE_DELAY_MS,
                            TimeUnit.MILLISECONDS, null);
                    autoSaveWalletNow(); // persist...
                    WalletUtils.autoBackupWallet(application, wallet); // ...and backup asap
                    watch.stop();
                    log.info("fresh {} wallet created, took {}", Constants.DEFAULT_OUTPUT_SCRIPT_TYPE, watch);

                    armBackupReminder();
                }
            }

            private void initMnemonicCode() {
                if (MnemonicCode.INSTANCE == null) {
                    try {
                        final Stopwatch watch = Stopwatch.createStarted();
                        MnemonicCode.INSTANCE = new MnemonicCode(application.getAssets().open(BIP39_WORDLIST_FILENAME), null);
                        watch.stop();
                        log.info("BIP39 wordlist loaded from: '{}', took {}", BIP39_WORDLIST_FILENAME, watch);
                    } catch (final IOException x) {
                        throw new Error(x);
                    }
                }
            }

        });
    }

    private void showToast(final int msgId) {
        mHandler.post(()->  XToast.info(application, msgId).show());
    }

    private void armBackupReminder() {
              mConfig.armBackupReminder();
        // SharedPreferences 存储变量
//        XToast.info(application, " armBackupReminder").show();
        log.info("armBackupReminder");
    }

    private Wallet restoreWalletFromAutoBackup() {
        Wallet wallet = WalletUtils.restoreWalletFromAutoBackup(application);
        // 存储钱包文件，重置区块链
//        XToast.info(application, " restoreWalletFromAutoBackup").show();
        log.info("restoreWalletFromAutoBackup");
        return wallet;
    }

    public void autoSaveWalletNow() {
        final Stopwatch watch = Stopwatch.createStarted();
        synchronized (getWalletLock) {
            if (walletFiles != null) {
                watch.stop();
                log.info("wallet saved to: '{}', took {}", walletFile, watch);
                try {
                    walletFiles.saveNow();
                } catch (final IOException x) {
                    log.warn("problem with forced autosaving of wallet", x);
                    CrashReporter.saveBackgroundTrace(x, application.packageInfo());
                }
            }
        }
    }

    public void replaceWallet(final Wallet newWallet) {
        newWallet.cleanup();
        if (newWallet.isDeterministicUpgradeRequired(Constants.UPGRADE_OUTPUT_SCRIPT_TYPE) && !newWallet.isEncrypted())
            newWallet.upgradeToDeterministic(Constants.UPGRADE_OUTPUT_SCRIPT_TYPE, null);
        BlockChainService.resetBlockChain(application);

        final Wallet oldWallet = getWallet();
        synchronized (getWalletLock) {
            oldWallet.shutdownAutosaveAndWait(); // this will also prevent BlockchainService to save
            walletFiles = newWallet.autosaveToFile(walletFile, Constants.Files.WALLET_AUTOSAVE_DELAY_MS,
                    TimeUnit.MILLISECONDS, null);
        }
        mConfig.maybeIncrementBestChainHeightEver(newWallet.getLastBlockSeenHeight());
        WalletUtils.autoBackupWallet(application, newWallet);

        final Intent broadcast = new Intent(ACTION_WALLET_REFERENCE_CHANGED);
        LocalBroadcastManager.getInstance(application).sendBroadcast(broadcast);
    }

    public void processDirectTransaction(final Transaction tx) throws VerificationException {
        final Wallet wallet = getWallet();
        if (wallet.isTransactionRelevant(tx)) {
            wallet.receivePending(tx, null);
            BlockChainService.broadcastTransaction(application, tx);
        }
    }

    /**
     * Sets the given context as the current thread context.
     * You should use this if you create your own threads that want to create core BitcoinJ objects.
     */
    public static void propagate() {
        org.bitcoinj.core.Context.propagate(Constants.CONTEXT);
    }
}
