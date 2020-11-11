package com.bethel.mycoolwallet.service;

import android.app.Service;
import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.IBinder;
import android.text.format.DateUtils;

import androidx.annotation.RequiresApi;

import com.bethel.mycoolwallet.CoolApplication;
import com.bethel.mycoolwallet.helper.Configuration;
import com.bethel.mycoolwallet.utils.Constants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class BlockChainJobService extends JobService {
    private static final Logger log = LoggerFactory.getLogger(BlockChainJobService.class);

    public static void startUp() {
        CoolApplication application = CoolApplication.getApplication();
        Configuration config = application.getConfiguration();
        final long lastUsedAgo = config.getLastUsedAgo();

        // apply some backoff
        final long interval;
        if (lastUsedAgo < Constants.LAST_USAGE_THRESHOLD_JUST_MS)
            interval = DateUtils.MINUTE_IN_MILLIS * 15;
        else if (lastUsedAgo < Constants.LAST_USAGE_THRESHOLD_TODAY_MS)
            interval = DateUtils.HOUR_IN_MILLIS;
        else if (lastUsedAgo < Constants.LAST_USAGE_THRESHOLD_RECENTLY_MS)
            interval = DateUtils.DAY_IN_MILLIS / 2;
        else
            interval = DateUtils.DAY_IN_MILLIS;

        log.info("last used {} minutes ago, rescheduling blockchain sync in roughly {} minutes",
                lastUsedAgo / DateUtils.MINUTE_IN_MILLIS, interval / DateUtils.MINUTE_IN_MILLIS);

        final JobScheduler jobScheduler = (JobScheduler) application.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        final JobInfo.Builder jobInfo = new JobInfo.Builder(0, new ComponentName(application,
                BlockChainJobService.class));
        jobInfo.setMinimumLatency(interval);
        jobInfo.setOverrideDeadline(DateUtils.WEEK_IN_MILLIS);
        jobInfo.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY);
        jobInfo.setRequiresDeviceIdle(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            jobInfo.setRequiresBatteryNotLow(true);
            jobInfo.setRequiresStorageNotLow(true);
        }
        jobScheduler.schedule(jobInfo.build());
    }

    @Override
    public boolean onStartJob(JobParameters jobParameters) {
        IntentFilter storageFilter = new IntentFilter(Intent.ACTION_DEVICE_STORAGE_LOW);
        IntentFilter batteryFilter = new IntentFilter(Intent.ACTION_BATTERY_LOW);

        Intent storageIntent = registerReceiver(null, storageFilter);
        Intent batteryIntent = registerReceiver(null, batteryFilter);
        boolean storageLow = null!=storageIntent;
        boolean batteryLow = null!=batteryIntent;
        if (storageLow) log.info("storage low, not starting block chain sync");
        if (batteryLow) log.info("battery low, not starting block chain sync");

        if (!storageLow && !batteryLow) {
            // start service
            BlockChainService.start(this, false);
        }
        return false;
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        return false;
    }
}
