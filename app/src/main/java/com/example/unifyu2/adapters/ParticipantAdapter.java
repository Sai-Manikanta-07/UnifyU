package com.example.unifyu2.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.unifyu2.R;
import com.example.unifyu2.models.User;

import java.util.List;
import java.util.Map;

public class ParticipantAdapter extends RecyclerView.Adapter<ParticipantAdapter.ViewHolder> {
    private List<User> participants;
    private Map<String, String> phoneNumbers; // Map of user IDs to phone numbers

    public ParticipantAdapter(List<User> participants) {
        this.participants = participants;
    }
    
    public ParticipantAdapter(List<User> participants, Map<String, String> phoneNumbers) {
        this.participants = participants;
        this.phoneNumbers = phoneNumbers;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_participant, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        User participant = participants.get(position);
        String phoneNumber = phoneNumbers != null ? phoneNumbers.get(participant.getId()) : null;
        holder.bind(participant, phoneNumber);
    }

    @Override
    public int getItemCount() {
        return participants.size();
    }

    public void updateParticipants(List<User> newParticipants) {
        this.participants = newParticipants;
        notifyDataSetChanged();
    }
    
    public void updatePhoneNumbers(Map<String, String> phoneNumbers) {
        this.phoneNumbers = phoneNumbers;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView nameText;
        private final TextView emailText;
        private final TextView phoneText;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.participantName);
            emailText = itemView.findViewById(R.id.participantEmail);
            phoneText = itemView.findViewById(R.id.participantPhone);
        }

        void bind(User participant, String phoneNumber) {
            nameText.setText(participant.getUsername());
            emailText.setText(participant.getEmail());
            
            if (phoneNumber != null && !phoneNumber.isEmpty()) {
                phoneText.setText(phoneNumber);
                phoneText.setVisibility(View.VISIBLE);
            } else {
                phoneText.setVisibility(View.GONE);
            }
        }
    }
} 