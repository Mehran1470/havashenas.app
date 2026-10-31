package com.havashenas.hormozgan;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.graphics.drawable.Icon;

import java.util.Arrays;

/**
 * Android 16+ (API 36) Live Update notification, shown by One UI 8 in the Now Bar.
 * This class is only ever touched when Build.VERSION.SDK_INT >= 36,
 * so older phones never load it.
 */
final class LiveUpdateNotification {

    private LiveUpdateNotification() {}

    /** True only if the user allows Live Updates for this app. */
    static boolean canPromote(Context ctx) {
        NotificationManager nm = ctx.getSystemService(NotificationManager.class);
        return nm != null && nm.canPostPromotedNotifications();
    }

    static Notification build(Context ctx, String channelId, PendingIntent pendingIntent,
                              String city, String temp, String desc, String emoji,
                              int smallIconRes, boolean isDay, int progressPercent,
                              String sunriseText, String sunsetText, String extras) {

        int progress = Math.max(1, Math.min(99, progressPercent));
        int doneColor = isDay ? 0xFFFFB300 : 0xFF7986CB;
        int restColor = isDay ? 0x55FFB300 : 0x557986CB;
        int trackerRes = isDay ? R.drawable.ic_sun : R.drawable.ic_moon_star;

        Notification.ProgressStyle style = new Notification.ProgressStyle()
                .setStyledByProgress(false)
                .setProgress(progress)
                .setProgressTrackerIcon(Icon.createWithResource(ctx, trackerRes))
                .setProgressSegments(Arrays.asList(
                        new Notification.ProgressStyle.Segment(progress).setColor(doneColor),
                        new Notification.ProgressStyle.Segment(100 - progress).setColor(restColor)));

        StringBuilder text = new StringBuilder(desc);
        if (sunriseText != null && sunsetText != null) {
            text.append(" • ☀️ ").append(sunriseText).append(" → ").append(sunsetText);
        }
        if (extras != null && !extras.isEmpty()) {
            text.append(" • ").append(extras);
        }

        return new Notification.Builder(ctx, channelId)
                .setSmallIcon(smallIconRes)
                .setContentTitle(emoji + " " + city + " " + temp)
                .setContentText(text.toString())
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setShowWhen(false)
                .setShortCriticalText(temp)
                .setRequestPromotedOngoing(true)
                .setStyle(style)
                .build();
    }
}
