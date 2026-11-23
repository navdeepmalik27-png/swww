# Overcast Mini — Ultra-Small, Battery-First Weather Widget

A zero-dependency Android weather widget styled after the open-source **[Overcast](https://github.com/sameerasw/overcast)** Material 3 Expressive design language and powered by the free **[Open-Meteo API](https://open-meteo.com/en/docs)** (no API key required).

## Why It's ~18–30 KB (vs. ~6.75 MB full app)
- **Zero external libraries**: Pure Java 11 + Android Framework SDK only (no Kotlin stdlib, no Jetpack Compose, no AndroidX, no Material library, no OkHttp, no Gson, no WorkManager).
- **Native Material 3 Expressive Dynamic Color**: Uses Android 12+ system dynamic color tokens (`@android:color/system_accent1_*`, `@android:color/system_neutral1_*`) directly in XML (`values-v31` / `values-night-v31`) with matching Overcast dark/light tonal fallbacks for Android 8–11.
- **Minimal Vector Icons**: 8 geometric `VectorDrawable` XMLs (~250 bytes each) covering all WMO weather codes.

## Battery-Saving Architecture
- **No background GPS / location polling**: Saves coordinates once via Open-Meteo Geocoding or an optional one-shot cached coarse location (`NETWORK_PROVIDER` / `PASSIVE_PROVIDER`).
- **Doze-friendly `JobScheduler`**: Batches updates with OS network windows (`NETWORK_TYPE_ANY`, `setRequiresBatteryNotLow(true)`, configurable 1h / 2h / 3h / 6h interval, `updatePeriodMillis="0"`).
- **Tiny gzipped payload**: Requests only the exact `current`, `daily` (1 day), and `hourly` (next 4 hours) fields (~700 bytes compressed).
- **Zero wake locks, zero foreground services, 30s tap-to-refresh debounce**.

## Widgets Included
1. **Overcast · Compact (2×2+)**: Expressive rounded card (`28dp` radius), location pill, rain probability pill, oversized display temperature (`50sp` bold), crisp vector weather icon, condition + H/L temperatures.
2. **Overcast · Forecast Pill (4×2+)**: Adds a nested Material 3 Expressive tonal inner card (`20dp` radius) showing the next 4 hourly forecast slots.

## How to Build the APK

### Option 1: Fastest / Smallest (~18–25 KB, requires Android SDK + JDK 11+)
```bash
chmod +x build_min_apk.sh
./build_min_apk.sh
# Output: build_min/overcast-mini-widget.apk
```

### Option 2: Zero Local Setup (GitHub Actions)
Push this folder to a GitHub repository -> open the **Actions** tab -> download `overcast-mini-widget-apk` (~45 seconds).

### Option 3: Android Studio / Gradle
Open this folder in Android Studio and run **Build -> Generate Signed App Bundle / APK** (or `./gradlew assembleRelease`).
