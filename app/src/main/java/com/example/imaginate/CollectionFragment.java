package com.example.imaginate;

import android.app.Activity;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.view.menu.MenuPopupHelper;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.cache.InternalCacheDiskCacheFactory;
import com.bumptech.glide.load.engine.cache.LruResourceCache;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestOptions;
import com.example.imaginate.models.Album;
import com.example.imaginate.models.Comment;
import com.example.imaginate.models.LikeData;
import com.example.imaginate.models.User;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class CollectionFragment extends Fragment {

    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView recyclerView;
    private ArrayList<Album> albumList;
    private RefreshAlbum refreshAlbum;
    private DatabaseReference databaseReference;
    private TextView usernameUploader;
    private LinearLayout noneTextView;
    private String currentUsername;
    private String currentUserId;
    private String currentUserPhotoprofile;
    private ImageView photoProfile;
    private View loadingPage;
    private boolean isReceiverRegistered = false;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_collection, container, false);

        RequestOptions requestOptions = new RequestOptions()
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .priority(Priority.HIGH);

        Glide.with(this)
                .setDefaultRequestOptions(requestOptions);

        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        recyclerView = view.findViewById(R.id.recyclerView);
        noneTextView = view.findViewById(R.id.noneTextView);
        loadingPage = view.findViewById(R.id.loading_page);
        recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setHasFixedSize(true);
        recyclerView.setItemViewCacheSize(20);
        // Enable large heap for bitmap pooling
        recyclerView.setDrawingCacheEnabled(true);
        recyclerView.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);

        albumList = new ArrayList<>();
        refreshAlbum = new RefreshAlbum(albumList, this);
        recyclerView.setAdapter(refreshAlbum);

        databaseReference = FirebaseDatabase.getInstance().getReference("users");
        SharedPreferences sharedPreferences = requireActivity().getSharedPreferences("user_prefs", getContext().MODE_PRIVATE);
        currentUsername = sharedPreferences.getString("username", null);
        currentUserId = sharedPreferences.getString("userId", null);
        currentUserPhotoprofile = sharedPreferences.getString("photoprofile", null);

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (layoutManager != null) {
                    int firstVisible = layoutManager.findFirstVisibleItemPosition();
                    int lastVisible = layoutManager.findLastVisibleItemPosition();

                    // Preload images for next items
                    for (int i = lastVisible + 1; i < Math.min(lastVisible + 5, albumList.size()); i++) {
                        Album album = albumList.get(i);
                        Glide.with(requireContext())
                                .load(album.getUrl())
                                .diskCacheStrategy(DiskCacheStrategy.ALL)
                                .preload();

                        // Also preload profile images
                        if (album.getPhotoprofile() != null && !album.getPhotoprofile().isEmpty()) {
                            Glide.with(requireContext())
                                    .load(album.getPhotoprofile())
                                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                                    .preload();
                        }
                    }
                }
            }
        });

        swipeRefreshLayout.setProgressViewEndTarget(true, 70);

        swipeRefreshLayout.setColorSchemeResources(
                R.color.yellow
        );

        swipeRefreshLayout.setProgressBackgroundColorSchemeResource(
                R.color.blacksmooth2
        );

        swipeRefreshLayout.setOnRefreshListener(() -> {
            fetchAlbums();
        });
        return view;
    }

    private final BroadcastReceiver refreshReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            fetchAlbums();
        }
    };

    @Override
    public void onStart() {
        super.onStart();
        if (!isReceiverRegistered) {
            requireContext().registerReceiver(refreshReceiver, new IntentFilter("REFRESH_ALBUMS"));
            isReceiverRegistered = true;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (isReceiverRegistered) {
            requireContext().unregisterReceiver(refreshReceiver);
            isReceiverRegistered = false;
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        fetchAlbums();
    }

    private void fetchAlbums() {
        String loggedInUser = requireActivity().getSharedPreferences("user_prefs", getContext().MODE_PRIVATE)
                .getString("userId", "");

        loadingPage.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);

        AtomicInteger loadingTasks = new AtomicInteger(2);

        Runnable checkLoadingComplete = () -> {
            if (loadingTasks.decrementAndGet() == 0) {
                loadingPage.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
                swipeRefreshLayout.setRefreshing(false);
            }
        };

        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(loggedInUser);
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                checkLoadingComplete.run();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Failed to load user data", Toast.LENGTH_SHORT).show();
                checkLoadingComplete.run();
            }
        });

        databaseReference.child(loggedInUser).child("album").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                albumList.clear();

                for (DataSnapshot itemSnapshot : snapshot.getChildren()) {
                    String albumId = itemSnapshot.getKey();
                    String title = itemSnapshot.child("title").getValue(String.class);
                    String description = itemSnapshot.child("description").getValue(String.class);
                    String url = itemSnapshot.child("url").getValue(String.class);
                    String photoprofile = itemSnapshot.child("photoprofile").getValue(String.class);
                    Long timestamp = itemSnapshot.child("timestamp").getValue(Long.class);
                    String uploadDate = itemSnapshot.child("humanReadableDate").getValue(String.class);

                    Boolean userHasLikedValue = itemSnapshot.child("userHasLiked").getValue(Boolean.class);
                    boolean userHasLiked = userHasLikedValue != null && userHasLikedValue;

                    Integer likesValue = itemSnapshot.child("likes").getValue(Integer.class);
                    int likes = likesValue != null ? likesValue : 0;

                    if (title != null && description != null && url != null && albumId != null) {
                        Album album = new Album(
                                loggedInUser,
                                albumId,
                                title,
                                loggedInUser,
                                description,
                                url,
                                photoprofile,
                                likes,
                                userHasLiked,
                                timestamp != null ? timestamp : 0
                        );

                        album.setUploadDate(uploadDate);
                        albumList.add(album);
                    }
                }

                albumList.sort((a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
                refreshAlbum.notifyDataSetChanged();

                if (albumList.isEmpty()) {
                    noneTextView.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.VISIBLE);
                } else {
                    noneTextView.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);
                }

                checkLoadingComplete.run();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Failed to load albums", Toast.LENGTH_SHORT).show();
                checkLoadingComplete.run();
            }
        });
    }

    public class RefreshAlbum extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private static final int TYPE_HEADER = 0;
        private static final int TYPE_ITEM = 1;
        private String albumId;
        private final ArrayList<Album> albumList;
        private final CollectionFragment collectionFragment;

        public RefreshAlbum(ArrayList<Album> albumList, CollectionFragment collectionFragment) {
            this.albumList = albumList;
            this.collectionFragment = collectionFragment;
        }

        @Override
        public int getItemViewType(int position) {
            if (position == 0) {
                return TYPE_HEADER;
            } else {
                return TYPE_ITEM;
            }
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            if (viewType == TYPE_HEADER) {
                View headerView = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_header, parent, false);
                return new HeaderViewHolder(headerView);
            } else {
                View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_collection, parent, false);
                return new AlbumViewHolder(itemView);
            }
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            if (holder instanceof HeaderViewHolder) {
                SharedPreferences sharedPreferences = holder.itemView.getContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
                String userId = sharedPreferences.getString("userId", null);
                ((HeaderViewHolder) holder).bind(userId, holder.itemView.getContext());
            }
            if (getItemViewType(position) == TYPE_HEADER) {
                HeaderViewHolder headerViewHolder = (HeaderViewHolder) holder;
            } else {
                AlbumViewHolder albumViewHolder = (AlbumViewHolder) holder;
                Album album = albumList.get(position - 1);
                albumViewHolder.album = album;

                albumViewHolder.btnOthers.setOnClickListener(v -> {
                    Context context = v.getContext();
                    PopupMenu popupMenu = new PopupMenu(context, v, 0, 0, R.style.CustomPopupMenu);
                    popupMenu.inflate(R.menu.popup_menu_collection_act);

                    popupMenu.getMenu().setGroupDividerEnabled(true); // Menambah divider antar item (Opsional)
                    try {
                        Field popup = PopupMenu.class.getDeclaredField("mPopup");
                        popup.setAccessible(true);
                        MenuPopupHelper menuHelper = (MenuPopupHelper) popup.get(popupMenu);
                        menuHelper.setForceShowIcon(true); // Agar ikon tetap terlihat

                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    final int adapterPosition = albumViewHolder.getAdapterPosition();
                    if (adapterPosition != RecyclerView.NO_POSITION && adapterPosition != 0) {
                        final int albumPosition = adapterPosition - 1;
                        if (albumPosition >= 0 && albumPosition < albumList.size()) {


                            popupMenu.setOnMenuItemClickListener(item -> {
                                int itemId = item.getItemId();

                                if (itemId == R.id.action_editalbum) {
                                    BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(v.getContext(), R.style.BottomSheetDialogTheme);
                                    View dialogView = LayoutInflater.from(v.getContext()).inflate(R.layout.dialog_edit_album, null);

                                    EditText editTitle = dialogView.findViewById(R.id.editTitle);
                                    EditText editDescription = dialogView.findViewById(R.id.editDescriptiom);
                                    TextView cancelBtn = dialogView.findViewById(R.id.cancel);
                                    TextView saveBtn = dialogView.findViewById(R.id.add);

                                    // Set current values
                                    editTitle.setText(album.getTitle());
                                    editDescription.setText(album.getDescription());

                                    bottomSheetDialog.setContentView(dialogView);
                                    bottomSheetDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

                                    // Handle cancel button
                                    cancelBtn.setOnClickListener(view -> bottomSheetDialog.dismiss());

                                    // Handle save button
                                    saveBtn.setOnClickListener(view -> {
                                        String newTitle = editTitle.getText().toString().trim();
                                        String newDescription = editDescription.getText().toString().trim();

                                        if (newTitle.isEmpty()) {
                                            editTitle.setError("Title cannot be empty");
                                            return;
                                        }

                                        if (newDescription.isEmpty()) {
                                            editDescription.setError("Description cannot be empty");
                                            return;
                                        }

                                        // Update in Firebase
                                        DatabaseReference albumRef = FirebaseDatabase.getInstance().getReference("users")
                                                .child(album.getUserId())
                                                .child("album")
                                                .child(album.getId());

                                        albumRef.child("title").setValue(newTitle);
                                        albumRef.child("description").setValue(newDescription)
                                                .addOnSuccessListener(aVoid -> {
                                                    Toast.makeText(v.getContext(), "Album berhasil diperbarui", Toast.LENGTH_SHORT).show();
                                                    album.setTitle(newTitle);
                                                    album.setDescription(newDescription);
                                                    notifyItemChanged(adapterPosition);
                                                    bottomSheetDialog.dismiss();
                                                })
                                                .addOnFailureListener(e -> {
                                                    Toast.makeText(v.getContext(), "Gagal memperbarui album", Toast.LENGTH_SHORT).show();
                                                });
                                    });

                                    bottomSheetDialog.show();
                                    return true;

                                } else if (itemId == R.id.action_delete) {
                                    AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
                                    View deleteDialogView = LayoutInflater.from(v.getContext()).inflate(R.layout.dialog_delete_album, null);

                                    TextView cancelButton = deleteDialogView.findViewById(R.id.textView4);
                                    TextView deleteButton = deleteDialogView.findViewById(R.id.textView3);

                                    builder.setView(deleteDialogView);
                                    AlertDialog dialog = builder.create();
                                    dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

                                    cancelButton.setOnClickListener(view -> dialog.dismiss());

                                    deleteButton.setOnClickListener(view -> {
                                        View itemView = albumViewHolder.itemView;

                                        // Initial animation
                                        itemView.animate()
                                                .alpha(0.5f)
                                                .translationX(-50f)
                                                .setDuration(300)
                                                .start();

                                        DatabaseReference albumRef = FirebaseDatabase.getInstance().getReference("users")
                                                .child(album.getUserId())
                                                .child("album")
                                                .child(album.getId());
                                        albumRef.removeValue()
                                                .addOnSuccessListener(aVoid -> {
                                                    DatabaseReference postRef = FirebaseDatabase.getInstance().getReference("users")
                                                            .child(album.getUserId())
                                                            .child("post");
                                                    postRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                                        @Override
                                                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                            Integer currentPostCount = snapshot.getValue(Integer.class);
                                                            if (currentPostCount != null && currentPostCount > 0) {
                                                                postRef.setValue(currentPostCount - 1);
                                                            }
                                                            StorageReference imageRef = FirebaseStorage.getInstance().getReference()
                                                                    .child("uploads")
                                                                    .child(album.getUserId())
                                                                    .child(album.getUrl());
                                                            imageRef.delete().addOnSuccessListener(aVoid2 -> {
                                                                albumList.remove(albumPosition);
                                                                new Handler(Looper.getMainLooper()).post(() -> {
                                                                    notifyItemRemoved(adapterPosition);
                                                                    notifyItemRangeChanged(adapterPosition, albumList.size());
                                                                    if (albumList.isEmpty()) {
                                                                        noneTextView.setVisibility(View.VISIBLE);
                                                                    }
                                                                    collectionFragment.fetchAlbums();
                                                                });
                                                                Toast.makeText(v.getContext(), "Album berhasil dihapus", Toast.LENGTH_SHORT).show();
                                                            }).addOnFailureListener(e -> {
                                                                Toast.makeText(v.getContext(), "Gagal menghapus gambar", Toast.LENGTH_SHORT).show();
                                                            });
                                                        }

                                                        @Override
                                                        public void onCancelled(@NonNull DatabaseError error) {
                                                            itemView.animate()
                                                                    .alpha(1f)
                                                                    .translationX(0f)
                                                                    .setDuration(200)
                                                                    .start();
                                                            Toast.makeText(v.getContext(), "Gagal memperbarui jumlah post", Toast.LENGTH_SHORT).show();
                                                        }
                                                    });
                                                })
                                                .addOnFailureListener(e -> {
                                                    Toast.makeText(v.getContext(), "Gagal menghapus album", Toast.LENGTH_SHORT).show();
                                                });
                                        dialog.dismiss();
                                    });
                                    dialog.show();
                                    return true;
                                } else {
                                    return false;
                                }
                            });
                        }
                    }

                    popupMenu.show();
                });

                albumViewHolder.likeCount.setText(formatLikesCount(album.getLikes()));
                albumViewHolder.titleTextView.setText(album.getTitle());
                albumViewHolder.descriptionTextView.setText(album.getDescription());
                albumViewHolder.uploadDateTextView.setText(album.getUploadDate());
                Glide.with(albumViewHolder.itemView.getContext())
                        .load(album.getUrl())
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .placeholder(R.drawable.blacksmooth2_drawable)
                        .override(1000, 1000)
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .into(albumViewHolder.imageView);

                DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(album.getUsername());
                userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String username = snapshot.child("username").getValue(String.class);
                        String photoProfileUrl = snapshot.child("photoprofile").getValue(String.class);

                        if (username != null) {
                            albumViewHolder.usernameUploader.setText(username);
                        } else {
                            albumViewHolder.usernameUploader.setText("Unknown User");
                        }


                        if (photoProfileUrl != null) {
                            Glide.with(albumViewHolder.itemView.getContext())
                                    .load(photoProfileUrl)
                                    .placeholder(R.drawable.default_profile_bct_v1)
                                    .error(R.drawable.default_profile_bct_v1)
                                    .into(albumViewHolder.photoProfile);
                        } else {
                            albumViewHolder.photoProfile.setImageResource(R.drawable.default_profile_bct_v1);
                        }
                    }


                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(albumViewHolder.itemView.getContext(), "Failed to load user data", Toast.LENGTH_SHORT).show();
                    }
                });
                albumViewHolder.commentButton.setOnClickListener(v -> {
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


                    ShapeableImageView photoProfileCommentBox = bottomSheetView.findViewById(R.id.photoprofilecmb);

                    RecyclerView commentRecyclerView = bottomSheetView.findViewById(R.id.commentRecyclerView);
                    EditText commentEditText = bottomSheetView.findViewById(R.id.commentEditText);
                    ImageButton sendButton = bottomSheetView.findViewById(R.id.sendButton);
                    ConstraintLayout commentBox = bottomSheetView.findViewById(R.id.commentBox);
                    ConstraintLayout containerTag = bottomSheetView.findViewById(R.id.containerTag);
                    TextView usernameTag = bottomSheetView.findViewById(R.id.usernameTag);
                    ImageButton cancelTag = bottomSheetView.findViewById(R.id.cancelTag);
                    LinearLayout noneTextView = bottomSheetView.findViewById(R.id.noneTextView);


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

                    // Inside the click listener for commentButton, after initializing the bottomSheetView
// Add this code after finding other views but before setting up the listeners


// Get current user's photo profile
                    DatabaseReference currentUserRef = FirebaseDatabase.getInstance().getReference("users").child(currentUserId);
                    currentUserRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            String photoProfileUrl = snapshot.child("photoprofile").getValue(String.class);

                            if (photoProfileUrl != null && !photoProfileUrl.isEmpty()) {
                                // Load photo profile using Glide
                                Glide.with(bottomSheetView.getContext())
                                        .load(photoProfileUrl)
                                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                                        .placeholder(R.drawable.default_profile_bct_v1)
                                        .error(R.drawable.default_profile_bct_v1)
                                        .into(photoProfileCommentBox);
                            } else {
                                // Set default profile image if no photo URL exists
                                photoProfileCommentBox.setImageResource(R.drawable.default_profile_bct_v1);
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            // Handle error by showing default profile image
                            photoProfileCommentBox.setImageResource(R.drawable.default_profile_bct_v1);
                            Toast.makeText(bottomSheetView.getContext(),
                                    "Failed to load profile photo", Toast.LENGTH_SHORT).show();
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

                albumViewHolder.likesButton.setOnClickListener(v -> {
                    showAlbumLikesBottomSheet(album.getUserId(), album.getId(), holder.itemView.getContext());
                });
//                albumViewHolder.deleteView.setOnClickListener(view -> {
//                    AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext());
//                    View deleteDialogView = LayoutInflater.from(view.getContext()).inflate(R.layout.dialog_delete_album, null);
//
//                    TextView cancelButton = deleteDialogView.findViewById(R.id.textView4);
//                    TextView deleteButton = deleteDialogView.findViewById(R.id.textView3);
//
//                    builder.setView(deleteDialogView);
//                    AlertDialog dialog = builder.create();
//                    dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
//
//                    cancelButton.setOnClickListener(v -> dialog.dismiss());
//
////                    deleteButton.setOnClickListener(v -> {
////                        final int adapterPosition = albumViewHolder.getAdapterPosition();
////                        if (adapterPosition == RecyclerView.NO_POSITION || adapterPosition == 0) {
////                            return;
////                        }
////
////                        final int albumPosition = adapterPosition - 1;
////                        if (albumPosition < 0 || albumPosition >= albumList.size()) {
////                            return;
////                        }
////
////                        Album albumToDelete = albumList.get(albumPosition);
////                        View itemView = albumViewHolder.itemView;
////
////                        // Initial animation
////                        itemView.animate()
////                                .alpha(0.5f)
////                                .translationX(-50f)
////                                .setDuration(300)
////                                .start();
////
////                        DatabaseReference albumRef = FirebaseDatabase.getInstance().getReference("users")
////                                .child(albumToDelete.getUserId())
////                                .child("album")
////                                .child(albumToDelete.getId());
////                        albumRef.removeValue()
////                                .addOnSuccessListener(aVoid -> {
////                                    DatabaseReference postRef = FirebaseDatabase.getInstance().getReference("users")
////                                            .child(albumToDelete.getUserId())
////                                            .child("post");
////                                    postRef.addListenerForSingleValueEvent(new ValueEventListener() {
////                                        @Override
////                                        public void onDataChange(@NonNull DataSnapshot snapshot) {
////                                            Integer currentPostCount = snapshot.getValue(Integer.class);
////                                            if (currentPostCount != null && currentPostCount > 0) {
////                                                postRef.setValue(currentPostCount - 1);
////                                            }
////                                            StorageReference imageRef = FirebaseStorage.getInstance().getReference()
////                                                    .child("uploads")
////                                                    .child(albumToDelete.getUserId())
////                                                    .child(albumToDelete.getUrl());
////                                            imageRef.delete().addOnSuccessListener(aVoid2 -> {
////                                                albumList.remove(albumPosition);
////                                                new Handler(Looper.getMainLooper()).post(() -> {
////                                                    notifyItemRemoved(adapterPosition);
////                                                    notifyItemRangeChanged(adapterPosition, albumList.size());
////                                                    if (albumList.isEmpty()) {
////                                                        noneTextView.setVisibility(View.VISIBLE);
////                                                    }
////                                                    collectionFragment.fetchAlbums();
////                                                });
////                                                Toast.makeText(view.getContext(), "Album berhasil dihapus", Toast.LENGTH_SHORT).show();
////                                            }).addOnFailureListener(e -> {
////                                                Toast.makeText(view.getContext(), "Gagal menghapus gambar", Toast.LENGTH_SHORT).show();
////                                            });
////                                        }
////
////                                        @Override
////                                        public void onCancelled(@NonNull DatabaseError error) {
////                                            itemView.animate()
////                                                    .alpha(1f)
////                                                    .translationX(0f)
////                                                    .setDuration(200)
////                                                    .start();
////                                            Toast.makeText(view.getContext(), "Gagal memperbarui jumlah post", Toast.LENGTH_SHORT).show();
////                                        }
////                                    });
////                                })
////                                .addOnFailureListener(e -> {
////                                    Toast.makeText(view.getContext(), "Gagal menghapus album", Toast.LENGTH_SHORT).show();
////                                });
////                        dialog.dismiss();
////                    });
////                    dialog.show();
//
//                });
            }
        }

        private void showAlbumLikesBottomSheet(String userId, String albumId, Context context) {
            BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(context, R.style.BottomSheetDialogTheme);
            View bottomSheetView = LayoutInflater.from(context).inflate(R.layout.fragment_likes, null);
            bottomSheetDialog.setContentView(bottomSheetView);
            bottomSheetDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

            // Set up RecyclerView
            RecyclerView likesRecyclerView = bottomSheetView.findViewById(R.id.likesRecyclerView);
            ArrayList<LikeData> likesList = new ArrayList<>();
            UserLikesAdapter userLikesAdapter = new UserLikesAdapter(likesList, context);
            likesRecyclerView.setLayoutManager(new LinearLayoutManager(context));
            likesRecyclerView.setAdapter(userLikesAdapter);

            // Get reference to album likes
            DatabaseReference likesRef = FirebaseDatabase.getInstance().getReference("users")
                    .child(userId)
                    .child("album")
                    .child(albumId)
                    .child("userLikes");

            likesRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    likesList.clear();
                    for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                        String userId = userSnapshot.child("userId").getValue(String.class);
                        String username = userSnapshot.child("username").getValue(String.class);
                        String photoProfileLikes = userSnapshot.child("photoProfile").getValue(String.class);
                        String tagName = userSnapshot.child("tagName").getValue(String.class); // Ambil tagName

                        Log.d("FirebaseData", "UserID: " + userId + ", Username: " + username + ", PhotoProfile: " + photoProfileLikes + ", TagName: " + tagName);

                        if (userId != null && username != null) {
                            LikeData user = new LikeData(
                                    userId,
                                    username,
                                    (photoProfileLikes != null && !photoProfileLikes.isEmpty()) ? photoProfileLikes : "DEFAULT_IMAGE_URL",
                                    (tagName != null && !tagName.isEmpty()) ? tagName : "No Tag" // Tambahkan tagName dengan default
                            );
                            likesList.add(user);
                        }
                    }
                    userLikesAdapter.notifyDataSetChanged();
                }


                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(context, "Failed to load likes", Toast.LENGTH_SHORT).show();
                }
            });


            bottomSheetDialog.show();
        }

        @Override
        public int getItemCount() {
            return albumList.size() + 1;
        }

        SharedPreferences sharedPreferences = requireActivity().getSharedPreferences("user_prefs", getContext().MODE_PRIVATE);
        String userId = sharedPreferences.getString("userId", null);

        public class HeaderViewHolder extends RecyclerView.ViewHolder {
            LinearLayout headerText;
            TextView profileUsername, tagName, followersCount, followedCount, postCount;
            TextView descriptionProfile, location;
            ImageView photoProfile;
            ImageButton btnLogout, editProfileButton;

            public HeaderViewHolder(View itemView) {
                super(itemView);
                headerText = itemView.findViewById(R.id.headercollection);
                profileUsername = itemView.findViewById(R.id.profile_username);
                tagName = itemView.findViewById(R.id.tagName);
                photoProfile = itemView.findViewById(R.id.photoProfile);
                btnLogout = itemView.findViewById(R.id.btnLogout);
                editProfileButton = itemView.findViewById(R.id.EditProfile);
                followersCount = itemView.findViewById(R.id.followers);
                followedCount = itemView.findViewById(R.id.followed);
                postCount = itemView.findViewById(R.id.post);
                descriptionProfile = itemView.findViewById(R.id.descriptionprofile);
                location = itemView.findViewById(R.id.location);

                followersCount.setOnClickListener(v -> showFollowersBottomSheet(userId, itemView.getContext()));
                followedCount.setOnClickListener(v -> showFollowedBottomSheet(userId, itemView.getContext()));
            }

            private void showFollowersBottomSheet(String userId, Context context) {
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

            private void showFollowedBottomSheet(String userId, Context context) {
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

            public void bind(String userId, Context context) {
                if (userId != null) {
                    DatabaseReference databaseRef = FirebaseDatabase.getInstance().getReference("users").child(userId);
                    StorageReference storageRef = FirebaseStorage.getInstance().getReference("uploads").child(userId).child("profile");

                    // Load data profil
                    databaseRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            String username = snapshot.child("username").getValue(String.class);
                            String tag = snapshot.child("tagName").getValue(String.class);
                            String photoUrl = snapshot.child("photoprofile").getValue(String.class);
                            String bio = snapshot.child("bio").getValue(String.class); // Get bio
                            String userLocation = snapshot.child("location").getValue(String.class);

                            if (username != null && tag != null) {
                                profileUsername.setText(username);
                                tagName.setText(tag);
                            } else {
                                Toast.makeText(context, "Gagal mengambil data profil", Toast.LENGTH_SHORT).show();
                            }

                            if (bio != null && !bio.isEmpty()) {
                                descriptionProfile.setText(bio);
                            } else {
                                descriptionProfile.setText("No bio available");
                            }

                            if (userLocation != null && !userLocation.isEmpty()) {
                                location.setText(userLocation);
                            } else {
                                location.setText("Location not set");
                            }

                            if (photoUrl != null && !photoUrl.isEmpty()) {
                                Glide.with(context).load(photoUrl).into(photoProfile);
                            }

                            // Load followers dan followed
                            Long followers = snapshot.child("followers").getValue(Long.class);
                            Long followed = snapshot.child("followed").getValue(Long.class);
                            Long post = snapshot.child("post").getValue(Long.class);

                            followersCount.setText(formatLikesCount(followers != null ? followers : 0));
                            followedCount.setText(formatLikesCount(followed != null ? followed : 0));
                            postCount.setText(String.valueOf(post != null ? post : 0));
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Toast.makeText(context, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });

                    // Logout
                    btnLogout.setOnClickListener(v -> {
                        Intent intent = new Intent(context, Settings.class);
                        context.startActivity(intent);
                    });

                    // Edit profil
                    editProfileButton.setOnClickListener(v -> {
                        Intent intent = new Intent(context, EditProfile.class);
                        context.startActivity(intent);
                    });
                }
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

        public class AlbumViewHolder extends RecyclerView.ViewHolder {
            TextView titleTextView, descriptionTextView, uploadDateTextView, usernameUploader, likeCount;
            ImageView imageView, commentButton, photoProfile;
            ImageButton likesButton;
            View deleteView;
            LinearLayout editAlbum;
            Album album;
            ImageButton btnOthers;

            public AlbumViewHolder(@NonNull View itemView) {
                super(itemView);
                titleTextView = itemView.findViewById(R.id.textTitle);
                descriptionTextView = itemView.findViewById(R.id.textDescription);
                likeCount = itemView.findViewById(R.id.likeCount);
                imageView = itemView.findViewById(R.id.imageView);
                commentButton = itemView.findViewById(R.id.btnComment);
                likesButton = itemView.findViewById(R.id.listLikes);
                uploadDateTextView = itemView.findViewById(R.id.uploadDateTextView);
                usernameUploader = itemView.findViewById(R.id.usernameUploader);
                photoProfile = itemView.findViewById(R.id.photoProfile);
                btnOthers = itemView.findViewById(R.id.btnOthers);


                imageView.setOnClickListener(v -> {
                    Intent intent = new Intent(itemView.getContext(), FullscreenImageActivity.class);
                    Album currentAlbum = albumList.get(getAdapterPosition() - 1);
                    intent.putExtra("imageUrl", currentAlbum.getUrl());
                    intent.putExtra("username", usernameUploader.getText().toString()); // Use the username from TextView
                    intent.putExtra("userId", currentAlbum.getUserId());
                    intent.putExtra("imageTitle", currentAlbum.getTitle());
                    intent.putExtra("imageDescription", currentAlbum.getDescription());

                    // Start activity without transition animation
                    itemView.getContext().startActivity(intent);
                    ((Activity) itemView.getContext()).overridePendingTransition(0, 0);
                });


                commentButton.setOnClickListener(v -> {
                    BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(itemView.getContext(), R.style.BottomSheetDialogTheme);
                    View bottomSheetView = LayoutInflater.from(itemView.getContext())
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
                                Glide.with(itemView.getContext())
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
                        private final int screenHeight = itemView.getContext().getResources().getDisplayMetrics().heightPixels;
                        private int lastVisibleDecorViewHeight = 0;
                        private final int BOTTOM_OFFSET_DP = -2; // Jarak tambahan 20dp
                        private final float density = itemView.getContext().getResources().getDisplayMetrics().density;
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

                    commentRecyclerView.setLayoutManager(new LinearLayoutManager(itemView.getContext()));
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
                                        new ForegroundColorSpan(ContextCompat.getColor(itemView.getContext(), R.color.blue)),
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
                                new ForegroundColorSpan(ContextCompat.getColor(itemView.getContext(), R.color.blue)),
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

                        InputMethodManager imm = (InputMethodManager) itemView.getContext()
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
                            Toast.makeText(itemView.getContext(), "Failed to load comments", Toast.LENGTH_SHORT).show();
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
                                                            InputMethodManager imm = (InputMethodManager) itemView.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                                                            if (imm != null) {
                                                                imm.hideSoftInputFromWindow(commentEditText.getWindowToken(), 0);
                                                            }

                                                            Toast.makeText(itemView.getContext(), "Reply added successfully", Toast.LENGTH_SHORT).show();
                                                        })
                                                        .addOnFailureListener(e -> {
                                                            Toast.makeText(itemView.getContext(), "Failed to add reply: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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
                                                            Toast.makeText(itemView.getContext(),
                                                                    "Comment added", Toast.LENGTH_SHORT).show();
                                                        })
                                                        .addOnFailureListener(e -> {
                                                            Toast.makeText(itemView.getContext(),
                                                                    "Failed to add comment", Toast.LENGTH_SHORT).show();
                                                        });
                                            }
                                        }
                                    } else {
                                        Toast.makeText(itemView.getContext(),
                                                "Failed to retrieve user data", Toast.LENGTH_SHORT).show();
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {
                                    Toast.makeText(itemView.getContext(),
                                            "Failed to load user profile", Toast.LENGTH_SHORT).show();
                                }
                            });
                        } else {
                            Toast.makeText(itemView.getContext(),
                                    "Comment cannot be empty", Toast.LENGTH_SHORT).show();
                        }
                    });

                    bottomSheetDialog.show();
                });

//                editAlbum.setOnClickListener(v -> {
//                    if (album != null) {
//                        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(itemView.getContext(), R.style.BottomSheetDialogTheme);
//                        View dialogView = LayoutInflater.from(itemView.getContext()).inflate(R.layout.dialog_edit_album, null);
//
//                        EditText editTitle = dialogView.findViewById(R.id.editTitle);
//                        EditText editDescription = dialogView.findViewById(R.id.editDescriptiom);
//                        TextView cancelBtn = dialogView.findViewById(R.id.cancel);
//                        TextView saveBtn = dialogView.findViewById(R.id.add);
//
//                        // Set current values
//                        editTitle.setText(album.getTitle());
//                        editDescription.setText(album.getDescription());
//
//                        bottomSheetDialog.setContentView(dialogView);
//                        bottomSheetDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
//
//                        // Handle cancel button
//                        cancelBtn.setOnClickListener(view -> bottomSheetDialog.dismiss());
//
//                        // Handle save button
//                        saveBtn.setOnClickListener(view -> {
//                            String newTitle = editTitle.getText().toString().trim();
//                            String newDescription = editDescription.getText().toString().trim();
//
//                            if (newTitle.isEmpty()) {
//                                editTitle.setError("Title cannot be empty");
//                                return;
//                            }
//
//                            if (newDescription.isEmpty()) {
//                                editDescription.setError("Description cannot be empty");
//                                return;
//                            }
//
//                            // Update in Firebase
//                            DatabaseReference albumRef = FirebaseDatabase.getInstance().getReference("users")
//                                    .child(album.getUserId())
//                                    .child("album")
//                                    .child(album.getId());
//
//                            albumRef.child("title").setValue(newTitle);
//                            albumRef.child("description").setValue(newDescription)
//                                    .addOnSuccessListener(aVoid -> {
//                                        Toast.makeText(itemView.getContext(), "Album updated successfully", Toast.LENGTH_SHORT).show();
//                                        bottomSheetDialog.dismiss();
//                                    })
//                                    .addOnFailureListener(e -> {
//                                        Toast.makeText(itemView.getContext(), "Failed to update album", Toast.LENGTH_SHORT).show();
//                                    });
//                        });
//
//                        bottomSheetDialog.show();
//                    } else {
//                        Toast.makeText(itemView.getContext(), "Cannot edit album at this time", Toast.LENGTH_SHORT).show();
//                    }
//                });

            }

        }


        private void showLikesBottomSheet(String albumId, String userId, Context context) {
            BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(context, R.style.BottomSheetDialogTheme);
            View bottomSheetView = LayoutInflater.from(context).inflate(R.layout.fragment_likes, null);
            bottomSheetDialog.setContentView(bottomSheetView);

            RecyclerView likesRecyclerView = bottomSheetView.findViewById(R.id.likesRecyclerView);
            ArrayList<User> likesList = new ArrayList<>();
            UserAdapter userAdapter = new UserAdapter(likesList, context);
            likesRecyclerView.setLayoutManager(new LinearLayoutManager(context));
            likesRecyclerView.setAdapter(userAdapter);

            DatabaseReference likesRef = FirebaseDatabase.getInstance().getReference("users")
                    .child(userId)
                    .child("album")
                    .child(albumId)
                    .child("userLikes");

            likesRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    likesList.clear();

                    for (DataSnapshot likeSnapshot : snapshot.getChildren()) {
                        String likeUserId = likeSnapshot.getKey();

                        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(likeUserId);
                        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot userSnapshot) {
                                String username = userSnapshot.child("username").getValue(String.class);
                                String photoProfile = userSnapshot.child("photoprofile").getValue(String.class);
                                String tagName = userSnapshot.child("tagName").getValue(String.class);

                                if (username != null) {
                                    User user = new User(likeUserId, username, photoProfile != null ? photoProfile : "");
                                    user.setTagName(tagName != null ? tagName : "");
                                    likesList.add(user);
                                    userAdapter.notifyDataSetChanged();
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                Toast.makeText(context, "Failed to load user data", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(context, "Failed to load likes", Toast.LENGTH_SHORT).show();
                }
            });

            bottomSheetDialog.show();
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


    }
}
