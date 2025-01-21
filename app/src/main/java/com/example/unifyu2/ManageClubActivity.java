package com.example.unifyu2;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

public class ManageClubActivity extends AppCompatActivity {
    private Club club;
    private DatabaseReference clubRef;
    private DatabaseReference postsRef;
    private DatabaseReference membershipsRef;
    private StorageReference storageRef;
    private PostAdapter postAdapter;
    private Uri selectedImageUri;
    private ActivityResultLauncher<Intent> imagePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_club);

        // Get club data from intent
        club = getIntent().getParcelableExtra("club");
        
        // Initialize Firebase refs
        clubRef = FirebaseDatabase.getInstance().getReference("clubs").child(club.getId());
        postsRef = FirebaseDatabase.getInstance().getReference("posts");
        membershipsRef = FirebaseDatabase.getInstance().getReference("memberships");
        storageRef = FirebaseStorage.getInstance().getReference();

        // Setup toolbar
        setSupportActionBar(findViewById(R.id.toolbar));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Manage " + club.getName());

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
        RecyclerView postsRecyclerView = findViewById(R.id.postsRecyclerView);
        postsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        postAdapter = new PostAdapter(new ArrayList<>());
        postsRecyclerView.setAdapter(postAdapter);

        // Setup UI
        setupUI();
        loadClubPosts();
    }

    private void setupUI() {
        findViewById(R.id.createPostButton).setOnClickListener(v -> showCreatePostDialog());
        findViewById(R.id.viewMembersButton).setOnClickListener(v -> showMembersDialog());
        findViewById(R.id.editClubButton).setOnClickListener(v -> showEditClubDialog());
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
        Post post = new Post(postId, club.getId(), content, userId);
        post.setImageUrl(imageUrl);
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
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_members_list, null);
        RecyclerView recyclerView = dialogView.findViewById(R.id.membersRecyclerView);
        TextView memberCountText = dialogView.findViewById(R.id.memberCountText);
        View progressBar = dialogView.findViewById(R.id.progressBar);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        
        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
            .setTitle(R.string.club_members)
            .setView(dialogView)
            .setPositiveButton(R.string.close, null)
            .create();

        progressBar.setVisibility(View.VISIBLE);

        // Get club members
        DatabaseReference membershipsRef = FirebaseDatabase.getInstance()
            .getReference("memberships");
        DatabaseReference usersRef = FirebaseDatabase.getInstance()
            .getReference("users");

        // Query memberships for this club
        membershipsRef.orderByChild("clubId")
            .equalTo(club.getId())
            .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    List<String> memberIds = new ArrayList<>();
                    for (DataSnapshot membershipSnapshot : snapshot.getChildren()) {
                        ClubMembership membership = membershipSnapshot.getValue(ClubMembership.class);
                        if (membership != null) {
                            memberIds.add(membership.getUserId());
                        }
                    }

                    if (memberIds.isEmpty()) {
                        progressBar.setVisibility(View.GONE);
                        memberCountText.setText(getString(R.string.member_count, 0));
                        return;
                    }

                    List<User> members = new ArrayList<>();
                    AtomicInteger counter = new AtomicInteger(memberIds.size());

                    for (String userId : memberIds) {
                        usersRef.child(userId).get().addOnCompleteListener(task -> {
                            if (task.isSuccessful() && task.getResult().exists()) {
                                User user = task.getResult().getValue(User.class);
                                if (user != null) {
                                    members.add(user);
                                }
                            }

                            if (counter.decrementAndGet() == 0) {
                                progressBar.setVisibility(View.GONE);
                                memberCountText.setText(getString(R.string.member_count, members.size()));
                                
                                MembersAdapter adapter = new MembersAdapter(members, club.getId(), 
                                    club.getAdminId(), new MembersAdapter.OnMemberActionListener() {
                                        @Override
                                        public void onMemberRemoved(User user) {
                                            removeMember(user.getId());
                                            dialog.dismiss();
                                        }

                                        @Override
                                        public void onMakeAdmin(User user) {
                                            makeUserAdmin(user.getId(), club.getId());
                                            dialog.dismiss();
                                        }
                                    });
                                recyclerView.setAdapter(adapter);
                            }
                        });
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(ManageClubActivity.this, 
                        "Error loading members: " + error.getMessage(), 
                        Toast.LENGTH_SHORT).show();
                }
            });

        dialog.show();
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

    private void loadClubPosts() {
        postsRef.orderByChild("clubId").equalTo(club.getId())
            .addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    List<Post> posts = new ArrayList<>();
                    for (DataSnapshot postSnapshot : snapshot.getChildren()) {
                        Post post = postSnapshot.getValue(Post.class);
                        if (post != null) {
                            posts.add(post);
                        }
                    }
                    Collections.sort(posts, (p1, p2) -> 
                        Long.compare(p2.getTimestamp(), p1.getTimestamp()));
                    postAdapter.updatePosts(posts);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(ManageClubActivity.this,
                        "Error loading posts", Toast.LENGTH_SHORT).show();
                }
            });
    }

    private void makeUserAdmin(String userId, String clubId) {
        DatabaseReference clubRef = FirebaseDatabase.getInstance().getReference("clubs").child(clubId);
        
        Map<String, Object> updates = new HashMap<>();
        updates.put("adminId", userId);

        clubRef.updateChildren(updates)
            .addOnSuccessListener(aVoid -> {
                Toast.makeText(this, "Admin rights transferred successfully", Toast.LENGTH_SHORT).show();
            })
            .addOnFailureListener(e -> 
                Toast.makeText(this, "Failed to transfer admin rights", Toast.LENGTH_SHORT).show()
            );
    }

    private void removeMember(String userId) {
        // First remove the membership
        String membershipId = userId + "_" + club.getId();
        membershipsRef.child(membershipId).removeValue()
            .addOnSuccessListener(aVoid -> {
                // Then decrement the member count
                DatabaseReference memberCountRef = clubRef.child("memberCount");
                memberCountRef.get().addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Integer currentCount = task.getResult().getValue(Integer.class);
                        if (currentCount != null && currentCount > 0) {
                            memberCountRef.setValue(currentCount - 1)
                                .addOnSuccessListener(aVoid2 -> {
                                    Toast.makeText(this, 
                                        getString(R.string.member_removed), 
                                        Toast.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(e -> 
                                    Toast.makeText(this, 
                                        getString(R.string.member_remove_failed), 
                                        Toast.LENGTH_SHORT).show()
                                );
                        }
                    }
                });
            })
            .addOnFailureListener(e -> 
                Toast.makeText(this, 
                    getString(R.string.member_remove_failed), 
                    Toast.LENGTH_SHORT).show()
            );
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