package com.example.unifyu2.fragments;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.example.unifyu2.R;
import com.example.unifyu2.adapters.PostAdapter;
import com.example.unifyu2.models.Post;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MyPostsFragment extends Fragment implements PostAdapter.OnPostInteractionListener {
    private RecyclerView recyclerView;
    private TextView emptyView;
    private View progressBar;
    private PostAdapter adapter;
    private SwipeRefreshLayout swipeRefreshLayout;
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_my_posts, container, false);
        
        recyclerView = view.findViewById(R.id.recyclerView);
        emptyView = view.findViewById(R.id.emptyView);
        progressBar = view.findViewById(R.id.progressBar);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new PostAdapter(requireContext(), this);
        recyclerView.setAdapter(adapter);
        
        swipeRefreshLayout.setOnRefreshListener(this::loadPosts);
        
        loadPosts();
        
        return view;
    }

    private void loadPosts() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference postsRef = FirebaseDatabase.getInstance().getReference("posts");
        Query query = postsRef.orderByChild("userId").equalTo(userId);
        
        query.addListenerForSingleValueEvent(new ValueEventListener() {
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
                
                Collections.sort(posts, (p1, p2) -> {
                    Long timestamp1 = (Long) p1.getTimestamp();
                    Long timestamp2 = (Long) p2.getTimestamp();
                    return timestamp2.compareTo(timestamp1);
                });
                
                adapter.setPosts(posts);
                swipeRefreshLayout.setRefreshing(false);
                
                if (posts.isEmpty()) {
                    recyclerView.setVisibility(View.GONE);
                    emptyView.setVisibility(View.VISIBLE);
                } else {
                    recyclerView.setVisibility(View.VISIBLE);
                    emptyView.setVisibility(View.GONE);
                }
                progressBar.setVisibility(View.GONE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                swipeRefreshLayout.setRefreshing(false);
                progressBar.setVisibility(View.GONE);
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