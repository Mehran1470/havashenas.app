package com.havashenas.hormozgan;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
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

    private void updateWidget(final Context context, final AppWidgetManager appWidgetManager, final int appWidgetId) {
        new AsyncTask<Void, Void, String[]>() {
            @Override
            protected String[] doInBackground(Void... voids) {
                try {
                    URL url = new URL("https://api.open-meteo.com/v1/forecast?latitude=27.1865&longitude=56.2808&current_weather=true");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setConnectTimeout(10000);
                    conn.setReadTimeout(10000);
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) sb.append(line);
                    reader.close();
                    JSONObject json = new JSONObject(sb.toString());
                    JSONObject current = json.getJSONObject("current_weather");
                    double temp = current.getDouble("temperature");
                    return new String[]{Math.round(temp) + "°", "بندرعباس"};
                } catch (Exception e) {
                    return new String[]{"—", "بندرعباس"};
                }
            }

            @Override
            protected void onPostExecute(String[] result) {
                RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.weather_widget_layout);
                views.setTextViewText(R.id.widget_temp, result[0]);
                views.setTextViewText(R.id.widget_city, result[1]);

                Intent intent = new Intent(context, MainActivity.class);
                PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                views.setOnClickPendingIntent(R.id.widget_root, pendingIntent);

                appWidgetManager.updateAppWidget(appWidgetId, views);
            }
        }.execute();
    }
}
