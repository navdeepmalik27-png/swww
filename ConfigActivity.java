package com.overcast.mini;

import android.Manifest;
import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.TextView;

/**
 * Minimal Material 3 Expressive widget configuration screen.
 * Uses Open-Meteo Geocoding API (no API key) or one-shot cached coarse location.
 */
public final class ConfigActivity extends Activity {
    private static final int REQ_COARSE = 42;
    private int widgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
    private EditText etCity;
    private Switch swFahrenheit;
    private RadioGroup rgInterval;
    private TextView tvStatus;
    private Button btnSave;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setResult(RESULT_CANCELED);
        setContentView(R.layout.activity_config);

        Intent intent = getIntent();
        if (intent != null && intent.getExtras() != null) {
            widgetId = intent.getExtras().getInt(
                    AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        etCity = findViewById(R.id.et_city);
        swFahrenheit = findViewById(R.id.sw_fahrenheit);
        rgInterval = findViewById(R.id.rg_interval);
        tvStatus = findViewById(R.id.tv_status);
        btnSave = findViewById(R.id.btn_save);
        Button btnGps = findViewById(R.id.btn_last_loc);

        etCity.setText(WeatherPrefs.getCity(this));
        swFahrenheit.setChecked(WeatherPrefs.isFahrenheit(this));

        int iv = WeatherPrefs.getIntervalMinutes(this);
        if (iv <= 60) rgInterval.check(R.id.rb_1h);
        else if (iv <= 120) rgInterval.check(R.id.rb_2h);
        else if (iv <= 180) rgInterval.check(R.id.rb_3h);
        else rgInterval.check(R.id.rb_6h);

        btnGps.setOnClickListener(v -> useLastKnownCoarseLocation());
        btnSave.setOnClickListener(v -> saveAndFinish());
    }

    private void useLastKnownCoarseLocation() {
        if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{ Manifest.permission.ACCESS_COARSE_LOCATION }, REQ_COARSE);
            return;
        }
        LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
        Location loc = null;
        if (lm != null) {
            loc = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            if (loc == null) loc = lm.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
        }
        if (loc == null) {
            tvStatus.setText("No cached location yet. Enter a city name instead.");
            return;
        }
        WeatherPrefs.saveLocation(this, "My Location", loc.getLatitude(), loc.getLongitude());
        etCity.setText("My Location");
        applySettingsAndFetch();
    }

    @Override
    public void onRequestPermissionsResult(int code, String[] perms, int[] res) {
        if (code == REQ_COARSE && res.length > 0 && res[0] == PackageManager.PERMISSION_GRANTED) {
            useLastKnownCoarseLocation();
        }
    }

    private void saveAndFinish() {
        String query = etCity.getText().toString().trim();
        if (query.isEmpty()) {
            tvStatus.setText("Enter a city name (e.g. Sonipat, London, Tokyo)");
            return;
        }
        if ("My Location".equals(query) && WeatherPrefs.hasLocation(this)) {
            applySettingsAndFetch();
            return;
        }
        btnSave.setEnabled(false);
        tvStatus.setText("Looking up city via Open-Meteo…");
        new Thread(() -> {
            OpenMeteoClient.GeoResult geo = OpenMeteoClient.searchCity(query);
            runOnUiThread(() -> {
                btnSave.setEnabled(true);
                if (geo == null) {
                    tvStatus.setText("City not found. Check spelling or network.");
                    return;
                }
                WeatherPrefs.saveLocation(this, geo.label, geo.lat, geo.lon);
                applySettingsAndFetch();
            });
        }).start();
    }

    private void applySettingsAndFetch() {
        int iv = 120;
        int checked = rgInterval.getCheckedRadioButtonId();
        if (checked == R.id.rb_1h) iv = 60;
        else if (checked == R.id.rb_2h) iv = 120;
        else if (checked == R.id.rb_3h) iv = 180;
        else if (checked == R.id.rb_6h) iv = 360;

        WeatherPrefs.saveSettings(this, swFahrenheit.isChecked(), iv);
        WeatherJobService.schedulePeriodic(this);
        WeatherJobService.triggerNow(this, true);
        WidgetRenderer.updateAll(this);

        if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            Intent res = new Intent();
            res.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
            setResult(RESULT_OK, res);
        }
        finish();
    }
}
