package com.havashenas.hormozgan;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;

/** Draws a glass bubble with a soft weather glyph inside (sun, moon, cloud, rain, storm, snow, fog, wind). */
final class WeatherBubble {

    private WeatherBubble() {}

    static Bitmap create(String kind, boolean isDay, int tint, int size) {
        Bitmap bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmp);
        float s = size;
        float cx = s / 2f, cy = s / 2f, r = s / 2f - s * 0.03f;

        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        int top = withAlpha(lighten(tint, 0.35f), 0xCC);
        int bottom = withAlpha(tint, 0xE6);
        p.setShader(new RadialGradient(cx - r * 0.35f, cy - r * 0.45f, r * 1.7f, top, bottom, Shader.TileMode.CLAMP));
        c.drawCircle(cx, cy, r, p);

        Paint rim = new Paint(Paint.ANTI_ALIAS_FLAG);
        rim.setStyle(Paint.Style.STROKE);
        rim.setStrokeWidth(s * 0.025f);
        rim.setShader(new LinearGradient(0, 0, s, s, 0xAAFFFFFF, 0x33A9C8FF, Shader.TileMode.CLAMP));
        c.drawCircle(cx, cy, r, rim);

        Paint glow = new Paint(Paint.ANTI_ALIAS_FLAG);
        glow.setShader(new RadialGradient(cx - r * 0.4f, cy - r * 0.55f, r * 0.55f, 0x44FFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP));
        c.drawCircle(cx, cy, r, glow);

        switch (kind) {
            case "sun": sun(c, cx, cy, s * 0.17f); break;
            case "moon": moon(c, cx, cy, s * 0.2f); break;
            case "partly":
                if (isDay) sunBall(c, cx - s * 0.1f, cy - s * 0.1f, s * 0.14f);
                else moon(c, cx - s * 0.09f, cy - s * 0.09f, s * 0.14f);
                cloud(c, cx + s * 0.05f, cy + s * 0.08f, s * 0.46f);
                break;
            case "rain":
                cloud(c, cx, cy - s * 0.06f, s * 0.46f);
                drop(c, cx - s * 0.1f, cy + s * 0.17f, s);
                drop(c, cx + s * 0.08f, cy + s * 0.17f, s);
                break;
            case "storm":
                cloud(c, cx, cy - s * 0.06f, s * 0.46f);
                bolt(c, cx, cy + s * 0.04f, s);
                break;
            case "snow":
                cloud(c, cx, cy - s * 0.06f, s * 0.46f);
                flake(c, cx, cy + s * 0.2f, s * 0.07f);
                break;
            case "fog":
                cloud(c, cx, cy - s * 0.08f, s * 0.44f);
                lines(c, cx, cy + s * 0.14f, s, 2);
                break;
            case "wind":
                lines(c, cx, cy - s * 0.08f, s, 3);
                break;
            default: cloud(c, cx, cy, s * 0.5f); break;
        }
        return bmp;
    }

    private static int withAlpha(int color, int a) { return (a << 24) | (color & 0xFFFFFF); }

    private static int lighten(int color, float f) {
        int r = (color >> 16) & 0xFF, g = (color >> 8) & 0xFF, b = color & 0xFF;
        r += (255 - r) * f; g += (255 - g) * f; b += (255 - b) * f;
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    private static void sunBall(Canvas c, float x, float y, float r) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setShader(new LinearGradient(x, y - r, x, y + r, 0xFFFFE27A, 0xFFFFB300, Shader.TileMode.CLAMP));
        c.drawCircle(x, y, r, p);
    }

    private static void sun(Canvas c, float x, float y, float r) {
        sunBall(c, x, y, r);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(0xFFFFD25A);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setStrokeWidth(r * 0.22f);
        for (int i = 0; i < 8; i++) {
            double a = i * Math.PI / 4;
            float cos = (float) Math.cos(a), sin = (float) Math.sin(a);
            c.drawLine(x + cos * r * 1.45f, y + sin * r * 1.45f, x + cos * r * 1.85f, y + sin * r * 1.85f, p);
        }
    }

    private static void moon(Canvas c, float x, float y, float r) {
        Path moon = new Path();
        moon.addCircle(x, y, r, Path.Direction.CW);
        Path cut = new Path();
        cut.addCircle(x + r * 0.55f, y - r * 0.35f, r * 0.85f, Path.Direction.CW);
        moon.op(cut, Path.Op.DIFFERENCE);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setShader(new LinearGradient(x - r, y - r, x + r, y + r, 0xFFFFFFFF, 0xFFC9D2FF, Shader.TileMode.CLAMP));
        c.drawPath(moon, p);
    }

    private static void cloud(Canvas c, float x, float y, float w) {
        float h = w * 0.55f;
        float left = x - w / 2f, top = y - h / 2f;
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setShader(new LinearGradient(left, top, left + w, top + h, 0xFFFFFFFF, 0xFFB9C6FF, Shader.TileMode.CLAMP));
        c.drawRoundRect(new RectF(left, y, left + w, y + h / 2f), h / 4f, h / 4f, p);
        c.drawCircle(left + w * 0.33f, y, h * 0.32f, p);
        c.drawCircle(left + w * 0.6f, y - h * 0.14f, h * 0.42f, p);
    }

    private static void drop(Canvas c, float x, float y, float s) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(0xFF6FC3FF);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setStrokeWidth(s * 0.035f);
        c.drawLine(x + s * 0.02f, y, x - s * 0.02f, y + s * 0.09f, p);
    }

    private static void bolt(Canvas c, float x, float y, float s) {
        Path b = new Path();
        b.moveTo(x + s * 0.03f, y);
        b.lineTo(x - s * 0.06f, y + s * 0.12f);
        b.lineTo(x - s * 0.005f, y + s * 0.12f);
        b.lineTo(x - s * 0.04f, y + s * 0.24f);
        b.lineTo(x + s * 0.07f, y + s * 0.08f);
        b.lineTo(x + s * 0.015f, y + s * 0.08f);
        b.close();
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setShader(new LinearGradient(x, y, x, y + s * 0.24f, 0xFFD9B8FF, 0xFF9C6BFF, Shader.TileMode.CLAMP));
        c.drawPath(b, p);
    }

    private static void flake(Canvas c, float x, float y, float r) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(0xFFBFE0FF);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setStrokeWidth(r * 0.35f);
        for (int i = 0; i < 3; i++) {
            double a = i * Math.PI / 3 + Math.PI / 2;
            float cos = (float) Math.cos(a) * r, sin = (float) Math.sin(a) * r;
            c.drawLine(x - cos, y - sin, x + cos, y + sin, p);
        }
    }

    private static void lines(Canvas c, float x, float y, float s, int count) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(0xFFE8EEFF);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setStrokeWidth(s * 0.045f);
        for (int i = 0; i < count; i++) {
            float yy = y + i * s * 0.11f;
            float half = s * (0.17f - (i % 2) * 0.04f);
            c.drawLine(x - half, yy, x + half, yy, p);
        }
    }
}
