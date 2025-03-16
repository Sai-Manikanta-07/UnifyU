package com.example.unifyu2.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.unifyu2.R;
import com.example.unifyu2.models.User;
import java.util.List;

public class ManageClubMembersAdapter extends RecyclerView.Adapter<ManageClubMembersAdapter.ViewHolder> {
    private List<User> members;
    private Context context;
    private OnMemberActionListener actionListener;

    public interface OnMemberActionListener {
        void onMakeAdmin(User member);
        void onRemoveMember(User member);
    }

    public ManageClubMembersAdapter(Context context, List<User> members, OnMemberActionListener listener) {
        this.context = context;
        this.members = members;
        this.actionListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_manage_club_member, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        User member = members.get(position);
        
        holder.memberName.setText(member.getUsername());
        holder.memberEmail.setText(member.getEmail());
        
        holder.makeAdminButton.setOnClickListener(v -> {
            if (actionListener != null) {
                actionListener.onMakeAdmin(member);
            }
        });
        
        holder.removeButton.setOnClickListener(v -> {
            if (actionListener != null) {
                actionListener.onRemoveMember(member);
            }
        });
    }

    @Override
    public int getItemCount() {
        return members.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView memberName;
        TextView memberEmail;
        Button makeAdminButton;
        Button removeButton;

        ViewHolder(View itemView) {
            super(itemView);
            memberName = itemView.findViewById(R.id.memberName);
            memberEmail = itemView.findViewById(R.id.memberEmail);
            makeAdminButton = itemView.findViewById(R.id.makeAdminButton);
            removeButton = itemView.findViewById(R.id.removeButton);
        }
    }

    public void updateMembers(List<User> newMembers) {
        this.members = newMembers;
        notifyDataSetChanged();
    }
} 