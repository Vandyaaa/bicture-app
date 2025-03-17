package com.example.imaginate;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
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
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.engine.cache.InternalCacheDiskCacheFactory;
import com.bumptech.glide.load.engine.cache.LruResourceCache;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
import com.example.imaginate.models.Album;
import com.example.imaginate.models.BookmarkData;
import com.example.imaginate.models.CollageItem;
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

public class HomeFragment extends Fragment {

    private RecyclerView recyclerView;
    private ArrayList<Album> albumList;
    private AlbumAdapter albumAdapter;
    private DatabaseReference databaseReference;
    private String currentUsername;
    private String currentUserId;
    private String currentUserPhotoprofile;
    private String profileButton;
    private Button homeButton;
    private Button followedButton;
    private ValueEventListener albumsListener;
    private ValueEventListener followedListener;
    private boolean isShowingFollowedOnly = false;
    private SwipeRefreshLayout swipeRefreshLayout;
    private View loadingPage;
    private LinearLayout noFollowers;
    // Tambahkan map untuk cache status di HomeFragment
    private Map<String, Boolean> likeStatusCache = new HashMap<>();
    private Map<String, Boolean> bookmarkStatusCache = new HashMap<>();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        RequestOptions requestOptions = new RequestOptions()
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .priority(Priority.HIGH)
                .override(1000, 1000) // Limit maximum image size
                .format(DecodeFormat.PREFER_RGB_565) // Use less memory
                .encodeQuality(85); // Slightly reduce quality for better performance

        Glide.with(this)
                .setDefaultRequestOptions(requestOptions);

        recyclerView = view.findViewById(R.id.recyclerView);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setHasFixedSize(true);
        recyclerView.setItemViewCacheSize(20);
        noFollowers = view.findViewById(R.id.noFollowers);
        loadingPage = view.findViewById(R.id.loading_page);
        recyclerView.setLayerType(View.LAYER_TYPE_HARDWARE, null);

        // Add prefetch for smoother scrolling
        layoutManager.setInitialPrefetchItemCount(4);

        RecyclerView.RecycledViewPool viewPool = new RecyclerView.RecycledViewPool();
        viewPool.setMaxRecycledViews(0, 12);
        recyclerView.setRecycledViewPool(viewPool);

        // Enable large heap for bitmap pooling
        recyclerView.setDrawingCacheEnabled(true);
        recyclerView.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);
        albumList = new ArrayList<>();
        albumAdapter = new AlbumAdapter(albumList);
        recyclerView.setAdapter(albumAdapter);

        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        swipeRefreshLayout.setProgressViewEndTarget(true, 70);

        swipeRefreshLayout.setColorSchemeResources(
                R.color.yellow
        );

        swipeRefreshLayout.setProgressBackgroundColorSchemeResource(
                R.color.blacksmooth2
        );

        // Set up refresh listener
        swipeRefreshLayout.setOnRefreshListener(() -> {
            fetchData();
            swipeRefreshLayout.setRefreshing(false);
        });

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    Glide.with(requireContext()).resumeRequests();
                } else {
                    Glide.with(requireContext()).pauseRequests();
                }
            }

            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                int firstVisible = layoutManager.findFirstVisibleItemPosition();
                int lastVisible = layoutManager.findLastVisibleItemPosition();

                // Preload only next 3 images instead of 5
                for (int i = lastVisible + 1; i < Math.min(lastVisible + 3, albumList.size()); i++) {
                    Album album = albumList.get(i);
                    Glide.with(requireContext())
                            .load(album.getUrl())
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .override(500, 500) // Limit image size
                            .preload();
                }
            }
        });

//        ImageView profileButton = view.findViewById(R.id.profileButton);

        databaseReference = FirebaseDatabase.getInstance().getReference("users");

        // Ambil data currentUser dari SharedPreferences (simpan userId dan username)
        SharedPreferences sharedPreferences = requireActivity().getSharedPreferences("user_prefs", getContext().MODE_PRIVATE);
        currentUsername = sharedPreferences.getString("username", null);
        currentUserId = sharedPreferences.getString("userId", null);  // Pastikan userId ada di SharedPreferences
        currentUserPhotoprofile = sharedPreferences.getString("photoprofile", null);

//        profileButton.setOnClickListener(v -> {
//            Intent intent = new Intent(getActivity(), ProfileFragment.class);
//            startActivity(intent);
//        });

        RecyclerView.OnScrollListener scrollListener = new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    Glide.with(requireContext()).resumeRequests();
                } else {
                    Glide.with(requireContext()).pauseRequests();
                }
            }
        };
        recyclerView.addOnScrollListener(scrollListener);

        fetchAlbums();

        return view;
    }

    private void fetchData() {
        // Remove any existing listeners first
        removeExistingListeners();
        loadingPage.setVisibility(View.VISIBLE);

        if (isShowingFollowedOnly) {
            fetchFollowedAlbums();
        } else {
            fetchAlbums();
        }
    }

    private void removeExistingListeners() {
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users");
        if (albumsListener != null) {
            userRef.removeEventListener(albumsListener);
            albumsListener = null;
        }
        if (followedListener != null) {
            userRef.child(currentUserId).child("followedUser").removeEventListener(followedListener);
            followedListener = null;
        }
    }

    private void fetchAlbums() {
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users");

        albumsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                albumList.clear();
                for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                    String userId = userSnapshot.getKey();
                    String username = userSnapshot.child("username").getValue(String.class);
                    String photoprofile = userSnapshot.child("photoprofile").getValue(String.class);

                    if (userSnapshot.hasChild("album")) {
                        DataSnapshot albumSnapshot = userSnapshot.child("album");
                        for (DataSnapshot itemSnapshot : albumSnapshot.getChildren()) {
                            String albumId = itemSnapshot.getKey();
                            String title = itemSnapshot.child("title").getValue(String.class);
                            String description = itemSnapshot.child("description").getValue(String.class);
                            String url = itemSnapshot.child("url").getValue(String.class);
                            Integer likesValue = itemSnapshot.child("likes").getValue(Integer.class);
                            long timestamp = itemSnapshot.child("timestamp").getValue(Long.class) != null
                                    ? itemSnapshot.child("timestamp").getValue(Long.class)
                                    : 0;
                            String uploadDate = itemSnapshot.child("humanReadableDate").getValue(String.class);
                            int likes = (likesValue != null) ? likesValue : 0;

                            boolean userHasLiked = false;
                            DataSnapshot userLikesSnapshot = itemSnapshot.child("userLikes");
                            for (DataSnapshot likeSnapshot : userLikesSnapshot.getChildren()) {
                                String likedUserId = likeSnapshot.child("userId").getValue(String.class);
                                if (currentUserId.equals(likedUserId)) {
                                    userHasLiked = true;
                                    break;
                                }
                            }

                            if (title != null && description != null && url != null) {
                                Album album = new Album(userId, albumId, title, username, description, url,
                                        photoprofile, likes, userHasLiked, timestamp);
                                album.setUploadDate(uploadDate);
                                albumList.add(album);
                            }
                        }
                    }
                }

                albumList.sort((a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
                albumAdapter.notifyDataSetChanged();

                loadingPage.setVisibility(View.GONE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Failed to load data", Toast.LENGTH_SHORT).show();
            }
        };

        userRef.addListenerForSingleValueEvent(albumsListener);
    }

    private void fetchFollowedAlbums() {
        removeExistingListeners();
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users");

        followedListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot followedSnapshot) {
                albumList.clear();

                if (!followedSnapshot.exists()) {
                    noFollowers.setVisibility(View.VISIBLE); // Tampilkan noFollowers jika tidak ada pengguna yang diikuti
                    albumAdapter.notifyDataSetChanged();
                    loadingPage.setVisibility(View.GONE);
                    return;
                }

                noFollowers.setVisibility(View.GONE); // Sembunyikan noFollowers jika ada pengguna yang diikuti

                for (DataSnapshot followedUserSnapshot : followedSnapshot.getChildren()) {
                    String followedUserId = followedUserSnapshot.getKey();
                    fetchUserAlbums(followedUserId);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Failed to load followed users", Toast.LENGTH_SHORT).show();
                loadingPage.setVisibility(View.GONE);
            }
        };
        userRef.child(currentUserId).child("followedUser").addValueEventListener(followedListener);
    }

    private void fetchUserAlbums(String userId) {
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(userId);

        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot userSnapshot) {
                String username = userSnapshot.child("username").getValue(String.class);
                String photoprofile = userSnapshot.child("photoprofile").getValue(String.class);
                DataSnapshot albumSnapshot = userSnapshot.child("album");

                for (DataSnapshot itemSnapshot : albumSnapshot.getChildren()) {
                    processAlbumData(itemSnapshot, userId, username, photoprofile);
                }

                albumList.sort((a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
                albumAdapter.notifyDataSetChanged();

                loadingPage.setVisibility(View.GONE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Failed to load user albums", Toast.LENGTH_SHORT).show();
                loadingPage.setVisibility(View.GONE);
            }
        });
    }

    private void processAlbumData(DataSnapshot itemSnapshot, String userId, String username, String photoprofile) {
        String albumId = itemSnapshot.getKey();
        String title = itemSnapshot.child("title").getValue(String.class);
        String description = itemSnapshot.child("description").getValue(String.class);
        String url = itemSnapshot.child("url").getValue(String.class);
        Integer likesValue = itemSnapshot.child("likes").getValue(Integer.class);
        long timestamp = itemSnapshot.child("timestamp").getValue(Long.class) != null
                ? itemSnapshot.child("timestamp").getValue(Long.class)
                : 0;
        String uploadDate = itemSnapshot.child("humanReadableDate").getValue(String.class);
        int likes = (likesValue != null) ? likesValue : 0;

        boolean userHasLiked = false;
        DataSnapshot userLikesSnapshot = itemSnapshot.child("userLikes");
        for (DataSnapshot likeSnapshot : userLikesSnapshot.getChildren()) {
            String likedUserId = likeSnapshot.child("userId").getValue(String.class);
            if (currentUserId.equals(likedUserId)) {
                userHasLiked = true;
                break;
            }
        }

        if (title != null && description != null && url != null) {
            Album album = new Album(userId, albumId, title, username, description, url,
                    photoprofile, likes, userHasLiked, timestamp);
            album.setUploadDate(uploadDate);
            albumList.add(album);
        }
    }



    public class AlbumAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private static final int TYPE_HEADER = 0;
        private static final int TYPE_ITEM = 1;
        private final ArrayList<Album> albumList;

        public AlbumAdapter(ArrayList<Album> albumList) {
            this.albumList = albumList;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            if (viewType == TYPE_HEADER) {
                // Inflate header layout
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.header_home, parent, false);
                return new HeaderViewHolder(view);
            } else {
                // Inflate album item layout
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_upload, parent, false);
                return new AlbumViewHolder(view);
            }
        }


        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {

            if (getItemViewType(position) == TYPE_HEADER) {
                // Set header data if needed
                HeaderViewHolder headerHolder = (HeaderViewHolder) holder;
            } else {
                // Bind album data
                Album album = albumList.get(position - 1); // Offset for header
                AlbumViewHolder albumHolder = (AlbumViewHolder) holder;
                albumHolder.usernameTextView.setText(album.getUsername());
                albumHolder.titleTextView.setText(album.getTitle());
                albumHolder.descriptionTextView.setText(album.getDescription());
                Glide.with(albumHolder.itemView.getContext())
                        .load(album.getUrl())
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .placeholder(R.drawable.blacksmooth2_drawable)
                        .override(1000, 1000)
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .listener(new RequestListener<Drawable>() {
                            @Override
                            public boolean onLoadFailed(@Nullable GlideException e, Object model,
                                                        Target<Drawable> target, boolean isFirstResource) {
                                return false;
                            }

                            @Override
                            public boolean onResourceReady(Drawable resource, Object model,
                                                           Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                                return false;
                            }
                        })
                        .into(albumHolder.imageView);


                // Optimize main image loading

                // Optimize profile image loading
                if (album.getPhotoprofile() != null && !album.getPhotoprofile().isEmpty()) {
                    Glide.with(albumHolder.itemView.getContext())
                            .load(album.getPhotoprofile())
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .placeholder(R.drawable.blacksmooth2_drawable)
                            .override(100, 100) // Smaller size for profile pics
                            .circleCrop()
                            .into(albumHolder.profileImageView);
                } else {
                    Glide.with(albumHolder.itemView.getContext())
                            .load(R.drawable.default_profile_bct_v1)
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .override(100, 100)
                            .circleCrop()
                            .into(albumHolder.profileImageView);
                }

                albumHolder.uploadDateTextView.setText(album.getUploadDate());
                albumHolder.likeCount.setText(formatLikesCount(album.getLikes()));

                if (album.getUserId().equals(currentUserId)) {
                    albumHolder.followButton.setVisibility(View.GONE);
                } else {
                    albumHolder.followButton.setVisibility(View.VISIBLE);
                    albumHolder.followButton.setOnClickListener(v -> {
                        handleFollow(album, albumHolder, holder);
                    });
                    checkFollowStatus(album, albumHolder);
                }

                // Handle like button and comment logic
                handleLikeAndComments(album, albumHolder);

                albumHolder.followButton.setOnClickListener(v -> {
                    handleFollow(album, albumHolder, holder);
                });

                checkFollowStatus(album, albumHolder);

                // Tombol Follow

            }
        }

        private void checkFollowStatus(Album album, AlbumViewHolder albumHolder) {
            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users");

            // Cek status follow setiap kali ViewHolder di-bind
            userRef.child(currentUserId).child("followedUser").child(album.getUserId())
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if (snapshot.exists()) {
                                albumHolder.followButton.setText("Following");
                                albumHolder.followButton.setSelected(true);
                            } else {
                                albumHolder.followButton.setText("Follow");
                                albumHolder.followButton.setSelected(false);
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Toast.makeText(albumHolder.itemView.getContext(), "Failed to load follow status", Toast.LENGTH_SHORT).show();
                        }
                    });
        }


        private void handleFollow(Album album, AlbumViewHolder albumHolder, RecyclerView.ViewHolder holder) {
            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users");

            userRef.child(currentUserId).child("followedUser").child(album.getUserId())
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            boolean isFollowing = snapshot.exists();
                            if (isFollowing) {
                                // Unfollow
                                albumHolder.followButton.setText("Follow");
                                albumHolder.followButton.setSelected(false);
                                userRef.child(album.getUserId()).child("followers").setValue(ServerValue.increment(-1));
                                userRef.child(currentUserId).child("followed").setValue(ServerValue.increment(-1));
                                userRef.child(album.getUserId()).child("followersUser").child(currentUserId).removeValue();
                                userRef.child(currentUserId).child("followedUser").child(album.getUserId()).removeValue();
                            } else {
                                // Follow
                                albumHolder.followButton.setText("Following");
                                albumHolder.followButton.setSelected(true);
                                userRef.child(album.getUserId()).child("followers").setValue(ServerValue.increment(1));
                                userRef.child(currentUserId).child("followed").setValue(ServerValue.increment(1));
                                userRef.child(album.getUserId()).child("followersUser").child(currentUserId).setValue(true);
                                userRef.child(currentUserId).child("followedUser").child(album.getUserId()).setValue(true);
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Toast.makeText(holder.itemView.getContext(), "Error checking follow status", Toast.LENGTH_SHORT).show();
                        }
                    });
        }

        @SuppressLint("ResourceType")
        private void handleLikeAndComments(Album album, AlbumViewHolder holder) {
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


            holder.commentButton.setOnClickListener(v -> {
                BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(holder.itemView.getContext(), R.style.BottomSheetDialogTheme);
                View bottomSheetView = LayoutInflater.from(holder.itemView.getContext())
                        .inflate(R.layout.fragment_comment, null);
                Window window = bottomSheetDialog.getWindow();
                bottomSheetDialog.setContentView(bottomSheetView);


                bottomSheetDialog.setContentView(bottomSheetView);
                bottomSheetDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

                if (window != null) {
                    window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
                }


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


            Boolean isBookmarked = bookmarkStatusCache.get(album.getId());
            if (isBookmarked != null) {
                holder.bookmarkButton.setImageResource(isBookmarked ? R.drawable.bookmark_24filled : R.drawable.bookmark_24filledgrey);
            } else {
                // Fetch bookmark status from database if not in cache
                DatabaseReference userBookmarksRef = FirebaseDatabase.getInstance().getReference("users")
                        .child(currentUserId)
                        .child("bookmark");
                userBookmarksRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot allCollagesSnapshot) {
                        boolean isBookmarkedAnywhere = false;
                        for (DataSnapshot collageSnapshot : allCollagesSnapshot.getChildren()) {
                            if (collageSnapshot.hasChild(album.getId())) {
                                isBookmarkedAnywhere = true;
                                break;
                            }
                        }
                        bookmarkStatusCache.put(album.getId(), isBookmarkedAnywhere);
                        holder.bookmarkButton.setImageResource(isBookmarkedAnywhere ? R.drawable.bookmark_24filled : R.drawable.bookmark_24filledgrey);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(holder.itemView.getContext(), "Failed to load bookmark status", Toast.LENGTH_SHORT).show();
                    }
                });
            }
            DatabaseReference userBookmarksRef = FirebaseDatabase.getInstance().getReference("users")
                    .child(currentUserId)
                    .child("bookmark");

// Check bookmark status across all collections
            userBookmarksRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot allCollagesSnapshot) {
                    boolean isBookmarkedAnywhere = false;

                    // Check if the album is bookmarked in any collection
                    for (DataSnapshot collageSnapshot : allCollagesSnapshot.getChildren()) {
                        if (collageSnapshot.hasChild(album.getId())) {
                            isBookmarkedAnywhere = true;
                            break;
                        }
                    }

                    // Update bookmark icon based on whether it's bookmarked anywhere
                    holder.bookmarkButton.setImageResource(isBookmarkedAnywhere ?
                            R.drawable.bookmark_24filled : R.drawable.bookmark_24filledgrey);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(holder.itemView.getContext(),
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
                                        bookmarkStatusCache.put(album.getId(), false);
                                        Toast.makeText(holder.itemView.getContext(), "Removed from collection", Toast.LENGTH_SHORT).show();
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(holder.itemView.getContext(), "Failed to remove from collection", Toast.LENGTH_SHORT).show();
                                    });
                        } else {
                            // Show bottom sheet to select collection
                            showCollageSelectionBottomSheet(holder, album);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(holder.itemView.getContext(), "Failed to check bookmark status", Toast.LENGTH_SHORT).show();
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

        @Override
        public int getItemCount() {
            return albumList.size() + 1; // +1 for the header
        }

        @Override
        public int getItemViewType(int position) {
            if (position == 0) {
                return TYPE_HEADER; // First item is header
            } else {
                return TYPE_ITEM; // Rest are album items
            }
        }

        public class HeaderViewHolder extends RecyclerView.ViewHolder {
            ConstraintLayout headerTitle;
            TextView homeButton;
            TextView followedButton;

            public HeaderViewHolder(View itemView) {
                super(itemView);
                headerTitle = itemView.findViewById(R.id.headerHome);
                homeButton = itemView.findViewById(R.id.DashboardPage);
                View headerView = itemView.findViewById(R.id.headerHome);
                followedButton = itemView.findViewById(R.id.FollowedPage);
//                usernameTextView = itemView.findViewById(R.id.hellousername);
//                locationTextView = itemView.findViewById(R.id.yourlocation);

                SharedPreferences sharedPreferences = requireActivity().getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
                currentUserId = sharedPreferences.getString("userId", null);

//                if (currentUserId != null) {
//                    fetchUserDataTwo();
//                }

                homeButton.setOnClickListener(v -> {
                    isShowingFollowedOnly = false;
                    showHomeContent();
                });

                followedButton.setOnClickListener(v -> {
                    isShowingFollowedOnly = true;
                    showFollowedContent();
                });



                // Atur tampilan sesuai state yang terakhir
                updateContent();
            }

            private void updateContent() {
                if (isShowingFollowedOnly) {
                    showFollowedContent();
                } else {
                    showHomeContent();
                }
            }

            private void showHomeContent() {
                homeButton.setSelected(true);
                followedButton.setSelected(false);
                isShowingFollowedOnly = false;
                noFollowers.setVisibility(View.GONE);
                fetchData();
            }

            private void showFollowedContent() {
                homeButton.setSelected(false);
                followedButton.setSelected(true);
                isShowingFollowedOnly = true;
                fetchData();
            }
        }


        public class AlbumViewHolder extends RecyclerView.ViewHolder {
            ImageView profileImageView, imageView, commentButton;
            ImageButton likeButton;
            TextView usernameTextView, titleTextView, descriptionTextView, uploadDateTextView, likeCount, followButton;
            ImageButton bookmarkButton;
            LinearLayout linearUserId;

            public AlbumViewHolder(View itemView) {
                super(itemView);
                profileImageView = itemView.findViewById(R.id.photoProfile);
                imageView = itemView.findViewById(R.id.imageView);
                likeButton = itemView.findViewById(R.id.btnLike);
                likeCount = itemView.findViewById(R.id.likeCount);
                usernameTextView = itemView.findViewById(R.id.usernameUploader);
                linearUserId = itemView.findViewById(R.id.linearUserId);
                titleTextView = itemView.findViewById(R.id.textTitle);
                descriptionTextView = itemView.findViewById(R.id.textDescription);
                uploadDateTextView = itemView.findViewById(R.id.uploadDateTextView);
                commentButton = itemView.findViewById(R.id.btnComment);
                followButton = itemView.findViewById(R.id.follow);
                bookmarkButton = itemView.findViewById(R.id.bookmark);

                // ... existing initializations ...
                imageView.setOnClickListener(v -> {
                    Intent intent = new Intent(itemView.getContext(), FullscreenImageActivity.class);
                    intent.putExtra("imageUrl", albumList.get(getAdapterPosition() - 1).getUrl());
                    intent.putExtra("userId", albumList.get(getAdapterPosition() - 1).getUserId());
                    intent.putExtra("username", albumList.get(getAdapterPosition() - 1).getUsername());
                    intent.putExtra("imageTitle", albumList.get(getAdapterPosition() - 1).getTitle());
                    intent.putExtra("imageDescription", albumList.get(getAdapterPosition() - 1).getDescription());

                    itemView.getContext().startActivity(intent);
                    ((Activity) itemView.getContext()).overridePendingTransition(0, 0);
                });


                linearUserId.setOnClickListener(v -> {
                    String userId = albumList.get(getAdapterPosition() - 1).getUserId(); // -1 because of header
                    navigateToUserProfile(userId);
                });

                // Add click listener for profile image as well
                profileImageView.setOnClickListener(v -> {
                    String userId = albumList.get(getAdapterPosition() - 1).getUserId();
                    navigateToUserProfile(userId);
                });
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        removeExistingListeners();
    }
}





//}