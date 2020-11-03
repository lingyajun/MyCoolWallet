package com.bethel.mycoolwallet;

import android.app.Application;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.StrictMode;

import androidx.multidex.MultiDex;

import com.bethel.mycoolwallet.interfaces.OnWalletLoadedListener;
import com.bethel.mycoolwallet.manager.MyWalletManager;
import com.bethel.mycoolwallet.utils.Constants;
import com.bethel.mycoolwallet.utils.CrashReporter;
import com.bethel.mycoolwallet.utils.Logging;
import com.xuexiang.xui.XUI;

import org.bitcoinj.crypto.LinuxSecureRandom;
import org.bitcoinj.utils.Threading;
import org.bitcoinj.wallet.Wallet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CoolApplication extends Application {
    public static final String ACTION_WALLET_REFERENCE_CHANGED = CoolApplication.class.getPackage().getName()
            + ".wallet_reference_changed";

    private static CoolApplication application = null;
    private  MyWalletManager myWalletManager = null;

    private static final Logger log = LoggerFactory.getLogger(CoolApplication.class);
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(base);
    }

    @Override
    public void onCreate() {
        new LinuxSecureRandom(); // init proper random number generator
        Logging.init(getFilesDir());
        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().detectAll().permitDiskReads()
                .permitDiskWrites().penaltyLog().build());

        Threading.throwOnLockCycles();
        org.bitcoinj.core.Context.enableStrictMode();
        org.bitcoinj.core.Context.propagate(Constants.CONTEXT);

        log.info("=== starting app using flavor: {}, build type: {}, network: {}", BuildConfig.FLAVOR,
                BuildConfig.BUILD_TYPE, Constants.NETWORK_PARAMETERS.getId());

        super.onCreate();
        application = this;

        CrashReporter.init(getCacheDir());
        Threading.uncaughtExceptionHandler = (thread, throwable) -> {
            log.info("bitcoinj uncaught exception", throwable);
            CrashReporter.saveBackgroundTrace(throwable, packageInfo());
        };

        myWalletManager = new MyWalletManager();
        myWalletManager.init(this);

        XUI.init(this);
        XUI.debug(true);
    }

    public Wallet getWallet() {
        return myWalletManager.getWallet();
    }

    public void getWalletAsync(OnWalletLoadedListener listener) {
         myWalletManager.getWalletAsync(listener);
    }

    public static CoolApplication getApplication() {
        return application;
    }

    private PackageInfo packageInfo;
    public synchronized PackageInfo packageInfo() {
        // replace by BuildConfig.VERSION_* as soon as it's possible
        if (packageInfo == null) {
            try {
                packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            } catch (final PackageManager.NameNotFoundException x) {
                throw new RuntimeException(x);
            }
        }
        return packageInfo;
    }

}
