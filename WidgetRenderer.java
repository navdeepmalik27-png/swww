package com.overcast.mini;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.RemoteViews;

/**
 * Renders both Overcast-styled Material 3 Expressive widgets purely from cached data.
 */
public final class WidgetRenderer {
    public static final String ACTION_REFRESH = "com.overcast.mini.ACTION_REFRESH";

    private static final int[] HOUR_TIME_IDS = { R.id.h0_time, R.id.h1_time, R.id.h2_time, R.id.h3_time };
    private static final int[] HOUR_ICON_IDS = { R.id.h0_icon, R.id.h1_icon, R.id.h2_icon, R.id.h3_icon };
    private static final int[] HOUR_TEMP_IDS = { R.id.h0_temp, R.id.h1_temp, R.id.h2_temp, R.id.h3_temp };

    private WidgetRenderer() {}

    public static void updateAll(Context context) {
        AppWidgetManager mgr = AppWidgetManager.getInstance(context);
        int[] compactIds = mgr.getAppWidgetIds(new ComponentName(context, OvercastWidgetProvider.class));
        for (int id : compactIds) {
            mgr.updateAppWidget(id, buildViews(context, false));
        }
        int[] forecastIds = mgr.getAppWidgetIds(new ComponentName(context, OvercastForecastWidgetProvider.class));
        for (int id : forecastIds) {
            mgr.updateAppWidget(id, buildViews(context, true));
        }
    }

    public static RemoteViews buildViews(Context context, boolean withHourly) {
        int layout = withHourly ? R.layout.widget_overcast_forecast : R.layout.widget_overcast_compact;
        RemoteViews rv = new RemoteViews(context.getPackageName(), layout);

        // Tap temperature/card to refresh; tap city pill to open config
        Intent refreshIntent = new Intent(context,
                withHourly ? OvercastForecastWidgetProvider.class : OvercastWidgetProvider.class);
        refreshIntent.setAction(ACTION_REFRESH);
        PendingIntent refreshPi = PendingIntent.getBroadcast(
                context, withHourly ? 2 : 1, refreshIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        rv.setOnClickPendingIntent(R.id.widget_root, refreshPi);

        Intent cfgIntent = new Intent(context, ConfigActivity.class);
        cfgIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent cfgPi = PendingIntent.getActivity(
                context, 3, cfgIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        rv.setOnClickPendingIntent(R.id.city_pill, cfgPi);

        String city = WeatherPrefs.getCity(context);
        WeatherData d = WeatherData.parse(WeatherPrefs.getCachedJson(context));

        if (d == null) {
            rv.setTextViewText(R.id.tv_city, city.isEmpty() ? "Tap to set city" : city);
            rv.setTextViewText(R.id.tv_temp, "--°");
            rv.setTextViewText(R.id.tv_condition, "Overcast Mini");
            rv.setTextViewText(R.id.tv_hl, "H:--°  L:--°");
            rv.setViewVisibility(R.id.tv_rain_pill, View.GONE);
            rv.setImageViewResource(R.id.iv_icon, R.drawable.ic_w_cloudy);
            if (withHourly) {
                rv.setViewVisibility(R.id.hourly_pill, View.GONE);
            }
            return rv;
        }

        rv.setTextViewText(R.id.tv_city, city.isEmpty() ? "Weather" : city);
        rv.setTextViewText(R.id.tv_temp, d.temp + "°");
        rv.setTextViewText(R.id.tv_condition, WeatherData.conditionLabel(d.wmoCode));
        rv.setTextViewText(R.id.tv_hl, "H:" + d.high + "°  L:" + d.low + "°");
        rv.setImageViewResource(R.id.iv_icon, WeatherData.iconRes(d.wmoCode, d.isDay));

        if (d.precipProb > 0) {
            rv.setViewVisibility(R.id.tv_rain_pill, View.VISIBLE);
            rv.setTextViewText(R.id.tv_rain_pill, d.precipProb + "%");
        } else {
            rv.setViewVisibility(R.id.tv_rain_pill, View.GONE);
        }

        if (withHourly) {
            if (d.hourCount > 0) {
                rv.setViewVisibility(R.id.hourly_pill, View.VISIBLE);
                for (int i = 0; i < 4; i++) {
                    if (i < d.hourCount) {
                        rv.setTextViewText(HOUR_TIME_IDS[i], d.hourLabels[i]);
                        rv.setImageViewResource(HOUR_ICON_IDS[i], WeatherData.iconRes(d.hourCodes[i], d.isDay));
                        rv.setTextViewText(HOUR_TEMP_IDS[i], d.hourTemps[i] + "°");
                    }
                }
            } else {
                rv.setViewVisibility(R.id.hourly_pill, View.GONE);
            }
        }
        return rv;
    }
}
