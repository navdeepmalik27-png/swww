package com.overcast.mini;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;

/**
 * Ultra-low-battery background updater using Android's Doze-friendly JobScheduler.
 * - Batches with OS network windows (15 min flex)
 * - Skips updates when offline or battery is low
 * - Zero wake locks, zero foreground service, zero background GPS
 */
public final class WeatherJobService extends JobService {
    private static final int JOB_PERIODIC = 101;
    private static final int JOB_IMMEDIATE = 102;
    private static final long MIN_REFRESH_GAP_MS = 30_000L; // 30s debounce

    private volatile boolean cancelled;

    public static void schedulePeriodic(Context context) {
        JobScheduler js = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        if (js == null) return;
        long intervalMs = Math.max(30, WeatherPrefs.getIntervalMinutes(context)) * 60_000L;
        long flexMs = Math.min(15 * 60_000L, intervalMs / 4);
        JobInfo job = new JobInfo.Builder(JOB_PERIODIC, new ComponentName(context, WeatherJobService.class))
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setRequiresBatteryNotLow(true)
                .setPeriodic(intervalMs, flexMs)
                .setPersisted(true)
                .build();
        js.schedule(job);
    }

    public static void triggerNow(Context context, boolean force) {
        if (!WeatherPrefs.hasLocation(context)) return;
        long age = System.currentTimeMillis() - WeatherPrefs.getLastUpdatedMs(context);
        if (!force && age < MIN_REFRESH_GAP_MS) {
            WidgetRenderer.updateAll(context);
            return;
        }
        JobScheduler js = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        if (js == null) return;
        JobInfo job = new JobInfo.Builder(JOB_IMMEDIATE, new ComponentName(context, WeatherJobService.class))
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setOverrideDeadline(10_000L)
                .build();
        js.schedule(job);
    }

    public static void cancelAll(Context context) {
        JobScheduler js = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        if (js != null) js.cancelAll();
    }

    @Override
    public boolean onStartJob(final JobParameters params) {
        if (!WeatherPrefs.hasLocation(this)) return false;
        cancelled = false;
        new Thread(() -> {
            try {
                String json = OpenMeteoClient.fetchForecastJson(
                        WeatherPrefs.getLat(this),
                        WeatherPrefs.getLon(this),
                        WeatherPrefs.isFahrenheit(this));
                if (!cancelled && json != null && !json.isEmpty()) {
                    WeatherPrefs.saveCache(this, json, System.currentTimeMillis());
                    WidgetRenderer.updateAll(this);
                }
            } finally {
                jobFinished(params, false);
            }
        }).start();
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        cancelled = true;
        return false; // do not aggressively retry; wait for next OS window
    }
}
