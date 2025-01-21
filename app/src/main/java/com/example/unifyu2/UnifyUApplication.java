package com.example.unifyu2;

import android.app.Application;
import android.util.Log;
import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class UnifyUApplication extends Application {
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
    }
} 