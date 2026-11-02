package com.havashenas.hormozgan;

import android.Manifest;
import android.view.View;
import android.content.Intent;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.provider.Settings;
import android.os.Build;
import android.os.Bundle;

import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.content.ContextCompat;

import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        registerPlugin(CityWidgetPlugin.class);
        super.onCreate(savedInstanceState);

        // targetSdk 36 forces edge-to-edge on Android 15+. Keep the page below the status bar
        // and above the navigation bar / keyboard, like before.
        View content = findViewById(android.R.id.content);
        if (content != null) {
            ViewCompat.setOnApplyWindowInsetsListener(content, (v, insets) -> {
                Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
                Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
                v.setPadding(bars.left, bars.top, bars.right, Math.max(bars.bottom, ime.bottom));
                return WindowInsetsCompat.CONSUMED;
            });
        }
        getWindow().getDecorView().setBackgroundColor(0xFF0A1628);
        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView()).setAppearanceLightStatusBars(false);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1001);
            }
        }

        Intent serviceIntent = new Intent(this, WeatherNotificationService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        askForLiveUpdatesOnce();
    }

    /** Android 16+: once, open the page where the user can switch on Live updates (Now Bar) for this app. */
    private void askForLiveUpdatesOnce() {
        if (Build.VERSION.SDK_INT < 36) return;
        try {
            SharedPreferences prefs = getSharedPreferences("havashenas_widget", Context.MODE_PRIVATE);
            if (prefs.getBoolean("asked_live_updates", false)) return;
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm == null || nm.canPostPromotedNotifications()) return;
            prefs.edit().putBoolean("asked_live_updates", true).apply();
            String action = Settings.ACTION_APP_NOTIFICATION_SETTINGS;
            try {
                action = (String) Settings.class.getField("ACTION_MANAGE_APP_PROMOTED_NOTIFICATIONS").get(null);
            } catch (Throwable ignored) {
            }
            Intent i = new Intent(action).putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
            startActivity(i);
        } catch (Throwable ignored) {
        }
    }
}
