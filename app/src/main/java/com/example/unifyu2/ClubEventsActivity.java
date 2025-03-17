package com.example.unifyu2;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.unifyu2.adapters.EnhancedEventAdapter;
import com.example.unifyu2.adapters.ParticipantAdapter;
import com.example.unifyu2.models.Event;
import com.example.unifyu2.models.User;
import com.example.unifyu2.utils.ExcelExporter;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClubEventsActivity extends AppCompatActivity implements EnhancedEventAdapter.OnEventAdminActionListener {
    private static final String TAG = "ClubEventsActivity";
    private String clubId;
    private RecyclerView eventsRecyclerView;
    private EnhancedEventAdapter adapter;
    private SwipeRefreshLayout swipeRefreshLayout;
    private TextView noEventsText;
    private DatabaseReference eventsRef;
    private DatabaseReference usersRef;
    private ValueEventListener eventsListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_club_events);

        // Get clubId from intent
        clubId = getIntent().getStringExtra("clubId");
        if (clubId == null) {
            Toast.makeText(this, "Club ID not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        // Initialize Firebase references
        eventsRef = FirebaseDatabase.getInstance().getReference("events");
        usersRef = FirebaseDatabase.getInstance().getReference("users");

        // Initialize views
        eventsRecyclerView = findViewById(R.id.eventsRecyclerView);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        noEventsText = findViewById(R.id.noEventsText);

        // Setup RecyclerView
        eventsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new EnhancedEventAdapter(this, clubId);
        adapter.setAdminActionListener(this);
        eventsRecyclerView.setAdapter(adapter);

        // Setup SwipeRefreshLayout
        swipeRefreshLayout.setOnRefreshListener(this::loadEvents);

        // Load events initially
        loadEvents();
    }

    private void loadEvents() {
        if (eventsListener != null) {
            eventsRef.removeEventListener(eventsListener);
        }

        Query query = eventsRef.orderByChild("clubId").equalTo(clubId);
        eventsListener = query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Event> events = new ArrayList<>();
                for (DataSnapshot eventSnapshot : snapshot.getChildren()) {
                    Event event = eventSnapshot.getValue(Event.class);
                    if (event != null) {
                        event.setEventId(eventSnapshot.getKey());
                        events.add(event);
                    }
                }
                
                adapter.updateEvents(events);
                swipeRefreshLayout.setRefreshing(false);
                noEventsText.setVisibility(events.isEmpty() ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error loading events: " + error.getMessage());
                Toast.makeText(ClubEventsActivity.this, 
                    "Error loading events", Toast.LENGTH_SHORT).show();
                swipeRefreshLayout.setRefreshing(false);
            }
        });
    }

    @Override
    public void onEditEvent(Event event) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_event, null);
        
        TextInputEditText titleInput = dialogView.findViewById(R.id.titleInput);
        TextInputEditText descriptionInput = dialogView.findViewById(R.id.descriptionInput);
        TextInputEditText venueInput = dialogView.findViewById(R.id.venueInput);
        TextInputEditText dateInput = dialogView.findViewById(R.id.dateInput);
        TextInputEditText timeInput = dialogView.findViewById(R.id.timeInput);
        TextInputEditText maxParticipantsInput = dialogView.findViewById(R.id.maxParticipantsInput);

        // Set current values
        titleInput.setText(event.getTitle());
        descriptionInput.setText(event.getDescription());
        venueInput.setText(event.getVenue());
        
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(event.getDate());
        
        dateInput.setText(String.format("%02d/%02d/%d", 
            calendar.get(Calendar.DAY_OF_MONTH),
            calendar.get(Calendar.MONTH) + 1,
            calendar.get(Calendar.YEAR)));
            
        timeInput.setText(String.format("%02d:%02d", 
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE)));
            
        maxParticipantsInput.setText(String.valueOf(event.getMaxParticipants()));

        // Setup date picker
        dateInput.setOnClickListener(v -> {
            DatePickerDialog picker = new DatePickerDialog(this,
                (view, year, month, dayOfMonth) -> {
                    dateInput.setText(String.format("%02d/%02d/%d", dayOfMonth, month + 1, year));
                    calendar.set(year, month, dayOfMonth);
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));
            picker.show();
        });

        // Setup time picker
        timeInput.setOnClickListener(v -> {
            TimePickerDialog picker = new TimePickerDialog(this,
                (view, hourOfDay, minute) -> {
                    timeInput.setText(String.format("%02d:%02d", hourOfDay, minute));
                    calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    calendar.set(Calendar.MINUTE, minute);
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                true);
            picker.show();
        });

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
            .setTitle("Edit Event")
            .setView(dialogView)
            .setPositiveButton("Save", null)
            .setNegativeButton("Cancel", null)
            .create();

        dialog.setOnShowListener(dialogInterface -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String title = titleInput.getText().toString().trim();
                String description = descriptionInput.getText().toString().trim();
                String venue = venueInput.getText().toString().trim();
                String maxParticipantsStr = maxParticipantsInput.getText().toString().trim();

                if (title.isEmpty() || description.isEmpty() || venue.isEmpty() || 
                    maxParticipantsStr.isEmpty()) {
                    Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                    return;
                }

                int maxParticipants;
                try {
                    maxParticipants = Integer.parseInt(maxParticipantsStr);
                    if (maxParticipants < 0) throw new NumberFormatException();
                } catch (NumberFormatException e) {
                    Toast.makeText(this, "Invalid maximum participants number", 
                        Toast.LENGTH_SHORT).show();
                    return;
                }

                // Update event
                event.setTitle(title);
                event.setDescription(description);
                event.setVenue(venue);
                event.setDate(calendar.getTimeInMillis());
                event.setMaxParticipants(maxParticipants);

                eventsRef.child(event.getEventId()).setValue(event)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Event updated successfully", 
                            Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    })
                    .addOnFailureListener(e -> 
                        Toast.makeText(this, "Failed to update event: " + e.getMessage(), 
                            Toast.LENGTH_SHORT).show()
                    );
            });
        });

        dialog.show();
    }

    @Override
    public void onDeleteEvent(Event event) {
        showDeleteConfirmationDialog(event);
    }

    private void showDeleteConfirmationDialog(Event event) {
        new MaterialAlertDialogBuilder(this)
            .setTitle("Delete Event")
            .setMessage("Are you sure you want to delete this event?")
            .setPositiveButton("Delete", (dialogInterface, i) -> {
                deleteEvent(event);
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void deleteEvent(Event event) {
        eventsRef.child(event.getEventId()).removeValue()
            .addOnSuccessListener(aVoid -> 
                Toast.makeText(this, "Event deleted successfully", 
                    Toast.LENGTH_SHORT).show())
            .addOnFailureListener(e -> 
                Toast.makeText(this, "Failed to delete event: " + e.getMessage(), 
                    Toast.LENGTH_SHORT).show()
            );
    }

    @Override
    public void onViewParticipants(Event event) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_participants, null);
        RecyclerView participantsRecyclerView = dialogView.findViewById(R.id.participantsRecyclerView);
        participantsRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Convert Map<String, Object> to Map<String, String>
        Map<String, String> phoneNumbers = new HashMap<>();
        for (Map.Entry<String, Object> entry : event.getRegisteredUsers().entrySet()) {
            phoneNumbers.put(entry.getKey(), entry.getValue().toString());
        }

        ParticipantAdapter adapter = new ParticipantAdapter(new ArrayList<>(), phoneNumbers);
        participantsRecyclerView.setAdapter(adapter);

        // Load user details for registered participants
        List<String> userIds = new ArrayList<>(event.getRegisteredUsers().keySet());
        if (!userIds.isEmpty()) {
            usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    List<User> participants = new ArrayList<>();
                    for (String userId : userIds) {
                        DataSnapshot userSnapshot = snapshot.child(userId);
                        if (userSnapshot.exists()) {
                            User user = userSnapshot.getValue(User.class);
                            if (user != null) {
                                user.setId(userId);
                                participants.add(user);
                            }
                        }
                    }
                    adapter.updateParticipants(participants);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(ClubEventsActivity.this, 
                        "Error loading participants", Toast.LENGTH_SHORT).show();
                }
            });
        }

        new MaterialAlertDialogBuilder(this)
            .setTitle("Event Participants")
            .setView(dialogView)
            .setPositiveButton("Close", null)
            .show();
    }

    @Override
    public void onDownloadParticipants(Event event) {
        List<User> participants = new ArrayList<>();
        
        if (event.getRegisteredUsers() != null) {
            for (String userId : event.getRegisteredUsers().keySet()) {
                usersRef.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        User user = snapshot.getValue(User.class);
                        if (user != null) {
                            user.setId(snapshot.getKey());
                            participants.add(user);
                            
                            // Once all participants are loaded, export to Excel
                            if (participants.size() == event.getRegisteredUsers().size()) {
                                ExcelExporter.exportParticipants(ClubEventsActivity.this, 
                                    event, participants);
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Error loading participant: " + error.getMessage());
                    }
                });
            }
        } else {
            Toast.makeText(this, "No participants to export", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (eventsListener != null) {
            eventsRef.removeEventListener(eventsListener);
        }
    }
} 