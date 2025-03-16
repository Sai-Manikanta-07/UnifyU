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
import com.example.unifyu2.models.Event;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class EventAdapter extends RecyclerView.Adapter<EventAdapter.EventViewHolder> {
    private final Context context;
    private final List<Event> events;
    private final String currentUserId;
    private final DatabaseReference eventsRef;

    public EventAdapter(Context context, List<Event> events) {
        this.context = context;
        this.events = events;
        this.currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        this.eventsRef = FirebaseDatabase.getInstance().getReference("events");
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_event, parent, false);
        return new EventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        Event event = events.get(position);
        holder.bind(event);
    }

    @Override
    public int getItemCount() {
        return events.size();
    }

    public void updateEvents(List<Event> newEvents) {
        events.clear();
        events.addAll(newEvents);
        notifyDataSetChanged();
    }

    class EventViewHolder extends RecyclerView.ViewHolder {
        private final ImageView eventImage;
        private final TextView eventTitle;
        private final TextView eventDateTime;
        private final TextView eventVenue;
        private final TextView eventDescription;
        private final TextView participantCount;
        private final MaterialButton registerButton;

        public EventViewHolder(@NonNull View itemView) {
            super(itemView);
            eventImage = itemView.findViewById(R.id.eventImage);
            eventTitle = itemView.findViewById(R.id.eventTitle);
            eventDateTime = itemView.findViewById(R.id.eventDateTime);
            eventVenue = itemView.findViewById(R.id.eventVenue);
            eventDescription = itemView.findViewById(R.id.eventDescription);
            participantCount = itemView.findViewById(R.id.participantCount);
            registerButton = itemView.findViewById(R.id.registerButton);
        }

        void bind(Event event) {
            eventTitle.setText(event.getTitle());
            eventVenue.setText(event.getVenue());
            eventDescription.setText(event.getDescription());

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
} 