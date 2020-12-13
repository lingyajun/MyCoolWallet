package com.bethel.mycoolwallet.helper.issue;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.admin.DevicePolicyManager;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Build;

import androidx.annotation.Nullable;
import androidx.core.app.ShareCompat;
import androidx.core.content.FileProvider;

import com.bethel.mycoolwallet.BuildConfig;
import com.bethel.mycoolwallet.CoolApplication;
import com.bethel.mycoolwallet.R;
import com.bethel.mycoolwallet.helper.Configuration;
import com.bethel.mycoolwallet.utils.BluetoothTools;
import com.bethel.mycoolwallet.utils.Commons;
import com.bethel.mycoolwallet.utils.Constants;
import com.bethel.mycoolwallet.utils.CrashReporter;
import com.bethel.mycoolwallet.utils.Utils;
import com.google.common.base.Joiner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Formatter;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public abstract class IssueReporter implements Runnable {
    private final Activity activity;
    private final String subject;
    private final CharSequence description;
    private final boolean hasDeviceInfo;
    private final boolean hasInstalledPackages;
    private final boolean hasApplicationLog;
    private final boolean hasWalletDump;

    private final CoolApplication application;
    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");

    public IssueReporter(Activity activity, String subject, CharSequence description, boolean hasDeviceInfo,
                         boolean hasInstalledPackages, boolean hasApplicationLog, boolean hasWalletDump) {
        this.activity = activity;
        this.subject = subject;
        this.description = description;
        this.hasDeviceInfo = hasDeviceInfo;
        this.hasInstalledPackages = hasInstalledPackages;
        this.hasApplicationLog = hasApplicationLog;
        this.hasWalletDump = hasWalletDump;

        application = CoolApplication.getApplication();
    }
    private static final Logger log = LoggerFactory.getLogger(IssueReporter.class);

    @Override
    public void run() {
        final StringBuilder text = new StringBuilder();
        final List<Uri> attachments = new ArrayList<>();
        final File cacheDir = application.getCacheDir();
        final File reportDir = new File(cacheDir, "report");
        reportDir.mkdir();

        text.append(description).append('\n');

        try {
            final CharSequence contextualData = collectContextualData();
            if (contextualData != null) {
                text.append("\n\n\n=== contextual data ===\n\n");
                text.append(contextualData);
            }
        } catch (final IOException x) {
            text.append(x.toString()).append('\n');
        }

        try {
            text.append("\n\n\n=== application info ===\n\n");

            final CharSequence applicationInfo = collectApplicationInfo();

            text.append(applicationInfo);
        } catch (final IOException x) {
            text.append(x.toString()).append('\n');
        }

        try {
            final CharSequence stackTrace = collectStackTrace();

            if (stackTrace != null) {
                text.append("\n\n\n=== stack trace ===\n\n");
                text.append(stackTrace);
            }
        } catch (final IOException x) {
            text.append("\n\n\n=== stack trace ===\n\n");
            text.append(x.toString()).append('\n');
        }

        if (hasDeviceInfo) {
            try {
                text.append("\n\n\n=== device info ===\n\n");

                final CharSequence deviceInfo = collectDeviceInfo();

                text.append(deviceInfo);
            } catch (final IOException x) {
                text.append(x.toString()).append('\n');
            }
        }

        if (hasInstalledPackages) {
            try {
                text.append("\n\n\n=== installed packages ===\n\n");
                CrashReporter.appendInstalledPackages(text, application);
            } catch (final IOException x) {
                text.append(x.toString()).append('\n');
            }
        }

        if (hasApplicationLog) {
            final File logDir = new File(application.getFilesDir(), "log");
            if (logDir.exists()) {
                for (final File logFile : logDir.listFiles()) {
                    if (logFile.isFile() && logFile.length() > 0) {
                        attachments.add(FileProvider.getUriForFile(application,
                                application.getPackageName() + ".file_attachment", logFile));
                    }
                } // end for
            }
        }

        if (hasWalletDump) {
            try {
                final CharSequence walletDump = collectWalletDump();

                if (walletDump != null) {
                    final File file = File.createTempFile("wallet-dump.", ".txt", reportDir);

                    final Writer writer = new OutputStreamWriter(new FileOutputStream(file), Commons.UTF_8);
                    writer.write(walletDump.toString());
                    writer.close();

                    attachments.add(
                            FileProvider.getUriForFile(activity, activity.getPackageName() + ".file_attachment", file));
                }
            } catch (final IOException x) {
                log.info("problem writing attachment", x);
            }
        }

        try {
            final File savedBackgroundTraces = File.createTempFile("background-traces.", ".txt", reportDir);
            if (CrashReporter.collectSavedBackgroundTraces(savedBackgroundTraces)) {
                attachments.add(FileProvider.getUriForFile(application,
                        application.getPackageName() + ".file_attachment", savedBackgroundTraces));
            }
            savedBackgroundTraces.deleteOnExit();
        } catch (final IOException x) {
            log.info("problem writing attachment", x);
        }

        text.append("\n\n 将注释放在顶部。这里没人注意。");

        startSend(subject(), text, attachments);
    }

    private void startSend(final String subject, final CharSequence text, final List<Uri> attachments) {
        final ShareCompat.IntentBuilder builder = ShareCompat.IntentBuilder.from(activity);
        for (final Uri attachment : attachments)
            builder.addStream(attachment);
        builder.addEmailTo(Constants.REPORT_EMAIL);
        if (subject != null)
            builder.setSubject(subject);
        builder.setText(text);
        builder.setType("text/plain");
        builder.setChooserTitle(R.string.report_issue_dialog_mail_intent_chooser);
        builder.startChooser();
        log.info("invoked chooser for sending issue report");
        log.info("subject ======= \n {}", subject);
        log.info("\n ======= subject");
        log.info("text ======= \n {}", text);
        log.info("\n ======= text");
    }

    @Nullable
    protected  String subject() {
        final StringBuilder builder = new StringBuilder(subject).append(": ");
        final PackageInfo pi = application.packageInfo();
        builder.append(Utils.versionLine(pi));

        builder.append(", android ").append(Build.VERSION.RELEASE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            builder.append(" (").append(Build.VERSION.SECURITY_PATCH).append(")");
        builder.append(", ").append(Build.MANUFACTURER);
        if (!Build.BRAND.equalsIgnoreCase(Build.MANUFACTURER))
            builder.append(' ').append(Build.BRAND);
        builder.append(' ').append(Build.MODEL);

        return builder.toString();
    }

    @Nullable
    protected CharSequence collectApplicationInfo() throws IOException {
        final StringBuilder report = new StringBuilder();
        final Calendar calendar = new GregorianCalendar(UTC);
        final PackageInfo pi = application.packageInfo();

        report.append("Version: " + pi.versionName + " (" + pi.versionCode + ")\n");
        report.append("APK Hash: " + Utils.apkHash(application).toString() + "\n");
        report.append("Package: " + pi.packageName + "\n");
        report.append("Flavor: " + BuildConfig.FLAVOR + "\n");
        report.append("Build Type: " + BuildConfig.BUILD_TYPE + "\n");

        report.append("Timezone: " + TimeZone.getDefault().getID() + "\n");
        calendar.setTimeInMillis(System.currentTimeMillis());
        report.append("Current time: " + String.format(Locale.US, "%tF %tT %tZ", calendar, calendar, calendar) + "\n");
        calendar.setTimeInMillis(CoolApplication.TIME_CREATE_APPLICATION);
        report.append(
                "Time of app launch: " + String.format(Locale.US, "%tF %tT %tZ", calendar, calendar, calendar) + "\n");
        calendar.setTimeInMillis(pi.firstInstallTime);
        report.append("Time of first app install: "
                + String.format(Locale.US, "%tF %tT %tZ", calendar, calendar, calendar) + "\n");
        calendar.setTimeInMillis(pi.lastUpdateTime);

        report.append("Time of last app update: "
                + String.format(Locale.US, "%tF %tT %tZ", calendar, calendar, calendar) + "\n");
        final long lastBackupTime = Configuration.INSTANCE.getLastBackupTime();
        calendar.setTimeInMillis(lastBackupTime);
        report.append("Time of last backup: "
                + (lastBackupTime > 0 ? String.format(Locale.US, "%tF %tT %tZ", calendar, calendar, calendar) : "none")
                + "\n");
        final long lastRestoreTime = Configuration.INSTANCE.getLastRestoreTime();
        calendar.setTimeInMillis(lastRestoreTime);
        report.append("Time of last restore: "
                + (lastRestoreTime > 0 ? String.format(Locale.US, "%tF %tT %tZ", calendar, calendar, calendar) : "none")
                + "\n");

//        final long lastEncryptKeysTime = Configuration.INSTANCE.getLastEncryptKeysTime();
//        calendar.setTimeInMillis(lastEncryptKeysTime);
//        report.append("Time of last encrypt keys: "
//                + (lastEncryptKeysTime > 0 ? String.format(Locale.US, "%tF %tT %tZ", calendar, calendar, calendar) :
//                "none")
//                + "\n");
        final long lastBlockchainResetTime = Configuration.INSTANCE.getLastBlockchainResetTime();
        calendar.setTimeInMillis(lastBlockchainResetTime);
        report.append(
                "Time of last blockchain reset: "
                        + (lastBlockchainResetTime > 0
                        ? String.format(Locale.US, "%tF %tT %tZ", calendar, calendar, calendar) : "none")
                        + "\n");
        report.append("Network: " + Constants.NETWORK_PARAMETERS.getId() + "\n");


        report.append("Databases:");
        for (final String db : application.databaseList())
            report.append(" " + db);
        report.append("\n");

        final File filesDir = application.getFilesDir();
        report.append("\nContents of FilesDir " + filesDir + ":\n");
        appendDir(report, filesDir, 0);
        report.append("free/usable space: ").append(Long.toString(filesDir.getFreeSpace() / 1024))
                .append("/").append(Long.toString(filesDir.getUsableSpace() / 1024)).append(" kB\n");

        return report;
    }

    private static void appendDir(final Appendable report, final File file, final int indent) throws IOException {
        for (int i = 0; i < indent; i++)
            report.append("  - ");

        final Formatter formatter = new Formatter(report);
        final Calendar calendar = new GregorianCalendar(UTC);
        calendar.setTimeInMillis(file.lastModified());
        formatter.format(Locale.US, "%tF %tT %8d kB  %s\n",
                calendar, calendar, file.length() / 1024, file.getName());
        formatter.close();

        final File[] files = file.listFiles();
        if (files != null)
            for (final File f : files)
                appendDir(report, f, indent + 1);
    }

    @Nullable
    protected CharSequence collectStackTrace() throws IOException {
        final StringBuilder stackTrace = new StringBuilder();
        CrashReporter.appendSavedCrashTrace(stackTrace);
        return stackTrace.length() > 0 ? stackTrace : null;
    }

    @Nullable
    protected CharSequence collectDeviceInfo() throws IOException {
        final StringBuilder report = new StringBuilder();
        final Resources res = application.getResources();
        final android.content.res.Configuration config = res.getConfiguration();
        final ActivityManager activityManager = (ActivityManager) application.getSystemService(Context.ACTIVITY_SERVICE);
        final DevicePolicyManager devicePolicyManager = (DevicePolicyManager) application
                .getSystemService(Context.DEVICE_POLICY_SERVICE);


        report.append("Manufacturer: " + Build.MANUFACTURER + "\n");
        report.append("Device Model: " + Build.MODEL + "\n");
        report.append("Android Version: " + Build.VERSION.RELEASE + "\n");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            report.append("Android security patch level: ").append(Build.VERSION.SECURITY_PATCH).append("\n");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            report.append("ABIs: ").append(Joiner.on(", ").skipNulls().join(Build.SUPPORTED_ABIS)).append("\n");
        report.append("Board: " + Build.BOARD + "\n");
        report.append("Brand: " + Build.BRAND + "\n");
        report.append("Device: " + Build.DEVICE + "\n");
        report.append("Product: " + Build.PRODUCT + "\n");
        report.append("Configuration: " + config + "\n");

        report.append("Screen Layout:" //
                + " size " + (config.screenLayout & android.content.res.Configuration.SCREENLAYOUT_SIZE_MASK) //
                + " long " + (config.screenLayout & android.content.res.Configuration.SCREENLAYOUT_LONG_MASK) //
                + " layoutdir " + (config.screenLayout & android.content.res.Configuration.SCREENLAYOUT_LAYOUTDIR_MASK) //
                + " round " + (config.screenLayout & android.content.res.Configuration.SCREENLAYOUT_ROUND_MASK) + "\n");
        report.append("Display Metrics: " + res.getDisplayMetrics() + "\n");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
            report.append("Memory Class: " + activityManager.getMemoryClass() + "/" + activityManager.getLargeMemoryClass()
                + (activityManager.isLowRamDevice() ? " (low RAM device)" : "") + "\n");
        report.append("Storage Encryption Status: " + devicePolicyManager.getStorageEncryptionStatus() + "\n");
        report.append("Bluetooth MAC: " + bluetoothMac() + "\n");
        report.append("Runtime: ").append(System.getProperty("java.vm.name")).append(" ")
                .append(System.getProperty("java.vm.version")).append("\n");
        return report;
    }

    @Nullable
    protected CharSequence collectContextualData() throws IOException {
        return null;
    }

    @Nullable
    protected CharSequence collectWalletDump() throws IOException {
        return null;
    }

    private static String bluetoothMac() {
        try {
            return BluetoothTools.getAddress(BluetoothAdapter.getDefaultAdapter());
        } catch (final Exception x) {
            return x.getMessage();
        }
    }
}
