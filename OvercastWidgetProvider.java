package com.overcast.mini;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;

public class OvercastWidgetProvider extends AppWidgetProvider {

    @Override
    public void onEnabled(Context context) {
        WeatherJobService.schedulePeriodic(context);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager mgr, int[] ids) {
        WidgetRenderer.updateAll(context);
        WeatherJobService.schedulePeriodic(context);
        // Only fetch if cache is older than half the configured interval
        long maxAgeMs = Math.max(15, WeatherPrefs.getIntervalMinutes(context) / 2) * 60_000L;
        if (System.currentTimeMillis() - WeatherPrefs.getLastUpdatedMs(context) > maxAgeMs) {
            WeatherJobService.triggerNow(context, false);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (WidgetRenderer.ACTION_REFRESH.equals(intent.getAction())) {
            WeatherJobService.triggerNow(context, false);
        }
    }

    @Override
    public void onDisabled(Context context) {
        // Cancel periodic job only if no widgets of either provider remain
        AppWidgetManager mgr = AppWidgetManager.getInstance(context);
        int c1 = mgr.getAppWidgetIds(new android.content.ComponentName(context, OvercastWidgetProvider.class)).length;
        int c2 = mgr.getAppWidgetIds(new android.content.ComponentName(context, OvercastForecastWidgetProvider.class)).length;
        if (c1 == 0 && c2 == 0) {
            WeatherJobService.cancelAll(context);
        }
    }
}
