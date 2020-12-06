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

import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.VerificationException;
import org.bitcoinj.core.VersionMessage;
import org.bitcoinj.crypto.LinuxSecureRandom;
import org.bitcoinj.wallet.Wallet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CoolApplication extends Application {

    private static CoolApplication application = null;
//    private final MyCoolWalletManager myWalletManager =  new MyCoolWalletManager();

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
        new LinuxSecureRandom(); // init proper random number generator: org.bitcoinj.crypto.LinuxSecureRandom;
        Logging.init(getFilesDir());
        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().detectAll().permitDiskReads()
                .permitDiskWrites().penaltyLog().build());

        log.info("=== starting app using flavor: {}, build type: {}", BuildConfig.FLAVOR, BuildConfig.BUILD_TYPE );

        super.onCreate();
        application = this;

        CrashReporter.init(getCacheDir());

        MyCoolWalletManager.INSTANCE.init(this);

        activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);

        XUI.init(this);
        XUI.debug(true);
    }

    public Wallet getWallet() {
        return MyCoolWalletManager.INSTANCE.getWallet();
    }

    public void getWalletAsync(OnWalletLoadedListener listener) {
        MyCoolWalletManager.INSTANCE.getWalletAsync(listener);
    }

    public void autoSaveWalletNow() {
        MyCoolWalletManager.INSTANCE.autoSaveWalletNow();
    }

    public void replaceWallet(final Wallet newWallet) {
        MyCoolWalletManager.INSTANCE.replaceWallet(newWallet);
    }

    public void processDirectTransaction(final Transaction tx) throws VerificationException {
        MyCoolWalletManager.INSTANCE.processDirectTransaction(tx);
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

    public static String httpUserAgent(final String versionName) {
        final VersionMessage versionMessage = new VersionMessage(Constants.NETWORK_PARAMETERS, 0);
        versionMessage.appendToSubVer(Constants.USER_AGENT, versionName, null);
        return versionMessage.subVer;
    }

    public String httpUserAgent() {
        return httpUserAgent(packageInfo().versionName);
    }

}
