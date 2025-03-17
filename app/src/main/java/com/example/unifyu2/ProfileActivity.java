package com.example.unifyu2;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.unifyu2.adapters.ClubAdapter;
import com.example.unifyu2.models.Club;
import com.example.unifyu2.models.ClubMembership;
import com.example.unifyu2.models.User;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class ProfileActivity extends AppCompatActivity {
    private TextView usernameText, emailText, noClubsText;
    private RecyclerView joinedClubsRecyclerView;
    private View progressBar;
    private ClubAdapter clubAdapter;
    
    private FirebaseAuth firebaseAuth;
    private DatabaseReference clubsRef;
    private DatabaseReference membershipsRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // Setup toolbar
        setSupportActionBar(findViewById(R.id.toolbar));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Initialize views
        usernameText = findViewById(R.id.usernameText);
        emailText = findViewById(R.id.emailText);
        noClubsText = findViewById(R.id.noClubsText);
        joinedClubsRecyclerView = findViewById(R.id.joinedClubsRecyclerView);
        progressBar = findViewById(R.id.progressBar);

        // Setup RecyclerView
        joinedClubsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        clubAdapter = new ClubAdapter(new ArrayList<>(), 
            new ClubAdapter.OnClubClickListener() {
                @Override
                public void onClubClick(Club club) {
                    // Handle club click
                }

                @Override
                public void onManageClubClick(Club club) {
                    Intent intent = new Intent(ProfileActivity.this, ManageClubActivity.class);
                    intent.putExtra("club", club);
                    startActivity(intent);
                }
                
                @Override
                public void onExitClubClick(Club club) {
                    // Handle exit club click
                    new MaterialAlertDialogBuilder(ProfileActivity.this)
                        .setTitle("Exit Club")
                        .setMessage("Are you sure you want to exit " + club.getName() + "?")
                        .setPositiveButton("Exit", (dialog, which) -> {
                            // Exit club logic
                            exitClub(club);
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
                }
            }, 
            FirebaseAuth.getInstance().getCurrentUser().getUid()
        );
        joinedClubsRecyclerView.setAdapter(clubAdapter);

        // Initialize Firebase
        firebaseAuth = FirebaseAuth.getInstance();
        clubsRef = FirebaseDatabase.getInstance().getReference("clubs");
        membershipsRef = FirebaseDatabase.getInstance().getReference("memberships");

        // Load user data
        loadUserProfile();
        loadJoinedClubs();

        // Setup edit profile button
        findViewById(R.id.editProfileButton).setOnClickListener(v -> showEditProfileDialog());
    }

    private void loadUserProfile() {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user != null) {
            // Get user data from Firebase Database instead of just Auth
            DatabaseReference userRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(user.getUid());
            
            userRef.get().addOnSuccessListener(snapshot -> {
                User userData = snapshot.getValue(User.class);
                if (userData != null) {
                    usernameText.setText(userData.getUsername());
                    emailText.setText(userData.getEmail());
                } else {
                    // If user data doesn't exist in database, use Auth data
                    usernameText.setText(user.getDisplayName());
                    emailText.setText(user.getEmail());
                }
            }).addOnFailureListener(e -> {
                Toast.makeText(this, "Failed to load profile", Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void loadJoinedClubs() {
        progressBar.setVisibility(View.VISIBLE);
        String userId = firebaseAuth.getCurrentUser().getUid();

        // First check if user exists in database
        DatabaseReference userRef = FirebaseDatabase.getInstance()
            .getReference("users")
            .child(userId);
        
        userRef.get().addOnSuccessListener(snapshot -> {
            if (!snapshot.exists()) {
                // Create user in database if doesn't exist
                User newUser = new User(
                    userId,
                    firebaseAuth.getCurrentUser().getDisplayName(),
                    firebaseAuth.getCurrentUser().getEmail()
                );
                userRef.setValue(newUser);
            }
            
            // Now load memberships
            membershipsRef.orderByChild("userId").equalTo(userId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot membershipsSnapshot) {
                        if (!membershipsSnapshot.exists()) {
                            showNoClubs();
                            return;
                        }

                        List<String> clubIds = new ArrayList<>();
                        for (DataSnapshot membershipSnapshot : membershipsSnapshot.getChildren()) {
                            ClubMembership membership = membershipSnapshot.getValue(ClubMembership.class);
                            if (membership != null) {
                                clubIds.add(membership.getClubId());
                            }
                        }

                        if (clubIds.isEmpty()) {
                            showNoClubs();
                        } else {
                            loadClubDetails(clubIds);
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(ProfileActivity.this,
                            "Error loading memberships", Toast.LENGTH_SHORT).show();
                    }
                });
        });
    }

    private void loadClubDetails(List<String> clubIds) {
        List<Club> clubs = new ArrayList<>();
        int[] loadedCount = {0};

        for (String clubId : clubIds) {
            clubsRef.child(clubId).get().addOnCompleteListener(task -> {
                loadedCount[0]++;
                
                if (task.isSuccessful()) {
                    Club club = task.getResult().getValue(Club.class);
                    if (club != null) {
                        club.setId(task.getResult().getKey());
                        clubs.add(club);
                    }
                }

                // Check if all clubs are loaded
                if (loadedCount[0] == clubIds.size()) {
                    progressBar.setVisibility(View.GONE);
                    if (clubs.isEmpty()) {
                        showNoClubs();
                    } else {
                        noClubsText.setVisibility(View.GONE);
                        joinedClubsRecyclerView.setVisibility(View.VISIBLE);
                        clubAdapter.updateClubs(clubs);
                    }
                }
            });
        }
    }

    private void showNoClubs() {
        progressBar.setVisibility(View.GONE);
        joinedClubsRecyclerView.setVisibility(View.GONE);
        noClubsText.setVisibility(View.VISIBLE);
    }

    private void showEditProfileDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_profile, null);
        TextInputEditText usernameEdit = dialogView.findViewById(R.id.usernameEditText);
        
        // Pre-fill current username
        usernameEdit.setText(firebaseAuth.getCurrentUser().getDisplayName());

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.edit_profile)
                .setView(dialogView)
                .setPositiveButton(R.string.save, (dialog, which) -> {
                    String newUsername = usernameEdit.getText().toString().trim();
                    if (!newUsername.isEmpty()) {
                        updateProfile(newUsername);
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void updateProfile(String newUsername) {
        progressBar.setVisibility(View.VISIBLE);

        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                .setDisplayName(newUsername)
                .build();

        firebaseAuth.getCurrentUser().updateProfile(profileUpdates)
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        Toast.makeText(this, R.string.profile_updated, Toast.LENGTH_SHORT).show();
                        loadUserProfile();
                    } else {
                        Toast.makeText(this, R.string.profile_update_failed, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void exitClub(Club club) {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        String membershipId = userId + "_" + club.getId();
        
        // Show progress
        progressBar.setVisibility(View.VISIBLE);
        
        // Remove membership
        DatabaseReference membershipsRef = FirebaseDatabase.getInstance().getReference("memberships");
        membershipsRef.child(membershipId).removeValue()
            .addOnSuccessListener(aVoid -> {
                // Decrement member count
                decrementMemberCount(club.getId());
            })
            .addOnFailureListener(e -> {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(this,
                    "Failed to exit club: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
            });
    }
    
    private void decrementMemberCount(String clubId) {
        DatabaseReference clubRef = FirebaseDatabase.getInstance().getReference("clubs").child(clubId).child("memberCount");
        clubRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Integer currentCount = task.getResult().getValue(Integer.class);
                if (currentCount != null && currentCount > 0) {
                    int newCount = currentCount - 1;
                    clubRef.setValue(newCount).addOnCompleteListener(updateTask -> {
                        progressBar.setVisibility(View.GONE);
                        if (updateTask.isSuccessful()) {
                            Toast.makeText(this,
                                "Successfully exited club",
                                Toast.LENGTH_SHORT).show();
                            // Refresh the clubs list
                            loadJoinedClubs();
                        }
                    });
                } else {
                    progressBar.setVisibility(View.GONE);
                }
            } else {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(this,
                    "Error updating member count",
                    Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
} 