package com.havashenas.hormozgan;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/** Shared weather download and mapping helpers for the card and detail widgets. */
final class WeatherFetcher {

    static final int CAT_SUN = 0;
    static final int CAT_BLUE = 1;
    static final int CAT_RAIN = 2;
    static final int CAT_NIGHT = 3;

    static final class Result {
        boolean ok;
        int temp;
        int feels;
        int humidity;
        int wind;
        int code;
        boolean isDay = true;
    }

    private WeatherFetcher() {}

    static Result fetch(double lat, double lon) {
        Result r = new Result();
        try {
            URL url = new URL("https://api.open-meteo.com/v1/forecast?latitude=" + lat + "&longitude=" + lon
                    + "&current=temperature_2m,apparent_temperature,relative_humidity_2m,weather_code,wind_speed_10m,is_day&timezone=auto");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            reader.close();
            JSONObject cur = new JSONObject(sb.toString()).getJSONObject("current");
            r.temp = (int) Math.round(cur.getDouble("temperature_2m"));
            r.feels = (int) Math.round(cur.getDouble("apparent_temperature"));
            r.humidity = (int) Math.round(cur.getDouble("relative_humidity_2m"));
            r.wind = (int) Math.round(cur.getDouble("wind_speed_10m"));
            r.code = cur.getInt("weather_code");
            r.isDay = cur.getInt("is_day") == 1;
            r.ok = true;
        } catch (Exception e) {
            r.ok = false;
        }
        return r;
    }

    static String describe(int code, double wind, boolean isDay) {
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

    /** Card colour family: yellow sun, blue cloudy, grey rain, navy night. */
    static int category(int code, boolean isDay) {
        if (code >= 45) return CAT_RAIN;
        if (!isDay) return CAT_NIGHT;
        if (code == 0) return CAT_SUN;
        return CAT_BLUE;
    }
}
