package com.havashenas.hormozgan;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;

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
                              String city, String temp, String desc, int smallIconRes, String windText) {
        String text = desc;
        if (windText != null && !windText.isEmpty()) text += " \u2022 " + windText;

        return new Notification.Builder(ctx, channelId)
                .setSmallIcon(smallIconRes)
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
}
