package com.example.unifyu2.fragments;

import android.os.Bundle;
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
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ClubFeedFragment extends Fragment {
    private RecyclerView feedRecyclerView;
    private SwipeRefreshLayout swipeRefresh;
    private TextView emptyView;
    private View progressBar;
    private FloatingActionButton createPostFab;
    private PostAdapter adapter;

    private FirebaseAuth firebaseAuth;
    private DatabaseReference postsRef;
    private DatabaseReference clubsRef;
    private DatabaseReference membershipsRef;
    private ValueEventListener feedListener;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_club_feed, container, false);

        // Initialize views
        feedRecyclerView = view.findViewById(R.id.feedRecyclerView);
        swipeRefresh = view.findViewById(R.id.swipeRefresh);
        emptyView = view.findViewById(R.id.emptyView);
        progressBar = view.findViewById(R.id.progressBar);
        createPostFab = view.findViewById(R.id.createPostFab);

        // Setup RecyclerView
        feedRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new PostAdapter(new ArrayList<>());
        feedRecyclerView.setAdapter(adapter);

        // Initialize Firebase
        firebaseAuth = FirebaseAuth.getInstance();
        postsRef = FirebaseDatabase.getInstance().getReference("posts");
        clubsRef = FirebaseDatabase.getInstance().getReference("clubs");
        membershipsRef = FirebaseDatabase.getInstance().getReference("memberships");

        // Setup SwipeRefreshLayout
        swipeRefresh.setOnRefreshListener(this::loadFeed);

        // Setup FAB
        createPostFab.setOnClickListener(v -> showCreatePostDialog());

        // Load initial data
        loadFeed();

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (feedListener != null) {
            postsRef.removeEventListener(feedListener);
        }
    }

    private void loadFeed() {
        String userId = firebaseAuth.getCurrentUser().getUid();
        progressBar.setVisibility(View.VISIBLE);
        emptyView.setVisibility(View.GONE);

        // First get user's club memberships
        membershipsRef.orderByChild("userId").equalTo(userId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        List<String> clubIds = new ArrayList<>();
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            String clubId = snapshot.child("clubId").getValue(String.class);
                            if (clubId != null) {
                                clubIds.add(clubId);
                            }
                        }

                        if (clubIds.isEmpty()) {
                            showEmptyView();
                            return;
                        }

                        // Now get posts from these clubs
                        loadPostsFromClubs(clubIds);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        showError("Error loading feed: " + databaseError.getMessage());
                    }
                });
    }

    private void loadPostsFromClubs(List<String> clubIds) {
        feedListener = postsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<Post> posts = new ArrayList<>();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Post post = snapshot.getValue(Post.class);
                    if (post != null && clubIds.contains(post.getClubId())) {
                        post.setId(snapshot.getKey());
                        posts.add(post);
                    }
                }

                if (posts.isEmpty()) {
                    showEmptyView();
                } else {
                    // Sort posts by timestamp (newest first)
                    Collections.sort(posts, (p1, p2) -> 
                        Long.compare(p2.getTimestamp(), p1.getTimestamp()));
                    adapter.updatePosts(posts);
                    feedRecyclerView.setVisibility(View.VISIBLE);
                    emptyView.setVisibility(View.GONE);
                }
                progressBar.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                showError("Error loading posts: " + databaseError.getMessage());
            }
        });
    }

    private void showCreatePostDialog() {
        // TODO: Implement post creation
        Toast.makeText(getContext(), "Create post feature coming soon!", Toast.LENGTH_SHORT).show();
    }

    private void showEmptyView() {
        progressBar.setVisibility(View.GONE);
        swipeRefresh.setRefreshing(false);
        feedRecyclerView.setVisibility(View.GONE);
        emptyView.setVisibility(View.VISIBLE);
    }

    private void showError(String message) {
        progressBar.setVisibility(View.GONE);
        swipeRefresh.setRefreshing(false);
        Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
    }
} 