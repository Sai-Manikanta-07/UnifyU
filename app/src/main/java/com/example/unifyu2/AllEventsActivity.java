package com.example.unifyu2;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.unifyu2.adapters.EventAdapter;
import com.example.unifyu2.models.Event;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class AllEventsActivity extends AppCompatActivity {
    private static final String TAG = "AllEventsActivity";
    private RecyclerView eventsRecyclerView;
    private EventAdapter eventAdapter;
    private SwipeRefreshLayout swipeRefreshLayout;
    private TextView noEventsText;
    private DatabaseReference eventsRef;
    private ValueEventListener eventsListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all_events);

        // Set title
        setTitle("All Events");

        // Initialize Firebase reference
        eventsRef = FirebaseDatabase.getInstance().getReference("events");
        Log.d(TAG, "Firebase reference initialized: " + eventsRef.toString());

        // Initialize views
        eventsRecyclerView = findViewById(R.id.eventsRecyclerView);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        noEventsText = findViewById(R.id.noEventsText);

        // Setup RecyclerView
        eventsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        eventAdapter = new EventAdapter(this, new ArrayList<>());
        eventsRecyclerView.setAdapter(eventAdapter);

        // Setup SwipeRefreshLayout
        swipeRefreshLayout.setOnRefreshListener(this::loadEvents);

        // Load events initially
        loadEvents();
    }

    private void loadEvents() {
        Log.d(TAG, "Loading all events...");
        if (eventsListener != null) {
            eventsRef.removeEventListener(eventsListener);
        }

        // First, check if the events node exists
        eventsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Log.d(TAG, "Events node exists: " + snapshot.exists());
                Log.d(TAG, "Events node has " + snapshot.getChildrenCount() + " children");
                
                // List all events for debugging
                for (DataSnapshot eventSnapshot : snapshot.getChildren()) {
                    Log.d(TAG, "Found event with ID: " + eventSnapshot.getKey());
                    Log.d(TAG, "Event clubId: " + eventSnapshot.child("clubId").getValue());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error checking events node: " + error.getMessage());
            }
        });
        
        eventsListener = eventsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Log.d(TAG, "Query returned " + snapshot.getChildrenCount() + " events");
                
                List<Event> events = new ArrayList<>();
                for (DataSnapshot eventSnapshot : snapshot.getChildren()) {
                    Log.d(TAG, "Processing event: " + eventSnapshot.getKey());
                    try {
                        Event event = eventSnapshot.getValue(Event.class);
                        if (event != null) {
                            Log.d(TAG, "Event parsed successfully: " + event.getTitle());
                            event.setEventId(eventSnapshot.getKey());
                            events.add(event);
                        } else {
                            Log.e(TAG, "Failed to parse event: " + eventSnapshot.getKey());
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing event: " + e.getMessage());
                    }
                }

                Log.d(TAG, "Total events found: " + events.size());
                
                // Update UI
                eventAdapter.updateEvents(events);
                swipeRefreshLayout.setRefreshing(false);
                
                // Show/hide no events text
                if (events.isEmpty()) {
                    Log.d(TAG, "No events found, showing empty state");
                    noEventsText.setVisibility(View.VISIBLE);
                    eventsRecyclerView.setVisibility(View.GONE);
                } else {
                    Log.d(TAG, "Events found, showing list");
                    noEventsText.setVisibility(View.GONE);
                    eventsRecyclerView.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error loading events: " + error.getMessage());
                swipeRefreshLayout.setRefreshing(false);
                Toast.makeText(AllEventsActivity.this, "Error loading events", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (eventsListener != null) {
            eventsRef.removeEventListener(eventsListener);
        }
    }
} 