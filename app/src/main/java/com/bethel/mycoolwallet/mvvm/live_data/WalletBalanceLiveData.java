package com.bethel.mycoolwallet.mvvm.live_data;

import android.content.SharedPreferences;

import com.bethel.mycoolwallet.CoolApplication;
import com.bethel.mycoolwallet.helper.Configuration;
import com.bethel.mycoolwallet.manager.MyCoolWalletManager;

import org.bitcoinj.core.Coin;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.utils.Threading;
import org.bitcoinj.wallet.Wallet;
import org.bitcoinj.wallet.listeners.WalletChangeEventListener;
import org.bitcoinj.wallet.listeners.WalletCoinsReceivedEventListener;
import org.bitcoinj.wallet.listeners.WalletCoinsSentEventListener;
import org.bitcoinj.wallet.listeners.WalletReorganizeEventListener;

public class WalletBalanceLiveData extends AbstractWalletLiveData<Coin> implements SharedPreferences.OnSharedPreferenceChangeListener {
    private Configuration mConfig;
    private Wallet.BalanceType balanceType;

    private final WalletListener mListener = new WalletListener();

    public WalletBalanceLiveData(CoolApplication application, Wallet.BalanceType balanceType) {
        super(application);
        this.balanceType = balanceType;
        this.mConfig = application.getConfiguration();
    }

    public WalletBalanceLiveData(CoolApplication application) {
        this(application, Wallet.BalanceType.ESTIMATED); // estimated 估计
    }

    @Override
    protected void onWalletActive(Wallet wallet) {
        addWalletListener(wallet);

        mConfig.registerOnSharedPreferenceChangeListener(this);
        load();
    }

    @Override
    protected void onWalletInactive(Wallet wallet) {
        mConfig.unregisterOnSharedPreferenceChangeListener(this);
        removeWalletListener(wallet);
    }

    @Override
    protected void load() {
        final Wallet wallet = getWallet();
        executeAsyncTask(()->{
            MyCoolWalletManager.propagate();
            postValue(wallet.getBalance(balanceType));
        });
    }

    private void removeWalletListener(Wallet wallet) {
        wallet.removeCoinsSentEventListener(mListener);
        wallet.removeCoinsReceivedEventListener(mListener);
        wallet.removeReorganizeEventListener(mListener);
        wallet.removeChangeEventListener(mListener);
    }

    private void addWalletListener(Wallet wallet) {
        wallet.addCoinsSentEventListener(Threading.SAME_THREAD, mListener);
        wallet.addCoinsReceivedEventListener(Threading.SAME_THREAD, mListener);
        wallet.addReorganizeEventListener(Threading.SAME_THREAD, mListener);
        wallet.addChangeEventListener(Threading.SAME_THREAD, mListener);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
        if (Configuration.PREFS_KEY_BTC_PRECISION.equals(s))  load();
    }

    private final class WalletListener implements WalletCoinsReceivedEventListener,
            WalletCoinsSentEventListener, WalletReorganizeEventListener, WalletChangeEventListener {
        @Override
        public void onWalletChanged(Wallet wallet) {
            triggerLoad();
        }

        @Override
        public void onCoinsReceived(Wallet wallet, Transaction tx, Coin prevBalance, Coin newBalance) {
            triggerLoad();
        }

        @Override
        public void onCoinsSent(Wallet wallet, Transaction tx, Coin prevBalance, Coin newBalance) {
            triggerLoad();
        }

        @Override
        public void onReorganize(Wallet wallet) {
            triggerLoad();
        }
    }
}
