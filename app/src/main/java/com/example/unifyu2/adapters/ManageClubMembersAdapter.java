package com.example.unifyu2.adapters;

import android.content.Context;
import android.util.Log;
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
    private static final String TAG = "MembersAdapter";
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
        Log.d(TAG, "Adapter created with " + (members != null ? members.size() : 0) + " members");
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Log.d(TAG, "Creating new ViewHolder");
        View view = LayoutInflater.from(context).inflate(R.layout.item_manage_club_member, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (members == null || position >= members.size()) {
            Log.e(TAG, "Invalid position or empty list: " + position);
            return;
        }
        
        User member = members.get(position);
        if (member == null) {
            Log.e(TAG, "Null member at position: " + position);
            return;
        }
        
        Log.d(TAG, "Binding member at position " + position + ": " + member.getUsername());
        
        // Set the member name, with a fallback if null
        String memberName = member.getUsername();
        if (memberName == null || memberName.isEmpty()) {
            memberName = "Unknown User";
            Log.w(TAG, "Member at position " + position + " has no username");
        }
        holder.memberName.setText(memberName);
        
        // Set the email, with a fallback if null
        String memberEmail = member.getEmail();
        if (memberEmail == null || memberEmail.isEmpty()) {
            memberEmail = "No email provided";
            Log.w(TAG, "Member at position " + position + " has no email");
        }
        holder.memberEmail.setText(memberEmail);
        
        // Setup click listeners
        holder.makeAdminButton.setOnClickListener(v -> {
            if (actionListener != null) {
                Log.d(TAG, "Make admin clicked for member: " + member.getUsername());
                actionListener.onMakeAdmin(member);
            }
        });
        
        holder.removeButton.setOnClickListener(v -> {
            if (actionListener != null) {
                Log.d(TAG, "Remove clicked for member: " + member.getUsername());
                actionListener.onRemoveMember(member);
            }
        });
    }

    @Override
    public int getItemCount() {
        int count = members != null ? members.size() : 0;
        Log.d(TAG, "getItemCount called, returning: " + count);
        return count;
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
        Log.d(TAG, "Updating members list with " + (newMembers != null ? newMembers.size() : 0) + " members");
        this.members = newMembers;
        notifyDataSetChanged();
    }
} 