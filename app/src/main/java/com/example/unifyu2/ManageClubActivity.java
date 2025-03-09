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
    private DatabaseReference usersRef;
    private StorageReference storageRef;
    private PostAdapter postAdapter;
    private Uri selectedImageUri;
    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private View progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_club);

        // Get club data from intent
        club = getIntent().getParcelableExtra("club");
        if (club == null) {
            Toast.makeText(this, "Error loading club data", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize Firebase refs
        clubRef = FirebaseDatabase.getInstance().getReference("clubs").child(club.getId());
        postsRef = FirebaseDatabase.getInstance().getReference("posts");
        membershipsRef = FirebaseDatabase.getInstance().getReference("memberships");
        usersRef = FirebaseDatabase.getInstance().getReference("users");
        storageRef = FirebaseStorage.getInstance().getReference();

        // Initialize views
        progressBar = findViewById(R.id.progressBar);

        // Setup toolbar
        setSupportActionBar(findViewById(R.id.toolbar));
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
        RecyclerView postsRecyclerView = findViewById(R.id.postsRecyclerView);
        postsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        postAdapter = new PostAdapter(this, new PostAdapter.OnPostInteractionListener() {
            @Override
            public void onLikeClicked(Post post) {
                // Handle like click
                // TODO: Implement like functionality
            }

            @Override
            public void onReactionSelected(Post post, String reactionType) {
                // Handle reaction selection
                // TODO: Implement reaction functionality
            }

            @Override
            public void onLinkClicked(String url) {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(intent);
            }

            @Override
            public void onImageClicked(String imageUrl) {
                // TODO: Implement full-screen image view
            }
        });
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
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_members_list, null);
        RecyclerView membersRecyclerView = dialogView.findViewById(R.id.membersRecyclerView);
        View dialogProgressBar = dialogView.findViewById(R.id.progressBar);

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this)
            .setTitle("Club Members")
            .setView(dialogView)
            .setPositiveButton("Close", null);

        androidx.appcompat.app.AlertDialog dialog = builder.create();

        // Setup RecyclerView
        membersRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        List<User> members = new ArrayList<>();
        MembersAdapter adapter = new MembersAdapter(members, user -> {
            // Show make admin confirmation dialog
            if (!user.getId().equals(FirebaseAuth.getInstance().getCurrentUser().getUid())) {
                showMakeAdminDialog(user, dialog);
            }
        });
        membersRecyclerView.setAdapter(adapter);

        // Load members
        dialogProgressBar.setVisibility(View.VISIBLE);
        membershipsRef.orderByChild("clubId").equalTo(club.getId())
            .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot membershipsSnapshot) {
                    List<String> memberIds = new ArrayList<>();
                    for (DataSnapshot membershipSnapshot : membershipsSnapshot.getChildren()) {
                        ClubMembership membership = membershipSnapshot.getValue(ClubMembership.class);
                        if (membership != null) {
                            memberIds.add(membership.getUserId());
                        }
                    }

                    // Load user details for each member
                    for (String memberId : memberIds) {
                        usersRef.child(memberId).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot userSnapshot) {
                                User user = userSnapshot.getValue(User.class);
                                if (user != null) {
                                    user.setId(userSnapshot.getKey());
                                    members.add(user);
                                    adapter.notifyDataSetChanged();
                                }
                                if (members.size() == memberIds.size()) {
                                    dialogProgressBar.setVisibility(View.GONE);
                                }
                            }

                            @Override
                            public void onCancelled(DatabaseError error) {
                                dialogProgressBar.setVisibility(View.GONE);
                                Toast.makeText(ManageClubActivity.this,
                                    "Error loading member details", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }

                @Override
                public void onCancelled(DatabaseError error) {
                    dialogProgressBar.setVisibility(View.GONE);
                    Toast.makeText(ManageClubActivity.this,
                        "Error loading members", Toast.LENGTH_SHORT).show();
                }
            });

        dialog.show();
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

    private void loadClubPosts() {
        postsRef.orderByChild("userId").equalTo(FirebaseAuth.getInstance().getCurrentUser().getUid())
            .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    List<Post> posts = new ArrayList<>();
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        Post post = snapshot.getValue(Post.class);
                        if (post != null) {
                            post.setPostId(snapshot.getKey());
                            posts.add(post);
                        }
                    }

                    // Sort posts by timestamp (newest first)
                    Collections.sort(posts, (p1, p2) -> {
                        Long timestamp1 = (Long) p1.getTimestamp();
                        Long timestamp2 = (Long) p2.getTimestamp();
                        return timestamp2.compareTo(timestamp1);
                    });

                    postAdapter.setPosts(posts);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Toast.makeText(ManageClubActivity.this,
                        "Error loading posts", Toast.LENGTH_SHORT).show();
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