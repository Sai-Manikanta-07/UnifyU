package com.example.unifyu2.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.PopupMenu;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.unifyu2.R;
import com.example.unifyu2.models.User;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.List;

public class MembersAdapter extends RecyclerView.Adapter<MembersAdapter.MemberViewHolder> {
    private List<User> members;
    private String clubId;
    private OnMemberActionListener listener;
    private String currentAdminId;

    public interface OnMemberActionListener {
        void onMemberRemoved(User user);
        void onMakeAdmin(User user);
    }

    public MembersAdapter(List<User> members, String clubId, String currentAdminId, 
                         OnMemberActionListener listener) {
        this.members = members;
        this.clubId = clubId;
        this.listener = listener;
        this.currentAdminId = currentAdminId;
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
        User user = members.get(position);
        holder.bind(user);
    }

    @Override
    public int getItemCount() {
        return members.size();
    }

    class MemberViewHolder extends RecyclerView.ViewHolder {
        private TextView usernameText;
        private TextView emailText;
        private MaterialButton removeButton;
        private DatabaseReference membershipsRef;
        private View adminBadge;

        MemberViewHolder(@NonNull View itemView) {
            super(itemView);
            usernameText = itemView.findViewById(R.id.usernameText);
            emailText = itemView.findViewById(R.id.emailText);
            removeButton = itemView.findViewById(R.id.removeButton);
            membershipsRef = FirebaseDatabase.getInstance().getReference("memberships");
            adminBadge = itemView.findViewById(R.id.adminBadge);
        }

        void bind(User user) {
            usernameText.setText(user.getUsername());
            emailText.setText(user.getEmail());

            // Show admin badge if user is admin
            boolean isAdmin = user.getId().equals(currentAdminId);
            adminBadge.setVisibility(isAdmin ? View.VISIBLE : View.GONE);

            itemView.setOnLongClickListener(v -> {
                if (!isAdmin) {
                    showOptionsMenu(v, user);
                }
                return true;
            });
        }

        private void showOptionsMenu(View view, User user) {
            PopupMenu popup = new PopupMenu(view.getContext(), view);
            popup.inflate(R.menu.menu_member_options);
            popup.setOnMenuItemClickListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.action_make_admin) {
                    listener.onMakeAdmin(user);
                    return true;
                } else if (itemId == R.id.action_remove_member) {
                    listener.onMemberRemoved(user);
                    return true;
                }
                return false;
            });
            popup.show();
        }
    }
} 