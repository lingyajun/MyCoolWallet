package com.bethel.mycoolwallet;

import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.StrictMode;

import androidx.multidex.MultiDex;

import com.bethel.mycoolwallet.helper.Configuration;
import com.bethel.mycoolwallet.interfaces.OnWalletLoadedListener;
import com.bethel.mycoolwallet.manager.MyCoolWalletManager;
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
    private MyCoolWalletManager myWalletManager = null;
    private ActivityManager activityManager;
    private Configuration mConfig;

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

        myWalletManager = new MyCoolWalletManager();
        myWalletManager.init(this);

        activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);

        XUI.init(this);
        XUI.debug(true);
    }

    public Wallet getWallet() {
        return myWalletManager.getWallet();
    }

    public void getWalletAsync(OnWalletLoadedListener listener) {
         myWalletManager.getWalletAsync(listener);
    }

    public Configuration getConfiguration() {
        if (null== mConfig) mConfig = Configuration.instance(this);
        return mConfig;
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

    // 加密 -- 迭代次数
    public int scryptIterationsTarget() {
        return activityManager.getMemoryClass() <= 128 ||
                (Build.VERSION.SDK_INT >=21 && Build.SUPPORTED_64_BIT_ABIS.length == 0)
                ? Constants.SCRYPT_ITERATIONS_TARGET_LOWRAM : Constants.SCRYPT_ITERATIONS_TARGET;
    }

    public int maxConnectedPeers() {
        return activityManager.getMemoryClass() <= 128 ? 4 : 6;
    }

    public final String applicationPackageFlavor() {
        final String packageName = getPackageName();
        final int index = packageName.lastIndexOf('_');

        if (index != -1)
            return packageName.substring(index + 1);
        else
            return null;
    }

}
