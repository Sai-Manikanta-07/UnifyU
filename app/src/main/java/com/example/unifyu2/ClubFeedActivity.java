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
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.example.unifyu2.models.Post;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.UUID;

public class ClubFeedActivity extends AppCompatActivity {
    private RecyclerView feedRecyclerView;
    private SwipeRefreshLayout swipeRefresh;
    private TextView emptyView;
    private View progressBar;

    private FirebaseAuth firebaseAuth;
    private DatabaseReference postsRef;
    private DatabaseReference clubsRef;

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

    private void createPost(String content, Uri imageUri) {
        String postId = postsRef.push().getKey();
        if (postId == null) return;

        if (imageUri != null) {
            String imagePath = "posts/" + UUID.randomUUID().toString();
            StorageReference imageRef = FirebaseStorage.getInstance().getReference().child(imagePath);
            
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
            savePost(postId, content, null);
        }
    }

    private void savePost(String postId, String content, String imageUrl) {
        Post post = new Post(
            firebaseAuth.getCurrentUser().getUid(),
            firebaseAuth.getCurrentUser().getDisplayName(),
            content,
            imageUrl != null ? "IMAGE" : "TEXT"
        );
        if (imageUrl != null) {
            post.setImageUrl(imageUrl);
        }

        postsRef.child(postId).setValue(post)
            .addOnSuccessListener(aVoid -> {
                Toast.makeText(this, "Post created successfully", Toast.LENGTH_SHORT).show();
                selectedImageUri = null;
                loadFeed(); // Reload the feed to show the new post
            })
            .addOnFailureListener(e -> {
                Toast.makeText(this, "Failed to create post", Toast.LENGTH_SHORT).show();
            });
    }

    private Uri selectedImageUri;
    private ActivityResultLauncher<Intent> imagePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_club_feed);

        // Setup toolbar
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Initialize views
        feedRecyclerView = findViewById(R.id.feedRecyclerView);
        swipeRefresh = findViewById(R.id.swipeRefresh);
        emptyView = findViewById(R.id.emptyView);
        progressBar = findViewById(R.id.progressBar);

        // Setup RecyclerView
        feedRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        
        // Initialize Firebase
        firebaseAuth = FirebaseAuth.getInstance();
        postsRef = FirebaseDatabase.getInstance().getReference("posts");
        clubsRef = FirebaseDatabase.getInstance().getReference("clubs");

        // Setup SwipeRefreshLayout
        swipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                loadFeed();
            }
        });

        // Load initial data
        loadFeed();
    }

    private void loadFeed() {
        progressBar.setVisibility(View.VISIBLE);
        if (swipeRefresh.isRefreshing()) {
            swipeRefresh.setRefreshing(false);
        }
        // TODO: Implement feed loading logic
        // This will involve:
        // 1. Getting user's joined clubs
        // 2. Loading posts from those clubs
        // 3. Sorting by timestamp
        // 4. Updating the adapter
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
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
}