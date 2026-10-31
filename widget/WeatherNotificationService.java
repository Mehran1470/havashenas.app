package com.havashenas.hormozgan;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.widget.RemoteViews;

import androidx.core.app.NotificationCompat;
import androidx.core.app.ServiceCompat;

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

        Notification first = buildLegacyNotification("بندرعباس", "—", "—", "☀️", theme, "", "", "");
        if (Build.VERSION.SDK_INT >= 34) {
            ServiceCompat.startForeground(this, NOTIF_ID, first, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        } else {
            startForeground(NOTIF_ID, first);
        }

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
                handler.postDelayed(this, 5 * 60 * 1000);
            }
        };
        handler.post(updateTask);
    }

    private String httpGet(String address) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(address).openConnection();
        try {
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            reader.close();
            return sb.toString();
        } finally {
            conn.disconnect();
        }
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
            String tideText = prefs.getString("tide_text", "");
            String windText = prefs.getString("wind_text", "");
            String humidityText = prefs.getString("humidity_text", "");
            try {
                String body = httpGet("https://api.open-meteo.com/v1/forecast?latitude=" + lat + "&longitude=" + lon
                        + "&current=temperature_2m,weather_code,wind_speed_10m,is_day");
                JSONObject json = new JSONObject(body);
                JSONObject current = json.getJSONObject("current");
                double temp = current.getDouble("temperature_2m");
                double wind = current.getDouble("wind_speed_10m");
                int code = current.getInt("weather_code");
                boolean isDay = current.getInt("is_day") == 1;
                String desc = describeCode(code, wind, isDay);
                String emoji = emojiForCode(code, wind, isDay);
                String tempText = Math.round(temp) + "°";

                Notification notif = null;
                // Android 16+: Live Update (Now Bar on One UI 8). Everything else keeps the old notification.
                if (Build.VERSION.SDK_INT >= 36 && LiveUpdateNotification.canPromote(this)) {
                    try {
                        notif = LiveUpdateNotification.build(this, CHANNEL_ID, openAppIntent(),
                                cityName, tempText, desc, iconForCode(code, wind, isDay), windText, circleColor(code, wind, isDay));
                    } catch (Throwable t) {
                        notif = null;
                    }
                }
                if (notif == null) {
                    notif = buildLegacyNotification(cityName, tempText, desc, emoji, theme, tideText, windText, humidityText);
                }
                NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                manager.notify(NOTIF_ID, notif);
            } catch (Exception e) {
            }
        }).start();
    }

    private PendingIntent openAppIntent() {
        Intent intent = new Intent(this, MainActivity.class);
        return PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private String describeCode(int code, double wind, boolean isDay) {
        if (code == 95) return "طوفانی";
        if (code >= 80 && code <= 82) return "رگبار باران";
        if (code >= 61 && code <= 65) return "بارانی";
        if (code >= 51 && code <= 55) return "نم‌نم باران";
        if (wind >= 30) return "بادی";
        if (code == 45 || code == 48) return "مه‌آلود";
        if (code >= 71 && code <= 75) return "برفی";
        if (code == 3) return "ابری";
        if (code == 1 || code == 2) return "نیمه ابری";
        if (code == 0) return isDay ? "آسمان صاف" : "شب صاف";
        return "نامشخص";
    }

    private String emojiForCode(int code, double wind, boolean isDay) {
        if (code == 95) return "⛈️";
        if (code >= 80 && code <= 82) return "🌧️";
        if (code >= 61 && code <= 65) return "🌧️";
        if (code >= 51 && code <= 55) return "🌦️";
        if (wind >= 30) return "💨";
        if (code == 45 || code == 48) return "🌫️";
        if (code >= 71 && code <= 75) return "❄️";
        if (code == 3) return "☁️";
        if (code == 1 || code == 2) return isDay ? "⛅" : "🌙";
        if (code == 0) return isDay ? "☀️" : "🌙";
        return "🌤️";
    }

    /** Status-bar / Now Bar chip icon for the weather. */
    private int iconForCode(int code, double wind, boolean isDay) {
        if (code == 95) return R.drawable.ic_storm;
        if (code >= 80 && code <= 82) return R.drawable.ic_rain;
        if (code >= 61 && code <= 65) return R.drawable.ic_rain;
        if (code >= 51 && code <= 55) return R.drawable.ic_rain;
        if (wind >= 30) return R.drawable.ic_wind;
        if (code == 45 || code == 48) return R.drawable.ic_fog;
        if (code >= 71 && code <= 75) return R.drawable.ic_snow;
        if (code == 3) return R.drawable.ic_cloud;
        if (code == 1 || code == 2) return isDay ? R.drawable.ic_partly_day : R.drawable.ic_partly_night;
        if (code == 0) return isDay ? R.drawable.ic_sun : R.drawable.ic_moon_star;
        return R.drawable.ic_cloud;
    }

    /** Circle color: changes with day/night and weather. */
    private int circleColor(int code, double wind, boolean isDay) {
        if (code == 95) return 0xFF4A3F78;
        if (code >= 51 && code <= 65 || code >= 80 && code <= 82) return 0xFF2F5D8A;
        if (code >= 71 && code <= 75) return 0xFF6C8EAD;
        if (wind >= 30) return 0xFF3E7C8C;
        if (code == 45 || code == 48 || code == 3) return 0xFF5B6B7A;
        if (code == 1 || code == 2) return isDay ? 0xFF4A90D9 : 0xFF3949AB;
        return isDay ? 0xFF1E88E5 : 0xFF283593;
    }

    /** The original notification (custom colored layout). Used on every phone below Android 16. */
    private Notification buildLegacyNotification(String city, String temp, String desc, String emoji, String theme, String tideText, String windText, String humidityText) {
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

        StringBuilder bigText = new StringBuilder();
        bigText.append(emoji).append(" ").append(desc).append(" • ").append(temp).append("\n");
        if (tideText != null && !tideText.isEmpty()) bigText.append("🌊 ").append(tideText).append("\n");
        if (windText != null && !windText.isEmpty()) bigText.append("💨 ").append(windText).append("\n");
        if (humidityText != null && !humidityText.isEmpty()) bigText.append("💧 ").append(humidityText);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setCustomContentView(views)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(bigText.toString().trim()))
                .setOngoing(true)
                .setContentIntent(openAppIntent())
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && "com.havashenas.hormozgan.REFRESH".equals(intent.getAction()) && updateTask != null) {
            fetchAndUpdate();
        }
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
