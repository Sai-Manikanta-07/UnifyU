package com.example.unifyu2.fragments;

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
import com.example.unifyu2.adapters.EnhancedEventAdapter;
import com.example.unifyu2.models.Event;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class MyEventsFragment extends Fragment implements EnhancedEventAdapter.OnEventAdminActionListener {
    private static final String TAG = "MyEventsFragment";
    
    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private TextView emptyView;
    private CircularProgressIndicator progressBar;
    private EnhancedEventAdapter adapter;
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_my_events, container, false);
        
        // Initialize views
        recyclerView = view.findViewById(R.id.eventsRecyclerView);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        emptyView = view.findViewById(R.id.emptyView);
        progressBar = view.findViewById(R.id.progressBar);
        
        // Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new EnhancedEventAdapter(requireContext());
        adapter.setAdminActionListener(this);
        recyclerView.setAdapter(adapter);
        
        // Setup SwipeRefreshLayout
        swipeRefreshLayout.setOnRefreshListener(this::loadEvents);
        
        // Load initial data
        loadEvents();
        
        return view;
    }
    
    private void loadEvents() {
        if (!isAdded() || getContext() == null) return;
        
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference eventsRef = FirebaseDatabase.getInstance().getReference("events");
        
        progressBar.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        emptyView.setVisibility(View.GONE);
        
        Log.d(TAG, "Starting to load events for user: " + userId);
        
        eventsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded() || getContext() == null) return;
                
                Log.d(TAG, "Total events found: " + snapshot.getChildrenCount());
                
                List<Event> events = new ArrayList<>();
                for (DataSnapshot eventSnapshot : snapshot.getChildren()) {
                    try {
                        // Log key for debugging
                        String eventKey = eventSnapshot.getKey();
                        Log.d(TAG, "Processing event with key: " + eventKey);
                        
                        Event event = eventSnapshot.getValue(Event.class);
                        if (event != null) {
                            event.setEventId(eventKey);
                            
                            // There are 3 ways a user might be registered:
                            // 1. registeredUsers/{userId} = true
                            // 2. registeredUsers/{userId} = phone number
                            // 3. registeredUsers/{userId} = registration object
                            
                            // First check if the registeredUsers node exists
                            if (eventSnapshot.hasChild("registeredUsers")) {
                                DataSnapshot registeredUsers = eventSnapshot.child("registeredUsers");
                                Log.d(TAG, "Event " + event.getTitle() + " has " + registeredUsers.getChildrenCount() + " registrations");
                                
                                if (registeredUsers.hasChild(userId)) {
                                    Log.d(TAG, "User " + userId + " is registered for event: " + event.getTitle());
                                    events.add(event);
                                } else {
                                    Log.d(TAG, "User " + userId + " is NOT registered for event: " + event.getTitle());
                                }
                            } else {
                                Log.d(TAG, "Event " + event.getTitle() + " has no registeredUsers node");
                            }
                        } else {
                            Log.e(TAG, "Failed to parse event from snapshot: " + eventKey);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing event data", e);
                    }
                }
                
                progressBar.setVisibility(View.GONE);
                swipeRefreshLayout.setRefreshing(false);
                
                if (events.isEmpty()) {
                    Log.d(TAG, "No registered events found for user: " + userId);
                    recyclerView.setVisibility(View.GONE);
                    emptyView.setVisibility(View.VISIBLE);
                    emptyView.setText("You haven't registered for any events yet");
                } else {
                    Log.d(TAG, "Found " + events.size() + " registered events for user");
                    recyclerView.setVisibility(View.VISIBLE);
                    emptyView.setVisibility(View.GONE);
                    adapter.updateEvents(events);
                }
            }
            
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (!isAdded() || getContext() == null) return;
                Log.e(TAG, "Error loading events: ", error.toException());
                progressBar.setVisibility(View.GONE);
                swipeRefreshLayout.setRefreshing(false);
                Toast.makeText(getContext(), 
                    "Error loading events", Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    @Override
    public void onEditEvent(Event event) {
        // Only event admins can edit events
    }
    
    @Override
    public void onDeleteEvent(Event event) {
        // Only event admins can delete events
    }
    
    @Override
    public void onViewParticipants(Event event) {
        // Only event admins can view participants
    }
    
    @Override
    public void onDownloadParticipants(Event event) {
        // Only event admins can download participants
    }
} 