package com.example.unifyu2;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;
import android.widget.ProgressBar;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.unifyu2.adapters.PostAdapter;
import com.example.unifyu2.adapters.MembersAdapter;
import com.example.unifyu2.models.Club;
import com.example.unifyu2.models.Post;
import com.example.unifyu2.models.ClubMembership;
import com.example.unifyu2.models.User;
import com.example.unifyu2.models.Event;
import com.example.unifyu2.notifications.FCMManager;
import com.example.unifyu2.notifications.NotificationHelper;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.example.unifyu2.adapters.ManageClubMembersAdapter;
import com.google.firebase.database.*;
import android.util.Log;
import androidx.appcompat.widget.Toolbar;

public class ManageClubActivity extends AppCompatActivity implements ManageClubMembersAdapter.OnMemberActionListener {
    private static final String TAG = "ManageClubActivity";
    private Club club;
    private DatabaseReference clubRef;
    private DatabaseReference postsRef;
    private DatabaseReference membershipsRef;
    private DatabaseReference usersRef;
    private StorageReference storageRef;
    private PostAdapter postAdapter;
    private Uri selectedImageUri;
    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private View progressBar;
    private String clubId;
    private RecyclerView membersRecyclerView;
    private ManageClubMembersAdapter adapter;
    private List<User> membersList;
    private FirebaseAuth firebaseAuth;
    private String currentUserId;
    private ValueEventListener membersEventListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_club);

        // Initialize Firebase
        firebaseAuth = FirebaseAuth.getInstance();
        currentUserId = firebaseAuth.getCurrentUser().getUid();

        // Get club from intent
        club = getIntent().getParcelableExtra("club");
        if (club == null) {
            Toast.makeText(this, "Error: Club details not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        clubId = club.getId();
        if (clubId == null) {
            Toast.makeText(this, "Error: Club ID not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        clubRef = FirebaseDatabase.getInstance().getReference("clubs").child(clubId);
        postsRef = FirebaseDatabase.getInstance().getReference("posts");
        membershipsRef = FirebaseDatabase.getInstance().getReference("memberships");
        usersRef = FirebaseDatabase.getInstance().getReference("users");
        storageRef = FirebaseStorage.getInstance().getReference();
        membersList = new ArrayList<>();

        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Manage " + club.getName());
        }

        // Setup image picker
        imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Intent data = result.getData();
                    if (data != null && data.getData() != null) {
                        selectedImageUri = data.getData();
                        showImagePreview(selectedImageUri);
                    }
                }
            });

        // Setup RecyclerView
        membersRecyclerView = findViewById(R.id.membersRecyclerView);
        membersRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ManageClubMembersAdapter(this, membersList, this);
        membersRecyclerView.setAdapter(adapter);

        // Setup UI and load members
        setupUI();
        loadMembers();
        
        // Synchronize member count
        synchronizeMemberCount();
    }

    private void setupUI() {
        // No buttons to setup in the new layout
    }

    private void loadMembers() {
        membersList.clear();
        adapter.notifyDataSetChanged();
        
        Log.d(TAG, "Starting to load members for main view for club: " + clubId);
        
        DatabaseReference membershipsRef = FirebaseDatabase.getInstance().getReference("memberships");
        Query clubMembershipsQuery = membershipsRef.orderByChild("clubId").equalTo(clubId);
        
        // Use ChildEventListener for real-time updates
        if (membersEventListener != null) {
            clubMembershipsQuery.removeEventListener(membersEventListener);
        }
        
        membersEventListener = clubMembershipsQuery.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // Clear the list first
                membersList.clear();
                
                List<String> userIds = new ArrayList<>();
                Log.d(TAG, "Found " + snapshot.getChildrenCount() + " memberships for club: " + clubId);
                
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
                    adapter.notifyDataSetChanged();
                    return;
                }
                
                // Create a counter to track when all users are loaded
                final int[] loadedCount = {0};
                final int totalToLoad = userIds.size();
                
                // Now load all user details at once
                for (String userId : userIds) {
                    DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(userId);
                    userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            loadedCount[0]++;
                            Log.d(TAG, "Loaded " + loadedCount[0] + "/" + totalToLoad + " users");
                            
                            if (snapshot.exists()) {
                                try {
                                    User user = new User();
                                    user.setId(snapshot.getKey());
                                    user.setUsername(snapshot.child("username").getValue(String.class));
                                    user.setEmail(snapshot.child("email").getValue(String.class));
                                    
                                    if (user.getUsername() != null) {
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
                                    } else {
                                        Log.w(TAG, "User exists but has null username: " + userId);
                                    }
                                } catch (Exception e) {
                                    Log.e(TAG, "Error parsing user data: " + e.getMessage());
                                }
                            } else {
                                Log.w(TAG, "User data not found for ID: " + userId);
                            }
                            
                            // If all users are loaded, update the UI
                            if (loadedCount[0] >= totalToLoad) {
                                adapter.notifyDataSetChanged();
                                Log.d(TAG, "All " + membersList.size() + " members loaded");
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            loadedCount[0]++;
                            Log.e(TAG, "Error loading user details: " + error.getMessage());
                            
                            // If all users are loaded (even with errors), update the UI
                            if (loadedCount[0] >= totalToLoad) {
                                adapter.notifyDataSetChanged();
                                Log.d(TAG, "All members loaded (with some errors)");
                            }
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error loading memberships: " + error.getMessage());
                Toast.makeText(ManageClubActivity.this, "Error loading members", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void synchronizeMemberCount() {
        membershipsRef.orderByChild("clubId").equalTo(clubId)
            .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    int actualMemberCount = (int) snapshot.getChildrenCount();
                    
                    // Update the club's member count if it's different
                    if (club.getMemberCount() != actualMemberCount) {
                        clubRef.child("memberCount").setValue(actualMemberCount)
                            .addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "Member count synchronized: " + actualMemberCount);
                                // Update local club object
                                club.setMemberCount(actualMemberCount);
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Failed to synchronize member count", e);
                            });
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e(TAG, "Error counting members", error.toException());
                }
            });
    }

    private void showCreatePostDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_create_post, null);
        TextInputEditText contentEdit = dialogView.findViewById(R.id.contentEditText);
        ImageView previewImage = dialogView.findViewById(R.id.previewImage);

        dialogView.findViewById(R.id.addImageButton).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            imagePickerLauncher.launch(intent);
        });

        new MaterialAlertDialogBuilder(this)
            .setTitle("Create Post")
            .setView(dialogView)
            .setPositiveButton("Post", (dialog, which) -> {
                String content = contentEdit.getText().toString().trim();
                if (!content.isEmpty()) {
                    createPost(content, selectedImageUri);
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void showImagePreview(Uri imageUri) {
        View dialogView = getWindow().getDecorView().findViewById(android.R.id.content);
        ImageView previewImage = dialogView.findViewById(R.id.previewImage);
        if (previewImage != null) {
            previewImage.setVisibility(View.VISIBLE);
            Glide.with(this)
                .load(imageUri)
                .centerCrop()
                .into(previewImage);
        }
    }

    private void createPost(String content, Uri imageUri) {
        String postId = postsRef.push().getKey();
        if (postId == null) return;

        if (imageUri != null) {
            String imagePath = "posts/" + UUID.randomUUID().toString();
            StorageReference imageRef = storageRef.child(imagePath);
            
            imageRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> {
                    imageRef.getDownloadUrl().addOnSuccessListener(downloadUri -> {
                        savePost(postId, content, downloadUri.toString());
                    });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to upload image", Toast.LENGTH_SHORT).show();
                });
        } else {
            savePost(postId, content, "");
        }
    }

    private void savePost(String postId, String content, String imageUrl) {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        Post post = new Post(
            FirebaseAuth.getInstance().getCurrentUser().getUid(),
            FirebaseAuth.getInstance().getCurrentUser().getDisplayName(),
            content,
            imageUrl != null ? "IMAGE" : "TEXT"
        );
        if (imageUrl != null) {
            post.setImageUrl(imageUrl);
        }
        post.setClubId(club.getId());
        post.setClubName(club.getName());
        post.setAuthorName(FirebaseAuth.getInstance().getCurrentUser().getDisplayName());

        postsRef.child(postId).setValue(post)
            .addOnSuccessListener(aVoid -> {
                Toast.makeText(this, "Post created successfully", Toast.LENGTH_SHORT).show();
                selectedImageUri = null;
            })
            .addOnFailureListener(e -> {
                Toast.makeText(this, "Failed to create post", Toast.LENGTH_SHORT).show();
            });
    }

    private void showMembersDialog() {
        AlertDialog.Builder builder = new MaterialAlertDialogBuilder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_members_list, null);
        
        // Find views
        RecyclerView membersRecyclerView = dialogView.findViewById(R.id.membersRecyclerView);
        TextView titleText = dialogView.findViewById(R.id.titleText);
        ProgressBar progressBar = dialogView.findViewById(R.id.progressBar);
        TextView emptyView = dialogView.findViewById(R.id.emptyView);
        
        // Set title
        titleText.setText("Club Members (" + club.getMemberCount() + ")");
        
        // Show loading state
        progressBar.setVisibility(View.VISIBLE);
        emptyView.setVisibility(View.GONE);
        membersRecyclerView.setVisibility(View.GONE);
        
        // Setup RecyclerView with a fixed height
        membersRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        membersRecyclerView.setHasFixedSize(true);
        
        // Create a new member list and adapter specifically for the dialog
        List<User> dialogMembersList = new ArrayList<>();
        ManageClubMembersAdapter dialogAdapter = new ManageClubMembersAdapter(this, dialogMembersList, this);
        membersRecyclerView.setAdapter(dialogAdapter);
        
        // Create and show dialog
        AlertDialog dialog = builder.setView(dialogView)
                .setNegativeButton("Close", null)
                .create();
        
        dialog.show();
        
        // Load members for dialog
        loadMembersForDialog(dialogMembersList, dialogAdapter, progressBar, emptyView, membersRecyclerView);
    }
    
    private void loadMembersForDialog(List<User> membersList, ManageClubMembersAdapter adapter, 
                                     ProgressBar progressBar, TextView emptyView, RecyclerView recyclerView) {
        // Clear existing data
        membersList.clear();
        adapter.notifyDataSetChanged();
        
        // Log the starting of member loading
        Log.d(TAG, "Starting to load members for dialog for club: " + clubId);
        
        DatabaseReference membershipsRef = FirebaseDatabase.getInstance().getReference("memberships");
        Query clubMembershipsQuery = membershipsRef.orderByChild("clubId").equalTo(clubId);
        
        clubMembershipsQuery.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<String> userIds = new ArrayList<>();
                
                Log.d(TAG, "Found " + snapshot.getChildrenCount() + " memberships in dialog loading");
                
                // First collect all user IDs
                for (DataSnapshot membershipSnapshot : snapshot.getChildren()) {
                    String userId = membershipSnapshot.child("userId").getValue(String.class);
                    if (userId != null) {
                        userIds.add(userId);
                        Log.d(TAG, "Will fetch user: " + userId);
                    }
                }
                
                if (userIds.isEmpty()) {
                    Log.d(TAG, "No members found for this club in dialog loading");
                    progressBar.setVisibility(View.GONE);
                    emptyView.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                    return;
                }
                
                // Create a counter to track when all users are loaded
                final int[] loadedCount = {0};
                final int totalToLoad = userIds.size();
                
                // Now load all user details at once
                for (String userId : userIds) {
                    Log.d(TAG, "Loading user details for: " + userId);
                    DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(userId);
                    userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            loadedCount[0]++;
                            Log.d(TAG, "Loaded " + loadedCount[0] + "/" + totalToLoad + " users");
                            
                            if (snapshot.exists()) {
                                try {
                                    User user = new User();
                                    user.setId(snapshot.getKey());
                                    user.setUsername(snapshot.child("username").getValue(String.class));
                                    user.setEmail(snapshot.child("email").getValue(String.class));
                                    
                                    if (user.getUsername() != null) {
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
                                            Log.d(TAG, "Added member to dialog list: " + user.getUsername());
                                        }
                                    } else {
                                        Log.w(TAG, "User exists but has null username: " + userId);
                                    }
                                } catch (Exception e) {
                                    Log.e(TAG, "Error parsing user data: " + e.getMessage());
                                }
                            } else {
                                Log.w(TAG, "User data not found for: " + userId);
                            }
                            
                            // If all users are loaded, update the UI
                            if (loadedCount[0] >= totalToLoad) {
                                Log.d(TAG, "All " + membersList.size() + " members loaded for dialog");
                                progressBar.setVisibility(View.GONE);
                                
                                if (membersList.isEmpty()) {
                                    emptyView.setVisibility(View.VISIBLE);
                                    recyclerView.setVisibility(View.GONE);
                                } else {
                                    emptyView.setVisibility(View.GONE);
                                    recyclerView.setVisibility(View.VISIBLE);
                                    adapter.notifyDataSetChanged();
                                }
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            loadedCount[0]++;
                            Log.e(TAG, "Error loading user details: " + error.getMessage());
                            
                            // If all users are loaded (even with errors), update the UI
                            if (loadedCount[0] >= totalToLoad) {
                                progressBar.setVisibility(View.GONE);
                                
                                if (membersList.isEmpty()) {
                                    emptyView.setVisibility(View.VISIBLE);
                                    recyclerView.setVisibility(View.GONE);
                                } else {
                                    emptyView.setVisibility(View.GONE);
                                    recyclerView.setVisibility(View.VISIBLE);
                                    adapter.notifyDataSetChanged();
                                }
                            }
                            }
                        });
                    }
                }

                @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error loading memberships for dialog: " + error.getMessage());
                progressBar.setVisibility(View.GONE);
                emptyView.setVisibility(View.VISIBLE);
                emptyView.setText("Error loading members");
            }
        });
    }

    private void showMakeAdminDialog(User user, androidx.appcompat.app.AlertDialog membersDialog) {
        new MaterialAlertDialogBuilder(this)
            .setTitle("Make Admin")
            .setMessage("Are you sure you want to make " + user.getUsername() + " the admin of this club? " +
                       "You will no longer be the admin.")
            .setPositiveButton("Confirm", (dialog, which) -> {
                // Update admin
                clubRef.child("adminId").setValue(user.getId())
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, user.getUsername() + " is now the admin", 
                            Toast.LENGTH_SHORT).show();
                        membersDialog.dismiss();
                        finish(); // Close the manage activity since user is no longer admin
                    })
                    .addOnFailureListener(e -> 
                        Toast.makeText(this, "Failed to update admin: " + e.getMessage(), 
                            Toast.LENGTH_SHORT).show()
                    );
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void showEditClubDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_club, null);
        TextInputEditText nameEdit = dialogView.findViewById(R.id.nameEditText);
        TextInputEditText descriptionEdit = dialogView.findViewById(R.id.descriptionEditText);

        nameEdit.setText(club.getName());
        descriptionEdit.setText(club.getDescription());

        new MaterialAlertDialogBuilder(this)
            .setTitle("Edit Club Details")
            .setView(dialogView)
            .setPositiveButton("Save", (dialog, which) -> {
                String newName = nameEdit.getText().toString().trim();
                String newDescription = descriptionEdit.getText().toString().trim();

                if (!newName.isEmpty() && !newDescription.isEmpty()) {
                    updateClubDetails(newName, newDescription);
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void updateClubDetails(String newName, String newDescription) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("name", newName);
        updates.put("description", newDescription);

        clubRef.updateChildren(updates)
            .addOnSuccessListener(aVoid -> {
                Toast.makeText(this, "Club details updated", Toast.LENGTH_SHORT).show();
                club.setName(newName);
                club.setDescription(newDescription);
                getSupportActionBar().setTitle("Manage " + newName);
            })
            .addOnFailureListener(e -> 
                Toast.makeText(this, "Failed to update club details", Toast.LENGTH_SHORT).show()
            );
    }

    @Override
    public void onMakeAdmin(User member) {
        new AlertDialog.Builder(this)
            .setTitle("Make Admin")
            .setMessage("Are you sure you want to make " + member.getUsername() + " the admin? You will lose your admin privileges.")
            .setPositiveButton("Yes", (dialog, which) -> {
                clubRef.child("adminId").setValue(member.getId())
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Admin rights transferred successfully", Toast.LENGTH_SHORT).show();
                        finish(); // Close activity since current user is no longer admin
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Failed to transfer admin rights", Toast.LENGTH_SHORT).show());
            })
            .setNegativeButton("No", null)
            .show();
    }

    @Override
    public void onRemoveMember(User member) {
        new AlertDialog.Builder(this)
            .setTitle("Remove Member")
            .setMessage("Are you sure you want to remove " + member.getUsername() + " from the club?")
            .setPositiveButton("Yes", (dialog, which) -> {
                String membershipKey = member.getId() + "_" + clubId;
                DatabaseReference membershipRef = FirebaseDatabase.getInstance()
                    .getReference("memberships")
                    .child(membershipKey);
                
                membershipRef.removeValue()
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Member removed successfully", Toast.LENGTH_SHORT).show();
                        // Update member count
                        clubRef.child("memberCount").get().addOnSuccessListener(dataSnapshot -> {
                            if (dataSnapshot.exists()) {
                                int currentCount = dataSnapshot.getValue(Integer.class);
                                clubRef.child("memberCount").setValue(currentCount - 1);
                            }
                        });
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Failed to remove member", Toast.LENGTH_SHORT).show());
            })
            .setNegativeButton("No", null)
            .show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.manage_club_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            onBackPressed();
            return true;
        } else if (itemId == R.id.action_view_events) {
            // Launch ClubEventsActivity
            Intent intent = new Intent(this, ClubEventsActivity.class);
            intent.putExtra("clubId", clubId);
            startActivity(intent);
            return true;
        } else if (itemId == R.id.action_create_post) {
            showCreatePostDialog();
            return true;
        } else if (itemId == R.id.action_create_event) {
            showCreateEventDialog();
            return true;
        } else if (itemId == R.id.action_edit_club) {
            showEditClubDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showCreateEventDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_create_event, null);
        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
            .setTitle("Create Event")
            .setView(dialogView)
            .setPositiveButton("Create", null)
            .setNegativeButton("Cancel", null)
            .create();

        TextInputEditText titleInput = dialogView.findViewById(R.id.eventTitleInput);
        TextInputEditText descriptionInput = dialogView.findViewById(R.id.eventDescriptionInput);
        TextInputEditText venueInput = dialogView.findViewById(R.id.eventVenueInput);
        TextInputEditText dateInput = dialogView.findViewById(R.id.eventDateInput);
        TextInputEditText timeInput = dialogView.findViewById(R.id.eventTimeInput);
        TextInputEditText maxParticipantsInput = dialogView.findViewById(R.id.maxParticipantsInput);
        ImageView imagePreview = dialogView.findViewById(R.id.eventImagePreview);
        MaterialButton uploadButton = dialogView.findViewById(R.id.uploadImageButton);

        // Setup date picker
        dateInput.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            new DatePickerDialog(this, (view, year, month, day) -> {
                calendar.set(year, month, day);
                dateInput.setText(String.format(Locale.getDefault(), "%02d/%02d/%d", day, month + 1, year));
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), 
               calendar.get(Calendar.DAY_OF_MONTH)).show();
        });

        // Setup time picker
        timeInput.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            new TimePickerDialog(this, (view, hour, minute) -> {
                timeInput.setText(String.format(Locale.getDefault(), "%02d:%02d", hour, minute));
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show();
        });

        // Setup image upload
        uploadButton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            imagePickerLauncher.launch(intent);
        });

        dialog.setOnShowListener(dialogInterface -> {
            Button button = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            button.setOnClickListener(view -> {
                String title = titleInput.getText().toString().trim();
                String description = descriptionInput.getText().toString().trim();
                String venue = venueInput.getText().toString().trim();
                String date = dateInput.getText().toString().trim();
                String time = timeInput.getText().toString().trim();
                String maxParticipantsStr = maxParticipantsInput.getText().toString().trim();

                if (title.isEmpty() || description.isEmpty() || venue.isEmpty() || 
                    date.isEmpty() || time.isEmpty() || maxParticipantsStr.isEmpty()) {
                    Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                    return;
                }

                int maxParticipants;
                try {
                    maxParticipants = Integer.parseInt(maxParticipantsStr);
                } catch (NumberFormatException e) {
                    Toast.makeText(this, "Invalid number for maximum participants", 
                        Toast.LENGTH_SHORT).show();
                    return;
                }

                // Parse date and time
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
                Date eventDate;
                try {
                    eventDate = sdf.parse(date + " " + time);
                } catch (ParseException e) {
                    Toast.makeText(this, "Invalid date or time format", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (eventDate != null && eventDate.before(new Date())) {
                    Toast.makeText(this, "Event date must be in the future", Toast.LENGTH_SHORT).show();
                    return;
                }

                createEvent(title, description, venue, eventDate.getTime(), maxParticipants, dialog);
            });
        });

        dialog.show();
    }

    private void createEvent(String title, String description, String venue, long date, 
                           int maxParticipants, AlertDialog dialog) {
        Log.d(TAG, "Creating event with title: " + title);
        DatabaseReference eventsRef = FirebaseDatabase.getInstance().getReference("events");
        String eventId = eventsRef.push().getKey();
        
        if (eventId == null) {
            Log.e(TAG, "Failed to generate event ID");
            Toast.makeText(this, "Error creating event", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d(TAG, "Generated event ID: " + eventId);
        Event event = new Event(eventId, clubId, title, description, venue, date, maxParticipants);
        Log.d(TAG, "Created event object with clubId: " + clubId);

        if (selectedImageUri != null) {
            Log.d(TAG, "Uploading event image");
            StorageReference eventImageRef = storageRef.child("event_images/" + eventId);
            eventImageRef.putFile(selectedImageUri)
                .addOnSuccessListener(taskSnapshot -> {
                    Log.d(TAG, "Image upload successful");
                    eventImageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        event.setImageUrl(uri.toString());
                        Log.d(TAG, "Got image URL: " + uri.toString());
                        saveEventToDatabase(event, dialog);
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to upload image: " + e.getMessage());
                    Toast.makeText(this, "Failed to upload image: " + e.getMessage(), 
                        Toast.LENGTH_SHORT).show();
                    saveEventToDatabase(event, dialog);
                });
        } else {
            Log.d(TAG, "No image selected, saving event directly");
            saveEventToDatabase(event, dialog);
        }
    }

    private void saveEventToDatabase(Event event, AlertDialog dialog) {
        Log.d(TAG, "Saving event to database with ID: " + event.getEventId());
        DatabaseReference eventsRef = FirebaseDatabase.getInstance().getReference("events");
        eventsRef.child(event.getEventId()).setValue(event)
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "Event saved successfully");
                Toast.makeText(this, "Event created successfully", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
                selectedImageUri = null;
                
                // Send notifications to all users
                Log.d(TAG, "Attempting to send notification for event: " + event.getTitle());
                
                // First try a local notification
                NotificationHelper.showEventNotification(this, event);
                Log.d(TAG, "Local notification attempted");
                
                // Then try FCM notification
                FCMManager.notifyNewEvent(event);
                Log.d(TAG, "FCM notification requested");
                
                // Show toast to confirm notification attempt
                Toast.makeText(this, "Notification sent for: " + event.getTitle(), 
                    Toast.LENGTH_SHORT).show();
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Failed to save event: " + e.getMessage());
                Toast.makeText(this, "Failed to create event: " + e.getMessage(), 
                    Toast.LENGTH_SHORT).show();
            });
    }

    // Method to send a test notification for debugging purposes
    private void sendTestNotification(Event event) {
        Log.d(TAG, "Attempting to send test notification for event: " + event.getTitle());
        
        // First try to show a local notification
        NotificationHelper.showEventNotification(this, event);
        
        // Then try the FCM notification
        FCMManager.notifyNewEvent(event);
        
        Toast.makeText(this, "Test notification triggered for: " + event.getTitle(), 
            Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up event listeners
        if (membersEventListener != null) {
            DatabaseReference membershipsRef = FirebaseDatabase.getInstance().getReference("memberships");
            Query clubMembershipsQuery = membershipsRef.orderByChild("clubId").equalTo(clubId);
            clubMembershipsQuery.removeEventListener(membersEventListener);
        }
    }
} 