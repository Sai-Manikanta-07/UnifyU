package com.example.unifyu2;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import com.example.unifyu2.fragments.ClubFeedFragment;
import com.example.unifyu2.fragments.ViewClubsFragment;
import com.example.unifyu2.fragments.ProfileFragment;
import com.example.unifyu2.fragments.EventsFragment;
import com.example.unifyu2.notifications.FCMManager;

import java.util.HashSet;
import java.util.Set;

public class MainActivity extends AppCompatActivity {
    private FirebaseAuth firebaseAuth;
    private BottomNavigationView bottomNav;
    private FragmentManager fragmentManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        firebaseAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();

        if (currentUser == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        // Update FCM token for push notifications
        FCMManager.updateUserToken();
        
        // Check notification permissions
        checkNotificationPermissions();
        
        // Debug: Log all tokens in database
        FCMManager.logAllTokens();

        // Initialize FragmentManager
        fragmentManager = getSupportFragmentManager();

        // Setup toolbar
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Setup bottom navigation
        bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.navigation_feed) {
                loadUserClubs();
                return true;
            } else if (itemId == R.id.navigation_clubs) {
                loadFragment(new ViewClubsFragment());
                return true;
            } else if (itemId == R.id.navigation_events) {
                loadFragment(new EventsFragment());
                return true;
            } else if (itemId == R.id.navigation_profile) {
                loadFragment(new ProfileFragment());
                return true;
            }
            return false;
        });

        // Set default fragment
        if (savedInstanceState == null) {
            // Post to main thread to ensure view is ready
            bottomNav.post(() -> bottomNav.setSelectedItemId(R.id.navigation_feed));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_logout) {
            firebaseAuth.signOut();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void loadFragment(Fragment fragment) {
        if (fragment != null && !isFinishing()) {
            try {
                fragmentManager.beginTransaction()
                    .setCustomAnimations(
                        android.R.anim.fade_in,
                        android.R.anim.fade_out
                    )
                    .replace(R.id.fragment_container, fragment)
                    .commitAllowingStateLoss();
            } catch (Exception e) {
                Toast.makeText(this, "Error loading screen: " + e.getMessage(), 
                    Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void loadUserClubs() {
        if (isFinishing()) return;

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        Log.d("MainActivity", "Loading clubs for user: " + userId);
        DatabaseReference membershipsRef = FirebaseDatabase.getInstance().getReference("memberships");
        
        membershipsRef.orderByChild("userId").equalTo(userId)
            .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (isFinishing()) return;

                    try {
                        Set<String> clubIds = new HashSet<>();
                        Log.d("MainActivity", "Found " + dataSnapshot.getChildrenCount() + " memberships");
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            Log.d("MainActivity", "Membership data: " + snapshot.toString());
                            String clubId = snapshot.child("clubId").getValue(String.class);
                            if (clubId != null) {
                                clubIds.add(clubId);
                                Log.d("MainActivity", "Added club ID: " + clubId);
                            }
                        }
                        
                        Log.d("MainActivity", "Total clubs found: " + clubIds.size());
                        loadFragment(new ClubFeedFragment(clubIds));
                    } catch (Exception e) {
                        Log.e("MainActivity", "Error loading clubs", e);
                        Toast.makeText(MainActivity.this, 
                            "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    if (isFinishing()) return;
                    
                    Log.e("MainActivity", "Database error: " + databaseError.getMessage());
                    Toast.makeText(MainActivity.this, 
                        "Error loading clubs: " + databaseError.getMessage(), 
                        Toast.LENGTH_SHORT).show();
                }
            });
    }

    private void checkNotificationPermissions() {
        // For Android 13 and above, we need to request POST_NOTIFICATIONS permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, 
                    Manifest.permission.POST_NOTIFICATIONS) != 
                    PackageManager.PERMISSION_GRANTED) {
                
                // Request the permission
                ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    100);
                
                Log.d("MainActivity", "Requesting notification permission");
            } else {
                Log.d("MainActivity", "Notification permission already granted");
            }
        } else {
            Log.d("MainActivity", "Notification permission not needed for this Android version");
        }
    }
}