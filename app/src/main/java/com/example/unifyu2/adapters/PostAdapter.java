package com.example.unifyu2.adapters;

import android.graphics.drawable.Drawable;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.example.unifyu2.R;
import com.example.unifyu2.models.Post;

import java.util.List;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostViewHolder> {
    private List<Post> posts;

    public PostAdapter(List<Post> posts) {
        this.posts = posts;
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_post, parent, false);
        return new PostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        Post post = posts.get(position);
        holder.bind(post);
    }

    @Override
    public int getItemCount() {
        return posts.size();
    }

    public void updatePosts(List<Post> newPosts) {
        this.posts = newPosts;
        notifyDataSetChanged();
    }

    static class PostViewHolder extends RecyclerView.ViewHolder {
        private TextView clubNameText;
        private TextView authorText;
        private TextView timestampText;
        private TextView contentText;
        private ImageView postImage;

        PostViewHolder(@NonNull View itemView) {
            super(itemView);
            clubNameText = itemView.findViewById(R.id.clubNameText);
            authorText = itemView.findViewById(R.id.authorText);
            timestampText = itemView.findViewById(R.id.timestampText);
            contentText = itemView.findViewById(R.id.contentText);
            postImage = itemView.findViewById(R.id.postImage);
        }

        void bind(Post post) {
            clubNameText.setText(post.getClubName());
            authorText.setText(post.getAuthorName());
            contentText.setText(post.getContent());
            
            // Format timestamp as relative time
            CharSequence relativeTime = DateUtils.getRelativeTimeSpanString(
                post.getTimestamp(),
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS
            );
            timestampText.setText(relativeTime);

            // Handle post image using Glide
            if (post.getImageUrl() != null && !post.getImageUrl().isEmpty()) {
                postImage.setVisibility(View.VISIBLE);
                Glide.with(itemView.getContext())
                    .load(post.getImageUrl())
                    .centerCrop()
                    .placeholder(R.drawable.ic_image_placeholder)
                    .error(R.drawable.ic_image_error)
                    .listener(new RequestListener<Drawable>() {
                        @Override
                        public boolean onLoadFailed(@Nullable GlideException e, Object model,
                                Target<Drawable> target, boolean isFirstResource) {
                            Log.e("PostAdapter", "Image load failed: " + post.getImageUrl(), e);
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(Drawable resource, Object model,
                                Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                            Log.d("PostAdapter", "Image loaded successfully: " + post.getImageUrl());
                            return false;
                        }
                    })
                    .into(postImage);
            } else {
                postImage.setVisibility(View.GONE);
            }
        }
    }
} 