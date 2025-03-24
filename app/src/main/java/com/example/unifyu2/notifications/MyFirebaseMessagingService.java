package com.example.unifyu2.notifications;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.example.unifyu2.MainActivity;
import com.example.unifyu2.R;
import com.example.unifyu2.ViewEventActivity;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

public class MyFirebaseMessagingService extends FirebaseMessagingService {
    private static final String TAG = "FCMService";
    private static final String CHANNEL_ID = "unifyu_notifications";
    private static final String CHANNEL_NAME = "UnifyU Notifications";
    private static final int NOTIFICATION_ID = 100;

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        Log.d(TAG, "🔔 Message received from: " + remoteMessage.getFrom());
        Log.d(TAG, "🔔 Message data payload: " + remoteMessage.getData());

        // Check if message contains notification payload
        if (remoteMessage.getNotification() != null) {
            Log.d(TAG, "🔔 Message has notification body: " + remoteMessage.getNotification().getBody());
            String title = remoteMessage.getNotification().getTitle();
            String body = remoteMessage.getNotification().getBody();
            showNotification(title, body, remoteMessage.getData());
        } 
        // Handle data payload
        else if (remoteMessage.getData().size() > 0) {
            Log.d(TAG, "🔔 Message has data payload: " + remoteMessage.getData());
            Map<String, String> data = remoteMessage.getData();
            
            // Handle event notifications specifically
            if (data.containsKey("eventId")) {
                String eventTitle = data.containsKey("title") ? data.get("title") : "New Event";
                String clubName = data.containsKey("clubName") ? data.get("clubName") : "";
                String eventDesc = data.containsKey("description") ? data.get("description") : "Check out this new event";
                
                Log.d(TAG, "🔔 Processing event notification for club: " + clubName);
                Log.d(TAG, "🔔 Event title: " + eventTitle);
                Log.d(TAG, "🔔 Event description: " + eventDesc);
                
                showNotification(eventTitle, eventDesc, data);
            } else {
                // Extract notification data for other types
                String title = data.get("title");
                String message = null;
                
                // Try different fields for message content
                if (data.containsKey("description")) {
                    message = data.get("description");
                } else if (data.containsKey("message")) {
                    message = data.get("message");
                } else if (data.containsKey("body")) {
                    message = data.get("body");
                }
                
                if (title != null && message != null) {
                    Log.d(TAG, "🔔 Showing notification with title: " + title);
                    showNotification(title, message, data);
                } else {
                    // Generic notification for other data messages
                    Log.d(TAG, "🔔 Showing generic notification");
                    showNotification("New Notification", "You have a new notification", data);
                }
            }
        }
    }

    private void showNotification(String title, String message, Map<String, String> data) {
        Log.d(TAG, "🔔 Preparing to show notification: " + title);
        NotificationManager notificationManager = 
            (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        
        // Create notification channel for Android O and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Notifications for new events and updates");
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{100, 200, 300, 400, 500});
            channel.enableLights(true);
            channel.setLightColor(Color.BLUE);
            notificationManager.createNotificationChannel(channel);
        }
        
        // Create an explicit intent for the appropriate activity
        Intent intent;
        if (data != null && data.containsKey("eventId")) {
            // If notification is about an event, open event details
            intent = new Intent(this, ViewEventActivity.class);
            intent.putExtra("eventId", data.get("eventId"));
            if (data.containsKey("clubId")) {
                intent.putExtra("clubId", data.get("clubId"));
            }
            Log.d(TAG, "🔔 Creating intent for event: " + data.get("eventId"));
        } else {
            // Otherwise open main activity
            intent = new Intent(this, MainActivity.class);
            Log.d(TAG, "🔔 Creating intent for main activity");
        }
        
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        
        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        
        // Build notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setStyle(new NotificationCompat.BigTextStyle().bigText(message));
        
        // Generate unique notification ID
        int notificationId = NOTIFICATION_ID;
        if (data != null && data.containsKey("eventId")) {
            notificationId = data.get("eventId").hashCode();
        }
            
        // Show the notification
        try {
            notificationManager.notify(notificationId, builder.build());
            Log.d(TAG, "🔔 Notification displayed with ID: " + notificationId);
        } catch (SecurityException e) {
            Log.e(TAG, "🔔 Error showing notification: " + e.getMessage());
        }
    }
    
    @Override
    public void onNewToken(@NonNull String token) {
        Log.d(TAG, "Refreshed token: " + token);
        // Update token in Firebase
        FCMManager.updateUserToken();
    }
} 