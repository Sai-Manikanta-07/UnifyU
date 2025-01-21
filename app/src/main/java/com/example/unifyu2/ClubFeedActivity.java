package com.example.unifyu2;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class ClubFeedActivity extends AppCompatActivity {
    private RecyclerView feedRecyclerView;
    private SwipeRefreshLayout swipeRefresh;
    private TextView emptyView;
    private View progressBar;
    private FloatingActionButton createPostFab;

    private FirebaseAuth firebaseAuth;
    private DatabaseReference postsRef;
    private DatabaseReference clubsRef;

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
        createPostFab = findViewById(R.id.createPostFab);

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

        // Setup FAB
        createPostFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO: Show create post dialog or navigate to create post activity
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
} 