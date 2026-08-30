package com.havashenas.hormozgan;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.widget.RemoteViews;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class WeatherWidgetProvider extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId);
        }
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
        if (code == 95) return "\u26C8\uFE0F";
        if (code >= 80 && code <= 82) return "\uD83C\uDF27\uFE0F";
        if (code >= 61 && code <= 65) return "\uD83C\uDF27\uFE0F";
        if (code >= 51 && code <= 55) return "\uD83C\uDF26\uFE0F";
        if (wind >= 30) return "\uD83D\uDCA8";
        if (code == 45 || code == 48) return "\uD83C\uDF2B\uFE0F";
        if (code >= 71 && code <= 75) return "\u2744\uFE0F";
        if (code == 3) return "\u2601\uFE0F";
        if (code == 1 || code == 2) return isDay ? "\u26C5" : "\uD83C\uDF19";
        if (code == 0) return isDay ? "\u2600\uFE0F" : "\uD83C\uDF19";
        return "\uD83C\uDF24\uFE0F";
    }

    static int backgroundForTheme(String theme) {
        if ("dark".equals(theme)) return R.drawable.widget_background_dark;
        if ("sunset".equals(theme)) return R.drawable.widget_background_sunset;
        return R.drawable.widget_background;
    }

    static int textColorForTheme(String theme) {
        if ("dark".equals(theme)) return 0xFFFFFFFF;
        return 0xFF1E2A4A;
    }

    private void updateWidget(final Context context, final AppWidgetManager appWidgetManager, final int appWidgetId) {
        final SharedPreferences prefs = context.getSharedPreferences("havashenas_widget", Context.MODE_PRIVATE);
        final String cityName = prefs.getString("city_name", "بندرعباس");
        final double lat = Double.parseDouble(prefs.getString("city_lat", "27.1832"));
        final double lon = Double.parseDouble(prefs.getString("city_lon", "56.2666"));
        final String theme = prefs.getString("theme", "blue");
        final boolean showHumidity = prefs.getBoolean("show_humidity", true);
        final boolean showWind = prefs.getBoolean("show_wind", true);
        final boolean showTide = prefs.getBoolean("show_tide", true);
        final String tideText = prefs.getString("tide_text", "");

        new AsyncTask<Void, Void, String[]>() {
            @Override
            protected String[] doInBackground(Void... voids) {
                try {
                    URL url = new URL("https://api.open-meteo.com/v1/forecast?latitude=" + lat + "&longitude=" + lon + "&current=temperature_2m,relative_humidity_2m,weather_code,wind_speed_10m,is_day");
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
                    double humidity = current.getDouble("relative_humidity_2m");
                    double wind = current.getDouble("wind_speed_10m");
                    int code = current.getInt("weather_code");
                    boolean isDay = current.getInt("is_day") == 1;
                    return new String[]{
                        Math.round(temp) + "°",
                        cityName,
                        describeCode(code, wind, isDay),
                        emojiForCode(code, wind, isDay),
                        Math.round(humidity) + "%",
                        Math.round(wind) + " km/h"
                    };
                } catch (Exception e) {
                    return new String[]{"—", cityName, "—", "\uD83C\uDF24\uFE0F", "—", "—"};
                }
            }

            @Override
            protected void onPostExecute(String[] result) {
                RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.weather_widget_layout);
                views.setTextViewText(R.id.widget_temp, result[0]);
                views.setTextViewText(R.id.widget_city, result[1]);
                views.setTextViewText(R.id.widget_desc, result[2]);
                views.setTextViewText(R.id.widget_icon, result[3]);
                views.setTextViewText(R.id.widget_humidity, "\uD83D\uDCA7 " + result[4]);
                views.setTextViewText(R.id.widget_wind, "\uD83D\uDCA8 " + result[5]);
                views.setTextViewText(R.id.widget_tide, "\uD83C\uDF0A " + (tideText == null || tideText.isEmpty() ? "—" : tideText));

                views.setViewVisibility(R.id.widget_stats_row, (showHumidity || showWind) ? android.view.View.VISIBLE : android.view.View.GONE);
                views.setViewVisibility(R.id.widget_humidity, showHumidity ? android.view.View.VISIBLE : android.view.View.GONE);
                views.setViewVisibility(R.id.widget_wind, showWind ? android.view.View.VISIBLE : android.view.View.GONE);
                views.setViewVisibility(R.id.widget_tide, showTide ? android.view.View.VISIBLE : android.view.View.GONE);

                views.setInt(R.id.widget_root, "setBackgroundResource", backgroundForTheme(theme));
                int textColor = textColorForTheme(theme);
                views.setTextColor(R.id.widget_city, textColor);
                views.setTextColor(R.id.widget_temp, textColor);
                views.setTextColor(R.id.widget_desc, textColor);
                views.setTextColor(R.id.widget_humidity, textColor);
                views.setTextColor(R.id.widget_wind, textColor);
                views.setTextColor(R.id.widget_tide, textColor);

                Intent intent = new Intent(context, MainActivity.class);
                PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                views.setOnClickPendingIntent(R.id.widget_root, pendingIntent);

                appWidgetManager.updateAppWidget(appWidgetId, views);
            }
        }.execute();
    }
}
