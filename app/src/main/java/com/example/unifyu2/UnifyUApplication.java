package com.example.unifyu2;

import android.app.Application;
import android.util.Log;
import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.auth.FirebaseAuth;
import com.example.unifyu2.utils.ClubMemberCountFixer;
import com.example.unifyu2.notifications.NotificationHelper;
import com.example.unifyu2.notifications.FCMManager;

public class UnifyUApplication extends Application {
    private static final String TAG = "UnifyUApplication";
    
    @Override
    public void onCreate() {
        super.onCreate();
        // Initialize Firebase
        FirebaseApp.initializeApp(this);
        
        // Enable Firebase persistence
        try {
            FirebaseDatabase.getInstance().setPersistenceEnabled(true);
        } catch (Exception e) {
            Log.w("Firebase", "Persistence already enabled");
        }
        
        // Enable offline capabilities
        DatabaseReference.goOnline();
        
        // Set database settings
        FirebaseDatabase.getInstance().getReference().keepSynced(true);
        
        // Fix club member counts
        fixClubMemberCounts();
        
        // Create notification channel
        NotificationHelper.createNotificationChannel(this);
    }
    
    private void fixClubMemberCounts() {
        // Only run this when a user is logged in
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            Log.d(TAG, "Starting club member count synchronization");
            ClubMemberCountFixer.synchronizeAllClubMemberCounts(() -> 
                Log.d(TAG, "Club member count synchronization completed"));
        }
    }
} 