package com.example.unifyu2.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.os.Handler;
import android.util.Log;
import android.os.Looper;

import androidx.fragment.app.Fragment;
import com.example.unifyu2.R;
import com.example.unifyu2.models.User;
import com.example.unifyu2.adapters.ProfilePagerAdapter;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import androidx.viewpager2.widget.ViewPager2;
import com.google.firebase.auth.UserProfileChangeRequest;
import java.util.HashMap;
import java.util.Map;

public class ProfileFragment extends Fragment {
    private TextView usernameText, emailText;
    private View progressBar;
    private FirebaseAuth firebaseAuth;
    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private ProfilePagerAdapter pagerAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        // Initialize views
        usernameText = view.findViewById(R.id.usernameText);
        emailText = view.findViewById(R.id.emailText);
        progressBar = view.findViewById(R.id.progressBar);

        // Initialize Firebase
        firebaseAuth = FirebaseAuth.getInstance();
        
        // Enable Firebase persistence
        try {
            FirebaseDatabase.getInstance().setPersistenceEnabled(true);
        } catch (Exception e) {
            Log.w("Firebase", "Persistence already enabled");
        }

        // Monitor connection state
        DatabaseReference connectedRef = FirebaseDatabase.getInstance().getReference(".info/connected");
        connectedRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                boolean connected = snapshot.getValue(Boolean.class);
                if (!connected) {
                    Toast.makeText(getContext(), 
                        "You are offline. Changes will sync when online.", 
                        Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.w("Firebase", "Listener was cancelled");
            }
        });

        // Setup edit profile button
        view.findViewById(R.id.editProfileButton).setOnClickListener(v -> showEditProfileDialog());

        // Initialize ViewPager and TabLayout
        tabLayout = view.findViewById(R.id.tabLayout);
        viewPager = view.findViewById(R.id.viewPager);
        
        pagerAdapter = new ProfilePagerAdapter(requireActivity());
        viewPager.setAdapter(pagerAdapter);

        // Connect TabLayout with ViewPager
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            tab.setText(position == 0 ? "My Clubs" : "My Posts");
        }).attach();

        // Load profile data
        loadUserProfile();
        loadUserStats();

        return view;
    }

    private void loadUserProfile() {
        if (!isAdded() || getContext() == null) return;

        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user != null) {
            try {
                DatabaseReference userRef = FirebaseDatabase.getInstance()
                    .getReference("users")
                    .child(user.getUid());
                
                userRef.get().addOnSuccessListener(snapshot -> {
                    if (!isAdded() || getContext() == null) return;
                    
                    try {
                        User userData = snapshot.getValue(User.class);
                        if (userData != null) {
                            usernameText.setText(userData.getUsername());
                            emailText.setText(userData.getEmail());
                        } else {
                            // If user data doesn't exist in database, use Auth data
                            usernameText.setText(user.getDisplayName());
                            emailText.setText(user.getEmail());
                            
                            // Create user in database if it doesn't exist
                            User newUser = new User(user.getUid(), 
                                user.getDisplayName(), user.getEmail());
                            userRef.setValue(newUser);
                        }
                    } catch (Exception e) {
                        Log.e("ProfileLoad", "Data Parse Error", e);
                        Toast.makeText(getContext(), 
                            "Error loading profile data", Toast.LENGTH_SHORT).show();
                    }
                }).addOnFailureListener(e -> {
                    if (!isAdded() || getContext() == null) return;
                    Log.e("ProfileLoad", "Database Error", e);
                    Toast.makeText(getContext(), 
                        "Failed to load profile", Toast.LENGTH_SHORT).show();
                });
            } catch (Exception e) {
                Log.e("ProfileLoad", "General Error", e);
                Toast.makeText(getContext(), 
                    "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void loadUserStats() {
        String userId = firebaseAuth.getCurrentUser().getUid();
        DatabaseReference membershipsRef = FirebaseDatabase.getInstance().getReference("memberships");
        DatabaseReference postsRef = FirebaseDatabase.getInstance().getReference("posts");

        // Load club count
        membershipsRef.orderByChild("userId").equalTo(userId)
            .addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    if (getView() != null) {
                        TextView clubCountText = getView().findViewById(R.id.clubCountText);
                        clubCountText.setText(String.valueOf(snapshot.getChildrenCount()));
                    }
                }

                @Override
                public void onCancelled(DatabaseError error) {}
            });

        // Load posts count
        postsRef.orderByChild("authorId").equalTo(userId)
            .addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    if (getView() != null) {
                        TextView postsCountText = getView().findViewById(R.id.postsCountText);
                        postsCountText.setText(String.valueOf(snapshot.getChildrenCount()));
                    }
                }

                @Override
                public void onCancelled(DatabaseError error) {}
            });
    }

    private void showEditProfileDialog() {
        View dialogView = LayoutInflater.from(getContext())
            .inflate(R.layout.dialog_edit_profile, null);
        TextInputEditText usernameEdit = dialogView.findViewById(R.id.usernameEditText);
        
        usernameEdit.setText(firebaseAuth.getCurrentUser().getDisplayName());

        new MaterialAlertDialogBuilder(requireContext())
            .setTitle("Edit Profile")
            .setView(dialogView)
            .setPositiveButton("Save", (dialog, which) -> {
                String newUsername = usernameEdit.getText().toString().trim();
                if (!newUsername.isEmpty()) {
                    updateProfile(newUsername);
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void updateProfile(String newUsername) {
        // Check connection first
        DatabaseReference connectedRef = FirebaseDatabase.getInstance().getReference(".info/connected");
        connectedRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Boolean connected = task.getResult().getValue(Boolean.class);
                if (connected == null || !connected) {
                    Toast.makeText(getContext(), 
                        "You are offline. Please check your internet connection.", 
                        Toast.LENGTH_LONG).show();
                    return;
                }
                
                // Proceed with update if online
                performProfileUpdate(newUsername);
            } else {
                Toast.makeText(getContext(), 
                    "Cannot check connection status. Please try again.", 
                    Toast.LENGTH_LONG).show();
            }
        });
    }

    private void performProfileUpdate(String newUsername) {
        Log.d("ProfileUpdate", "Starting profile update for username: " + newUsername);
        
        if (!isAdded() || getContext() == null) {
            Log.e("ProfileUpdate", "Fragment not attached or context is null");
            return;
        }
        
        progressBar.setVisibility(View.VISIBLE);
        Log.d("ProfileUpdate", "Progress bar shown");

        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user == null) {
            Log.e("ProfileUpdate", "User is null");
            progressBar.setVisibility(View.GONE);
            Toast.makeText(getContext(), "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d("ProfileUpdate", "Current user ID: " + user.getUid());

        requireActivity().runOnUiThread(() -> {
            try {
                // First update Auth profile
                UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                    .setDisplayName(newUsername)
                    .build();

                Log.d("ProfileUpdate", "Updating Auth profile...");
                user.updateProfile(profileUpdates)
                    .addOnSuccessListener(aVoid -> {
                        Log.d("ProfileUpdate", "Auth profile updated successfully");
                        if (!isAdded() || getContext() == null) {
                            Log.e("ProfileUpdate", "Fragment not attached after Auth update");
                            return;
                        }

                        try {
                            // Then update database
                            DatabaseReference userRef = FirebaseDatabase.getInstance()
                                .getReference("users")
                                .child(user.getUid());

                            Map<String, Object> updates = new HashMap<>();
                            updates.put("id", user.getUid());
                            updates.put("username", newUsername);
                            updates.put("email", user.getEmail());

                            Log.d("ProfileUpdate", "Updating database...");
                            userRef.updateChildren(updates)
                                .addOnSuccessListener(aVoid2 -> {
                                    Log.d("ProfileUpdate", "Database updated successfully");
                                    if (!isAdded() || getContext() == null) {
                                        Log.e("ProfileUpdate", "Fragment not attached after DB update");
                                        return;
                                    }
                                    progressBar.setVisibility(View.GONE);
                                    Toast.makeText(getContext(), 
                                        "Profile updated successfully", Toast.LENGTH_SHORT).show();
                                    
                                    // Reload profile after short delay
                                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                        if (isAdded() && getContext() != null) {
                                            loadUserProfile();
                                        }
                                    }, 500);
                                })
                                .addOnFailureListener(e -> {
                                    Log.e("ProfileUpdate", "Database update failed", e);
                                    if (!isAdded() || getContext() == null) return;
                                    progressBar.setVisibility(View.GONE);
                                    String error = "Database Error: " + e.getMessage();
                                    Toast.makeText(getContext(), error, Toast.LENGTH_LONG).show();
                                });
                        } catch (Exception e) {
                            Log.e("ProfileUpdate", "Database update error", e);
                            if (isAdded() && getContext() != null) {
                                progressBar.setVisibility(View.GONE);
                                Toast.makeText(getContext(), 
                                    "Database Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e("ProfileUpdate", "Auth update failed", e);
                        if (!isAdded() || getContext() == null) return;
                        progressBar.setVisibility(View.GONE);
                        String error = "Auth Error: " + e.getMessage();
                        Toast.makeText(getContext(), error, Toast.LENGTH_LONG).show();
                    });
            } catch (Exception e) {
                Log.e("ProfileUpdate", "Profile update error", e);
                if (isAdded() && getContext() != null) {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(getContext(), 
                        "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        });
    }
} 