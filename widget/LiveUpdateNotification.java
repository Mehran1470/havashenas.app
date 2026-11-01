package com.havashenas.hormozgan;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;

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
                              String city, String temp, String word, String line2, int smallIconRes,
                              Bitmap bubble, int accentColor) {
        Notification.Builder b = new Notification.Builder(ctx, channelId)
                .setSmallIcon(smallIconRes)
                .setLargeIcon(bubble)
                .setColor(accentColor)
                .setContentTitle(temp + " " + word)
                .setContentText(line2)
                .setSubText(city)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setShowWhen(false)
                .setShortCriticalText(temp);
        requestPromotion(b);
        return b.build();
    }

    /**
     * Asks the system to show this as a Live Update. The builder method is not in every
     * compileSdk 36 jar, so it is called by name, with the raw extra as a backup.
     */
    private static void requestPromotion(Notification.Builder b) {
        Bundle extras = new Bundle();
        extras.putBoolean("android.requestPromotedOngoing", true);
        b.addExtras(extras);
        try {
            Notification.Builder.class.getMethod("setRequestPromotedOngoing", boolean.class).invoke(b, true);
        } catch (Throwable ignored) {
        }
    }
}
