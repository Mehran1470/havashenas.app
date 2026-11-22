package com.havashenas.hormozgan;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.view.View;
import android.widget.RemoteViews;

/** Dark detail widget: city, temperature, feels-like, wind, humidity and tide. */
public class WeatherDetailWidgetProvider extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int id : appWidgetIds) {
            updateWidget(context, appWidgetManager, id);
        }
    }

    private void updateWidget(final Context context, final AppWidgetManager manager, final int appWidgetId) {
        SharedPreferences prefs = context.getSharedPreferences("havashenas_widget", Context.MODE_PRIVATE);
        final String cityName = prefs.getString("city_name", "بندرعباس");
        final double lat = Double.parseDouble(prefs.getString("city_lat", "27.1832"));
        final double lon = Double.parseDouble(prefs.getString("city_lon", "56.2666"));
        final boolean showTide = prefs.getBoolean("show_tide", true);
        final String tideText = prefs.getString("tide_text", "");

        new AsyncTask<Void, Void, WeatherFetcher.Result>() {
            @Override
            protected WeatherFetcher.Result doInBackground(Void... voids) {
                return WeatherFetcher.fetch(lat, lon);
            }

            @Override
            protected void onPostExecute(WeatherFetcher.Result r) {
                RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.detail_widget_layout);

                views.setTextViewText(R.id.detail_city, cityName);
                views.setTextViewText(R.id.detail_desc, r.ok ? WeatherFetcher.describe(r.code, r.wind, r.isDay) : "—");
                views.setTextViewText(R.id.detail_temp, r.ok ? r.temp + "°" : "—");
                views.setTextViewText(R.id.detail_feels, r.ok ? "احساس \u202A" + r.feels + "°\u202C" : "");
                views.setTextViewText(R.id.detail_wind_value, r.ok ? r.wind + " km/h" : "—");
                views.setTextViewText(R.id.detail_humidity_value, r.ok ? r.humidity + "%" : "—");
                views.setTextViewText(R.id.detail_tide_value, (tideText == null || tideText.isEmpty()) ? "—" : tideText);
                views.setViewVisibility(R.id.detail_tide_box, showTide ? View.VISIBLE : View.GONE);

                int icon;
                if (!r.ok) icon = R.drawable.ic_card_cloud;
                else if (r.code >= 51) icon = R.drawable.ic_detail_rain;
                else if (!r.isDay) icon = R.drawable.ic_card_moon;
                else if (r.code == 0) icon = R.drawable.ic_card_sun;
                else if (r.code == 3 || r.code == 45 || r.code == 48) icon = R.drawable.ic_card_cloud;
                else icon = R.drawable.ic_card_partly;
                views.setImageViewResource(R.id.detail_icon, icon);

                Intent intent = new Intent(context, MainActivity.class);
                PendingIntent pi = PendingIntent.getActivity(context, appWidgetId + 1000, intent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                views.setOnClickPendingIntent(R.id.detail_root, pi);

                manager.updateAppWidget(appWidgetId, views);
            }
        }.execute();
    }
}
