package com.example.imaginate;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.example.imaginate.models.Album;
import com.example.imaginate.models.ProfileData;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

public class ProfileShowFragment extends Fragment {
    private RecyclerView recyclerView;
    private ArrayList<Album> albumList;
    private ProfileAdapter profileAdapter;
    private String userId, currentUserId;
    private DatabaseReference userRef;
    private ImageButton backButton;
    private TextView followButton;
    private TextView tagname;
    private LinearLayout noneTextView;
    private ValueEventListener followStatusListener;
    private String albumId;
    private View loadingView;
    private long loadingStartTime;
    private final AtomicBoolean dataLoaded = new AtomicBoolean(false);
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Animation fadeOut;
    private Animation fadeIn;
    private boolean isLoading = false;


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile_show, container, false);
        RequestOptions requestOptions = new RequestOptions()
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .priority(Priority.HIGH);

        Glide.with(this)
                .setDefaultRequestOptions(requestOptions);
        recyclerView = view.findViewById(R.id.albumRecyclerView);
        noneTextView = view.findViewById(R.id.noneTextView);
        albumList = new ArrayList<>();
        backButton = view.findViewById(R.id.backButton);
        followButton = view.findViewById(R.id.followButton);
        loadingView = view.findViewById(R.id.loadingLayout);

        fadeOut = AnimationUtils.loadAnimation(getContext(), R.anim.slide_in_right);
        fadeIn = AnimationUtils.loadAnimation(getContext(), R.anim.slide_in_right);

        fadeOut.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {}

            @Override
            public void onAnimationEnd(Animation animation) {
                // Actually hide the view after animation completes
                loadingView.setVisibility(View.GONE);

                // Show the appropriate content with fade-in animation
                if (albumList != null && !albumList.isEmpty()) {
                    if (noneTextView != null) noneTextView.setVisibility(View.GONE);
                    if (recyclerView != null) {
                        recyclerView.setVisibility(View.VISIBLE);
//                        recyclerView.startAnimation(fadeIn);
                    }
                } else {
                    if (recyclerView != null) recyclerView.setVisibility(View.VISIBLE);
                    if (noneTextView != null) {
                        noneTextView.setVisibility(View.VISIBLE);
//                        noneTextView.startAnimation(fadeIn);
                    }
                }
            }

            @Override
            public void onAnimationRepeat(Animation animation) {}
        });
        // Get current user ID from SharedPreferences
        SharedPreferences prefs = requireActivity().getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        currentUserId = prefs.getString("userId", null);

        // Get user ID and profile data from arguments
        Bundle args = getArguments();
        if (args != null) {
            userId = args.getString("userId");
            ProfileData profileData = new ProfileData(
                    args.getString("username"),
                    args.getString("photoprofile"),
                    args.getString("location"),
                    args.getString("bio"),
                    args.getLong("followers", 0),
                    args.getLong("following", 0),
                    args.getLong("post", 0),
                    args.getString("tagname")
            );

            // Setup RecyclerView with the new adapter
            profileAdapter = new ProfileAdapter(getContext(), albumList, userId, currentUserId, profileData);
            recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            recyclerView.setAdapter(profileAdapter);
            recyclerView.setItemViewCacheSize(20);
            recyclerView.setDrawingCacheEnabled(true);
            recyclerView.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);
            setupFollowButton(followButton, profileData);

            // Setup back button
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


            // Setup Firebase reference and load albums
            userRef = FirebaseDatabase.getInstance().getReference("users").child(userId);

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
        }

        // Initialize views - hide content initially
        recyclerView.setVisibility(View.GONE);
        noneTextView.setVisibility(View.GONE);
        loadingView.setVisibility(View.VISIBLE);

        // Start loading data
        startLoading();

        return view;
    }

    private void checkLoadingStatus() {
        if (dataLoaded.get() && isLoading) {
            hideLoading();
        }
    }

    private void hideLoading() {
        if (!isAdded() || getActivity() == null) return;

        isLoading = false;

        if (loadingView != null && loadingView.getVisibility() == View.VISIBLE) {
            loadingView.startAnimation(fadeOut);
            // Animation listener will handle the rest
        } else {
            // If loading view is already gone, just make sure content is visible
            updateContentVisibility();
        }
    }

    private void updateContentVisibility() {
        if (!isAdded()) return;

        if (albumList != null && !albumList.isEmpty()) {
            if (noneTextView != null) noneTextView.setVisibility(View.GONE);
            if (recyclerView != null) recyclerView.setVisibility(View.VISIBLE);
        } else {
            if (noneTextView != null) noneTextView.setVisibility(View.VISIBLE);
            if (recyclerView != null) recyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void startLoading() {
        if (isLoading) return; // Prevent multiple loading calls

        isLoading = true;
        dataLoaded.set(false);

        // Show loading view
        if (loadingView != null) {
            loadingView.setVisibility(View.VISIBLE);
        }

        // Hide content views
        if (recyclerView != null) {
            recyclerView.setVisibility(View.GONE);
        }
        if (noneTextView != null) {
            noneTextView.setVisibility(View.GONE);
        }

        // Record start time
        loadingStartTime = System.currentTimeMillis();

        // Load the actual data
        loadUserAlbums();

        // Set a minimum timeout
        handler.postDelayed(this::checkLoadingStatus, 0);
    }

    private void setupFollowButton(TextView followButton, ProfileData profileData) {
        if (currentUserId.equals(userId)) {
            followButton.setVisibility(View.GONE);
            return;
        }

        DatabaseReference currentUserRef = FirebaseDatabase.getInstance()
                .getReference("users").child(currentUserId);
        DatabaseReference targetUserRef = FirebaseDatabase.getInstance()
                .getReference("users").child(userId);

        followStatusListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded() || getContext() == null) return;

                boolean isFollowing = snapshot.exists();
                if (isFollowing) {
                    followButton.setBackgroundTintList(ContextCompat.getColorStateList(getContext(), R.color.blacksmooth2));
                    followButton.setTextColor(ContextCompat.getColorStateList(getContext(), R.color.whitesmooth));
                    followButton.setText("Following");
                } else {
                    followButton.setBackgroundTintList(ContextCompat.getColorStateList(getContext(), R.color.white));
                    followButton.setTextColor(ContextCompat.getColorStateList(getContext(), R.color.black));
                    followButton.setText("Follow");
                }
                followButton.setSelected(isFollowing);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (!isAdded() || getContext() == null) return;
                Toast.makeText(getContext(), "Failed to check follow status", Toast.LENGTH_SHORT).show();
            }
        };

        currentUserRef.child("followedUser").child(userId).addValueEventListener(followStatusListener);

        followButton.setOnClickListener(v -> {
            boolean isFollowing = followButton.isSelected();
            if (isFollowing) {
                // Unfollow logic
                currentUserRef.child("followedUser").child(userId).removeValue()
                        .addOnSuccessListener(aVoid -> {
                            // Update followers count for target user
                            targetUserRef.child("followers").setValue(ServerValue.increment(-1));
                            // Update following count for current user
                            currentUserRef.child("followed").setValue(ServerValue.increment(-1));
                            // Remove current user from target user's followers list
                            targetUserRef.child("followersUser").child(currentUserId).removeValue();
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(getContext(), "Failed to unfollow", Toast.LENGTH_SHORT).show();
                        });
            } else {
                // Follow logic
                currentUserRef.child("followedUser").child(userId).setValue(true)
                        .addOnSuccessListener(aVoid -> {
                            // Update followers count for target user
                            targetUserRef.child("followers").setValue(ServerValue.increment(1));
                            // Update following count for current user
                            currentUserRef.child("followed").setValue(ServerValue.increment(1));
                            // Add current user to target user's followers list
                            targetUserRef.child("followersUser").child(currentUserId).setValue(true);
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(getContext(), "Failed to follow", Toast.LENGTH_SHORT).show();
                        });
            }
        });
    }

    @Override
    public void onDestroyView() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroyView();
        if (followStatusListener != null) {
            DatabaseReference currentUserRef = FirebaseDatabase.getInstance()
                    .getReference("users").child(currentUserId);
            currentUserRef.child("followedUser").child(userId).removeEventListener(followStatusListener);
        }
    }

    private void loadUserAlbums() {
        // Loading view and visibility already handled in startLoading()
        DatabaseReference albumsRef = userRef.child("album");
        albumsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;

                albumList.clear();
                for (DataSnapshot albumSnapshot : snapshot.getChildren()) {
                    String albumId = albumSnapshot.getKey();
                    String title = albumSnapshot.child("title").getValue(String.class);
                    String description = albumSnapshot.child("description").getValue(String.class);
                    String url = albumSnapshot.child("url").getValue(String.class);
                    Integer likes = albumSnapshot.child("likes").getValue(Integer.class);
                    Long timestamp = albumSnapshot.child("timestamp").getValue(Long.class);
                    String uploadDate = albumSnapshot.child("humanReadableDate").getValue(String.class);
                    String photoProfile = albumSnapshot.child("photoprofile").getValue(String.class);

                    // Get username from ProfileData
                    Bundle args = getArguments();
                    String username = args != null ? args.getString("username") : "Unknown User";

                    boolean userHasLiked = false;
                    DataSnapshot likesSnapshot = albumSnapshot.child("userLikes");
                    for (DataSnapshot likeSnapshot : likesSnapshot.getChildren()) {
                        if (currentUserId.equals(likeSnapshot.child("userId").getValue(String.class))) {
                            userHasLiked = true;
                            break;
                        }
                    }

                    Album album = new Album(userId, albumId, title, username, // Use the username here
                            description, url, photoProfile, likes != null ? likes : 0,
                            userHasLiked, timestamp != null ? timestamp : 0);
                    album.setUploadDate(uploadDate);
                    albumList.add(album);
                }
                albumList.sort((a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
                profileAdapter.notifyDataSetChanged();

                // Mark data as loaded
                dataLoaded.set(true);

                // Check if we've waited the minimum time
                long elapsedTime = System.currentTimeMillis() - loadingStartTime;
                if (elapsedTime >= 0) {
                    hideLoading();
                }
                // Otherwise, let the timer in startLoading() handle it
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (!isAdded()) return;

                // Mark data as "loaded" even though it failed
                dataLoaded.set(true);

                // Calculate elapsed time
                long elapsedTime = System.currentTimeMillis() - loadingStartTime;

                if (elapsedTime >= 0) {
                    // If minimum time already passed, hide loading immediately
                    hideLoading();
                    Toast.makeText(getContext(), "Failed to load albums", Toast.LENGTH_SHORT).show();
                } else {
                    // Wait for the minimum time before showing error
                    handler.postDelayed(() -> {
                        if (isAdded()) {
                            hideLoading();
                            Toast.makeText(getContext(), "Failed to load albums", Toast.LENGTH_SHORT).show();
                        }
                    }, 0 - elapsedTime);
                }
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
}