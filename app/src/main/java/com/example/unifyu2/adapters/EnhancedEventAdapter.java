package com.example.unifyu2.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.unifyu2.R;
import com.example.unifyu2.models.Club;
import com.example.unifyu2.models.Event;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class EnhancedEventAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int VIEW_TYPE_HEADER = 0;
    private static final int VIEW_TYPE_ACTIVE_EVENT = 1;
    private static final int VIEW_TYPE_PAST_HEADER = 2;
    private static final int VIEW_TYPE_PAST_EVENT = 3;
    
    private final Context context;
    private List<Event> activeEvents;
    private List<Event> pastEvents;
    private final String currentUserId;
    private final DatabaseReference eventsRef;
    private final DatabaseReference clubsRef;
    private final Map<String, String> clubNames;
    
    public EnhancedEventAdapter(Context context) {
        this.context = context;
        this.activeEvents = new ArrayList<>();
        this.pastEvents = new ArrayList<>();
        this.currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        this.eventsRef = FirebaseDatabase.getInstance().getReference("events");
        this.clubsRef = FirebaseDatabase.getInstance().getReference("clubs");
        this.clubNames = new HashMap<>();
    }
    
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        
        switch (viewType) {
            case VIEW_TYPE_HEADER:
                View headerView = inflater.inflate(R.layout.item_section_header, parent, false);
                return new HeaderViewHolder(headerView);
                
            case VIEW_TYPE_ACTIVE_EVENT:
                View activeEventView = inflater.inflate(R.layout.item_event, parent, false);
                return new ActiveEventViewHolder(activeEventView);
                
            case VIEW_TYPE_PAST_HEADER:
                View pastHeaderView = inflater.inflate(R.layout.item_section_header, parent, false);
                return new HeaderViewHolder(pastHeaderView);
                
            case VIEW_TYPE_PAST_EVENT:
                View pastEventView = inflater.inflate(R.layout.item_event, parent, false);
                return new PastEventViewHolder(pastEventView);
                
            default:
                throw new IllegalArgumentException("Invalid view type: " + viewType);
        }
    }
    
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof HeaderViewHolder) {
            HeaderViewHolder headerHolder = (HeaderViewHolder) holder;
            if (getItemViewType(position) == VIEW_TYPE_HEADER) {
                headerHolder.headerTitle.setText("Upcoming Events");
            } else {
                headerHolder.headerTitle.setText("Past Events");
            }
        } else if (holder instanceof ActiveEventViewHolder) {
            ActiveEventViewHolder eventHolder = (ActiveEventViewHolder) holder;
            Event event = getActiveEventAt(position);
            eventHolder.bind(event);
        } else if (holder instanceof PastEventViewHolder) {
            PastEventViewHolder eventHolder = (PastEventViewHolder) holder;
            Event event = getPastEventAt(position);
            eventHolder.bind(event);
        }
    }
    
    @Override
    public int getItemCount() {
        int count = 0;
        
        // Active events section (header + events)
        if (!activeEvents.isEmpty()) {
            count += 1 + activeEvents.size(); // Header + events
        }
        
        // Past events section (header + events)
        if (!pastEvents.isEmpty()) {
            count += 1 + pastEvents.size(); // Header + events
        }
        
        return count;
    }
    
    @Override
    public int getItemViewType(int position) {
        int activeEventsCount = activeEvents.isEmpty() ? 0 : activeEvents.size() + 1; // +1 for header
        
        if (activeEvents.isEmpty() && pastEvents.isEmpty()) {
            return VIEW_TYPE_HEADER; // Just show the active header even if empty
        }
        
        if (!activeEvents.isEmpty()) {
            if (position == 0) {
                return VIEW_TYPE_HEADER;
            } else if (position < activeEventsCount) {
                return VIEW_TYPE_ACTIVE_EVENT;
            }
        }
        
        if (!pastEvents.isEmpty()) {
            if (position == activeEventsCount) {
                return VIEW_TYPE_PAST_HEADER;
            } else {
                return VIEW_TYPE_PAST_EVENT;
            }
        }
        
        return VIEW_TYPE_HEADER; // Default fallback
    }
    
    private Event getActiveEventAt(int position) {
        // Position 0 is the header, so subtract 1
        return activeEvents.get(position - 1);
    }
    
    private Event getPastEventAt(int position) {
        int activeEventsCount = activeEvents.isEmpty() ? 0 : activeEvents.size() + 1; // +1 for header
        int pastHeaderPosition = activeEventsCount;
        
        // Subtract the active events section and the past header
        return pastEvents.get(position - pastHeaderPosition - 1);
    }
    
    public void updateEvents(List<Event> events) {
        activeEvents.clear();
        pastEvents.clear();
        
        long currentTime = System.currentTimeMillis();
        
        for (Event event : events) {
            if (event.getDate() > currentTime) {
                activeEvents.add(event);
            } else {
                pastEvents.add(event);
            }
            
            // Load club name if not already loaded
            if (!clubNames.containsKey(event.getClubId())) {
                loadClubName(event.getClubId());
            }
        }
        
        notifyDataSetChanged();
    }
    
    private void loadClubName(String clubId) {
        clubsRef.child(clubId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Club club = snapshot.getValue(Club.class);
                if (club != null) {
                    clubNames.put(clubId, club.getName());
                    notifyDataSetChanged();
                }
            }
            
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Handle error
            }
        });
    }
    
    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        TextView headerTitle;
        
        HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            headerTitle = itemView.findViewById(R.id.sectionTitle);
        }
    }
    
    class ActiveEventViewHolder extends RecyclerView.ViewHolder {
        private final MaterialCardView cardView;
        private final ImageView eventImage;
        private final TextView eventTitle;
        private final TextView eventDateTime;
        private final TextView eventVenue;
        private final TextView eventDescription;
        private final TextView participantCount;
        private final TextView clubNameText;
        private final MaterialButton registerButton;
        
        ActiveEventViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = (MaterialCardView) itemView;
            eventImage = itemView.findViewById(R.id.eventImage);
            eventTitle = itemView.findViewById(R.id.eventTitle);
            eventDateTime = itemView.findViewById(R.id.eventDateTime);
            eventVenue = itemView.findViewById(R.id.eventVenue);
            eventDescription = itemView.findViewById(R.id.eventDescription);
            participantCount = itemView.findViewById(R.id.participantCount);
            clubNameText = itemView.findViewById(R.id.clubName);
            registerButton = itemView.findViewById(R.id.registerButton);
            
            // Make active events stand out with elevation and stroke
            cardView.setStrokeWidth(2);
            cardView.setStrokeColor(context.getResources().getColor(R.color.primary, null));
            cardView.setCardElevation(8);
        }
        
        void bind(Event event) {
            eventTitle.setText(event.getTitle());
            eventVenue.setText(event.getVenue());
            eventDescription.setText(event.getDescription());
            
            // Set club name
            String clubName = clubNames.get(event.getClubId());
            if (clubName != null) {
                clubNameText.setText(clubName);
                clubNameText.setVisibility(View.VISIBLE);
            } else {
                clubNameText.setText("Loading club...");
                clubNameText.setVisibility(View.VISIBLE);
            }
            
            // Format and set date
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault());
            eventDateTime.setText(sdf.format(new Date(event.getDate())));
            
            // Load event image if available
            if (event.getImageUrl() != null && !event.getImageUrl().isEmpty()) {
                Glide.with(context)
                    .load(event.getImageUrl())
                    .centerCrop()
                    .into(eventImage);
                eventImage.setVisibility(View.VISIBLE);
            } else {
                eventImage.setVisibility(View.GONE);
            }
            
            // Update participant count
            int registered = event.getRegisteredCount();
            String countText = registered + " registered";
            if (event.getMaxParticipants() > 0) {
                countText += " / " + event.getMaxParticipants() + " max";
            }
            participantCount.setText(countText);
            
            // Update register button state
            boolean isRegistered = event.isRegistered(currentUserId);
            updateRegisterButton(event, isRegistered);
            
            registerButton.setOnClickListener(v -> handleRegistration(event));
        }
        
        private void updateRegisterButton(Event event, boolean isRegistered) {
            if (isRegistered) {
                registerButton.setText("Unregister");
                registerButton.setStrokeColor(context.getColorStateList(R.color.error));
                registerButton.setTextColor(context.getColor(R.color.error));
            } else {
                registerButton.setText("Register");
                registerButton.setStrokeColor(context.getColorStateList(R.color.primary));
                registerButton.setTextColor(context.getColor(R.color.primary));
            }
            
            registerButton.setEnabled(event.isRegistrationOpen());
        }
        
        private void handleRegistration(Event event) {
            boolean isRegistered = event.isRegistered(currentUserId);
            
            if (isRegistered) {
                // Unregister
                eventsRef.child(event.getEventId())
                    .child("registeredUsers")
                    .child(currentUserId)
                    .removeValue()
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(context, "Successfully unregistered", 
                            Toast.LENGTH_SHORT).show();
                        updateRegisterButton(event, false);
                    })
                    .addOnFailureListener(e -> 
                        Toast.makeText(context, "Failed to unregister: " + e.getMessage(), 
                            Toast.LENGTH_SHORT).show()
                    );
            } else {
                // Check if registration is still possible
                if (!event.canRegister()) {
                    Toast.makeText(context, "Registration is closed or event is full", 
                        Toast.LENGTH_SHORT).show();
                    return;
                }
                
                // Register
                eventsRef.child(event.getEventId())
                    .child("registeredUsers")
                    .child(currentUserId)
                    .setValue(true)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(context, "Successfully registered", 
                            Toast.LENGTH_SHORT).show();
                        updateRegisterButton(event, true);
                    })
                    .addOnFailureListener(e -> 
                        Toast.makeText(context, "Failed to register: " + e.getMessage(), 
                            Toast.LENGTH_SHORT).show()
                    );
            }
        }
    }
    
    class PastEventViewHolder extends RecyclerView.ViewHolder {
        private final MaterialCardView cardView;
        private final ImageView eventImage;
        private final TextView eventTitle;
        private final TextView eventDateTime;
        private final TextView eventVenue;
        private final TextView eventDescription;
        private final TextView participantCount;
        private final TextView clubNameText;
        private final MaterialButton registerButton;
        
        PastEventViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = (MaterialCardView) itemView;
            eventImage = itemView.findViewById(R.id.eventImage);
            eventTitle = itemView.findViewById(R.id.eventTitle);
            eventDateTime = itemView.findViewById(R.id.eventDateTime);
            eventVenue = itemView.findViewById(R.id.eventVenue);
            eventDescription = itemView.findViewById(R.id.eventDescription);
            participantCount = itemView.findViewById(R.id.participantCount);
            clubNameText = itemView.findViewById(R.id.clubName);
            registerButton = itemView.findViewById(R.id.registerButton);
            
            // Make past events look faded
            cardView.setCardElevation(1);
            itemView.setAlpha(0.8f);
        }
        
        void bind(Event event) {
            eventTitle.setText(event.getTitle());
            eventVenue.setText(event.getVenue());
            eventDescription.setText(event.getDescription());
            
            // Set club name
            String clubName = clubNames.get(event.getClubId());
            if (clubName != null) {
                clubNameText.setText(clubName);
                clubNameText.setVisibility(View.VISIBLE);
            } else {
                clubNameText.setText("Loading club...");
                clubNameText.setVisibility(View.VISIBLE);
            }
            
            // Format and set date
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault());
            eventDateTime.setText(sdf.format(new Date(event.getDate())));
            
            // Load event image if available
            if (event.getImageUrl() != null && !event.getImageUrl().isEmpty()) {
                Glide.with(context)
                    .load(event.getImageUrl())
                    .centerCrop()
                    .into(eventImage);
                eventImage.setVisibility(View.VISIBLE);
            } else {
                eventImage.setVisibility(View.GONE);
            }
            
            // Update participant count
            int registered = event.getRegisteredCount();
            String countText = registered + " attended";
            participantCount.setText(countText);
            
            // Disable register button for past events
            registerButton.setText("Event Ended");
            registerButton.setEnabled(false);
            registerButton.setAlpha(0.5f);
        }
    }
} 