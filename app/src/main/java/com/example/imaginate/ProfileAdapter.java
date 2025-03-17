package com.example.imaginate;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.cache.InternalCacheDiskCacheFactory;
import com.bumptech.glide.load.engine.cache.LruResourceCache;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.example.imaginate.models.Album;
import com.example.imaginate.models.BookmarkData;
import com.example.imaginate.models.CollageItem;
import com.example.imaginate.models.Comment;
import com.example.imaginate.models.LikeData;
import com.example.imaginate.models.ProfileData;
import com.example.imaginate.models.User;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ProfileAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ALBUM = 1;

    private final Context context;
    private final ArrayList<Album> albums;
    private static String userId;
    private final String currentUserId;
    private ProfileData profileData;
    private final DatabaseReference userRef;
    private final DatabaseReference currentUserRef;
    private ValueEventListener profileListener;
    private Map<String, Boolean> likeStatusCache = new HashMap<>();
    private Map<String, Boolean> followStatusCache = new HashMap<>();
    private Map<String, Boolean> bookmarkStatusCache = new HashMap<>();

    public ProfileAdapter(Context context, ArrayList<Album> albums, String userId,
                          String currentUserId, ProfileData profileData) {
        this.context = context;
        this.albums = albums;
        this.userId = userId;
        this.currentUserId = currentUserId;
        this.profileData = profileData;
        this.userRef = FirebaseDatabase.getInstance().getReference("users").child(userId);
        this.currentUserRef = FirebaseDatabase.getInstance().getReference("users").child(currentUserId);

        // Set up real-time listener for profile data
        setupProfileListener();
    }

    private void bindAlbumData(AlbumViewHolder holder, Album album) {
        holder.albumTitle.setText(album.getTitle());
        holder.albumDescription.setText(album.getDescription());
        holder.date.setText(album.getUploadDate());
        holder.likeCount.setText(formatLikesCount(album.getLikes()));
        holder.followButton.setVisibility(View.GONE);
        // Set album image
        if (album.getUrl() != null && !album.getUrl().isEmpty()) {
            Glide.with(context)
                    .load(album.getUrl())
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(R.drawable.borderradiussmall)
                    .override(1000, 1000)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .into(holder.albumImage);

            holder.albumImage.setOnClickListener(v -> {
                Intent intent = new Intent(holder.itemView.getContext(), FullscreenImageActivity.class);
                intent.putExtra("imageUrl", album.getUrl());
                intent.putExtra("username", album.getUsername());
                intent.putExtra("imageTitle", album.getTitle());
                intent.putExtra("userId", album.getUserId());
                intent.putExtra("imageDescription", album.getDescription());

                // Start activity without default transition animation
                holder.itemView.getContext().startActivity(intent);
                ((Activity) holder.itemView.getContext()).overridePendingTransition(0, 0);
            });
        }

        // Load user data for the album
        DatabaseReference albumUserRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(album.getUserId());

        albumUserRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    // Get username and photo profile from user data
                    String username = snapshot.child("username").getValue(String.class);
                    String photoProfile = snapshot.child("photoprofile").getValue(String.class);
                    String tagname = snapshot.child("tagName").getValue(String.class);
                    // Set username
                    if (username != null) {
                        holder.albumUsername.setText(username);
                    }

                    // Set photo profile
                    if (photoProfile != null && !photoProfile.isEmpty()) {
                        Glide.with(context)
                                .load(photoProfile)
                                .diskCacheStrategy(DiskCacheStrategy.ALL)
                                .placeholder(R.drawable.blacksmooth2_drawable)
                                .override(100, 100)
                                .circleCrop()
                                .transition(DrawableTransitionOptions.withCrossFade())
                                .into(holder.albumPhotoprofile);
                    } else {
                        // Set default profile picture if no photo profile exists
                        holder.albumPhotoprofile.setImageResource(R.drawable.default_profile_bct_v1);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(context,
                        "Failed to load user data", Toast.LENGTH_SHORT).show();
            }
        });

        handleLikeAndComments(album, holder);
    }
    public class MyApplication extends Application {
        @Override
        public void onCreate() {
            super.onCreate();

            // Configure Glide for app-wide caching
            GlideBuilder builder = new GlideBuilder()
                    .setMemoryCache(new LruResourceCache(30 * 1024 * 1024))
                    .setDiskCache(new InternalCacheDiskCacheFactory(this, 250 * 1024 * 1024));

            Glide.init(this, builder);
        }
    }
    @SuppressLint("ResourceType")
    private void handleLikeAndComments(Album album, AlbumViewHolder holder) {
        // ... other existing code ...
        if (album.isUserHasLiked()) {
            holder.likeButton.setImageResource(R.drawable.heart_24filled);
        } else {
            holder.likeButton.setImageResource(R.drawable.heart_24);
        }

        Boolean hasLiked = likeStatusCache.get(album.getId());
        if (hasLiked != null) {
            holder.likeButton.setImageResource(hasLiked ? R.drawable.heart_24filled : R.drawable.heart_24);
        } else {
            // Fetch like status from database if not in cache
            DatabaseReference albumRef = FirebaseDatabase.getInstance().getReference("users")
                    .child(album.getUserId())
                    .child("album")
                    .child(album.getId());
            albumRef.child("userLikes").orderByChild("userId").equalTo(currentUserId).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    boolean hasLiked = snapshot.exists();
                    likeStatusCache.put(album.getId(), hasLiked);
                    holder.likeButton.setImageResource(hasLiked ? R.drawable.heart_24filled : R.drawable.heart_24);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(holder.itemView.getContext(), "Failed to load like status", Toast.LENGTH_SHORT).show();
                }
            });
        }

        holder.likeButton.setOnClickListener(v -> {
            DatabaseReference albumRef = FirebaseDatabase.getInstance().getReference("users")
                    .child(album.getUserId())
                    .child("album")
                    .child(album.getId());
            DatabaseReference userLikesRef = albumRef.child("userLikes");

            // Play animation
            Animation likeAnimation = AnimationUtils.loadAnimation(holder.itemView.getContext(), R.anim.like_button_animation);
            holder.likeButton.startAnimation(likeAnimation);

            // Get current user's data
            DatabaseReference currentUserRef = FirebaseDatabase.getInstance().getReference("users")
                    .child(currentUserId);

            currentUserRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot userSnapshot) {
                    String currentUsername = userSnapshot.child("username").getValue(String.class);
                    String currentPhotoProfile = userSnapshot.child("photoprofile").getValue(String.class);
                    String currentTagName = userSnapshot.child("tagName").getValue(String.class);

                    userLikesRef.orderByChild("userId").equalTo(currentUserId)
                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    if (snapshot.exists()) {
                                        // Unlike animation
                                        ObjectAnimator scaleDownX = ObjectAnimator.ofFloat(holder.likeButton, "scaleX", 1f, 0.8f, 1f);
                                        ObjectAnimator scaleDownY = ObjectAnimator.ofFloat(holder.likeButton, "scaleY", 1f, 0.8f, 1f);
                                        scaleDownX.setDuration(300);
                                        scaleDownY.setDuration(300);
                                        AnimatorSet scaleDown = new AnimatorSet();
                                        scaleDown.play(scaleDownX).with(scaleDownY);
                                        scaleDown.start();

                                        // Remove like
                                        for (DataSnapshot likeSnapshot : snapshot.getChildren()) {
                                            likeSnapshot.getRef().removeValue();
                                        }
                                        albumRef.child("likes").setValue(album.getLikes() - 1);
                                        album.setLikes(album.getLikes() - 1);
                                        album.setUserHasLiked(false);
                                        holder.likeButton.setImageResource(R.drawable.heart_24);
                                        likeStatusCache.put(album.getId(), false);
                                    } else {
                                        // Like animation
                                        ObjectAnimator scaleUpX = ObjectAnimator.ofFloat(holder.likeButton, "scaleX", 1f, 1.2f, 1f);
                                        ObjectAnimator scaleUpY = ObjectAnimator.ofFloat(holder.likeButton, "scaleY", 1f, 1.2f, 1f);
                                        scaleUpX.setDuration(300);
                                        scaleUpY.setDuration(300);
                                        AnimatorSet scaleUp = new AnimatorSet();
                                        scaleUp.play(scaleUpX).with(scaleUpY);
                                        scaleUp.start();

                                        // Add like
                                        String likeId = userLikesRef.push().getKey();
                                        if (likeId != null) {
                                            LikeData likeData = new LikeData(
                                                    currentUserId,
                                                    currentUsername,
                                                    currentPhotoProfile != null ? currentPhotoProfile : "",
                                                    currentTagName != null ? currentTagName : ""
                                            );
                                            userLikesRef.child(likeId).setValue(likeData);
                                            albumRef.child("likes").setValue(album.getLikes() + 1);
                                            album.setLikes(album.getLikes() + 1);
                                            album.setUserHasLiked(true);
                                            holder.likeButton.setImageResource(R.drawable.heart_24filled);
                                            likeStatusCache.put(album.getId(), true);
                                        }
                                    }
                                    holder.likeCount.setText(formatLikesCount(album.getLikes()));
                                    notifyDataSetChanged();
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {
                                    Toast.makeText(holder.itemView.getContext(),
                                            "Error processing like", Toast.LENGTH_SHORT).show();
                                }
                            });
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(holder.itemView.getContext(),
                            "Error fetching user data", Toast.LENGTH_SHORT).show();
                }
            });
        });

        // Handle comment button click
        holder.commentButton.setOnClickListener(v -> {
            BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(holder.itemView.getContext(), R.style.BottomSheetDialogTheme);
            View bottomSheetView = LayoutInflater.from(holder.itemView.getContext())
                    .inflate(R.layout.fragment_comment, null);

            bottomSheetDialog.setContentView(bottomSheetView);

            Window window = bottomSheetDialog.getWindow();
            if (window != null) {
                // Remove the SOFT_INPUT_ADJUST_RESIZE flag
                window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);
            }



            bottomSheetDialog.setContentView(bottomSheetView);
            bottomSheetDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);



            RecyclerView commentRecyclerView = bottomSheetView.findViewById(R.id.commentRecyclerView);
            EditText commentEditText = bottomSheetView.findViewById(R.id.commentEditText);
            ImageButton sendButton = bottomSheetView.findViewById(R.id.sendButton);
            ConstraintLayout commentBox = bottomSheetView.findViewById(R.id.commentBox);
            ConstraintLayout containerTag = bottomSheetView.findViewById(R.id.containerTag);
            TextView usernameTag = bottomSheetView.findViewById(R.id.usernameTag);
            ImageButton cancelTag = bottomSheetView.findViewById(R.id.cancelTag);
            LinearLayout noneTextView = bottomSheetView.findViewById(R.id.noneTextView);
            ShapeableImageView photoProfileCmb = bottomSheetView.findViewById(R.id.photoprofilecmb);

            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(currentUserId);
            userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    String photoProfileUrl = snapshot.child("photoprofile").getValue(String.class);

                    if (photoProfileUrl != null && !photoProfileUrl.isEmpty()) {
                        // Load image using Glide
                        Glide.with(holder.itemView.getContext())
                                .load(photoProfileUrl)
                                .placeholder(R.color.blacksmooth2)
                                .error(R.color.blacksmooth2)
                                .into(photoProfileCmb);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    // Handle error
                    photoProfileCmb.setImageResource(R.color.blacksmooth2);
                }
            });


            ViewTreeObserver.OnGlobalLayoutListener keyboardLayoutListener = new ViewTreeObserver.OnGlobalLayoutListener() {
                private final Rect r = new Rect();
                private final int screenHeight = holder.itemView.getContext().getResources().getDisplayMetrics().heightPixels;
                private int lastVisibleDecorViewHeight = 0;
                private final int BOTTOM_OFFSET_DP = -2; // Jarak tambahan 20dp
                private final float density = holder.itemView.getContext().getResources().getDisplayMetrics().density;
                private final int bottomOffset = (int) (BOTTOM_OFFSET_DP * density); // Konversi ke piksel

                @Override
                public void onGlobalLayout() {
                    // Get visible screen height
                    window.getDecorView().getWindowVisibleDisplayFrame(r);
                    int visibleDecorViewHeight = r.height();

                    if (lastVisibleDecorViewHeight != 0) {
                        if (visibleDecorViewHeight > lastVisibleDecorViewHeight) {
                            // Keyboard is hidden
                            commentBox.setTranslationY(0);
                        } else if (visibleDecorViewHeight < lastVisibleDecorViewHeight) {
                            // Keyboard is shown
                            int keyboardHeight = screenHeight - visibleDecorViewHeight;
                            commentBox.setTranslationY(-(keyboardHeight + bottomOffset)); // Tambahkan offset 20dp
                        }
                    }

                    lastVisibleDecorViewHeight = visibleDecorViewHeight;
                }
            };
            window.getDecorView().getViewTreeObserver().addOnGlobalLayoutListener(keyboardLayoutListener);


            bottomSheetDialog.setOnDismissListener(dialog -> {
                window.getDecorView().getViewTreeObserver().removeOnGlobalLayoutListener(keyboardLayoutListener);
            });

// Set up comment box initial state
            commentBox.post(() -> {
                commentBox.setTranslationY(-10);
            });

            commentEditText.setOnFocusChangeListener((v1, hasFocus) -> {
                if (hasFocus && window != null) {
                    window.clearFlags(
                            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                                    WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM
                    );
                }
            });


            containerTag.setVisibility(View.GONE);

            String albumId = album.getId();
            ArrayList<Comment> commentList = new ArrayList<>();
            CommentAdapter commentAdapter = new CommentAdapter(commentList, albumId, currentUserId, album.getUserId());

            commentRecyclerView.setLayoutManager(new LinearLayoutManager(holder.itemView.getContext()));
            commentRecyclerView.setAdapter(commentAdapter);

            commentBox.setTranslationY(200);
            commentBox.setAlpha(0f);
            commentBox.animate()
                    .translationY(0)
                    .alpha(1f)
                    .setDuration(300)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();

            // Variable to track which comment is being replied to
            final Comment[] currentReplyingTo = {null};
            final String[] currentParentCommentId = {null}; // Tambahkan ini
            final int[] protectedLength = {0};

            TextWatcher replyTextWatcher = new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    // No action needed
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    // No action needed
                }

                @Override
                public void afterTextChanged(Editable s) {
                    if (currentReplyingTo[0] != null && s.length() < protectedLength[0]) {
                        // Restore the protected text if it was deleted
                        String replyPrefix = "@" + currentReplyingTo[0].getUsername() + " ";
                        SpannableString spannableString = new SpannableString(replyPrefix);
                        spannableString.setSpan(
                                new ForegroundColorSpan(ContextCompat.getColor(holder.itemView.getContext(), R.color.blue)),
                                replyPrefix.indexOf("@"),
                                replyPrefix.indexOf("@") + currentReplyingTo[0].getUsername().length() + 1,
                                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                        );
                        commentEditText.removeTextChangedListener(this);
                        commentEditText.setText(spannableString);
                        commentEditText.setSelection(spannableString.length());
                        commentEditText.addTextChangedListener(this);
                    }
                }
            };

//                bottomSheetDialog.setOnShowListener(dialog -> {
//                    BottomSheetDialog d = (BottomSheetDialog) dialog;
//                    FrameLayout bottomSheet = d.findViewById(com.google.android.material.R.id.design_bottom_sheet);
//                    if (bottomSheet != null) {
//                        BottomSheetBehavior<FrameLayout> behavior = BottomSheetBehavior.from(bottomSheet);
//                        behavior.setSkipCollapsed(true);
//                        behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
//                    }
//                });

            // Di dalam setup BottomSheetDialog

            commentAdapter.setOnReplyClickListener(comment -> {
                currentReplyingTo[0] = comment;
                // Set parent comment ID berdasarkan tipe reply
                currentParentCommentId[0] = comment.getParentCommentId() != null ?
                        comment.getParentCommentId() : comment.getId();

                containerTag.setVisibility(View.VISIBLE);
                usernameTag.setText("@" + comment.getUsername());

                // Remove existing TextWatcher if any
                if (commentEditText.getTag() instanceof TextWatcher) {
                    commentEditText.removeTextChangedListener((TextWatcher) commentEditText.getTag());
                }

                String replyPrefix = "@" + comment.getUsername() + " ";
                SpannableString spannableString = new SpannableString(replyPrefix);
                spannableString.setSpan(
                        new ForegroundColorSpan(ContextCompat.getColor(holder.itemView.getContext(), R.color.blue)),
                        replyPrefix.indexOf("@"),
                        replyPrefix.indexOf("@") + comment.getUsername().length() + 1,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                );

                protectedLength[0] = replyPrefix.length();
                commentEditText.setTag(replyTextWatcher);
                commentEditText.addTextChangedListener(replyTextWatcher);
                commentEditText.setText(spannableString);
                commentEditText.setSelection(spannableString.length());
                commentEditText.requestFocus();

                InputMethodManager imm = (InputMethodManager) holder.itemView.getContext()
                        .getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.showSoftInput(commentEditText, InputMethodManager.SHOW_IMPLICIT);
            });

            cancelTag.setOnClickListener(v1 -> {
                containerTag.setVisibility(View.GONE);
                currentReplyingTo[0] = null;
                // Remove TextWatcher when canceling reply
                if (commentEditText.getTag() instanceof TextWatcher) {
                    commentEditText.removeTextChangedListener((TextWatcher) commentEditText.getTag());
                }
                commentEditText.setText("");
                protectedLength[0] = 0;
            });

            DatabaseReference commentsRef = FirebaseDatabase.getInstance().getReference("users")
                    .child(album.getUserId())
                    .child("album")
                    .child(album.getId())
                    .child("comments");

            // Load existing comments
            // Di dalam bagian ValueEventListener untuk comments
            commentsRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    commentList.clear();
                    for (DataSnapshot commentSnapshot : snapshot.getChildren()) {
                        String commentId = commentSnapshot.getKey();
                        String userId = commentSnapshot.child("userId").getValue(String.class);
                        String username = commentSnapshot.child("username").getValue(String.class);
                        String photoProfile = commentSnapshot.child("photoprofile").getValue(String.class);
                        String text = commentSnapshot.child("text").getValue(String.class);
                        Long timestamp = commentSnapshot.child("timestamp").getValue(Long.class);

                        if (userId != null && username != null && text != null) {
                            Comment comment = new Comment(userId, username, text,
                                    photoProfile != null ? photoProfile : "",
                                    timestamp != null ? timestamp : 0L);
                            comment.setId(commentId);

                            // Load replies
                            ArrayList<Comment> replies = new ArrayList<>();
                            DataSnapshot repliesSnapshot = commentSnapshot.child("replies");
                            if (repliesSnapshot.exists()) {  // Pastikan node replies ada
                                for (DataSnapshot replySnapshot : repliesSnapshot.getChildren()) {
                                    String replyId = replySnapshot.getKey();
                                    String replyUserId = replySnapshot.child("userId").getValue(String.class);
                                    String replyUsername = replySnapshot.child("username").getValue(String.class);
                                    String replyText = replySnapshot.child("text").getValue(String.class);
                                    String replyPhotoProfile = replySnapshot.child("photoprofile").getValue(String.class);
                                    Long replyTimestamp = replySnapshot.child("timestamp").getValue(Long.class);

                                    if (replyUserId != null && replyUsername != null && replyText != null) {
                                        Comment reply = new Comment(replyUserId, replyUsername, replyText,
                                                replyPhotoProfile != null ? replyPhotoProfile : "",
                                                replyTimestamp != null ? replyTimestamp : 0L);
                                        reply.setId(replyId);  // Simpan ID reply
                                        replies.add(reply);
                                    }
                                }

                                // Sort replies by timestamp (newest first)
                                replies.sort((a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
                            }
                            comment.setReplies(replies);
                            commentList.add(comment);
                        }
                    }

                    if (commentList.isEmpty()) {
                        noneTextView.setVisibility(View.VISIBLE);
                        commentRecyclerView.setVisibility(View.VISIBLE);
                    } else {
                        noneTextView.setVisibility(View.GONE);
                        commentRecyclerView.setVisibility(View.VISIBLE);
                    }
                    // Sort main comments by timestamp (newest first)
                    commentList.sort((a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
                    commentAdapter.notifyDataSetChanged();
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(holder.itemView.getContext(), "Failed to load comments", Toast.LENGTH_SHORT).show();
                }
            });

            bottomSheetDialog.show();

            // Send comment handler
            // Inside the sendButton click listener, modify the reply handling section:

            sendButton.setOnClickListener(v1 -> {
                String commentText = commentEditText.getText().toString().trim();
                if (!commentText.isEmpty()) {
                    long timestamp = System.currentTimeMillis();

                    DatabaseReference userRef2 = FirebaseDatabase.getInstance().getReference("users").child(currentUserId);
                    userRef2.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            String username = snapshot.child("username").getValue(String.class);
                            String photoProfileUrl = snapshot.child("photoprofile").getValue(String.class);

                            if (username != null) {
                                if (currentReplyingTo[0] != null) {
                                    // Handle reply to comment or reply
                                    DatabaseReference commentRef = FirebaseDatabase.getInstance().getReference("users")
                                            .child(album.getUserId())
                                            .child("album")
                                            .child(album.getId())
                                            .child("comments")
                                            .child(currentParentCommentId[0]); // Gunakan currentParentCommentId

                                    // Create reply data
                                    Map<String, Object> replyData = new HashMap<>();
                                    replyData.put("userId", currentUserId);
                                    replyData.put("username", username);
                                    replyData.put("text", commentText);
                                    replyData.put("photoprofile", photoProfileUrl != null ? photoProfileUrl : "");
                                    replyData.put("timestamp", timestamp);

                                    // Generate new reply ID
                                    String replyId = commentRef.child("replies").push().getKey();

                                    if (replyId != null) {
                                        // Add reply to the specific comment
                                        // Add reply to the specific comment
                                        // Add reply to the specific comment
                                        commentRef.child("replies").child(replyId)
                                                .setValue(replyData)
                                                .addOnSuccessListener(aVoid -> {
                                                    // Reset current replying state
                                                    currentReplyingTo[0] = null;
                                                    currentParentCommentId[0] = null; // Reset ID parent

                                                    // Hapus mention di EditText dengan benar
                                                    commentEditText.setText(""); // Kosongkan text
                                                    commentEditText.getText().clear(); // Pastikan benar-benar bersih

                                                    // Sembunyikan container mention jika ada
                                                    containerTag.setVisibility(View.GONE);

                                                    // Hapus TextWatcher jika ada
                                                    if (commentEditText.getTag() instanceof TextWatcher) {
                                                        commentEditText.removeTextChangedListener((TextWatcher) commentEditText.getTag());
                                                        commentEditText.setTag(null);
                                                    }

                                                    // Reset panjang teks yang dilindungi (jika ada mekanisme untuk itu)
                                                    protectedLength[0] = 0;

                                                    // Pastikan keyboard tertutup agar tidak ada kesalahan input
                                                    InputMethodManager imm = (InputMethodManager) holder.itemView.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                                                    if (imm != null) {
                                                        imm.hideSoftInputFromWindow(commentEditText.getWindowToken(), 0);
                                                    }

                                                    Toast.makeText(holder.itemView.getContext(), "Reply added successfully", Toast.LENGTH_SHORT).show();
                                                })
                                                .addOnFailureListener(e -> {
                                                    Toast.makeText(holder.itemView.getContext(), "Failed to add reply: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                                });

                                    }
                                } else {
                                    // Handle new comment (kode tetap sama seperti sebelumnya)
                                    Comment newComment = new Comment(currentUserId, username, commentText,
                                            photoProfileUrl != null ? photoProfileUrl : "", timestamp);
                                    newComment.setParentUserId(album.getUserId()); // Set ID pemilik album

                                    String commentId = commentsRef.push().getKey();
                                    if (commentId != null) {
                                        commentsRef.child(commentId).setValue(newComment)
                                                .addOnSuccessListener(aVoid -> {
                                                    commentEditText.setText("");
                                                    Toast.makeText(holder.itemView.getContext(),
                                                            "Comment added", Toast.LENGTH_SHORT).show();
                                                })
                                                .addOnFailureListener(e -> {
                                                    Toast.makeText(holder.itemView.getContext(),
                                                            "Failed to add comment", Toast.LENGTH_SHORT).show();
                                                });
                                    }
                                }
                            } else {
                                Toast.makeText(holder.itemView.getContext(),
                                        "Failed to retrieve user data", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Toast.makeText(holder.itemView.getContext(),
                                    "Failed to load user profile", Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    Toast.makeText(holder.itemView.getContext(),
                            "Comment cannot be empty", Toast.LENGTH_SHORT).show();
                }
            });

            bottomSheetDialog.show();
        });

        // Handle bookmark functionality
        DatabaseReference userBookmarksRef = FirebaseDatabase.getInstance().getReference("users")
                .child(currentUserId)
                .child("bookmark");

        // Check bookmark status across all collections
        userBookmarksRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot allCollagesSnapshot) {
                boolean isBookmarkedAnywhere = false;

                for (DataSnapshot collageSnapshot : allCollagesSnapshot.getChildren()) {
                    if (collageSnapshot.hasChild(album.getId())) {
                        isBookmarkedAnywhere = true;
                        break;
                    }
                }

                holder.bookmarkButton.setImageResource(isBookmarkedAnywhere ?
                        R.drawable.bookmark_24filled : R.drawable.bookmark_24filledgrey);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(context,
                        "Failed to load bookmark status", Toast.LENGTH_SHORT).show();
            }
        });

        holder.bookmarkButton.setOnClickListener(v -> {
            userBookmarksRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot allCollagesSnapshot) {
                    boolean isBookmarkedAnywhere = false;
                    String existingCollageId = null;

                    for (DataSnapshot collageSnapshot : allCollagesSnapshot.getChildren()) {
                        if (collageSnapshot.hasChild(album.getId())) {
                            isBookmarkedAnywhere = true;
                            existingCollageId = collageSnapshot.getKey();
                            break;
                        }
                    }

                    if (isBookmarkedAnywhere && existingCollageId != null) {
                        // Remove from existing collection
                        userBookmarksRef.child(existingCollageId).child(album.getId()).removeValue()
                                .addOnSuccessListener(aVoid -> {
                                    holder.bookmarkButton.setImageResource(R.drawable.bookmark_24filledgrey);
                                    Toast.makeText(context,
                                            "Removed from collection", Toast.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(context,
                                            "Failed to remove from collection", Toast.LENGTH_SHORT).show();
                                });
                    } else {
                        // Show bottom sheet to select collection
                        showCollageSelectionBottomSheet(holder, album);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(context,
                            "Failed to check bookmark status", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void showCollageSelectionBottomSheet(AlbumViewHolder holder, Album album) {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(holder.itemView.getContext(),
                R.style.BottomSheetDialogTheme);
        View bottomSheetView = LayoutInflater.from(holder.itemView.getContext())
                .inflate(R.layout.bottom_sheet_collage_list, null);

        RecyclerView collageRecyclerView = bottomSheetView.findViewById(R.id.collageRecyclerView);
        ArrayList<CollageItem> collageList = new ArrayList<>();

        CollageSelectionAdapter collageAdapter = new CollageSelectionAdapter(collageList,
                (collageId, collageName) -> {
                    DatabaseReference selectedCollageRef = FirebaseDatabase.getInstance().getReference("users")
                            .child(currentUserId)
                            .child("bookmark")
                            .child(collageId)
                            .child(album.getId());

                    BookmarkData bookmarkData = new BookmarkData(
                            album.getId(),
                            album.getUserId(),
                            album.getId(),
                            album.getTitle(),
                            album.getDescription(),
                            album.getUrl(),
                            album.getUsername(),
                            album.getPhotoprofile(),
                            album.getLikes(),
                            album.isUserHasLiked(),
                            System.currentTimeMillis()
                    );

                    selectedCollageRef.setValue(bookmarkData)
                            .addOnSuccessListener(aVoid -> {
                                holder.bookmarkButton.setImageResource(R.drawable.bookmark_24filled);
                                Toast.makeText(holder.itemView.getContext(),
                                        "Saved to " + collageName, Toast.LENGTH_SHORT).show();
                                bottomSheetDialog.dismiss();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(holder.itemView.getContext(),
                                        "Failed to save bookmark", Toast.LENGTH_SHORT).show();
                            });
                });

        collageRecyclerView.setLayoutManager(new LinearLayoutManager(holder.itemView.getContext()));
        collageRecyclerView.setAdapter(collageAdapter);

        // Load collages
        loadCollageList(holder, collageList, collageAdapter);

        bottomSheetDialog.setContentView(bottomSheetView);
        bottomSheetDialog.show();
    }

    private void loadCollageList(AlbumViewHolder holder, ArrayList<CollageItem> collageList,
                                 CollageSelectionAdapter collageAdapter) {

        DatabaseReference collagesRef = FirebaseDatabase.getInstance().getReference("users")
                .child(currentUserId)
                .child("bookmark");



        collagesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                collageList.clear();

                if (!snapshot.hasChild("allCollage")) {
                    DatabaseReference allCollageRef = collagesRef.child("allCollage");
                    allCollageRef.child("name").setValue("All Collage");
                    allCollageRef.child("timestamp").setValue(ServerValue.TIMESTAMP);
                }

                for (DataSnapshot collageSnapshot : snapshot.getChildren()) {
                    String collageName = collageSnapshot.child("name").getValue(String.class);
                    String collageId = collageSnapshot.getKey();
                    long timestamp = collageSnapshot.child("timestamp").getValue(Long.class) != null ?
                            collageSnapshot.child("timestamp").getValue(Long.class) : 0;

                    if (collageName != null && collageId != null) {
                        CollageItem collage = new CollageItem(collageId, collageName, timestamp);
                        collageList.add(collage);
                    }
                }

                collageList.sort((a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
                collageAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(holder.itemView.getContext(),
                        "Failed to load collages", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupProfileListener() {
        // Remove any existing listener
        if (profileListener != null) {
            userRef.removeEventListener(profileListener);
        }

        profileListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String username = snapshot.child("username").getValue(String.class);
                    String tagname = snapshot.child("tagName").getValue(String.class);
                    String photoprofile = snapshot.child("photoprofile").getValue(String.class);
                    String bio = snapshot.child("bio").getValue(String.class);
                    String location = snapshot.child("location").getValue(String.class); // Ambil data location
                    Long followers = snapshot.child("followers").getValue(Long.class);
                    Long following = snapshot.child("following").getValue(Long.class);
                    Long post = snapshot.child("post").getValue(Long.class);

                    // Update profile data
                    profileData = new ProfileData(
                            username != null ? username : profileData.username,
                            photoprofile != null ? photoprofile : profileData.photoprofile,
                            location != null ? location : profileData.location,
                            bio != null ? bio : profileData.bio,
                            followers != null ? followers : profileData.followers,
                            following != null ? following : profileData.following,
                            post != null ? post : profileData.post,
                            tagname != null ? tagname : profileData.tagname
                    ); // Add this closing parenthesis

// Notify header item changed to refresh UI
                    notifyItemChanged(0);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(context, "Failed to load profile data", Toast.LENGTH_SHORT).show();
            }
        };


        // Add the listener
        userRef.addValueEventListener(profileListener);
    }

    @Override
    public int getItemViewType(int position) {
        return position == 0 ? TYPE_HEADER : TYPE_ALBUM;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_HEADER) {
            View headerView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.header_show_profile, parent, false);
            return new HeaderViewHolder(headerView);
        } else {
            View albumView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_upload, parent, false);
            return new AlbumViewHolder(albumView);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof HeaderViewHolder) {
            HeaderViewHolder headerHolder = (HeaderViewHolder) holder;
            bindHeaderData(headerHolder);
        } else if (holder instanceof AlbumViewHolder) {
            AlbumViewHolder albumHolder = (AlbumViewHolder) holder;
            bindAlbumData(albumHolder, albums.get(position - 1));
        }
    }

    @Override
    public int getItemCount() {
        return albums.size() + 1;
    }

    private void bindHeaderData(HeaderViewHolder holder) {
        holder.usernameText.setText(profileData.username);
        holder.bioText.setText(profileData.bio);
        holder.tagname.setText(profileData.tagname);
        holder.locationText.setText(profileData.location);
        holder.followersCount.setText(formatLikesCount((int) profileData.followers));
        holder.followingCount.setText(formatLikesCount((int) profileData.following));
        holder.postCount.setText(formatLikesCount((int) profileData.post));

        if (profileData.photoprofile != null && !profileData.photoprofile.isEmpty()) {
            Glide.with(context).load(profileData.photoprofile).into(holder.profileImage);
        } else {
            holder.profileImage.setImageResource(R.drawable.default_profile_bct_v1);
        }

        if (profileData.bio != null && !profileData.bio.isEmpty()) {
            holder.bioText.setText(profileData.bio);
        } else {
            holder.bioText.setText("No bio Available");
        }

        holder.tagname.setText(profileData.tagname);

        // Handle empty location
        if (profileData.location != null && !profileData.location.isEmpty()) {
            holder.locationText.setText(profileData.location);
        } else {
            holder.locationText.setText("Location not set");
        }

    }


    // Clean up listener when adapter is no longer needed
    public void cleanup() {
        if (profileListener != null) {
            userRef.removeEventListener(profileListener);
        }
    }

    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        ImageView profileImage, commentButton;
        ImageButton likeButton;
        TextView usernameText, bioText, followersCount, followingCount, postCount, locationText, tagname;
//        ImageButton backButton;

        HeaderViewHolder(View view) {
            super(view);
            profileImage = view.findViewById(R.id.photoProfile);
            usernameText = view.findViewById(R.id.profile_username);
            tagname = view.findViewById(R.id.profile_email);
            bioText = view.findViewById(R.id.descriptionprofile);
            followersCount = view.findViewById(R.id.followers);
            followingCount = view.findViewById(R.id.followed);
            likeButton = itemView.findViewById(R.id.btnLike);
            commentButton = itemView.findViewById(R.id.btnComment);
            postCount = view.findViewById(R.id.post);
            locationText = view.findViewById(R.id.location);

            followersCount.setOnClickListener(v -> showFollowersBottomSheet(userId, itemView.getContext()));
            followingCount.setOnClickListener(v -> showFollowedBottomSheet(userId, itemView.getContext()));
        }
    }

    private static void showFollowersBottomSheet(String userId, Context context) {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(context, R.style.BottomSheetDialogTheme);
        View bottomSheetView = LayoutInflater.from(context).inflate(R.layout.fragment_followers, null);
        bottomSheetDialog.setContentView(bottomSheetView);

        RecyclerView followersRecyclerView = bottomSheetView.findViewById(R.id.followersRecyclerView);
        ArrayList<User> followersList = new ArrayList<>();
        UserAdapter userAdapter = new UserAdapter(followersList, context);
        followersRecyclerView.setLayoutManager(new LinearLayoutManager(context));
        followersRecyclerView.setAdapter(userAdapter);

        // Mengambil referensi ke followersUser
        DatabaseReference followersRef = FirebaseDatabase.getInstance().getReference("users").child(userId).child("followersUser");
        followersRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                followersList.clear();

                // Untuk setiap follower ID
                for (DataSnapshot followerSnapshot : snapshot.getChildren()) {
                    String followerId = followerSnapshot.getKey();

                    // Ambil data user dari node users/{followerId}
                    DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(followerId);
                    userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot userSnapshot) {
                            String username = userSnapshot.child("username").getValue(String.class);
                            String photoProfile = userSnapshot.child("photoprofile").getValue(String.class);
                            String tagName = userSnapshot.child("tagName").getValue(String.class);

                            if (username != null) {
                                // Tambahkan tagName ke objek User
                                User user = new User(followerId, username, photoProfile != null ? photoProfile : "");
                                user.setTagName(tagName != null ? tagName : "");
                                followersList.add(user);
                                userAdapter.notifyDataSetChanged();
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Toast.makeText(context, "Failed to load follower data", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(context, "Failed to load followers", Toast.LENGTH_SHORT).show();
            }
        });

        bottomSheetDialog.show();
    }

    private static void showFollowedBottomSheet(String userId, Context context) {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(context, R.style.BottomSheetDialogTheme);
        View bottomSheetView = LayoutInflater.from(context).inflate(R.layout.fragment_followed, null);
        bottomSheetDialog.setContentView(bottomSheetView);

        RecyclerView followedRecyclerView = bottomSheetView.findViewById(R.id.followedRecyclerView);
        ArrayList<User> followedList = new ArrayList<>();
        UserAdapter userAdapter = new UserAdapter(followedList, context);
        followedRecyclerView.setLayoutManager(new LinearLayoutManager(context));
        followedRecyclerView.setAdapter(userAdapter);

        // Mengambil referensi ke followedUser
        DatabaseReference followedRef = FirebaseDatabase.getInstance().getReference("users").child(userId).child("followedUser");
        followedRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                followedList.clear();

                // Untuk setiap followed ID
                for (DataSnapshot followedSnapshot : snapshot.getChildren()) {
                    String followedId = followedSnapshot.getKey();

                    // Ambil data user dari node users/{followedId}
                    DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(followedId);
                    userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot userSnapshot) {
                            String username = userSnapshot.child("username").getValue(String.class);
                            String photoProfile = userSnapshot.child("photoprofile").getValue(String.class);
                            String tagName = userSnapshot.child("tagName").getValue(String.class);

                            if (username != null) {
                                // Tambahkan tagName ke objek User
                                User user = new User(followedId, username, photoProfile != null ? photoProfile : "");
                                user.setTagName(tagName != null ? tagName : "");
                                followedList.add(user);
                                userAdapter.notifyDataSetChanged();
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Toast.makeText(context, "Failed to load followed user data", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(context, "Failed to load followed users", Toast.LENGTH_SHORT).show();
            }
        });

        bottomSheetDialog.show();
    }

    static class AlbumViewHolder extends RecyclerView.ViewHolder {
        ImageButton bookmarkButton;
        ImageView commentButton, albumPhotoprofile;
        ImageButton likeButton;
        ImageView albumImage;
        TextView albumTitle, albumUsername, date, likeCount, followButton;
        TextView albumDescription;


        AlbumViewHolder(View view) {
            super(view);
            followButton = itemView.findViewById(R.id.follow);
            albumImage = view.findViewById(R.id.imageView);
            albumTitle = view.findViewById(R.id.textTitle);
            albumDescription = view.findViewById(R.id.textDescription);
            likeCount = itemView.findViewById(R.id.likeCount);
            albumUsername = view.findViewById(R.id.usernameUploader);
            albumPhotoprofile = view.findViewById(R.id.photoProfile);
            date = view.findViewById(R.id.uploadDateTextView);
            likeButton = itemView.findViewById(R.id.btnLike);
            commentButton = itemView.findViewById(R.id.btnComment);
            bookmarkButton = itemView.findViewById(R.id.bookmark);
        }


    }

    private String formatLikesCount(long count) {
        if (count >= 1_000_000_000) {
            return String.format("%.1fb", count / 1_000_000_000.0);
        } else if (count >= 1_000_000) {
            return String.format("%.1fm", count / 1_000_000.0);
        } else if (count >= 1_000) {
            return String.format("%.1fk", count / 1_000.0);
        }
        return String.valueOf(count);
    }


}
