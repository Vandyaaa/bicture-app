package com.example.imaginate;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.imaginate.models.Comment;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.CommentViewHolder> {

    private final ArrayList<Comment> commentList;
    private OnReplyClickListener replyClickListener;
    private final String albumId;
    private final String currentUserId;
    private final String albumOwnerId;

    public interface OnReplyClickListener {
        void onReplyClick(Comment comment);
    }
    public CommentAdapter(ArrayList<Comment> commentList, String albumId, String currentUserId, String albumOwnerId) {
        this.commentList = commentList;
        this.albumId = albumId;
        this.currentUserId = currentUserId;
        this.albumOwnerId = albumOwnerId;
    }

    public void setOnReplyClickListener(OnReplyClickListener listener) {
        this.replyClickListener = listener;
    }

    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_comment, parent, false);
        return new CommentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        Comment comment = commentList.get(position);

        if (comment == null || albumOwnerId == null || albumId == null) {
            return;
        }

        holder.usernameTextView.setText(comment.getUsername());
        holder.commentTextView.setText(comment.getText());


        if (comment.getPhotoprofile() != null && !comment.getPhotoprofile().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(comment.getPhotoprofile())
                    .placeholder(R.drawable.default_profile_bct_v1)
                    .error(R.drawable.default_profile_bct_v1)
                    .into(holder.profileImageView);
        } else {
            holder.profileImageView.setImageResource(R.drawable.default_profile_bct_v1);
        }


        holder.usernameTextView.setOnClickListener(v -> navigateToUserProfile(comment.getUserId(), holder));
        holder.profileImageView.setOnClickListener(v -> navigateToUserProfile(comment.getUserId(), holder));

        // Set click listener for reply button
        holder.replyTextView.setOnClickListener(v -> {
            if (replyClickListener != null) {
                replyClickListener.onReplyClick(comment);
            }
        });

        ArrayList<Comment> replyList = new ArrayList<>();
        String commentId = comment.getId(); // Simpan commentId untuk digunakan di ReplyAdapter
        ReplyAdapter replyAdapter = new ReplyAdapter(replyList, commentId, currentUserId, albumOwnerId, albumId);
        holder.repliesRecyclerView.setLayoutManager(new LinearLayoutManager(holder.itemView.getContext()));
        holder.repliesRecyclerView.setAdapter(replyAdapter);
        replyAdapter.setOnReplyClickListener(replyClickListener);


        // Initially hide replies
        holder.repliesRecyclerView.setVisibility(View.GONE);
        holder.expandCollapseText.setVisibility(View.GONE);

        if (commentId == null) {
            return;
        }

        // Load replies from Firebase
        DatabaseReference repliesRef = FirebaseDatabase.getInstance().getReference("users")
                .child(albumOwnerId) // Gunakan albumOwnerId yang sudah dipastikan tidak null
                .child("album")
                .child(albumId)
                .child("comments")
                .child(commentId)
                .child("replies");



        repliesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                replyList.clear();
                for (DataSnapshot replySnapshot : snapshot.getChildren()) {
                    String userId = replySnapshot.child("userId").getValue(String.class);
                    String username = replySnapshot.child("username").getValue(String.class);
                    String photoProfile = replySnapshot.child("photoprofile").getValue(String.class);
                    String text = replySnapshot.child("text").getValue(String.class);
                    Long timestamp = replySnapshot.child("timestamp").getValue(Long.class);

                    if (userId != null && username != null && text != null) {
                        Comment reply = new Comment(userId, username, text,
                                photoProfile != null ? photoProfile : "",
                                timestamp != null ? timestamp : 0L);
                        reply.setId(replySnapshot.getKey());
                        replyList.add(reply);
                    }
                }

                // Show/hide expand button based on replies count
                if (replyList.size() > 0) {
                    holder.expandCollapseText.setVisibility(View.VISIBLE);
                    holder.expandCollapseText.setText("⸻ Show " + replyList.size() + " replies");
                } else {
                    holder.expandCollapseText.setVisibility(View.GONE);
                }

                replyAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(holder.itemView.getContext(), "Failed to load replies", Toast.LENGTH_SHORT).show();
            }
        });


        // Handle expand/collapse
        holder.expandCollapseText.setOnClickListener(v -> {
            if (holder.repliesRecyclerView.getVisibility() == View.VISIBLE) {
                holder.repliesRecyclerView.setVisibility(View.GONE);
                holder.expandCollapseText.setText("⸻ Show " + replyList.size() + " replies");
            } else {
                holder.repliesRecyclerView.setVisibility(View.VISIBLE);
                holder.expandCollapseText.setText("⸻ Hide replies");
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (comment.getUserId().equals(currentUserId)) {
                // Inflate custom dialog layout
                View dialogView = LayoutInflater.from(holder.itemView.getContext())
                        .inflate(R.layout.delete_comment_dialog, null);

                AlertDialog.Builder builder = new AlertDialog.Builder(holder.itemView.getContext());
                builder.setView(dialogView);
                AlertDialog dialog = builder.create();

                // Set transparent background
                dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

                // Find views in custom layout
                TextView deleteButton = dialogView.findViewById(R.id.textView3);
                TextView cancelButton = dialogView.findViewById(R.id.textView4);

                // Set click listeners
                deleteButton.setOnClickListener(deleteView -> {
                    DatabaseReference commentRef = FirebaseDatabase.getInstance().getReference("users")
                            .child(albumOwnerId)
                            .child("album")
                            .child(albumId)
                            .child("comments")
                            .child(commentId);

                    commentRef.removeValue()
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(holder.itemView.getContext(),
                                        "Comment deleted", Toast.LENGTH_SHORT).show();
                                dialog.dismiss();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(holder.itemView.getContext(),
                                        "Failed to delete comment", Toast.LENGTH_SHORT).show();
                                dialog.dismiss();
                            });
                });

                cancelButton.setOnClickListener(cancelView -> dialog.dismiss());

                dialog.show();
            }
            return true;
        });
    }

    private void navigateToUserProfile(String userId, CommentViewHolder holder) {
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(userId);
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Bundle bundle = new Bundle();
                    bundle.putString("userId", userId);
                    bundle.putString("username", snapshot.child("username").getValue(String.class));
                    bundle.putString("photoprofile", snapshot.child("photoprofile").getValue(String.class));
                    bundle.putString("bio", snapshot.child("bio").getValue(String.class));
                    bundle.putString("location", snapshot.child("location").getValue(String.class));
                    bundle.putString("tagname", snapshot.child("tagname").getValue(String.class));
                    bundle.putLong("followers", snapshot.child("followers").getValue(Long.class) != null ?
                            snapshot.child("followers").getValue(Long.class) : 0);
                    bundle.putLong("following", snapshot.child("followed").getValue(Long.class) != null ?
                            snapshot.child("followed").getValue(Long.class) : 0);
                    bundle.putLong("post", snapshot.child("post").getValue(Long.class) != null ?
                            snapshot.child("post").getValue(Long.class) : 0);

                    ProfileShowFragment profileShowFragment = new ProfileShowFragment();
                    profileShowFragment.setArguments(bundle);

                    FragmentManager fragmentManager = ((FragmentActivity) holder.itemView.getContext()).getSupportFragmentManager();

                    // Get the current active fragment's tag
                    String currentFragmentTag = null;
                    Fragment currentFragment = fragmentManager.findFragmentById(R.id.frame_layout);
                    if (currentFragment != null && currentFragment.isVisible()) {
                        currentFragmentTag = currentFragment.getTag();
                    }

                    // Create a unique backstack name that includes the parent fragment tag
                    String backStackName = "ProfileShowFragment_" + currentFragmentTag;

                    FragmentTransaction transaction = fragmentManager.beginTransaction();

                    // Keep the current fragment visible, but add the profile fragment on top
                    transaction.add(R.id.frame_layout, profileShowFragment, "ProfileShowFragment");

                    // Add to back stack with the unique name
                    transaction.addToBackStack(backStackName);
                    transaction.commit();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(holder.itemView.getContext(), "Failed to load user profile", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public class ReplyAdapter extends RecyclerView.Adapter<ReplyAdapter.ReplyViewHolder> {
        private final ArrayList<Comment> replyList;
        private final String parentCommentId;
        private final String currentUserId;
        private final String albumOwnerId;
        private final String albumId;
        private OnReplyClickListener replyClickListener;

        public ReplyAdapter(ArrayList<Comment> replyList, String parentCommentId,
                            String currentUserId, String albumOwnerId, String albumId) {
            this.replyList = replyList;
            this.parentCommentId = parentCommentId;
            this.currentUserId = currentUserId;
            this.albumOwnerId = albumOwnerId;
            this.albumId = albumId;
        }

        public void setOnReplyClickListener(OnReplyClickListener listener) {
            this.replyClickListener = listener;
        }

        @NonNull
        @Override
        public ReplyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_reply, parent, false);
            return new ReplyViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ReplyViewHolder holder, int position) {
            Comment reply = replyList.get(position);

            holder.usernameTextView.setText(reply.getUsername());

            // Handle reply text and username being replied to
            String replyText = reply.getText();
            if (replyText.startsWith("@")) {
                int spaceIndex = replyText.indexOf(" ");
                if (spaceIndex != -1) {
                    // Extract username and remaining text
                    String username = replyText.substring(1, spaceIndex); // Remove @ and get username
                    String remainingText = replyText.substring(spaceIndex + 1);

                    // Set the replied username in tagUsernameReplyed TextView
                    String repliedText = username;
                    holder.tagUsernameReplyed.setText(repliedText);
                    holder.tagUsernameReplyed.setVisibility(View.VISIBLE);

                    // Set only the remaining text in commentTextView
                    holder.commentTextView.setText(remainingText);
                } else {
                    // If there's no space after @username, hide the tag and show full text
                    holder.tagUsernameReplyed.setVisibility(View.GONE);
                    holder.commentTextView.setText(replyText);
                }
            } else {
                // If there's no @username, hide the tag and show full text
                holder.tagUsernameReplyed.setVisibility(View.GONE);
                holder.commentTextView.setText(replyText);
            }

            if (reply.getPhotoprofile() != null && !reply.getPhotoprofile().isEmpty()) {
                Glide.with(holder.itemView.getContext())
                        .load(reply.getPhotoprofile())
                        .placeholder(R.drawable.default_profile_bct_v1)
                        .error(R.drawable.default_profile_bct_v1)
                        .into(holder.profileImageView);
            } else {
                holder.profileImageView.setImageResource(R.drawable.default_profile_bct_v1);
            }


            holder.usernameTextView.setOnClickListener(v -> navigateToUserProfile(reply.getUserId(), holder));
            holder.profileImageView.setOnClickListener(v -> navigateToUserProfile(reply.getUserId(), holder));

            // Add reply button click listener
            holder.replyButton.setOnClickListener(v -> {
                if (replyClickListener != null) {
                    reply.setParentCommentId(parentCommentId);
                    replyClickListener.onReplyClick(reply);
                }
            });

            // Long press to delete functionality remains the same
            holder.itemView.setOnLongClickListener(v -> {
                if (reply.getUserId().equals(currentUserId)) {
                    View dialogView = LayoutInflater.from(holder.itemView.getContext())
                            .inflate(R.layout.delete_comment_dialog, null);

                    AlertDialog.Builder builder = new AlertDialog.Builder(holder.itemView.getContext());
                    builder.setView(dialogView);
                    AlertDialog dialog = builder.create();
                    dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

                    TextView deleteButton = dialogView.findViewById(R.id.textView3);
                    TextView cancelButton = dialogView.findViewById(R.id.textView4);

                    deleteButton.setOnClickListener(deleteView -> {
                        DatabaseReference replyRef = FirebaseDatabase.getInstance().getReference("users")
                                .child(albumOwnerId)
                                .child("album")
                                .child(albumId)
                                .child("comments")
                                .child(parentCommentId)
                                .child("replies")
                                .child(reply.getId());

                        replyRef.removeValue()
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(holder.itemView.getContext(),
                                            "Reply deleted", Toast.LENGTH_SHORT).show();
                                    dialog.dismiss();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(holder.itemView.getContext(),
                                            "Failed to delete reply", Toast.LENGTH_SHORT).show();
                                    dialog.dismiss();
                                });
                    });

                    cancelButton.setOnClickListener(cancelView -> dialog.dismiss());
                    dialog.show();
                }
                return true;
            });
        }

        @Override
        public int getItemCount() {
            return replyList.size();
        }

        public class ReplyViewHolder extends RecyclerView.ViewHolder {
            ImageView profileImageView;
            TextView usernameTextView, commentTextView, tagUsernameReplyed;
            TextView replyButton;

            public ReplyViewHolder(View itemView) {
                super(itemView);
                profileImageView = itemView.findViewById(R.id.replyProfileImage);
                usernameTextView = itemView.findViewById(R.id.replyUsername);
                commentTextView = itemView.findViewById(R.id.replyText);
                replyButton = itemView.findViewById(R.id.balas2);
                tagUsernameReplyed = itemView.findViewById(R.id.tagUsernameReplyed);
            }
        }

        private void navigateToUserProfile(String userId, ReplyViewHolder holder) {
            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(userId);
            userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        Bundle bundle = new Bundle();
                        bundle.putString("userId", userId);
                        bundle.putString("username", snapshot.child("username").getValue(String.class));
                        bundle.putString("photoprofile", snapshot.child("photoprofile").getValue(String.class));
                        bundle.putString("bio", snapshot.child("bio").getValue(String.class));
                        bundle.putString("location", snapshot.child("location").getValue(String.class));
                        bundle.putString("tagname", snapshot.child("tagname").getValue(String.class));
                        bundle.putLong("followers", snapshot.child("followers").getValue(Long.class) != null ?
                                snapshot.child("followers").getValue(Long.class) : 0);
                        bundle.putLong("following", snapshot.child("followed").getValue(Long.class) != null ?
                                snapshot.child("followed").getValue(Long.class) : 0);
                        bundle.putLong("post", snapshot.child("post").getValue(Long.class) != null ?
                                snapshot.child("post").getValue(Long.class) : 0);

                        ProfileShowFragment profileShowFragment = new ProfileShowFragment();
                        profileShowFragment.setArguments(bundle);

                        FragmentManager fragmentManager = ((FragmentActivity) holder.itemView.getContext()).getSupportFragmentManager();

                        // Get the current active fragment's tag
                        String currentFragmentTag = null;
                        Fragment currentFragment = fragmentManager.findFragmentById(R.id.frame_layout);
                        if (currentFragment != null && currentFragment.isVisible()) {
                            currentFragmentTag = currentFragment.getTag();
                        }

                        // Create a unique backstack name that includes the parent fragment tag
                        String backStackName = "ProfileShowFragment_" + currentFragmentTag;

                        FragmentTransaction transaction = fragmentManager.beginTransaction();

                        // Keep the current fragment visible, but add the profile fragment on top
                        transaction.add(R.id.frame_layout, profileShowFragment, "ProfileShowFragment");

                        // Add to back stack with the unique name
                        transaction.addToBackStack(backStackName);
                        transaction.commit();

                        if (holder.itemView.getContext() instanceof FragmentActivity) {
                            FragmentActivity activity = (FragmentActivity) holder.itemView.getContext();
                            Fragment bottomSheetFragment = activity.getSupportFragmentManager().findFragmentByTag("BottomSheetDialog");
                            if (bottomSheetFragment instanceof BottomSheetDialogFragment) {
                                BottomSheetDialogFragment bottomSheetDialogFragment = (BottomSheetDialogFragment) bottomSheetFragment;
                                if (bottomSheetDialogFragment.getDialog() != null) {
                                    bottomSheetDialogFragment.getDialog().dismiss();
                                }
                            }
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(holder.itemView.getContext(), "Failed to load user profile", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }



    @Override
    public int getItemCount() {
        return commentList.size();
    }

    public static class CommentViewHolder extends RecyclerView.ViewHolder {
        ImageView profileImageView;
        TextView usernameTextView, commentTextView, replyTextView, expandCollapseText;
        RecyclerView repliesRecyclerView;
        LinearLayout containerReplies;

        public CommentViewHolder(View itemView) {
            super(itemView);
            profileImageView = itemView.findViewById(R.id.photoProfileComment);
            usernameTextView = itemView.findViewById(R.id.commentUsername);
            commentTextView = itemView.findViewById(R.id.commentText);
            replyTextView = itemView.findViewById(R.id.balas);
            repliesRecyclerView = itemView.findViewById(R.id.replies);
            containerReplies = itemView.findViewById(R.id.containerReplies);
            expandCollapseText = itemView.findViewById(R.id.expandAndcCollapse);
        }
    }
}