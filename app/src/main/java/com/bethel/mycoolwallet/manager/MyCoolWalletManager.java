package com.bethel.mycoolwallet.manager;

import android.os.Looper;

import androidx.annotation.MainThread;
import androidx.annotation.WorkerThread;

import com.bethel.mycoolwallet.CoolApplication;
import com.bethel.mycoolwallet.R;
import com.bethel.mycoolwallet.interfaces.OnWalletLoadedListener;
import com.bethel.mycoolwallet.utils.Constants;
import com.bethel.mycoolwallet.utils.CrashReporter;
import com.bethel.mycoolwallet.utils.WalletUtils;
import com.google.common.base.Stopwatch;
import com.google.common.util.concurrent.SettableFuture;
import com.xuexiang.xui.widget.toast.XToast;

import org.bitcoinj.crypto.MnemonicCode;
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

public class MyCoolWalletManager {
    private static final Logger log = LoggerFactory.getLogger(MyCoolWalletManager.class);
    private static final String BIP39_WORDLIST_FILENAME = "bip39-wordlist.txt";

    private CoolApplication application;
    private File walletFile;
    private WalletFiles walletFiles;

    public void init(CoolApplication app) {
        application = app;
        walletFile = application.getFileStreamPath(Constants.Files.WALLET_FILENAME_PROTOBUF);
        cleanupFiles();
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
//      todo                  wallet = WalletUtils.restoreWalletFromAutoBackup(WalletApplication.this);
                        wallet = restoreWalletFromAutoBackup();
                        if (wallet != null)
                            XToast.info(application, R.string.toast_wallet_reset);
                    }
                    if (!wallet.isConsistent()) {
                        log.warn("inconsistent wallet, auto-restoring: " + walletFile);
//    todo                    wallet = WalletUtils.restoreWalletFromAutoBackup(WalletApplication.this);
                        wallet = restoreWalletFromAutoBackup();
                        if (wallet != null)
                            XToast.info(application, R.string.toast_wallet_reset);
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
                    autosaveWalletNow(); // persist...
                    WalletUtils.autoBackupWallet(application, wallet); // ...and backup asap
                    watch.stop();
                    log.info("fresh {} wallet created, took {}", Constants.DEFAULT_OUTPUT_SCRIPT_TYPE, watch);

//       todo             config.armBackupReminder();
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

    private void armBackupReminder() {
//       todo       config.armBackupReminder();
        // SharedPreferences 存储变量
//        XToast.info(application, " armBackupReminder").show();
        log.info("armBackupReminder");
    }

    private Wallet restoreWalletFromAutoBackup() {
//    todo       wallet = WalletUtils.restoreWalletFromAutoBackup(WalletApplication.this);
        // 存储钱包文件，重置区块链
//        XToast.info(application, " restoreWalletFromAutoBackup").show();
        log.info("restoreWalletFromAutoBackup");
        return null;
    }

    public void autosaveWalletNow() {
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

//    public void replaceWallet(final Wallet newWallet) {
//        newWallet.cleanup();
//        if (newWallet.isDeterministicUpgradeRequired(Constants.UPGRADE_OUTPUT_SCRIPT_TYPE) && !newWallet.isEncrypted())
//            newWallet.upgradeToDeterministic(Constants.UPGRADE_OUTPUT_SCRIPT_TYPE, null);
//        BlockchainService.resetBlockchain(this);
//
//        final Wallet oldWallet = getWallet();
//        synchronized (getWalletLock) {
//            oldWallet.shutdownAutosaveAndWait(); // this will also prevent BlockchainService to save
//            walletFiles = newWallet.autosaveToFile(walletFile, Constants.Files.WALLET_AUTOSAVE_DELAY_MS,
//                    TimeUnit.MILLISECONDS, null);
//        }
//        config.maybeIncrementBestChainHeightEver(newWallet.getLastBlockSeenHeight());
//        WalletUtils.autoBackupWallet(this, newWallet);
//
//        final Intent broadcast = new Intent(ACTION_WALLET_REFERENCE_CHANGED);
//        LocalBroadcastManager.getInstance(this).sendBroadcast(broadcast);
//    }

    /**
     * Sets the given context as the current thread context.
     * You should use this if you create your own threads that want to create core BitcoinJ objects.
     */
    public static void propagate() {
        org.bitcoinj.core.Context.propagate(Constants.CONTEXT);
    }
}
