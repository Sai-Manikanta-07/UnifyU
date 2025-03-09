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
import androidx.appcompat.app.AlertDialog;
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
import android.widget.Spinner;
import android.widget.ArrayAdapter;

public class ProfileFragment extends Fragment {
    private TextView usernameText, emailText, rollNumberText, semesterText;
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
        rollNumberText = view.findViewById(R.id.rollNumberText);
        semesterText = view.findViewById(R.id.semesterText);
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
                            
                            // Set roll number and semester if they exist
                            String rollNumber = userData.getRollNumber();
                            String semester = userData.getSemester();
                            
                            if (rollNumber != null && !rollNumber.isEmpty()) {
                                rollNumberText.setText("Roll: " + rollNumber);
                                rollNumberText.setVisibility(View.VISIBLE);
                            } else {
                                rollNumberText.setVisibility(View.GONE);
                            }
                            
                            if (semester != null && !semester.isEmpty()) {
                                semesterText.setText("Semester: " + semester);
                                semesterText.setVisibility(View.VISIBLE);
                            } else {
                                semesterText.setVisibility(View.GONE);
                            }
                        } else {
                            // If user data doesn't exist in database, use Auth data
                            usernameText.setText(user.getDisplayName());
                            emailText.setText(user.getEmail());
                            rollNumberText.setVisibility(View.GONE);
                            semesterText.setVisibility(View.GONE);
                            
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
        TextInputEditText rollNumberEdit = dialogView.findViewById(R.id.rollNumberEditText);
        Spinner semesterSpinner = dialogView.findViewById(R.id.semesterSpinner);
        
        // Setup semester spinner with Material style
        String[] semesters = new String[]{"Select Semester", "1st Semester", "2nd Semester", 
            "3rd Semester", "4th Semester", "5th Semester", "6th Semester", 
            "7th Semester", "8th Semester"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
            requireContext(), 
            R.layout.simple_spinner_item,  // Use custom layout
            semesters
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        semesterSpinner.setAdapter(adapter);
        
        // Set current values
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user != null) {
            DatabaseReference userRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(user.getUid());
            
            userRef.get().addOnSuccessListener(snapshot -> {
                if (!isAdded() || getContext() == null) return;
                
                User userData = snapshot.getValue(User.class);
                if (userData != null) {
                    usernameEdit.setText(userData.getUsername());
                    rollNumberEdit.setText(userData.getRollNumber());
                    
                    // Set spinner selection if semester exists
                    String currentSemester = userData.getSemester();
                    if (currentSemester != null && !currentSemester.isEmpty()) {
                        for (int i = 0; i < semesters.length; i++) {
                            if (semesters[i].equals(currentSemester)) {
                                semesterSpinner.setSelection(i);
                                break;
                            }
                        }
                    }
                }
            });
        }

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext())
            .setTitle("Edit Profile")
            .setView(dialogView)
            .setPositiveButton("Save", null)  // Set to null initially
            .setNegativeButton("Cancel", null);

        AlertDialog dialog = builder.create();
        dialog.show();

        // Set click listener after dialog is shown to prevent automatic dismiss
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String newUsername = usernameEdit.getText().toString().trim();
            String newRollNumber = rollNumberEdit.getText().toString().trim();
            String newSemester = semesterSpinner.getSelectedItemPosition() == 0 ? 
                "" : semesterSpinner.getSelectedItem().toString();
            
            if (newUsername.isEmpty()) {
                Toast.makeText(getContext(), "Username cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }

            // Show progress in dialog
            View progressView = dialogView.findViewById(R.id.progressBar);
            if (progressView != null) {
                progressView.setVisibility(View.VISIBLE);
            }

            performProfileUpdate(newUsername, newRollNumber, newSemester, dialog);
        });
    }

    private void performProfileUpdate(String newUsername, String newRollNumber, String newSemester, AlertDialog dialog) {
        if (!isAdded() || getContext() == null) return;
        
        FirebaseUser user = firebaseAuth.getCurrentUser();
        
        if (user == null) {
            Toast.makeText(getContext(), "User not logged in", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
            return;
        }

        // Update Auth profile
        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
            .setDisplayName(newUsername)
            .build();

        user.updateProfile(profileUpdates)
            .addOnCompleteListener(task -> {
                if (!isAdded() || getContext() == null) return;

                if (task.isSuccessful()) {
                    // Update database with all user fields
                    DatabaseReference userRef = FirebaseDatabase.getInstance()
                        .getReference("users")
                        .child(user.getUid());

                    Map<String, Object> updates = new HashMap<>();
                    updates.put("id", user.getUid());
                    updates.put("username", newUsername);
                    updates.put("email", user.getEmail());
                    
                    // Only update roll number and semester if they are not empty
                    if (!newRollNumber.isEmpty()) {
                        updates.put("rollNumber", newRollNumber);
                    }
                    if (!newSemester.isEmpty()) {
                        updates.put("semester", newSemester);
                    }

                    userRef.updateChildren(updates)
                        .addOnSuccessListener(aVoid -> {
                            if (!isAdded() || getContext() == null) return;
                            dialog.dismiss();
                            Toast.makeText(getContext(), 
                                "Profile updated successfully", 
                                Toast.LENGTH_SHORT).show();
                            loadUserProfile();
                        })
                        .addOnFailureListener(e -> {
                            if (!isAdded() || getContext() == null) return;
                            dialog.dismiss();
                            Log.e("ProfileUpdate", "Database update failed", e);
                            Toast.makeText(getContext(),
                                "Failed to update profile: " + e.getMessage(), 
                                Toast.LENGTH_SHORT).show();
                        });
                } else {
                    dialog.dismiss();
                    Toast.makeText(getContext(),
                        "Failed to update profile", 
                        Toast.LENGTH_SHORT).show();
                }
            });
    }
} 