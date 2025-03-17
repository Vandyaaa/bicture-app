    package com.example.imaginate;

    import android.animation.AnimatorSet;
    import android.animation.ObjectAnimator;
    import android.app.Activity;
    import android.app.Dialog;
    import android.content.Context;
    import android.content.Intent;
    import android.content.SharedPreferences;
    import android.graphics.Color;
    import android.graphics.Rect;
    import android.graphics.drawable.ColorDrawable;
    import android.os.Bundle;
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
    import androidx.fragment.app.Fragment;
    import androidx.fragment.app.FragmentManager;
    import androidx.fragment.app.FragmentTransaction;
    import androidx.recyclerview.widget.LinearLayoutManager;
    import androidx.recyclerview.widget.RecyclerView;

    import com.bumptech.glide.Glide;
    import com.example.imaginate.models.BookmarkData;
    import com.example.imaginate.models.Comment;
    import com.example.imaginate.models.LikeData;
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

    public class SavedAlbumsFragment extends Fragment {
        private RecyclerView recyclerView;
        private ArrayList<BookmarkData> savedAlbums;
        private SavedAlbumsAdapter albumsAdapter;
        private String currentUserId;
        private String currentUsername;
        private String currentUserPhotoprofile;
        private String tagName;
        private String collageId;
        private String collageName;
        private ValueEventListener savedAlbumsListener;
        private String albumid;
        private Map<String, Boolean> likeStatusCache = new HashMap<>();
        private Map<String, Boolean> followStatusCache = new HashMap<>();
        private Map<String, Boolean> bookmarkStatusCache = new HashMap<>();
        private ValueEventListener likeCountListener;
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.fragment_saved_albums, container, false);

            Bundle args = getArguments();
            if (args != null) {
                collageId = args.getString("collageId");
                collageName = args.getString("collageName");
            }

            SharedPreferences sharedPreferences = requireActivity().getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
            currentUserId = sharedPreferences.getString("userId", null);
            tagName = sharedPreferences.getString("tagName", null);
            currentUsername = sharedPreferences.getString("username", null);
            currentUserPhotoprofile = sharedPreferences.getString("photoprofile", null);

            TextView headerTitle = view.findViewById(R.id.headerTitle);
            ImageButton backButton = view.findViewById(R.id.backButton);
            ImageButton deleteCollageButton = view.findViewById(R.id.deleteCollage);
            deleteCollageButton.setOnClickListener(v -> deleteCollageAndContents());
            headerTitle.setText(collageName);
            backButton.setOnClickListener(v -> {
                FragmentManager fragmentManager = requireActivity().getSupportFragmentManager();

                // Add exit animation
                FragmentTransaction ft = fragmentManager.beginTransaction();
                ft.setCustomAnimations(0, R.anim.kirikekanan);

                fragmentManager.popBackStack(); // Go back to previous fragment

                // Show home fragment if available
                Fragment homeFragment = fragmentManager.findFragmentByTag("home_fragment");
                if (homeFragment != null) {
                    ft.show(homeFragment);
                    ft.commit();
                }
            });

            recyclerView = view.findViewById(R.id.savedAlbumsRecyclerView);
            recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            savedAlbums = new ArrayList<BookmarkData>();
            albumsAdapter = new SavedAlbumsAdapter();
            recyclerView.setAdapter(albumsAdapter);

            loadSavedAlbums();

            return view;
        }

        private void navigateToUserProfile(String userId) {
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

                        FragmentManager fragmentManager = requireActivity().getSupportFragmentManager();

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
                    Toast.makeText(getContext(), "Failed to load user profile", Toast.LENGTH_SHORT).show();
                }
            });
        }

        private void loadSavedAlbums() {
            DatabaseReference bookmarkRef = FirebaseDatabase.getInstance().getReference()
                    .child("users").child(currentUserId).child("bookmark").child(collageId);


            savedAlbumsListener = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    // Pastikan view masih ada dan fragment masih attached
                    View view = getView();
                    if (view == null || !isAdded()) {
                        return;
                    }

                    savedAlbums.clear();

                    for (DataSnapshot albumSnapshot : snapshot.getChildren()) {
                        if (albumSnapshot.getKey().equals("name") ||
                                albumSnapshot.getKey().equals("timestamp")) {
                            continue;
                        }

                        BookmarkData album = albumSnapshot.getValue(BookmarkData.class);
                        if (album != null) {
                            // Load like status for each album
                            DatabaseReference originalAlbumRef = FirebaseDatabase.getInstance().getReference()
                                    .child("users").child(album.getUserId())
                                    .child("album").child(album.getAlbumId());

                            originalAlbumRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot albumSnapshot) {
                                    if (albumSnapshot.exists()) {
                                        // Update like count from original album
                                        Integer likesValue = albumSnapshot.child("likes").getValue(Integer.class);
                                        int currentLikes = (likesValue != null) ? likesValue : 0;
                                        album.setLikes(currentLikes);

                                        // Update upload date
                                        String uploadDate = albumSnapshot.child("humanReadableDate").getValue(String.class);
                                        if (uploadDate != null) {
                                            album.setUploadDate(uploadDate);
                                        }

                                        // Notify adapter of data change
                                        albumsAdapter.notifyDataSetChanged();
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {
                                    // Handle error
                                }
                            });
                            loadLikeStatus(album);
                            savedAlbums.add(album);
                        }
                    }

                    savedAlbums.sort((a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
                    albumsAdapter.notifyDataSetChanged();

                    View emptyState = view.findViewById(R.id.emptyState);
                    if (savedAlbums.isEmpty()) {
                        emptyState.setVisibility(View.VISIBLE);
                    } else {
                        emptyState.setVisibility(View.GONE);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Failed to load saved albums", Toast.LENGTH_SHORT).show();
                    }
                }
            };

            bookmarkRef.addValueEventListener(savedAlbumsListener);
        }

        private void loadLikeStatus(BookmarkData album) {
            DatabaseReference likesRef = FirebaseDatabase.getInstance().getReference()
                    .child("users").child(album.getUserId())
                    .child("album").child(album.getAlbumId())
                    .child("userLikes");

            likesRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {

                    boolean userHasLiked = false;
                    for (DataSnapshot likeSnapshot : snapshot.getChildren()) {
                        String likedUserId = likeSnapshot.child("userId").getValue(String.class);
                        if (currentUserId.equals(likedUserId)) {
                            userHasLiked = true;
                            break;
                        }
                    }
                    album.setUserHasLiked(userHasLiked);
                    albumsAdapter.notifyDataSetChanged();
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(getContext(), "Failed to load like status", Toast.LENGTH_SHORT).show();
                }
            });
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

        private void deleteCollageAndContents() {
            // Create and show custom dialog
            Dialog deleteDialog = new Dialog(requireContext());
            deleteDialog.setContentView(R.layout.delete_collage_dialog);
            deleteDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

            // Get dialog views
            TextView cancelButton = deleteDialog.findViewById(R.id.textView4);
            TextView deleteButton = deleteDialog.findViewById(R.id.textView3);

            // Set click listeners
            cancelButton.setOnClickListener(v -> deleteDialog.dismiss());

            deleteButton.setOnClickListener(v -> {
                DatabaseReference userRef = FirebaseDatabase.getInstance().getReference()
                        .child("users").child(currentUserId);
                DatabaseReference collageRef = userRef.child("bookmark").child(collageId);

                // Delete the collage and its contents
                collageRef.removeValue()
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(getContext(), "Collection deleted successfully", Toast.LENGTH_SHORT).show();
                            deleteDialog.dismiss();

                            // Get fragment manager
                            FragmentManager fragmentManager = requireActivity().getSupportFragmentManager();

                            // Create transaction with animation
                            FragmentTransaction ft = fragmentManager.beginTransaction();
                            ft.setCustomAnimations(0, R.anim.kirikekanan);

                            // Pop back stack
                            fragmentManager.popBackStack();

                            // Show home fragment if available
                            Fragment homeFragment = fragmentManager.findFragmentByTag("home_fragment");
                            if (homeFragment != null) {
                                ft.show(homeFragment);
                                ft.commit();
                            }
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(getContext(), "Failed to delete collection", Toast.LENGTH_SHORT).show();
                            deleteDialog.dismiss();
                        });
            });

            // Show dialog
            deleteDialog.show();
        }

        private class SavedAlbumsAdapter extends RecyclerView.Adapter<SavedAlbumsAdapter.AlbumViewHolder> {
            @NonNull
            @Override
            public AlbumViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_saved_album, parent, false);
                return new AlbumViewHolder(view);
            }

            @Override
            public void onBindViewHolder(@NonNull AlbumViewHolder holder, int position) {
                BookmarkData album = savedAlbums.get(position);
                holder.titleTextView.setText(album.getTitle());
                holder.descriptionTextView.setText(album.getDescription());
                holder.usernameTextView.setText(album.getUsername());
                holder.likeCount.setText(formatLikesCount(album.getLikes()));
                holder.uploadDateTextView.setText(album.getUploadDate());
                // Set like button state
                holder.likeButton.setImageResource(album.isUserHasLiked() ?
                        R.drawable.heart_24filled : R.drawable.heart_24);

                // Check follow status
                checkFollowStatus(album, holder);

                holder.setupLikeCountListener(holder, album);

                Glide.with(holder.itemView.getContext())
                        .load(album.getImageUrl())
                        .into(holder.albumImageView);

                if (album.getPhotoprofile() != null && !album.getPhotoprofile().isEmpty()) {
                    Glide.with(holder.itemView.getContext())
                            .load(album.getPhotoprofile())
                            .into(holder.profileImageView);
                } else {
                    holder.profileImageView.setImageResource(R.drawable.ppdf2);
                }

                // Handle like button click
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
                                                albumRef.child("likes").setValue(ServerValue.increment(-1));
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
                                                    albumRef.child("likes").setValue(ServerValue.increment(1));
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
                                recyclerView.setVisibility(View.VISIBLE);
                            } else {
                                noneTextView.setVisibility(View.GONE);
                                recyclerView.setVisibility(View.VISIBLE);
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

                // Handle follow button click
                holder.followButton.setOnClickListener(v -> handleFollow(album, holder));

                // Handle remove button click
                holder.removeButton.setOnClickListener(v -> {
                    DatabaseReference bookmarkRef = FirebaseDatabase.getInstance().getReference()
                            .child("users").child(currentUserId)
                            .child("bookmark").child(collageId)
                            .child(album.getAlbumId());

                    bookmarkRef.removeValue()
                            .addOnSuccessListener(aVoid -> Toast.makeText(getContext(),
                                    "Removed from collection", Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e -> Toast.makeText(getContext(),
                                    "Failed to remove", Toast.LENGTH_SHORT).show());
                });
            }

            @Override
            public void onViewRecycled(@NonNull AlbumViewHolder holder) {
                super.onViewRecycled(holder);
                int position = holder.getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    BookmarkData album = savedAlbums.get(position);
                    DatabaseReference likesRef = FirebaseDatabase.getInstance().getReference()
                            .child("users")
                            .child(album.getUserId())
                            .child("album")
                            .child(album.getAlbumId())
                            .child("likes");

                    // Remove the stored listener
                    if (holder.getLikeCountListener() != null) {
                        likesRef.removeEventListener(holder.getLikeCountListener());
                    }
                }
            }



            private void checkFollowStatus(BookmarkData album, AlbumViewHolder holder) {
                DatabaseReference followRef = FirebaseDatabase.getInstance().getReference()
                        .child("users").child(currentUserId)
                        .child("followedUser").child(album.getUserId());

                followRef.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        boolean isFollowing = snapshot.exists();
                        holder.followButton.setText(isFollowing ? "Following" : "Follow");
                        holder.followButton.setSelected(isFollowing);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(getContext(), "Failed to check follow status", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            private void handleFollow(BookmarkData album, AlbumViewHolder holder) {
                DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users");
                DatabaseReference followRef = userRef.child(currentUserId)
                        .child("followedUser").child(album.getUserId());

                followRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        boolean isFollowing = snapshot.exists();
                        if (isFollowing) {
                            // Unfollow
                            userRef.child(album.getUserId()).child("followers").setValue(ServerValue.increment(-1));
                            userRef.child(currentUserId).child("followed").setValue(ServerValue.increment(-1));
                            userRef.child(album.getUserId()).child("followersUser").child(currentUserId).removeValue();
                            userRef.child(currentUserId).child("followedUser").child(album.getUserId()).removeValue();

                            holder.followButton.setText("Follow");
                            holder.followButton.setSelected(false);
                        } else {
                            // Follow
                            userRef.child(album.getUserId()).child("followers").setValue(ServerValue.increment(1));
                            userRef.child(currentUserId).child("followed").setValue(ServerValue.increment(1));
                            userRef.child(album.getUserId()).child("followersUser").child(currentUserId).setValue(true);
                            userRef.child(currentUserId).child("followedUser").child(album.getUserId()).setValue(true);

                            holder.followButton.setText("Following");
                            holder.followButton.setSelected(true);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(holder.itemView.getContext(),
                                "Error updating follow status", Toast.LENGTH_SHORT).show();
                    }
                });
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

            @Override
            public int getItemCount() {
                return savedAlbums.size();
            }

            class AlbumViewHolder extends RecyclerView.ViewHolder {
                ImageView profileImageView;
                ImageView albumImageView;
                ImageButton likeButton;
                ImageView commentButton;
                ImageButton removeButton;
                TextView usernameTextView;
                TextView titleTextView;
                TextView descriptionTextView;
                TextView uploadDateTextView;
                TextView likeCount;
                TextView followButton;
                LinearLayout linearUserId;

                AlbumViewHolder(@NonNull View itemView) {
                    super(itemView);
                    profileImageView = itemView.findViewById(R.id.photoProfile);
                    albumImageView = itemView.findViewById(R.id.imageView);
                    likeButton = itemView.findViewById(R.id.btnLike);
                    commentButton = itemView.findViewById(R.id.btnComment);
                    removeButton = itemView.findViewById(R.id.bookmark);
                    usernameTextView = itemView.findViewById(R.id.usernameUploader);
                    titleTextView = itemView.findViewById(R.id.textTitle);
                    linearUserId = itemView.findViewById(R.id.linearUserId);
                    descriptionTextView = itemView.findViewById(R.id.textDescription);
                    uploadDateTextView = itemView.findViewById(R.id.uploadDateTextView);
                    likeCount = itemView.findViewById(R.id.likeCount);
                    followButton = itemView.findViewById(R.id.follow);

                    linearUserId.setOnClickListener(v -> {
                        String userId = savedAlbums.get(getAdapterPosition()).getUserId();
                        SavedAlbumsFragment.this.navigateToUserProfile(userId);
                    });

                    albumImageView.setOnClickListener(v -> {
                        // Create intent to open FullscreenImageActivity
                        Intent intent = new Intent(itemView.getContext(), FullscreenImageActivity.class);
                        intent.putExtra("imageUrl", savedAlbums.get(getAdapterPosition()).getImageUrl());
                        intent.putExtra("userId", savedAlbums.get(getAdapterPosition()).getUserId());
                        intent.putExtra("username", savedAlbums.get(getAdapterPosition()).getUsername());
                        intent.putExtra("imageTitle", savedAlbums.get(getAdapterPosition()).getTitle());
                        intent.putExtra("imageDescription", savedAlbums.get(getAdapterPosition()).getDescription());

                        // Start activity without default transition animation
                        itemView.getContext().startActivity(intent);
                        ((Activity) itemView.getContext()).overridePendingTransition(0, 0);
                    });


                    // Add click listener for profile image as well
                    profileImageView.setOnClickListener(v -> {
                        String userId = savedAlbums.get(getAdapterPosition()).getUserId();
                        SavedAlbumsFragment.this.navigateToUserProfile(userId);
                    });
                }

                public void setupLikeCountListener(AlbumViewHolder holder, BookmarkData album) {
                    // Remove existing listener if any
                    if (likeCountListener != null) {
                        DatabaseReference likesRef = FirebaseDatabase.getInstance().getReference()
                                .child("users")
                                .child(album.getUserId())
                                .child("album")
                                .child(album.getAlbumId())
                                .child("likes");
                        likesRef.removeEventListener(likeCountListener);
                    }

                    // Create and store new listener
                    DatabaseReference likesRef = FirebaseDatabase.getInstance().getReference()
                            .child("users")
                            .child(album.getUserId())
                            .child("album")
                            .child(album.getAlbumId())
                            .child("likes");

                    likeCountListener = new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            Integer likes = snapshot.getValue(Integer.class);
                            int likeCount = (likes != null) ? likes : 0;
                            album.setLikes(likeCount);
                            holder.likeCount.setText(formatLikesCount(likeCount));
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            // Handle error
                        }
                    };
                    likesRef.addValueEventListener(likeCountListener);
                }

                public ValueEventListener getLikeCountListener() {
                    return likeCountListener;
                }

            }
        }

        @Override
        public void onDestroyView() {
            super.onDestroyView();

            // Bersihkan listener saat view dihancurkan
            if (savedAlbumsListener != null) {
                DatabaseReference bookmarkRef = FirebaseDatabase.getInstance().getReference()
                        .child("users").child(currentUserId).child("bookmark").child(collageId);
                bookmarkRef.removeEventListener(savedAlbumsListener);
            }
        }
    }