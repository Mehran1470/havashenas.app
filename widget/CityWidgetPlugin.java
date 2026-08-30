package com.havashenas.hormozgan;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

@CapacitorPlugin(name = "CityWidget")
public class CityWidgetPlugin extends Plugin {

    @PluginMethod
    public void setCity(PluginCall call) {
        @PluginMethod
    public void setTideInfo(PluginCall call) {
        String tideText = call.getString("tideText");
        String windText = call.getString("windText");
        String rainText = call.getString("rainText");
        Context context = getContext();
        SharedPreferences prefs = context.getSharedPreferences("havashenas_widget", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        if (tideText != null) editor.putString("tide_text", tideText);
        if (windText != null) editor.putString("wind_text", windText);
        if (rainText != null) editor.putString("rain_text", rainText);
        editor.apply();

        JSObject ret = new JSObject();
        ret.put("success", true);
        call.resolve(ret);
    }
        String name = call.getString("name");
        Double lat = call.getDouble("lat");
        Double lon = call.getDouble("lon");
        if (name == null || lat == null || lon == null) {
            call.reject("Missing name/lat/lon");
            return;
        }
        Context context = getContext();
        SharedPreferences prefs = context.getSharedPreferences("havashenas_widget", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("city_name", name);
        editor.putString("city_lat", String.valueOf(lat));
        editor.putString("city_lon", String.valueOf(lon));
        editor.apply();

        Intent intent = new Intent(context, WeatherWidgetProvider.class);
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        AppWidgetManager widgetManager = AppWidgetManager.getInstance(context);
        int[] ids = widgetManager.getAppWidgetIds(new ComponentName(context, WeatherWidgetProvider.class));
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
        context.sendBroadcast(intent);

        JSObject ret = new JSObject();
        ret.put("success", true);
        call.resolve(ret);
    }
}
