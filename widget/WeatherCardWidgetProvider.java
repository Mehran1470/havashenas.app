package com.havashenas.hormozgan;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.widget.RemoteViews;

/** Soft card widget: colour follows the weather (yellow, blue, grey, navy). */
public class WeatherCardWidgetProvider extends AppWidgetProvider {

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

        new AsyncTask<Void, Void, WeatherFetcher.Result>() {
            @Override
            protected WeatherFetcher.Result doInBackground(Void... voids) {
                return WeatherFetcher.fetch(lat, lon);
            }

            @Override
            protected void onPostExecute(WeatherFetcher.Result r) {
                RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.card_widget_layout);
                int cat = r.ok ? WeatherFetcher.category(r.code, r.isDay) : WeatherFetcher.CAT_BLUE;

                int bg;
                int textColor;
                int cityColor;
                switch (cat) {
                    case WeatherFetcher.CAT_SUN:
                        bg = R.drawable.card_bg_sun; textColor = 0xFFFFFFFF; cityColor = 0xE6FFFFFF; break;
                    case WeatherFetcher.CAT_RAIN:
                        bg = R.drawable.card_bg_gray; textColor = 0xFF2F63B2; cityColor = 0xFF5A6272; break;
                    case WeatherFetcher.CAT_NIGHT:
                        bg = R.drawable.card_bg_night; textColor = 0xFFFFFFFF; cityColor = 0xE6FFFFFF; break;
                    default:
                        bg = R.drawable.card_bg_blue; textColor = 0xFFFFFFFF; cityColor = 0xE6FFFFFF; break;
                }

                int icon;
                if (!r.ok) {
                    icon = R.drawable.ic_card_cloud;
                } else if (cat == WeatherFetcher.CAT_SUN) {
                    icon = R.drawable.ic_card_sun;
                } else if (cat == WeatherFetcher.CAT_NIGHT) {
                    icon = R.drawable.ic_moon_star;
                } else if (cat == WeatherFetcher.CAT_RAIN) {
                    icon = r.code >= 51 ? R.drawable.ic_card_rain : R.drawable.ic_card_cloud;
                } else {
                    icon = r.code == 3 ? R.drawable.ic_card_cloud : R.drawable.ic_card_partly;
                }

                views.setInt(R.id.card_root, "setBackgroundResource", bg);
                views.setTextViewText(R.id.card_city, cityName);
                views.setTextViewText(R.id.card_temp, r.ok ? r.temp + "°" : "—");
                views.setTextColor(R.id.card_city, cityColor);
                views.setTextColor(R.id.card_temp, textColor);
                views.setImageViewResource(R.id.card_icon, icon);

                Intent intent = new Intent(context, MainActivity.class);
                PendingIntent pi = PendingIntent.getActivity(context, appWidgetId, intent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                views.setOnClickPendingIntent(R.id.card_root, pi);

                manager.updateAppWidget(appWidgetId, views);
            }
        }.execute();
    }
}
