package com.example.unifyu2.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.example.unifyu2.R;
import com.example.unifyu2.adapters.ProfileTabAdapter;
import com.example.unifyu2.models.User;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class ProfileFragment extends Fragment {
    private static final String TAG = "ProfileFragment";
    
    private TextView usernameText, emailText, rollNumberText, semesterText;
    private TextView clubCountText, postsCountText;
    private ShapeableImageView profileImage;
    private MaterialButton editProfileButton;
    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private CircularProgressIndicator progressBar;
    
    private FirebaseAuth firebaseAuth;
    private DatabaseReference userRef;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        // Initialize views
        usernameText = view.findViewById(R.id.usernameText);
        emailText = view.findViewById(R.id.emailText);
        rollNumberText = view.findViewById(R.id.rollNumberText);
        semesterText = view.findViewById(R.id.semesterText);
        clubCountText = view.findViewById(R.id.clubCountText);
        postsCountText = view.findViewById(R.id.postsCountText);
        profileImage = view.findViewById(R.id.profileImage);
        editProfileButton = view.findViewById(R.id.editProfileButton);
        tabLayout = view.findViewById(R.id.tabLayout);
        viewPager = view.findViewById(R.id.viewPager);
        progressBar = view.findViewById(R.id.progressBar);

        // Initialize Firebase
        firebaseAuth = FirebaseAuth.getInstance();
        
        // Enable Firebase persistence
        try {
            FirebaseDatabase.getInstance().setPersistenceEnabled(true);
        } catch (Exception e) {
            Log.w(TAG, "Persistence already enabled");
        }

        // Monitor connection state
        DatabaseReference connectedRef = FirebaseDatabase.getInstance().getReference(".info/connected");
        connectedRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                boolean connected = snapshot.getValue(Boolean.class);
                if (!connected) {
                    Toast.makeText(getContext(), 
                        "You are offline. Changes will sync when online.", 
                        Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.w(TAG, "Listener was cancelled");
            }
        });

        // Setup edit profile button
        editProfileButton.setOnClickListener(v -> showEditProfileDialog());

        // Setup tabs
        setupTabs();

        // Load profile data
        loadUserProfile();

        return view;
    }
    
    private void setupTabs() {
        ProfileTabAdapter adapter = new ProfileTabAdapter(this);
        viewPager.setAdapter(adapter);
        
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            switch (position) {
                case 0:
                    tab.setText("My Clubs");
                    break;
                case 1:
                    tab.setText("My Posts");
                    break;
                case 2:
                    tab.setText("My Events");
                    break;
            }
        }).attach();
    }

    private void showEditProfileDialog() {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_edit_profile, null);
        
        TextInputEditText usernameEdit = dialogView.findViewById(R.id.usernameEditText);
        TextInputEditText rollNumberEdit = dialogView.findViewById(R.id.rollNumberEditText);
        Spinner semesterSpinner = dialogView.findViewById(R.id.semesterSpinner);
        
        // Setup semester spinner
        String[] semesters = new String[]{"1st", "2nd", "3rd", "4th", "5th", "6th", "7th", "8th"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), 
            android.R.layout.simple_spinner_dropdown_item, semesters);
        semesterSpinner.setAdapter(adapter);
        
        // Pre-fill current values
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user != null) {
            usernameEdit.setText(user.getDisplayName());
            
            // Get current user data
            userRef = FirebaseDatabase.getInstance().getReference("users").child(user.getUid());
            userRef.get().addOnSuccessListener(snapshot -> {
                User userData = snapshot.getValue(User.class);
                if (userData != null) {
                    if (userData.getRollNumber() != null) {
                        rollNumberEdit.setText(userData.getRollNumber());
                    }
                    if (userData.getSemester() != null) {
                        int semesterIndex = getSemesterIndex(userData.getSemester());
                        if (semesterIndex >= 0) {
                            semesterSpinner.setSelection(semesterIndex);
                        }
                    }
                }
            });
        }

        new MaterialAlertDialogBuilder(getContext())
            .setTitle("Edit Profile")
            .setView(dialogView)
            .setPositiveButton("Save", (dialog, which) -> {
                String newUsername = usernameEdit.getText().toString().trim();
                String newRollNumber = rollNumberEdit.getText().toString().trim();
                String newSemester = semesterSpinner.getSelectedItem().toString();
                
                if (newUsername.isEmpty()) {
                    Toast.makeText(getContext(), "Username cannot be empty", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                if (!newRollNumber.isEmpty() && newRollNumber.length() != 10) {
                    Toast.makeText(getContext(), "Roll number must be 10 characters", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                updateProfile(newUsername, newRollNumber, newSemester);
            })
            .setNegativeButton("Cancel", null)
            .show();
    }
    
    private int getSemesterIndex(String semester) {
        if (semester == null) return -1;
        semester = semester.toLowerCase();
        switch (semester) {
            case "1st": return 0;
            case "2nd": return 1;
            case "3rd": return 2;
            case "4th": return 3;
            case "5th": return 4;
            case "6th": return 5;
            case "7th": return 6;
            case "8th": return 7;
            default: return -1;
        }
    }

    private void updateProfile(String newUsername, String newRollNumber, String newSemester) {
        if (!isAdded() || getContext() == null) return;
        
        progressBar.setVisibility(View.VISIBLE);
        FirebaseUser user = firebaseAuth.getCurrentUser();
        
        if (user == null) {
            progressBar.setVisibility(View.GONE);
            return;
        }

        // Update display name in Firebase Auth
        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
            .setDisplayName(newUsername)
            .build();

        user.updateProfile(profileUpdates)
            .addOnCompleteListener(task -> {
                if (!isAdded() || getContext() == null) return;
                
                if (task.isSuccessful()) {
                    // Update additional info in Firebase Database
                    DatabaseReference userRef = FirebaseDatabase.getInstance()
                        .getReference("users")
                        .child(user.getUid());
                    
                    userRef.child("username").setValue(newUsername);
                    userRef.child("rollNumber").setValue(newRollNumber);
                    userRef.child("semester").setValue(newSemester)
                        .addOnCompleteListener(dbTask -> {
                            if (!isAdded() || getContext() == null) return;
                            
                            progressBar.setVisibility(View.GONE);
                            if (dbTask.isSuccessful()) {
                                Toast.makeText(getContext(), 
                                    "Profile updated successfully", Toast.LENGTH_SHORT).show();
                                loadUserProfile(); // Reload profile data
                            } else {
                                Toast.makeText(getContext(), 
                                    "Failed to update profile in database", Toast.LENGTH_SHORT).show();
                            }
                        });
                } else {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(getContext(), 
                        "Failed to update profile", Toast.LENGTH_SHORT).show();
                }
            });
    }

    private void loadUserProfile() {
        if (!isAdded() || getContext() == null) return;
        
        progressBar.setVisibility(View.VISIBLE);

        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user != null) {
            try {
                userRef = FirebaseDatabase.getInstance()
                    .getReference("users")
                    .child(user.getUid());
                
                userRef.get().addOnSuccessListener(snapshot -> {
                    if (!isAdded() || getContext() == null) return;
                    
                    try {
                        User userData = snapshot.getValue(User.class);
                        if (userData != null) {
                            usernameText.setText(userData.getUsername());
                            emailText.setText(userData.getEmail());
                            
                            // Set roll number and semester if available
                            String rollNumber = userData.getRollNumber();
                            if (rollNumber != null && !rollNumber.isEmpty()) {
                                rollNumberText.setText(rollNumber);
                            } else {
                                rollNumberText.setText("Roll Number: Not set");
                            }
                            
                            String semester = userData.getSemester();
                            if (semester != null && !semester.isEmpty()) {
                                semesterText.setText("Semester: " + semester);
                            } else {
                                semesterText.setText("Semester: Not set");
                            }
                            
                            // Load stats
                            loadUserStats(user.getUid());
                            
                        } else {
                            // If user data doesn't exist in database, use Auth data
                            usernameText.setText(user.getDisplayName());
                            emailText.setText(user.getEmail());
                            rollNumberText.setText("Roll Number: Not set");
                            semesterText.setText("Semester: Not set");
                            
                            // Create user in database if it doesn't exist
                            User newUser = new User(user.getUid(), 
                                user.getDisplayName(), user.getEmail());
                            userRef.setValue(newUser);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Data Parse Error", e);
                        Toast.makeText(getContext(), 
                            "Error loading profile data", Toast.LENGTH_SHORT).show();
                    } finally {
                        progressBar.setVisibility(View.GONE);
                    }
                }).addOnFailureListener(e -> {
                    if (!isAdded() || getContext() == null) return;
                    Log.e(TAG, "Database Error", e);
                    Toast.makeText(getContext(), 
                        "Failed to load profile", Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                });
            } catch (Exception e) {
                Log.e(TAG, "General Error", e);
                Toast.makeText(getContext(), 
                    "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                progressBar.setVisibility(View.GONE);
            }
        } else {
            progressBar.setVisibility(View.GONE);
        }
    }

    private void loadUserStats(String userId) {
        // Load club count
        DatabaseReference clubsRef = FirebaseDatabase.getInstance().getReference("clubs");
        clubsRef.orderByChild("members/" + userId).equalTo(true)
            .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (isAdded() && getContext() != null) {
                        clubCountText.setText(String.valueOf(snapshot.getChildrenCount()));
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.w(TAG, "loadClubCount:onCancelled", error.toException());
                }
            });

        // Load posts count
        DatabaseReference postsRef = FirebaseDatabase.getInstance().getReference("posts");
        postsRef.orderByChild("userId").equalTo(userId)
            .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (isAdded() && getContext() != null) {
                        postsCountText.setText(String.valueOf(snapshot.getChildrenCount()));
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.w(TAG, "loadPostCount:onCancelled", error.toException());
                }
            });
    }
} 