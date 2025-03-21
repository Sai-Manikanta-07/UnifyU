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
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MyPostsFragment extends Fragment implements PostAdapter.OnPostInteractionListener {
    private static final String TAG = "MyPostsFragment";
    
    private RecyclerView recyclerView;
    private TextView noPostsText;
    private CircularProgressIndicator progressBar;
    private PostAdapter adapter;
    private List<Post> postList;
    private SwipeRefreshLayout swipeRefreshLayout;
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_my_posts, container, false);
        
        recyclerView = view.findViewById(R.id.postsRecyclerView);
        noPostsText = view.findViewById(R.id.noPostsText);
        progressBar = view.findViewById(R.id.progressBar);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        
        postList = new ArrayList<>();
        adapter = new PostAdapter(getContext(), this);
        
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
        
        swipeRefreshLayout.setOnRefreshListener(this::loadMyPosts);
        
        loadMyPosts();
        
        return view;
    }
    
    private void loadMyPosts() {
        if (!isAdded() || getContext() == null) return;
        
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        progressBar.setVisibility(View.VISIBLE);
        
        DatabaseReference postsRef = FirebaseDatabase.getInstance().getReference("posts");
        Query query = postsRef.orderByChild("userId").equalTo(userId);
        
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded() || getContext() == null) return;
                
                postList.clear();
                for (DataSnapshot postSnapshot : snapshot.getChildren()) {
                    Post post = postSnapshot.getValue(Post.class);
                    if (post != null) {
                        post.setPostId(postSnapshot.getKey());
                        postList.add(post);
                    }
                }
                
                // Sort posts by timestamp (newest first)
                Collections.sort(postList, (p1, p2) -> 
                    Long.compare(p2.getTimestampLong(), p1.getTimestampLong()));
                
                adapter.setPosts(postList);
                adapter.notifyDataSetChanged();
                
                if (postList.isEmpty()) {
                    noPostsText.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                } else {
                    noPostsText.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);
                }
                
                progressBar.setVisibility(View.GONE);
                swipeRefreshLayout.setRefreshing(false);
            }
            
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (!isAdded() || getContext() == null) return;
                Log.e(TAG, "Database error: ", error.toException());
                Toast.makeText(getContext(), "Failed to load posts", Toast.LENGTH_SHORT).show();
                progressBar.setVisibility(View.GONE);
                swipeRefreshLayout.setRefreshing(false);
            }
        });
    }

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
} 