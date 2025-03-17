package com.example.unifyu2.fragments;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.unifyu2.R;
import com.example.unifyu2.adapters.PostAdapter;
import com.example.unifyu2.models.Post;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ClubFeedFragment extends Fragment implements PostAdapter.OnPostInteractionListener {
    private static final String TAG = "ClubFeedFragment";
    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private View emptyStateContainer;
    private TextView emptyView;
    private View progressBar;
    private ExtendedFloatingActionButton createPostFab;
    private PostAdapter adapter;
    private Set<String> clubIds;
    private DatabaseReference postsRef;
    private FirebaseAuth firebaseAuth;
    private DatabaseReference membershipsRef;
    private ValueEventListener feedListener;

    public ClubFeedFragment() {
        // Required empty constructor
        this.clubIds = new HashSet<>();
    }

    public ClubFeedFragment(Set<String> clubIds) {
        this.clubIds = clubIds != null ? clubIds : new HashSet<>();
        Log.d(TAG, "ClubFeedFragment created with " + (clubIds != null ? clubIds.size() : 0) + " club IDs");
        if (clubIds != null) {
            for (String clubId : clubIds) {
                Log.d(TAG, "Club ID in constructor: " + clubId);
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        try {
            View view = inflater.inflate(R.layout.fragment_club_feed, container, false);

            // Initialize Firebase
            firebaseAuth = FirebaseAuth.getInstance();
            postsRef = FirebaseDatabase.getInstance().getReference("posts");
            membershipsRef = FirebaseDatabase.getInstance().getReference("memberships");

            Log.d(TAG, "Firebase References initialized");
            Log.d(TAG, "Posts Reference: " + postsRef.toString());
            Log.d(TAG, "Club IDs at fragment creation: " + clubIds);

            // Initialize views
            recyclerView = view.findViewById(R.id.recyclerView);
            swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
            emptyStateContainer = view.findViewById(R.id.emptyStateContainer);
            emptyView = view.findViewById(R.id.emptyView);
            progressBar = view.findViewById(R.id.progressBar);
            //createPostFab = view.findViewById(R.id.createPostFab);

            // Initialize RecyclerView for posts
            recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            adapter = new PostAdapter(requireContext(), this);
            recyclerView.setAdapter(adapter);

            // Setup SwipeRefreshLayout
            swipeRefreshLayout.setOnRefreshListener(this::loadPosts);

            // Setup FAB
            // createPostFab.setOnClickListener(v -> showCreatePostDialog());

            // Load initial data
            loadPosts();

            return view;
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreateView", e);
            Toast.makeText(getContext(), "Error loading feed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            return inflater.inflate(R.layout.fragment_club_feed, container, false);
        }
    }

    private void loadPosts() {
        if (!isAdded() || getContext() == null) {
            Log.d(TAG, "Fragment not attached, skipping loadPosts");
            return;
        }

        if (clubIds.isEmpty()) {
            Log.d(TAG, "No club IDs available, showing empty view");
            showEmptyView();
            return;
        }

        if (feedListener != null) {
            postsRef.removeEventListener(feedListener);
        }

        progressBar.setVisibility(View.VISIBLE);
        emptyStateContainer.setVisibility(View.GONE);
        recyclerView.setVisibility(View.GONE);

        try {
            Log.d(TAG, "Starting to load posts from: " + postsRef.toString());
            Log.d(TAG, "Looking for posts matching club IDs: " + clubIds);
            
            // Query posts for all clubs in one go
            feedListener = postsRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (!isAdded() || getContext() == null) {
                        Log.d(TAG, "Fragment not attached during data change");
                        return;
                    }

                    try {
                        Log.d(TAG, "Processing " + dataSnapshot.getChildrenCount() + " total posts");
                        List<Post> posts = new ArrayList<>();
                        
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            try {
                                // Get post data
                                Post post = snapshot.getValue(Post.class);
                                if (post != null) {
                                    // Set post ID
                                    post.setPostId(snapshot.getKey());
                                    
                                    // Check if post belongs to one of the user's clubs
                                    String postClubId = post.getClubId();
                                    if (postClubId != null && clubIds.contains(postClubId)) {
                                        posts.add(post);
                                    } else {
                                        Log.d(TAG, "Skipped post: clubId " + postClubId + 
                                            " not in user's clubs: " + clubIds);
                                    }
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error parsing post: " + snapshot.getKey(), e);
                            }
                        }

                        Log.d(TAG, "Found " + posts.size() + " matching posts");

                        if (posts.isEmpty()) {
                            Log.d(TAG, "No posts to display");
                            showEmptyView();
                        } else {
                            // Sort posts by timestamp (newest first)
                            Collections.sort(posts, (p1, p2) -> {
                                Long timestamp1 = p1.getTimestamp() instanceof Long ? (Long) p1.getTimestamp() : 0L;
                                Long timestamp2 = p2.getTimestamp() instanceof Long ? (Long) p2.getTimestamp() : 0L;
                                return timestamp2.compareTo(timestamp1);
                            });
                            
                            Log.d(TAG, "Setting " + posts.size() + " posts to adapter");
                            adapter.setPosts(posts);
                            recyclerView.setVisibility(View.VISIBLE);
                            emptyStateContainer.setVisibility(View.GONE);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error processing posts", e);
                        showError("Error loading posts: " + e.getMessage());
                    } finally {
                        progressBar.setVisibility(View.GONE);
                        swipeRefreshLayout.setRefreshing(false);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    if (!isAdded() || getContext() == null) return;
                    Log.e(TAG, "Database error", databaseError.toException());
                    showError("Error loading posts: " + databaseError.getMessage());
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error setting up posts listener", e);
            showError("Error loading posts: " + e.getMessage());
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (feedListener != null) {
            postsRef.removeEventListener(feedListener);
        }
    }

    private void showCreatePostDialog() {
        if (!isAdded() || getContext() == null) return;
        Toast.makeText(getContext(), "Create post feature coming soon!", Toast.LENGTH_SHORT).show();
    }

    private void showEmptyView() {
        if (!isAdded() || getContext() == null) return;
        progressBar.setVisibility(View.GONE);
        swipeRefreshLayout.setRefreshing(false);
        recyclerView.setVisibility(View.GONE);
        emptyStateContainer.setVisibility(View.VISIBLE);
        Log.d(TAG, "Showing empty view");
    }

    private void showError(String message) {
        if (!isAdded() || getContext() == null) return;
        progressBar.setVisibility(View.GONE);
        swipeRefreshLayout.setRefreshing(false);
        Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
        Log.e(TAG, "Error: " + message);
    }

    @Override
    public void onLikeClicked(Post post) {
        if (!isAdded() || getContext() == null) return;
        
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference postRef = FirebaseDatabase.getInstance().getReference("posts").child(post.getPostId());
        
        // Update reactions
        postRef.child("reactions").child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    // User already reacted, toggle like off
                    postRef.child("reactions").child(userId).removeValue()
                        .addOnSuccessListener(aVoid -> Log.d(TAG, "Reaction removed"))
                        .addOnFailureListener(e -> Log.e(TAG, "Failed to remove reaction", e));
                } else {
                    // User hasn't reacted, add like
                    postRef.child("reactions").child(userId).setValue("LIKE")
                        .addOnSuccessListener(aVoid -> Log.d(TAG, "Like added"))
                        .addOnFailureListener(e -> Log.e(TAG, "Failed to add like", e));
                }
            }
            
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Database error", error.toException());
            }
        });
    }

    @Override
    public void onReactionSelected(Post post, String reactionType) {
        if (!isAdded() || getContext() == null) return;
        
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference postRef = FirebaseDatabase.getInstance().getReference("posts").child(post.getPostId());
        
        if (reactionType == null) {
            // Remove reaction
            postRef.child("reactions").child(userId).removeValue()
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Reaction removed"))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to remove reaction", e));
        } else {
            // Add or update reaction
            postRef.child("reactions").child(userId).setValue(reactionType)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Reaction updated: " + reactionType))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to update reaction", e));
        }
    }

    @Override
    public void onLinkClicked(String url) {
        if (!isAdded() || getContext() == null) return;
        
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error opening link", e);
            Toast.makeText(getContext(), "Could not open link: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onImageClicked(String imageUrl) {
        if (!isAdded() || getContext() == null) return;
        
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(imageUrl));
            startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error opening image", e);
            Toast.makeText(getContext(), "Could not open image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
} 