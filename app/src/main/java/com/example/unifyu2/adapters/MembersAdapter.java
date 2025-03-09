package com.example.unifyu2.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.unifyu2.R;
import com.example.unifyu2.models.User;
import com.google.android.material.chip.Chip;

import java.util.List;

public class MembersAdapter extends RecyclerView.Adapter<MembersAdapter.MemberViewHolder> {
    private final List<User> members;
    private final OnMemberClickListener listener;

    public interface OnMemberClickListener {
        void onMemberClick(User user);
    }

    public MembersAdapter(List<User> members, OnMemberClickListener listener) {
        this.members = members;
        this.listener = listener;
    }

    @NonNull
    @Override
    public MemberViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_member, parent, false);
        return new MemberViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MemberViewHolder holder, int position) {
        holder.bind(members.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return members.size();
    }

    static class MemberViewHolder extends RecyclerView.ViewHolder {
        private final TextView usernameText;
        private final TextView emailText;
        private final Chip makeAdminChip;

        MemberViewHolder(@NonNull View itemView) {
            super(itemView);
            usernameText = itemView.findViewById(R.id.usernameText);
            emailText = itemView.findViewById(R.id.emailText);
            makeAdminChip = itemView.findViewById(R.id.makeAdminChip);
        }

        void bind(User user, OnMemberClickListener listener) {
            usernameText.setText(user.getUsername());
            emailText.setText(user.getEmail());
            
            makeAdminChip.setOnClickListener(v -> listener.onMemberClick(user));
        }
    }
} 