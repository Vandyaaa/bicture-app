package com.example.imaginate;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.imaginate.models.Comment;

import java.util.ArrayList;

public class ReplyAdapter extends RecyclerView.Adapter<ReplyAdapter.ReplyViewHolder> {
    private final ArrayList<Comment> replyList;

    public ReplyAdapter(ArrayList<Comment> replyList) {
        this.replyList = replyList;
    }

    @NonNull
    @Override
    public ReplyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_comment, parent, false);
        return new ReplyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReplyViewHolder holder, int position) {
        Comment reply = replyList.get(position);

        holder.usernameTextView.setText(reply.getUsername());
        holder.commentTextView.setText(reply.getText());

        if (reply.getPhotoprofile() != null && !reply.getPhotoprofile().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(reply.getPhotoprofile())
                    .placeholder(R.drawable.profiledefault)
                    .error(R.drawable.profiledefault)
                    .into(holder.profileImageView);
        } else {
            holder.profileImageView.setImageResource(R.drawable.profiledefault);
        }

        // Hide reply elements for replies
        holder.replyTextView.setVisibility(View.GONE);
        holder.repliesRecyclerView.setVisibility(View.GONE);
        holder.expandCollapseText.setVisibility(View.GONE);
    }

    @Override
    public int getItemCount() {
        return replyList.size();
    }

    public static class ReplyViewHolder extends RecyclerView.ViewHolder {
        ImageView profileImageView;
        TextView usernameTextView, commentTextView, replyTextView, expandCollapseText;
        RecyclerView repliesRecyclerView;

        public ReplyViewHolder(View itemView) {
            super(itemView);
            profileImageView = itemView.findViewById(R.id.photoProfileComment);
            usernameTextView = itemView.findViewById(R.id.commentUsername);
            commentTextView = itemView.findViewById(R.id.commentText);
            replyTextView = itemView.findViewById(R.id.balas);
            repliesRecyclerView = itemView.findViewById(R.id.replies);
            expandCollapseText = itemView.findViewById(R.id.expandAndcCollapse);
        }
    }
}