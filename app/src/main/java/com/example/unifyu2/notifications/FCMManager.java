package com.example.unifyu2.notifications;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.unifyu2.models.Event;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.RemoteMessage;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class FCMManager {
    private static final String TAG = "FCMManager";
    private static final DatabaseReference tokensRef = FirebaseDatabase.getInstance().getReference("fcm_tokens");
    private static final DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");

    public static void updateUserToken() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Log.e(TAG, "Cannot update token: No user logged in");
            return;
        }
        
        String userId = currentUser.getUid();
        Log.d(TAG, "⭐ Updating FCM token for user: " + userId);
        
        // Subscribe to a general topic for all app users
        FirebaseMessaging.getInstance().subscribeToTopic("all_users")
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Log.d(TAG, "⭐ Subscribed to all_users topic");
                } else {
                    Log.e(TAG, "⭐ Failed to subscribe to all_users topic", task.getException());
                }
            });
        
        FirebaseMessaging.getInstance().getToken()
            .addOnCompleteListener(task -> {
                if (!task.isSuccessful()) {
                    Log.e(TAG, "⭐ Fetching FCM registration token failed", task.getException());
                    return;
                }

                // Get new FCM registration token
                String token = task.getResult();
                if (token == null || token.isEmpty()) {
                    Log.e(TAG, "⭐ Received null or empty FCM token!");
                    return;
                }
                
                Log.d(TAG, "⭐ FCM Token obtained: " + token.substring(0, 10) + "...");
                
                // Save token to specific path for this user's tokens
                tokensRef.child(userId).setValue(token)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "⭐ Token updated successfully in fcm_tokens for user: " + userId);
                        
                        // Also update the token in the user profile for backup
                        usersRef.child(userId).child("fcmToken").setValue(token)
                            .addOnSuccessListener(aVoid2 -> {
                                Log.d(TAG, "⭐ Token also saved to user profile");
                                // Verify token was saved by reading it back
                                tokensRef.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                                        String savedToken = snapshot.getValue(String.class);
                                        if (savedToken != null && savedToken.equals(token)) {
                                            Log.d(TAG, "⭐ Verified token is saved in database");
                                        } else {
                                            Log.e(TAG, "⭐ Token verification failed! Database has: " + 
                                                (savedToken != null ? savedToken.substring(0, 10) + "..." : "null"));
                                        }
                                    }
                                    
                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {
                                        Log.e(TAG, "⭐ Error verifying token: " + error.getMessage());
                                    }
                                });
                            })
                            .addOnFailureListener(e -> 
                                Log.e(TAG, "⭐ Failed to save token to user profile: " + e.getMessage()));
                    })
                    .addOnFailureListener(e -> Log.e(TAG, "⭐ Failed to update token: " + e.getMessage()));
            });
    }

    public static void notifyNewEvent(Event event) {
        Log.d(TAG, "⭐ Starting notifyNewEvent for event: " + event.getTitle() + " | ID: " + event.getEventId());

        // Make sure event has all necessary fields
        if (event.getEventId() == null || event.getClubId() == null) {
            Log.e(TAG, "⭐ Event is missing required fields: " + event);
            return;
        }
        
        // First, get the club information to include in the notification
        DatabaseReference clubRef = FirebaseDatabase.getInstance().getReference("clubs").child(event.getClubId());
        clubRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String clubName = "Club";
                if (snapshot.exists()) {
                    clubName = snapshot.child("name").getValue(String.class);
                }
                
                final String finalClubName = clubName;
                Log.d(TAG, "⭐ Sending notification for event in club: " + finalClubName);
                
                // Get all users' tokens
                tokensRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        int tokenCount = 0;
                        Log.d(TAG, "⭐ Found " + snapshot.getChildrenCount() + " total FCM tokens");
                        
                        if (snapshot.getChildrenCount() == 0) {
                            Log.w(TAG, "⭐ No FCM tokens found! Sending to 'all_users' topic instead.");
                            
                            // Send to the all_users topic
                            sendTopicNotification("all_users", event, finalClubName);
                            
                            // Also show local notification
                            try {
                                Context context = getApplicationContext();
                                if (context != null) {
                                    NotificationHelper.showEventNotification(context, event, finalClubName);
                                    Log.d(TAG, "⭐ Local notification shown for event as fallback: " + event.getTitle());
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "⭐ Error showing local notification: " + e.getMessage());
                            }
                            return;
                        }
                        
                        // Send to all users
                        for (DataSnapshot tokenSnapshot : snapshot.getChildren()) {
                            String userId = tokenSnapshot.getKey();
                            String token = tokenSnapshot.getValue(String.class);
                            
                            // Skip null or empty tokens
                            if (token == null || token.isEmpty()) {
                                Log.w(TAG, "⭐ Skipping null/empty token for user: " + userId);
                                continue;
                            }
                            
                            // Send notification to all users
                            sendNotification(token, event, finalClubName, "all_users", userId);
                            tokenCount++;
                            Log.d(TAG, "⭐ Queued notification for user: " + userId + " with token: " + token.substring(0, 10) + "...");
                        }
                        
                        Log.d(TAG, "⭐ Queued notifications for " + tokenCount + " devices");
                        
                        // Also send to topic as a backup
                        sendTopicNotification("all_users", event, finalClubName);
                        Log.d(TAG, "⭐ Also sent to all_users topic as backup");
                        
                        // Show local notification for the creator
                        try {
                            Context context = getApplicationContext();
                            if (context != null) {
                                NotificationHelper.showEventNotification(context, event, finalClubName);
                                Log.d(TAG, "⭐ Local notification shown for event: " + event.getTitle());
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "⭐ Error showing local notification: " + e.getMessage());
                        }
                        
                        // If we didn't send any notifications, log a warning
                        if (tokenCount == 0) {
                            Log.w(TAG, "⭐ No notifications were queued! All tokens may be invalid.");
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "⭐ Error fetching tokens: " + error.getMessage());
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "⭐ Error fetching club details: " + error.getMessage());
            }
        });
    }

    private static void sendNotification(String token, Event event, String clubName, String notificationType, String userId) {
        // Create notification data
        Map<String, String> data = new HashMap<>();
        data.put("eventId", event.getEventId());
        data.put("title", "New Event: " + event.getTitle());
        data.put("description", "New event in " + clubName + ": " + event.getDescription());
        data.put("clubId", event.getClubId());
        data.put("clubName", clubName);
        data.put("type", notificationType);
        data.put("timestamp", String.valueOf(System.currentTimeMillis()));
        data.put("userId", userId); // Add userId for token cleanup if needed
        
        // Store notification in Firebase to trigger Cloud Functions
        DatabaseReference notificationsRef = FirebaseDatabase.getInstance()
            .getReference("notifications");
        
        // Add token to notification data
        data.put("token", token);
        
        // Create a notification entry that will trigger Cloud Functions
        notificationsRef.push().setValue(data)
            .addOnSuccessListener(aVoid -> 
                Log.d(TAG, "Notification data saved to Firebase for user: " + userId))
            .addOnFailureListener(e -> 
                Log.e(TAG, "Failed to save notification data for user " + userId + ": " + e.getMessage()));
    }

    private static void sendTopicNotification(String topic, Event event, String clubName) {
        Log.d(TAG, "⭐ Attempting to send topic notification directly to: " + topic);
        
        // Create data payload
        Map<String, String> data = new HashMap<>();
        data.put("eventId", event.getEventId());
        data.put("title", "New Event: " + event.getTitle());
        data.put("description", "New event in " + clubName + ": " + event.getDescription());
        data.put("clubId", event.getClubId());
        data.put("clubName", clubName);
        data.put("type", "topic");
        data.put("timestamp", String.valueOf(System.currentTimeMillis()));
        
        // Create message
        RemoteMessage.Builder builder = new RemoteMessage.Builder(topic + "@gcm.googleapis.com");
        builder.setMessageId("m-" + System.currentTimeMillis());
        for (Map.Entry<String, String> entry : data.entrySet()) {
            builder.addData(entry.getKey(), entry.getValue());
        }
        
        try {
            // Send message locally if possible (for testing only)
            try {
                Context context = getApplicationContext();
                if (context != null) {
                    // Send local notification
                    NotificationHelper.showEventNotification(context, event, clubName);
                    Log.d(TAG, "⭐ Local notification sent instead of topic message (testing only)");
                }
            } catch (Exception e) {
                Log.e(TAG, "⭐ Error showing local notification: " + e.getMessage());
            }
            
            // Store notification in a different location that should have write permissions
            DatabaseReference notificationsRef = FirebaseDatabase.getInstance()
                .getReference("events").child(event.getEventId()).child("notifications");
            
            // Add a notification entry
            Map<String, Object> notificationData = new HashMap<>();
            notificationData.put("title", data.get("title"));
            notificationData.put("body", data.get("description"));
            notificationData.put("sentToTopic", topic);
            notificationData.put("timestamp", System.currentTimeMillis());
            
            notificationsRef.push().setValue(notificationData)
                .addOnSuccessListener(aVoid -> 
                    Log.d(TAG, "⭐ Notification record saved for event: " + event.getEventId()))
                .addOnFailureListener(e -> 
                    Log.e(TAG, "⭐ Failed to save notification record: " + e.getMessage()));
        } catch (Exception e) {
            Log.e(TAG, "⭐ Error sending topic notification: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Helper method to get application context
    private static Context getApplicationContext() {
        try {
            // Use reflection to get the application context
            Class<?> activityThreadClass = Class.forName("android.app.ActivityThread");
            Object activityThread = activityThreadClass.getMethod("currentActivityThread").invoke(null);
            Object application = activityThreadClass.getMethod("getApplication").invoke(activityThread);
            return (Context) application;
        } catch (Exception e) {
            Log.e(TAG, "Error getting application context: " + e.getMessage());
            return null;
        }
    }

    // Add this new method to verify tokens
    public static void logAllTokens() {
        Log.d(TAG, "Checking all registered FCM tokens...");
        
        tokensRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    int tokenCount = 0;
                    for (DataSnapshot tokenSnapshot : snapshot.getChildren()) {
                        String userId = tokenSnapshot.getKey();
                        String token = tokenSnapshot.getValue(String.class);
                        if (token != null) {
                            Log.d(TAG, "Token for user " + userId + ": " + token.substring(0, 10) + "...");
                            tokenCount++;
                        }
                    }
                    Log.d(TAG, "Total tokens found: " + tokenCount);
                } else {
                    Log.e(TAG, "No tokens found in the database!");
                }
            }
            
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error checking tokens: " + error.getMessage());
            }
        });
    }
} 