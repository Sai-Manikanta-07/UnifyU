package com.example.unifyu2.adapters;

import android.content.Context;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.unifyu2.R;
import com.example.unifyu2.models.Post;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostViewHolder> {
    private List<Post> posts;
    private Context context;
    private OnPostInteractionListener listener;
    private FirebaseAuth firebaseAuth;

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
            holder.contentText.setText(post.getContent() != null ? post.getContent() : "");
            
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

    private void updateReactionUI(PostViewHolder holder, Post post) {
        try {
            String currentUserId = firebaseAuth.getCurrentUser().getUid();
            Map<String, Integer> reactions = post.getReactions();
            Map<String, List<String>> reactedUsers = post.getReactedUsers();
            
            // Update reaction count
            int totalReactions = post.getTotalReactions();
            holder.reactionCountText.setText(totalReactions + " reactions");
            
            // Check if user has reacted
            boolean hasReacted = false;
            String userReaction = null;
            
            // Check in reactedUsers first
            if (reactedUsers != null) {
                for (Map.Entry<String, List<String>> entry : reactedUsers.entrySet()) {
                    if (entry.getValue().contains(currentUserId)) {
                        hasReacted = true;
                        userReaction = entry.getKey();
                        break;
                    }
                }
            }
            
            // If not found in reactedUsers, check reactions count
            if (!hasReacted && reactions != null) {
                for (Map.Entry<String, Integer> entry : reactions.entrySet()) {
                    if (entry.getValue() > 0) {
                        hasReacted = true;
                        userReaction = entry.getKey();
                        break;
                    }
                }
            }
            
            // Update like button state
            boolean isLiked = "LIKE".equals(userReaction);
            int colorRes = isLiked ? android.R.color.holo_blue_dark : android.R.color.darker_gray;
            int color = ContextCompat.getColor(context, colorRes);
            
            holder.likeButton.setTextColor(color);
            holder.likeButton.setIconTint(ContextCompat.getColorStateList(context, colorRes));
        } catch (Exception e) {
            // Set default state
            holder.reactionCountText.setText("0 reactions");
            holder.likeButton.setTextColor(ContextCompat.getColor(context, android.R.color.darker_gray));
            holder.likeButton.setIconTint(ContextCompat.getColorStateList(context, android.R.color.darker_gray));
        }
    }

    private void showReactionOptions(Post post) {
        try {
            String currentUserId = firebaseAuth.getCurrentUser().getUid();
            Map<String, List<String>> reactedUsers = post.getReactedUsers();
            
            // Find current reaction
            String currentReaction = null;
            if (reactedUsers != null) {
                for (Map.Entry<String, List<String>> entry : reactedUsers.entrySet()) {
                    if (entry.getValue().contains(currentUserId)) {
                        currentReaction = entry.getKey();
                        break;
                    }
                }
            }
            
            // If no reaction found in reactedUsers, check reactions map
            if (currentReaction == null) {
                Map<String, Integer> reactions = post.getReactions();
                if (reactions != null) {
                    for (Map.Entry<String, Integer> entry : reactions.entrySet()) {
                        if (entry.getValue() > 0) {
                            currentReaction = entry.getKey();
                            break;
                        }
                    }
                }
            }
            
            // Determine next reaction
            String newReaction;
            if (currentReaction == null) newReaction = "LIKE";
            else if (currentReaction.equals("LIKE")) newReaction = "LOVE";
            else if (currentReaction.equals("LOVE")) newReaction = "HAHA";
            else if (currentReaction.equals("HAHA")) newReaction = "WOW";
            else if (currentReaction.equals("WOW")) newReaction = "SAD";
            else if (currentReaction.equals("SAD")) newReaction = "ANGRY";
            else newReaction = null;
            
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