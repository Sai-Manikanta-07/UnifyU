package com.example.unifyu2;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.unifyu2.adapters.ClubMembersAdapter;
import com.example.unifyu2.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import java.util.ArrayList;
import java.util.List;

public class ClubDetailsActivity extends AppCompatActivity implements ClubMembersAdapter.OnMemberActionListener {
    private static final String TAG = "ClubDetailsActivity";
    private String clubId;
    private RecyclerView membersRecyclerView;
    private ClubMembersAdapter membersAdapter;
    private List<User> membersList;
    private DatabaseReference clubRef;
    private ValueEventListener membershipsListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_club_details);

        try {
            // Get clubId from intent
            clubId = getIntent().getStringExtra("clubId");
            if (clubId == null || clubId.isEmpty()) {
                Log.e(TAG, "No club ID provided");
                Toast.makeText(this, "Invalid club ID", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            
            Log.d(TAG, "Initializing club details for club: " + clubId);

            // Initialize Firebase reference
            clubRef = FirebaseDatabase.getInstance().getReference("clubs").child(clubId);

            // Initialize members list
            membersList = new ArrayList<>();

            // Initialize RecyclerView
            membersRecyclerView = findViewById(R.id.membersRecyclerView);
            if (membersRecyclerView == null) {
                Log.e(TAG, "membersRecyclerView not found in layout");
                Toast.makeText(this, "Error initializing view", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            // Set up RecyclerView
            membersRecyclerView.setLayoutManager(new LinearLayoutManager(this));
            membersAdapter = new ClubMembersAdapter(this, membersList, this);
            membersRecyclerView.setAdapter(membersAdapter);

            // Set up events button
            findViewById(R.id.viewEventsButton).setOnClickListener(v -> {
                Intent intent = new Intent(this, ClubEventsActivity.class);
                intent.putExtra("clubId", clubId);
                startActivity(intent);
            });

            // Check admin status first, then load members
            checkAdminStatus();

        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate: ", e);
            Toast.makeText(this, "Error initializing club details", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void checkAdminStatus() {
        try {
            String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            Log.d(TAG, "Checking admin status for user: " + currentUserId);
            
            clubRef.child("adminId").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    try {
                        String adminId = snapshot.getValue(String.class);
                        boolean isAdmin = snapshot.exists() && currentUserId.equals(adminId);
                        
                        if (isAdmin) {
                            Toast.makeText(ClubDetailsActivity.this, 
                                "Admin mode active - Long press members to manage", 
                                Toast.LENGTH_LONG).show();
                        }
                        
                        membersAdapter.setAdminStatus(isAdmin);
                        loadMembers();
                        
                    } catch (Exception e) {
                        Log.e(TAG, "Error processing admin status: ", e);
                        loadMembers();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e(TAG, "Error checking admin status: " + error.getMessage());
                    loadMembers();
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error in checkAdminStatus: ", e);
            loadMembers();
        }
    }

    private void loadMembers() {
        try {
            Log.d(TAG, "Starting to load members");
            membersList.clear();
            membersAdapter.notifyDataSetChanged();
            
            DatabaseReference membershipsRef = FirebaseDatabase.getInstance().getReference("memberships");
            
            // Query to find memberships for this club
            Query clubMembershipsQuery = membershipsRef.orderByChild("clubId").equalTo(clubId);
            
            clubMembershipsQuery.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    try {
                        List<String> userIds = new ArrayList<>();
                        Log.d(TAG, "Found " + snapshot.getChildrenCount() + " memberships");
                        
                        // First collect all user IDs
                        for (DataSnapshot membershipSnapshot : snapshot.getChildren()) {
                            String userId = membershipSnapshot.child("userId").getValue(String.class);
                            if (userId != null) {
                                userIds.add(userId);
                                Log.d(TAG, "Added user ID to fetch: " + userId);
                            }
                        }
                        
                        if (userIds.isEmpty()) {
                            Log.d(TAG, "No members found for this club");
                            membersList.clear();
                            membersAdapter.notifyDataSetChanged();
                            return;
                        }
                        
                        // Create a counter to track when all users are loaded
                        final int[] loadedCount = {0};
                        final int totalToLoad = userIds.size();
                        
                        // Now load all user details at once
                        membersList.clear();
                        for (String userId : userIds) {
                            loadMemberDetails(userId, (user) -> {
                                loadedCount[0]++;
                                
                                if (user != null) {
                                    // Add user if not already in the list
                                    boolean alreadyInList = false;
                                    for (User existingUser : membersList) {
                                        if (existingUser.getId().equals(user.getId())) {
                                            alreadyInList = true;
                                            break;
                                        }
                                    }
                                    
                                    if (!alreadyInList) {
                                        membersList.add(user);
                                        Log.d(TAG, "Added member: " + user.getUsername() + " (ID: " + user.getId() + ")");
                                    }
                                }
                                
                                // If all users are loaded, update the UI
                                if (loadedCount[0] >= totalToLoad) {
                                    membersAdapter.notifyDataSetChanged();
                                    Log.d(TAG, "All " + membersList.size() + " members loaded");
                                }
                            });
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error processing memberships: ", e);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e(TAG, "Error loading memberships: " + error.getMessage());
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error in loadMembers: ", e);
        }
    }

    // Using callback to handle asynchronous user loading
    private interface UserLoadCallback {
        void onUserLoaded(User user);
    }

    private void loadMemberDetails(String userId, UserLoadCallback callback) {
        try {
            Log.d(TAG, "Loading details for user: " + userId);
            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(userId);
            
            userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    try {
                        if (snapshot.exists()) {
                            User user = new User();
                            user.setId(snapshot.getKey());
                            user.setUsername(snapshot.child("username").getValue(String.class));
                            user.setEmail(snapshot.child("email").getValue(String.class));
                            
                            if (user.getUsername() != null) {
                                callback.onUserLoaded(user);
                            } else {
                                Log.e(TAG, "Username is null for user: " + userId);
                                callback.onUserLoaded(null);
                            }
                        } else {
                            Log.e(TAG, "No user data found for ID: " + userId);
                            callback.onUserLoaded(null);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error processing user details: ", e);
                        callback.onUserLoaded(null);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e(TAG, "Error loading user details: " + error.getMessage());
                    callback.onUserLoaded(null);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error in loadMemberDetails: ", e);
            callback.onUserLoaded(null);
        }
    }

    @Override
    public void onMakeAdmin(String memberId) {
        new AlertDialog.Builder(this)
            .setTitle("Make Admin")
            .setMessage("Are you sure you want to make this member the admin? You will lose your admin privileges.")
            .setPositiveButton("Yes", (dialog, which) -> {
                makeAdmin(memberId);
            })
            .setNegativeButton("No", null)
            .show();
    }

    private void makeAdmin(String memberId) {
        clubRef.child("adminId").setValue(memberId)
            .addOnSuccessListener(aVoid -> {
                Toast.makeText(this, "Admin rights transferred successfully", Toast.LENGTH_SHORT).show();
                // Refresh the view
                checkAdminStatus();
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error making admin: " + e.getMessage());
                Toast.makeText(this, "Failed to transfer admin rights", Toast.LENGTH_SHORT).show();
            });
    }

    @Override
    public void onRemoveMember(String memberId) {
        new AlertDialog.Builder(this)
            .setTitle("Remove Member")
            .setMessage("Are you sure you want to remove this member?")
            .setPositiveButton("Yes", (dialog, which) -> {
                removeMember(memberId);
            })
            .setNegativeButton("No", null)
            .show();
    }

    private void removeMember(String memberId) {
        try {
            if (memberId == null || memberId.isEmpty()) {
                Log.e(TAG, "Invalid member ID for removal");
                return;
            }

            Log.d(TAG, "Attempting to remove member: " + memberId);
            
            // Remove from memberships
            String membershipKey = memberId + "_" + clubId;
            DatabaseReference membershipRef = FirebaseDatabase.getInstance()
                .getReference("memberships")
                .child(membershipKey);
                
            membershipRef.removeValue()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Successfully removed membership");
                    Toast.makeText(this, "Member removed successfully", Toast.LENGTH_SHORT).show();
                    
                    // Update member count in club
                    clubRef.child("memberCount").get().addOnSuccessListener(dataSnapshot -> {
                        if (dataSnapshot.exists()) {
                            int currentCount = dataSnapshot.getValue(Integer.class);
                            clubRef.child("memberCount").setValue(currentCount - 1);
                        }
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to remove member: " + e.getMessage());
                    Toast.makeText(this, "Failed to remove member", Toast.LENGTH_SHORT).show();
                });
        } catch (Exception e) {
            Log.e(TAG, "Error in removeMember: ", e);
            Toast.makeText(this, "Error removing member", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up listeners if needed
        if (membershipsListener != null && clubRef != null) {
            clubRef.child("members").removeEventListener(membershipsListener);
        }
    }
} 