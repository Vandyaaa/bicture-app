package com.example.imaginate;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.imaginate.models.User;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {

    private final ArrayList<User> userList;
    private ArrayList<User> followersList;
    private ArrayList<User> followedList;
    private final Context context;
    // Add a cache to store user data and reduce Firebase calls
    private Map<String, Bundle> userBundleCache = new HashMap<>();

    // Constructor for when only userList is provided (used in showFollowedBottomSheet)
    public UserAdapter(ArrayList<User> userList) {
        this.userList = userList;
        this.followersList = new ArrayList<>();
        this.followedList = new ArrayList<>();
        this.context = null;
    }

    // Constructor for when userList and context are provided
    public UserAdapter(ArrayList<User> userList, Context context) {
        this.userList = userList;
        this.followersList = new ArrayList<>();
        this.followedList = new ArrayList<>();
        this.context = context;
    }

    // Constructor for when userList, followersList, and context are provided (used in showFollowersBottomSheet)
    public UserAdapter(ArrayList<User> userList, ArrayList<User> followersList, Context context) {
        this.userList = userList;
        this.followersList = followersList;
        this.followedList = new ArrayList<>();
        this.context = context;
    }

    // Full constructor with all parameters
    public UserAdapter(ArrayList<User> userList, ArrayList<User> followersList, ArrayList<User> followedList, Context context) {
        this.userList = userList;
        this.followersList = followersList;
        this.followedList = followedList;
        this.context = context;
    }

    @NonNull
    @Override
    public UserAdapter.UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserAdapter.UserViewHolder holder, int position) {
        User user = userList.get(position);
        holder.usernameTextView.setText(user.getUsername());
        holder.tagNameTextView.setText(user.getTagName());

        // Get context from view if adapter's context is null
        Context contextToUse = context != null ? context : holder.itemView.getContext();

        // Load image using Glide
        Glide.with(contextToUse)
                .load(user.getPhotoProfile())
                .placeholder(R.drawable.ppdf2) // Placeholder if image not available
                .error(R.drawable.ppdf2) // Default image if error
                .into(holder.photoProfileImageView);

        // Add click listener to the user profile container
        holder.userProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (contextToUse instanceof AppCompatActivity) {
                    AppCompatActivity activity = (AppCompatActivity) contextToUse;

                    // Check if user data is already cached
                    if (userBundleCache.containsKey(user.getUserId())) {
                        // Use cached data if available
                        Bundle cachedBundle = userBundleCache.get(user.getUserId());
                        navigateWithBundle(cachedBundle, activity);
                    } else {
                        // Fetch from Firebase if not cached
                        navigateToUserProfile(user.getUserId(), activity);
                    }
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    private void navigateToUserProfile(String userId, AppCompatActivity activity) {
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

                    // Store in cache for future use
                    userBundleCache.put(userId, bundle);

                    // Navigate with the bundle
                    navigateWithBundle(bundle, activity);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(activity, "Failed to load user profile", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Separated the navigation logic to make it reusable
    private void navigateWithBundle(Bundle bundle, AppCompatActivity activity) {
        ProfileShowFragment profileShowFragment = new ProfileShowFragment();
        profileShowFragment.setArguments(bundle);

        FragmentManager fragmentManager = activity.getSupportFragmentManager();

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
        // Use add() to match your original implementation
        transaction.add(R.id.frame_layout, profileShowFragment, "ProfileShowFragment");

        // Add to back stack with the unique name
        transaction.addToBackStack(backStackName);
        transaction.commit();
    }

    public static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView usernameTextView, tagNameTextView;
        ImageView photoProfileImageView;
        ConstraintLayout userProfile;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            usernameTextView = itemView.findViewById(R.id.usernameTextView);
            tagNameTextView = itemView.findViewById(R.id.tagNameTextView);
            photoProfileImageView = itemView.findViewById(R.id.photoProfileImageView);
            userProfile = itemView.findViewById(R.id.userprofile);
        }
    }
}