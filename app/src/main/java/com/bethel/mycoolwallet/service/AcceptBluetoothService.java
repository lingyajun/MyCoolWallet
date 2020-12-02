package com.bethel.mycoolwallet.service;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.text.format.DateUtils;

import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleService;

import com.bethel.mycoolwallet.CoolApplication;
import com.bethel.mycoolwallet.R;
import com.bethel.mycoolwallet.mvvm.live_data.WalletLiveData;
import com.bethel.mycoolwallet.request.bluetooth_server.AbsAcceptBluetoothTask;
import com.bethel.mycoolwallet.request.bluetooth_server.AcceptClassicBluetoothTask;
import com.bethel.mycoolwallet.request.bluetooth_server.AcceptPaymentProtocolTask;
import com.bethel.mycoolwallet.utils.CrashReporter;
import com.xuexiang.xui.widget.toast.XToast;

import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.VerificationException;
import org.bitcoinj.wallet.Wallet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static androidx.core.util.Preconditions.checkNotNull;

public class AcceptBluetoothService extends LifecycleService {
    private WalletLiveData wallet;
    private PowerManager.WakeLock wakeLock;
    private AbsAcceptBluetoothTask classicBleTask;
    private AbsAcceptBluetoothTask paymentProtocolBleTask;
    private CoolApplication application;

    private final Handler handler = new Handler();

    private static final long TIMEOUT_MS = 5 * DateUtils.MINUTE_IN_MILLIS;
    private static final Logger log = LoggerFactory.getLogger(AcceptBluetoothService.class);

    @Override
    public IBinder onBind(Intent intent) {
       return super.onBind(intent);
    }

    @Override
    public int onStartCommand(@NonNull Intent intent, int flags, int startId) {
         super.onStartCommand(intent, flags, startId);

        handler.removeCallbacks(timeoutRunnable);
        handler.postDelayed(timeoutRunnable, TIMEOUT_MS);
        return START_NOT_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        application = CoolApplication.getApplication();
        log.debug(".onCreate()");

        initWakeLock();
        initBroadcastReceiver();
        initBluetoothServers();

        observeWallet();
    }

    private void initWakeLock() {
        final PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, getClass().getName());
        if (null!=wakeLock) {
            wakeLock.acquire(TIMEOUT_MS);
        }
    }

    private void initBroadcastReceiver() {
        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(bluetoothReceiver, filter);
    }

    private void observeWallet() {
        wallet = new WalletLiveData(application);
        wallet.observe(this, wallet1 -> {
            if (null!=classicBleTask) classicBleTask.executeAsyncTask();
            if (null!=paymentProtocolBleTask) paymentProtocolBleTask.executeAsyncTask();
        });
    }

    private void initBluetoothServers() {
        final BluetoothAdapter bluetoothAdapter = checkNotNull(BluetoothAdapter.getDefaultAdapter());

        try {
            classicBleTask = new AcceptClassicBluetoothTask(bluetoothAdapter) {
                @Override
                protected boolean handleTx(Transaction tx) {
                    log.debug("AcceptClassicBluetoothTask, handleTx");
                    return AcceptBluetoothService.this.handleTx(tx);
                }
            };

            paymentProtocolBleTask = new AcceptPaymentProtocolTask(bluetoothAdapter) {
                @Override
                protected boolean handleTx(Transaction tx) {
                    log.debug("AcceptPaymentProtocolTask, handleTx");
                    return AcceptBluetoothService.this.handleTx(tx);
                }
            };
        } catch (IOException e) {
            XToast.error(this, getString(R.string.error_bluetooth, e.getMessage())).show();
            log.warn("problem with listening, stopping service", e);
            CrashReporter.saveBackgroundTrace(e, application.packageInfo());
            stopSelf();
        }
    }

    protected  boolean handleTx(final Transaction tx){
        log.info("tx {} arrived via bluetooth", tx.getTxId());
        final Wallet wallet = this.wallet.getValue();
        if (null==wallet) return false;

        try {
            // true if the given transaction sends coins to any of our keys,
            // or has inputs spending any of our outputs,
//            returns true if tx has inputs that are spending outputs which are
//                     not ours but which are spent by pending transactions.
            if (wallet.isTransactionRelevant(tx)) {

//when we have found a transaction (via network broadcast or otherwise) that is relevant to this wallet
//      and want to record it.
                wallet.receivePending(tx, null);

                handler.post(()->
                        BlockChainService.broadcastTransaction(AcceptBluetoothService.this, tx));
            } else {
                log.info("tx {} irrelevant", tx.getTxId());
            }

            return true;

        } catch (VerificationException e) {
            log.info("cannot verify tx {} received via bluetooth \n {}", tx.getTxId(), e);
        }
        return false;
    }

    @Override
    public void onDestroy() {
        if (null!=classicBleTask) {
            classicBleTask.stopAccepting();
        }
        if (null!=paymentProtocolBleTask) {
            paymentProtocolBleTask.stopAccepting();
        }

        unregisterReceiver(bluetoothReceiver);

        if (null!=wakeLock && wakeLock.isHeld()) {
            wakeLock.release();
        }
        handler.removeCallbacksAndMessages(null);

        super.onDestroy();
        log.debug(".onDestroy()");
    }

    private final BroadcastReceiver bluetoothReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0);

            switch (state) {
                case BluetoothAdapter.STATE_OFF:
                case BluetoothAdapter.STATE_TURNING_OFF:
                    // off
                    log.info("bluetooth was turned off, stopping service");
                    stopSelf();
                    break;
            }
        }
    };

    private final Runnable timeoutRunnable = ()-> {
            log.info("timeout expired, stopping service");
            stopSelf();
        };
}
