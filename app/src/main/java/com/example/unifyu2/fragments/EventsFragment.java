package com.example.unifyu2.fragments;

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
import com.example.unifyu2.adapters.EnhancedEventAdapter;
import com.example.unifyu2.models.Event;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class EventsFragment extends Fragment {
    private static final String TAG = "EventsFragment";
    
    private RecyclerView recyclerView;
    private TextView noEventsText;
    private CircularProgressIndicator progressBar;
    private EnhancedEventAdapter adapter;
    private SwipeRefreshLayout swipeRefreshLayout;
    private DatabaseReference eventsRef;
    private ValueEventListener eventsListener;
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_events, container, false);
        
        // Initialize Firebase
        eventsRef = FirebaseDatabase.getInstance().getReference("events");
        
        // Initialize views
        recyclerView = view.findViewById(R.id.eventsRecyclerView);
        noEventsText = view.findViewById(R.id.noEventsText);
        progressBar = view.findViewById(R.id.progressBar);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        
        // Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new EnhancedEventAdapter(getContext());
        recyclerView.setAdapter(adapter);
        
        // Setup SwipeRefreshLayout
        swipeRefreshLayout.setOnRefreshListener(this::loadEvents);
        
        // Load events
        loadEvents();
        
        return view;
    }
    
    private void loadEvents() {
        if (!isAdded() || getContext() == null) return;
        
        progressBar.setVisibility(View.VISIBLE);
        
        if (eventsListener != null) {
            eventsRef.removeEventListener(eventsListener);
        }
        
        eventsListener = eventsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded() || getContext() == null) return;
                
                List<Event> events = new ArrayList<>();
                for (DataSnapshot eventSnapshot : snapshot.getChildren()) {
                    try {
                        Event event = eventSnapshot.getValue(Event.class);
                        if (event != null) {
                            event.setEventId(eventSnapshot.getKey());
                            events.add(event);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing event", e);
                    }
                }
                
                adapter.updateEvents(events);
                
                if (events.isEmpty()) {
                    noEventsText.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                } else {
                    noEventsText.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);
                }
                
                progressBar.setVisibility(View.GONE);
                swipeRefreshLayout.setRefreshing(false);
            }
            
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (!isAdded() || getContext() == null) return;
                
                Log.e(TAG, "Database error", error.toException());
                Toast.makeText(getContext(), "Error loading events", Toast.LENGTH_SHORT).show();
                progressBar.setVisibility(View.GONE);
                swipeRefreshLayout.setRefreshing(false);
            }
        });
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (eventsListener != null) {
            eventsRef.removeEventListener(eventsListener);
        }
    }
} 