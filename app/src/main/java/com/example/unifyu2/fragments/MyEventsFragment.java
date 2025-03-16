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
import com.example.unifyu2.adapters.EventAdapter;
import com.example.unifyu2.models.Event;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MyEventsFragment extends Fragment {
    private static final String TAG = "MyEventsFragment";
    
    private RecyclerView recyclerView;
    private TextView noEventsText;
    private CircularProgressIndicator progressBar;
    private EventAdapter adapter;
    private List<Event> eventList;
    private SwipeRefreshLayout swipeRefreshLayout;
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_my_events, container, false);
        
        recyclerView = view.findViewById(R.id.eventsRecyclerView);
        noEventsText = view.findViewById(R.id.noEventsText);
        progressBar = view.findViewById(R.id.progressBar);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        
        eventList = new ArrayList<>();
        adapter = new EventAdapter(getContext(), eventList);
        
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
        
        swipeRefreshLayout.setOnRefreshListener(this::loadMyEvents);
        
        loadMyEvents();
        
        return view;
    }
    
    private void loadMyEvents() {
        if (!isAdded() || getContext() == null) return;
        
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        progressBar.setVisibility(View.VISIBLE);
        
        DatabaseReference eventsRef = FirebaseDatabase.getInstance().getReference("events");
        
        eventsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded() || getContext() == null) return;
                
                eventList.clear();
                for (DataSnapshot eventSnapshot : snapshot.getChildren()) {
                    Event event = eventSnapshot.getValue(Event.class);
                    if (event != null && event.isRegistered(userId)) {
                        event.setEventId(eventSnapshot.getKey());
                        eventList.add(event);
                    }
                }
                
                // Sort events by date (upcoming first)
                Collections.sort(eventList, Comparator.comparing(Event::getDate));
                
                adapter.notifyDataSetChanged();
                
                if (eventList.isEmpty()) {
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
                Log.e(TAG, "Database error: ", error.toException());
                Toast.makeText(getContext(), "Failed to load events", Toast.LENGTH_SHORT).show();
                progressBar.setVisibility(View.GONE);
                swipeRefreshLayout.setRefreshing(false);
            }
        });
    }
} 