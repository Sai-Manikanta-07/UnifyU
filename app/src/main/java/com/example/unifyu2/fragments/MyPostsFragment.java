package com.example.unifyu2.fragments;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.example.unifyu2.R;
import com.example.unifyu2.adapters.PostAdapter;
import com.example.unifyu2.models.Post;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MyPostsFragment extends Fragment implements PostAdapter.OnPostInteractionListener {
    private static final String TAG = "MyPostsFragment";
    
    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private TextView emptyView;
    private CircularProgressIndicator progressBar;
    private PostAdapter adapter;
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_my_posts, container, false);
        
        // Initialize views
        recyclerView = view.findViewById(R.id.postsRecyclerView);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        emptyView = view.findViewById(R.id.emptyView);
        progressBar = view.findViewById(R.id.progressBar);
        
        // Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new PostAdapter(requireContext(), this);
        recyclerView.setAdapter(adapter);
        
        // Setup SwipeRefreshLayout
        swipeRefreshLayout.setOnRefreshListener(this::loadPosts);
        
        // Load initial data
        loadPosts();
        
        return view;
    }
    
    private void loadPosts() {
        if (!isAdded() || getContext() == null) return;
        
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference postsRef = FirebaseDatabase.getInstance().getReference("posts");
        
        progressBar.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        emptyView.setVisibility(View.GONE);
        
        postsRef.orderByChild("userId").equalTo(userId)
            .addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded() || getContext() == null) return;
                
                    List<Post> posts = new ArrayList<>();
                for (DataSnapshot postSnapshot : snapshot.getChildren()) {
                        try {
                    Post post = postSnapshot.getValue(Post.class);
                    if (post != null) {
                        post.setPostId(postSnapshot.getKey());
                                posts.add(post);
                                Log.d(TAG, "Loaded post: " + post.getPostId());
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing post: " + postSnapshot.getKey(), e);
                        }
                    }
                    
                    // Sort posts by timestamp (newest first)
                    Collections.sort(posts, (p1, p2) -> {
                        Long timestamp1 = p1.getTimestamp() instanceof Long ? (Long) p1.getTimestamp() : 0L;
                        Long timestamp2 = p2.getTimestamp() instanceof Long ? (Long) p2.getTimestamp() : 0L;
                        return timestamp2.compareTo(timestamp1);
                    });
                    
                    progressBar.setVisibility(View.GONE);
                    swipeRefreshLayout.setRefreshing(false);
                    
                    if (posts.isEmpty()) {
                        recyclerView.setVisibility(View.GONE);
                        emptyView.setVisibility(View.VISIBLE);
                        emptyView.setText("You haven't created any posts yet");
                        Log.d(TAG, "No posts found for user: " + userId);
                    } else {
                        recyclerView.setVisibility(View.VISIBLE);
                        emptyView.setVisibility(View.GONE);
                        adapter.setPosts(posts);
                        Log.d(TAG, "Found " + posts.size() + " posts for user: " + userId);
                    }
                }
                
                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    if (!isAdded() || getContext() == null) return;
                    Log.e(TAG, "Error loading posts: ", error.toException());
                    progressBar.setVisibility(View.GONE);
                    swipeRefreshLayout.setRefreshing(false);
                    Toast.makeText(getContext(), 
                        "Error loading posts", Toast.LENGTH_SHORT).show();
                }
            });
    }
    
    @Override
    public void onLikeClicked(Post post) {
        if (!isAdded() || getContext() == null) return;
        
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference likeRef = FirebaseDatabase.getInstance()
            .getReference("posts")
            .child(post.getPostId())
            .child("likes")
            .child(userId);
        
        likeRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    likeRef.removeValue();
                } else {
                    likeRef.setValue(true);
                }
            }
            
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), 
                    "Failed to update like", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onImageClicked(String imageUrl) {
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(imageUrl));
            startActivity(intent);
        }
    }

    @Override
    public void onLinkClicked(String url) {
        if (url != null && !url.isEmpty()) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(intent);
        }
    }

    @Override
    public void onReactionSelected(Post post, String reactionType) {
        if (!isAdded() || getContext() == null) return;
        
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference reactionRef = FirebaseDatabase.getInstance()
            .getReference("posts")
            .child(post.getPostId())
            .child("reactions")
            .child(userId);
        
        if (reactionType == null) {
            // Remove reaction
            reactionRef.removeValue()
                .addOnFailureListener(e -> Toast.makeText(getContext(), 
                    "Failed to remove reaction", Toast.LENGTH_SHORT).show());
        } else {
            // Add or update reaction
            reactionRef.setValue(reactionType)
                .addOnFailureListener(e -> Toast.makeText(getContext(), 
                    "Failed to update reaction", Toast.LENGTH_SHORT).show());
        }
    }
    
    public void onCommentClicked(Post post) {
        // Handle comment click if needed
    }
    
    public void onShareClicked(Post post) {
        if (!isAdded() || getContext() == null) return;
        
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, post.getContent());
        startActivity(Intent.createChooser(shareIntent, "Share post via"));
    }
    
    public void onDeleteClicked(Post post) {
        if (!isAdded() || getContext() == null) return;
        
        new MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Post")
            .setMessage("Are you sure you want to delete this post?")
            .setPositiveButton("Delete", (dialog, which) -> {
                DatabaseReference postRef = FirebaseDatabase.getInstance()
                    .getReference("posts")
                    .child(post.getPostId());
                
                postRef.removeValue()
                    .addOnSuccessListener(aVoid -> 
                        Toast.makeText(getContext(), 
                            "Post deleted", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e -> 
                        Toast.makeText(getContext(), 
                            "Failed to delete post", Toast.LENGTH_SHORT).show());
            })
            .setNegativeButton("Cancel", null)
            .show();
    }
} 