package com.example.unifyu2.adapters;

import android.content.Context;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.unifyu2.R;
import com.example.unifyu2.models.Post;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostViewHolder> {
    private static final String TAG = "PostAdapter";
    private List<Post> posts;
    private Context context;
    private OnPostInteractionListener listener;
    private FirebaseAuth firebaseAuth;
    private DatabaseReference clubsRef;

    public interface OnPostInteractionListener {
        void onLikeClicked(Post post);
        void onReactionSelected(Post post, String reactionType);
        void onLinkClicked(String url);
        void onImageClicked(String imageUrl);
    }

    public PostAdapter(Context context, OnPostInteractionListener listener) {
        this.context = context;
        this.listener = listener;
        this.posts = new ArrayList<>();
        this.firebaseAuth = FirebaseAuth.getInstance();
        this.clubsRef = FirebaseDatabase.getInstance().getReference("clubs");
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_post, parent, false);
        return new PostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        try {
            Post post = posts.get(position);
            
            // Set basic post info with null checks
            holder.authorNameText.setText(post.getUserName() != null ? post.getUserName() : "Unknown User");
            
            // Handle content text
            String content = post.getContent();
            if (content != null && !content.trim().isEmpty()) {
                holder.contentText.setText(content);
                holder.contentText.setVisibility(View.VISIBLE);
            } else {
                holder.contentText.setVisibility(View.GONE);
            }
            
            // Set club name
            if (post.getClubName() != null && !post.getClubName().isEmpty()) {
                holder.clubNameText.setText(post.getClubName());
                holder.clubNameText.setVisibility(View.VISIBLE);
            } else if (post.getClubId() != null && !post.getClubId().isEmpty()) {
                // If club name is not available but club ID is, fetch the club name
                fetchClubName(post.getClubId(), holder.clubNameText);
            } else {
                holder.clubNameText.setVisibility(View.GONE);
            }
            
            // Set timestamp
            if (post.getTimestamp() instanceof Long) {
                CharSequence timeAgo = DateUtils.getRelativeTimeSpanString(
                    (Long) post.getTimestamp(),
                    System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS
                );
                holder.timestampText.setText(timeAgo);
            } else {
                holder.timestampText.setText("");
            }

            // Handle different post types
            String postType = post.getPostType();
            if (postType == null) postType = "TEXT";

            switch (postType) {
                case "IMAGE":
                    holder.postImage.setVisibility(View.VISIBLE);
                    holder.linkPreviewCard.setVisibility(View.GONE);
                    if (post.getImageUrl() != null) {
                        Glide.with(context)
                            .load(post.getImageUrl())
                            .into(holder.postImage);
                        holder.postImage.setOnClickListener(v -> 
                            listener.onImageClicked(post.getImageUrl()));
                    } else {
                        holder.postImage.setVisibility(View.GONE);
                    }
                    break;

                case "LINK":
                    holder.postImage.setVisibility(View.GONE);
                    if (post.getLinkUrl() != null) {
                        holder.linkPreviewCard.setVisibility(View.VISIBLE);
                        holder.linkTitleText.setText(post.getLinkTitle() != null ? post.getLinkTitle() : "");
                        holder.linkDescriptionText.setText(post.getLinkDescription() != null ? post.getLinkDescription() : "");
                        holder.linkUrlText.setText(post.getLinkUrl());
                        holder.linkPreviewCard.setOnClickListener(v -> 
                            listener.onLinkClicked(post.getLinkUrl()));
                    } else {
                        holder.linkPreviewCard.setVisibility(View.GONE);
                    }
                    break;

                default: // TEXT
                    holder.postImage.setVisibility(View.GONE);
                    holder.linkPreviewCard.setVisibility(View.GONE);
                    break;
            }

            // Update reaction UI
            updateReactionUI(holder, post);

            // Set click listeners
            holder.likeButton.setOnClickListener(v -> listener.onLikeClicked(post));
            holder.reactButton.setOnClickListener(v -> showReactionOptions(post));
        } catch (Exception e) {
            // Log error or show minimal UI
            holder.contentText.setText("Error loading post");
            holder.postImage.setVisibility(View.GONE);
            holder.linkPreviewCard.setVisibility(View.GONE);
        }
    }
    
    private void fetchClubName(String clubId, TextView clubNameText) {
        clubNameText.setText("Loading club...");
        clubNameText.setVisibility(View.VISIBLE);
        
        clubsRef.child(clubId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String clubName = snapshot.child("name").getValue(String.class);
                    if (clubName != null && !clubName.isEmpty()) {
                        clubNameText.setText(clubName);
                    } else {
                        clubNameText.setText("Unknown Club");
                    }
                } else {
                    clubNameText.setText("Unknown Club");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                clubNameText.setText("Unknown Club");
            }
        });
    }

    private void updateReactionUI(PostViewHolder holder, Post post) {
        try {
            String currentUserId = firebaseAuth.getCurrentUser().getUid();
            
            // Get reaction counts
            int totalReactions = 0;
            String userReaction = null;
            
            // Check if user has reacted
            if (post.getReactions() != null) {
                Map<String, String> userReactions = new HashMap<>();
                
                // Convert reactions to a more usable format
                for (Map.Entry<String, Object> entry : post.getReactions().entrySet()) {
                    if (entry.getKey().equals(currentUserId)) {
                        userReaction = entry.getValue().toString();
                    }
                    totalReactions++;
                }
            }
            
            // Update reaction count text
            holder.reactionCountText.setText(totalReactions + " reactions");
            
            // Update like button state based on user's reaction
            if (userReaction != null) {
                int colorRes = android.R.color.holo_blue_dark;
                int color = ContextCompat.getColor(context, colorRes);
                
                holder.likeButton.setTextColor(color);
                holder.likeButton.setIconTint(ContextCompat.getColorStateList(context, colorRes));
                
                // Update button text based on reaction type
                switch (userReaction) {
                    case "LIKE":
                        holder.likeButton.setText("Liked");
                        holder.reactButton.setText("👍");
                        break;
                    case "LOVE":
                        holder.likeButton.setText("Loved");
                        holder.reactButton.setText("❤️");
                        break;
                    case "HAHA":
                        holder.likeButton.setText("Haha");
                        holder.reactButton.setText("😂");
                        break;
                    case "WOW":
                        holder.likeButton.setText("Wow");
                        holder.reactButton.setText("😮");
                        break;
                    case "SAD":
                        holder.likeButton.setText("Sad");
                        holder.reactButton.setText("😢");
                        break;
                    case "ANGRY":
                        holder.likeButton.setText("Angry");
                        holder.reactButton.setText("😠");
                        break;
                    default:
                        holder.likeButton.setText("Like");
                        holder.reactButton.setText("😊");
                        break;
                }
            } else {
                // Reset to default state
                int colorRes = android.R.color.darker_gray;
                int color = ContextCompat.getColor(context, colorRes);
                
                holder.likeButton.setTextColor(color);
                holder.likeButton.setIconTint(ContextCompat.getColorStateList(context, colorRes));
                holder.likeButton.setText("Like");
                holder.reactButton.setText("😊");
            }
        } catch (Exception e) {
            // Set default state
            holder.reactionCountText.setText("0 reactions");
            holder.likeButton.setTextColor(ContextCompat.getColor(context, android.R.color.darker_gray));
            holder.likeButton.setIconTint(ContextCompat.getColorStateList(context, android.R.color.darker_gray));
            holder.likeButton.setText("Like");
            holder.reactButton.setText("😊");
        }
    }

    private void showReactionOptions(Post post) {
        try {
            String currentUserId = firebaseAuth.getCurrentUser().getUid();
            String currentReaction = null;
            
            // Find current reaction
            if (post.getReactions() != null && post.getReactions().containsKey(currentUserId)) {
                currentReaction = post.getReactions().get(currentUserId).toString();
            }
            
            // Determine next reaction
            String newReaction;
            if (currentReaction == null) newReaction = "LIKE";
            else if (currentReaction.equals("LIKE")) newReaction = "LOVE";
            else if (currentReaction.equals("LOVE")) newReaction = "HAHA";
            else if (currentReaction.equals("HAHA")) newReaction = "WOW";
            else if (currentReaction.equals("WOW")) newReaction = "SAD";
            else if (currentReaction.equals("SAD")) newReaction = "ANGRY";
            else if (currentReaction.equals("ANGRY")) newReaction = null; // Remove reaction
            else newReaction = "LIKE";
            
            // Show toast with the reaction
            if (newReaction != null) {
                Toast.makeText(context, "Reacted with: " + newReaction, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context, "Reaction removed", Toast.LENGTH_SHORT).show();
            }
            
            listener.onReactionSelected(post, newReaction);
        } catch (Exception e) {
            // Default to like
            listener.onReactionSelected(post, "LIKE");
        }
    }

    @Override
    public int getItemCount() {
        return posts.size();
    }

    public void setPosts(List<Post> posts) {
        this.posts = posts != null ? posts : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void addPost(Post post) {
        if (post != null) {
            posts.add(0, post);
            notifyItemInserted(0);
        }
    }

    public void updatePost(Post post) {
        if (post != null && post.getPostId() != null) {
            int index = -1;
            for (int i = 0; i < posts.size(); i++) {
                Post existingPost = posts.get(i);
                if (existingPost != null && 
                    existingPost.getPostId() != null && 
                    existingPost.getPostId().equals(post.getPostId())) {
                    index = i;
                    break;
                }
            }
            if (index != -1) {
                posts.set(index, post);
                notifyItemChanged(index);
            }
        }
    }

    static class PostViewHolder extends RecyclerView.ViewHolder {
        TextView clubNameText;
        TextView authorNameText;
        TextView timestampText;
        TextView contentText;
        ImageView postImage;
        MaterialCardView linkPreviewCard;
        TextView linkTitleText;
        TextView linkDescriptionText;
        TextView linkUrlText;
        TextView reactionCountText;
        MaterialButton likeButton;
        MaterialButton reactButton;

        PostViewHolder(@NonNull View itemView) {
            super(itemView);
            clubNameText = itemView.findViewById(R.id.clubNameText);
            authorNameText = itemView.findViewById(R.id.authorNameText);
            timestampText = itemView.findViewById(R.id.timestampText);
            contentText = itemView.findViewById(R.id.contentText);
            postImage = itemView.findViewById(R.id.postImage);
            linkPreviewCard = itemView.findViewById(R.id.linkPreviewCard);
            linkTitleText = itemView.findViewById(R.id.linkTitleText);
            linkDescriptionText = itemView.findViewById(R.id.linkDescriptionText);
            linkUrlText = itemView.findViewById(R.id.linkUrlText);
            reactionCountText = itemView.findViewById(R.id.reactionCountText);
            likeButton = itemView.findViewById(R.id.likeButton);
            reactButton = itemView.findViewById(R.id.reactButton);
        }
    }
} 