package com.havashenas.hormozgan;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;

/**
 * Android 16+ (API 36) compact Live Update, shown by One UI 8 as a small capsule in the Now Bar.
 * Only used when Build.VERSION.SDK_INT >= 36.
 */
final class LiveUpdateNotification {

    private LiveUpdateNotification() {}

    /** True only if the user allows Live Updates for this app. */
    static boolean canPromote(Context ctx) {
        NotificationManager nm = ctx.getSystemService(NotificationManager.class);
        return nm != null && nm.canPostPromotedNotifications();
    }

    static Notification build(Context ctx, String channelId, PendingIntent pendingIntent,
                              String city, String temp, String desc, int smallIconRes, String windText, int circleColor) {
        String text = desc;
        if (windText != null && !windText.isEmpty()) text += " \u2022 " + windText;

        return new Notification.Builder(ctx, channelId)
                .setSmallIcon(smallIconRes)
                .setLargeIcon(circleIcon(ctx, smallIconRes, circleColor))
                .setColor(circleColor)
                .setContentTitle(city + " " + temp)
                .setContentText(text)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setShowWhen(false)
                .setShortCriticalText(temp)
                .setRequestPromotedOngoing(true)
                .build();
    }

    /** Weather icon inside a colored circle (color changes with day, night and weather). */
    private static Icon circleIcon(Context ctx, int iconRes, int bgColor) {
        int size = 128;
        Bitmap bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmp);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(bgColor);
        c.drawCircle(size / 2f, size / 2f, size / 2f, p);
        Drawable d = ctx.getDrawable(iconRes);
        if (d != null) {
            int pad = size / 5;
            d.setBounds(pad, pad, size - pad, size - pad);
            d.draw(c);
        }
        return Icon.createWithBitmap(bmp);
    }
}
