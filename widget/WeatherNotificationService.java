package com.havashenas.hormozgan;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.widget.RemoteViews;

import androidx.core.app.NotificationCompat;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class WeatherNotificationService extends Service {

    private static final String CHANNEL_ID = "havashenas_weather_channel";
    private static final int NOTIF_ID = 501;
    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable updateTask;

    @Override
    public void onCreate() {
        super.onCreate();
        createChannel();
        SharedPreferences prefs = getSharedPreferences("havashenas_widget", Context.MODE_PRIVATE);
        String theme = prefs.getString("theme", "blue");
        boolean notifEnabled = prefs.getBoolean("notif_enabled", true);

        startForeground(NOTIF_ID, buildNotification("بندرعباس", "—", "—", "☀️", theme));

        if (!notifEnabled) {
            stopForeground(true);
            stopSelf();
            return;
        }
        scheduleUpdates();
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "وضعیت هوا", NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("نمایش دائمی دمای هوا");
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    private void scheduleUpdates() {
        updateTask = new Runnable() {
            @Override
            public void run() {
                fetchAndUpdate();
                handler.postDelayed(this, 30 * 60 * 1000);
            }
        };
        handler.post(updateTask);
    }

    private void fetchAndUpdate() {
        new Thread(() -> {
            SharedPreferences prefs = getSharedPreferences("havashenas_widget", Context.MODE_PRIVATE);
            boolean notifEnabled = prefs.getBoolean("notif_enabled", true);
            if (!notifEnabled) {
                NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                manager.cancel(NOTIF_ID);
                stopSelf();
                return;
            }
            String cityName = prefs.getString("city_name", "بندرعباس");
            double lat = Double.parseDouble(prefs.getString("city_lat", "27.1832"));
            double lon = Double.parseDouble(prefs.getString("city_lon", "56.2666"));
            String theme = prefs.getString("theme", "blue");
            try {
                URL url = new URL("https://api.open-meteo.com/v1/forecast?latitude=" + lat + "&longitude=" + lon + "&current=temperature_2m,weather_code");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();
                JSONObject json = new JSONObject(sb.toString());
                JSONObject current = json.getJSONObject("current");
                double temp = current.getDouble("temperature_2m");
                int code = current.getInt("weather_code");
                String desc = describeCode(code);
                String emoji = emojiForCode(code);
                Notification notif = buildNotification(cityName, Math.round(temp) + "°", desc, emoji, theme);
                NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                manager.notify(NOTIF_ID, notif);
            } catch (Exception e) {
            }
        }).start();
    }

    private String describeCode(int code) {
        if (code == 0) return "آسمان صاف";
        if (code == 1) return "کمی ابری";
        if (code == 2) return "نیمه ابری";
        if (code == 3) return "ابری";
        if (code == 45 || code == 48) return "مه‌آلود";
        if (code >= 51 && code <= 55) return "نم‌نم باران";
        if (code >= 61 && code <= 65) return "بارانی";
        if (code >= 71 && code <= 75) return "برفی";
        if (code >= 80 && code <= 82) return "رگبار باران";
        if (code == 95) return "رعد و برق";
        return "نامشخص";
    }

    private String emojiForCode(int code) {
        if (code == 0) return "\u2600\uFE0F";
        if (code == 1 || code == 2) return "\u26C5";
        if (code == 3) return "\u2601\uFE0F";
        if (code == 45 || code == 48) return "\uD83C\uDF2B\uFE0F";
        if (code >= 51 && code <= 65) return "\uD83C\uDF27\uFE0F";
        if (code >= 71 && code <= 75) return "\u2744\uFE0F";
        if (code >= 80 && code <= 82) return "\uD83C\uDF27\uFE0F";
        if (code == 95) return "\u26C8\uFE0F";
        return "\uD83C\uDF24\uFE0F";
    }

    private Notification buildNotification(String city, String temp, String desc, String emoji, String theme) {
        RemoteViews views = new RemoteViews(getPackageName(), R.layout.notification_layout);
        views.setTextViewText(R.id.notif_city, city);
        views.setTextViewText(R.id.notif_temp, temp);
        views.setTextViewText(R.id.notif_desc, desc);
        views.setTextViewText(R.id.notif_icon, emoji);

        int bgRes = WeatherWidgetProvider.backgroundForTheme(theme);
        int textColor = WeatherWidgetProvider.textColorForTheme(theme);
        views.setInt(R.id.notif_root, "setBackgroundResource", bgRes);
        views.setTextColor(R.id.notif_city, textColor);
        views.setTextColor(R.id.notif_desc, textColor);
        views.setTextColor(R.id.notif_temp, textColor);

        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setCustomContentView(views)
                .setOngoing(true)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(updateTask);
    }
}
