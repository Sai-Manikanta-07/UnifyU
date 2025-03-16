package com.example.unifyu2.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.unifyu2.R;
import com.example.unifyu2.models.User;
import java.util.List;

public class ClubMembersAdapter extends RecyclerView.Adapter<ClubMembersAdapter.ViewHolder> {
    private List<User> members;
    private Context context;
    private boolean isAdmin = false;
    private OnMemberActionListener actionListener;

    public interface OnMemberActionListener {
        void onRemoveMember(String memberId);
        void onMakeAdmin(String memberId);
    }

    public ClubMembersAdapter(Context context, List<User> members, OnMemberActionListener listener) {
        this.context = context;
        this.members = members;
        this.actionListener = listener;
    }

    public void setAdminStatus(boolean isAdmin) {
        this.isAdmin = isAdmin;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_club_member, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        User member = members.get(position);
        holder.memberName.setText(member.getUsername());
        
        if (isAdmin) {
            holder.menuButton.setVisibility(View.VISIBLE);
            holder.menuButton.setOnClickListener(v -> showPopupMenu(v, member));
        } else {
            holder.menuButton.setVisibility(View.GONE);
        }
    }

    private void showPopupMenu(View view, User member) {
        PopupMenu popup = new PopupMenu(context, view);
        popup.getMenu().add(0, 1, 0, "Make Admin");
        popup.getMenu().add(0, 2, 0, "Remove Member");

        popup.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case 1:
                    if (actionListener != null) {
                        actionListener.onMakeAdmin(member.getId());
                    }
                    return true;
                case 2:
                    if (actionListener != null) {
                        actionListener.onRemoveMember(member.getId());
                    }
                    return true;
                default:
                    return false;
            }
        });

        popup.show();
    }

    @Override
    public int getItemCount() {
        return members.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView memberName;
        ImageButton menuButton;

        ViewHolder(View itemView) {
            super(itemView);
            memberName = itemView.findViewById(R.id.memberName);
            menuButton = itemView.findViewById(R.id.menuButton);
        }
    }
} 