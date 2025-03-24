package com.example.unifyu2.notifications;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.unifyu2.R;
import com.example.unifyu2.ViewEventActivity;
import com.example.unifyu2.models.Event;

public class NotificationHelper {
    private static final String CHANNEL_ID = "new_events";
    private static final String CHANNEL_NAME = "New Events";
    private static final String CHANNEL_DESC = "Notifications for new club events";
    private static final int NOTIFICATION_ID = 1;

    public static void createNotificationChannel(Context context) {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription(CHANNEL_DESC);

            // Register the channel with the system
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    public static void showEventNotification(Context context, Event event) {
        showEventNotification(context, event, null);
    }

    public static void showEventNotification(Context context, Event event, String clubName) {
        // Create an explicit intent for the ViewEventActivity
        Intent intent = new Intent(context, ViewEventActivity.class);
        intent.putExtra("eventId", event.getEventId());
        intent.putExtra("clubId", event.getClubId());
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        // Create the pending intent
        PendingIntent pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );

        // Build title and content based on available info
        String title = "New Event: " + event.getTitle();
        String content = event.getDescription();
        
        // Add club name if available
        if (clubName != null && !clubName.isEmpty()) {
            content = "New event in " + clubName + ": " + content;
        }

        // Build the notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent);

        // Show the notification with a unique ID based on event
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        int notificationId = event.getEventId().hashCode();
        try {
            notificationManager.notify(notificationId, builder.build());
            Log.d("NotificationHelper", "Local notification displayed with ID: " + notificationId);
        } catch (SecurityException e) {
            Log.e("NotificationHelper", "Permission denied for notification: " + e.getMessage());
        }
    }
} 