package com.example.imaginate;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.imaginate.models.AlbumItem;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class OpenAlbumSearchFragment extends Fragment {
    private RecyclerView recyclerView;
    private OpenAlbumSearchAdapter adapter;
    private List<AlbumItem> albumItems;
    private DatabaseReference databaseRef;
    private String searchedAlbumTitle;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.open_album_search_fragment, container, false);

        // Initialize views
        ImageButton backButton = view.findViewById(R.id.backButton);
        TextView searchInputText = view.findViewById(R.id.searchInputText);
        recyclerView = view.findViewById(R.id.openAlbumSearchRecyclerview);

        // Get album title from arguments
        if (getArguments() != null) {
            searchedAlbumTitle = getArguments().getString("albumTitle", "");
            searchInputText.setText(searchedAlbumTitle);
        }

        // Setup back button
        backButton.setOnClickListener(v -> requireActivity().onBackPressed());

        // Initialize RecyclerView
        albumItems = new ArrayList<>();
        adapter = new OpenAlbumSearchAdapter(getContext(), albumItems);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        // Fetch album details
        fetchAlbumDetails();

        return view;
    }

    private void fetchAlbumDetails() {
        databaseRef = FirebaseDatabase.getInstance().getReference("users");
        databaseRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                albumItems.clear();
                for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                    DataSnapshot albumsSnapshot = userSnapshot.child("album");
                    for (DataSnapshot albumSnapshot : albumsSnapshot.getChildren()) {
                        String title = albumSnapshot.child("title").getValue(String.class);

                        if (title != null && title.equals(searchedAlbumTitle)) {
                            // Fetch all details for this specific album
                            String url = albumSnapshot.child("url").getValue(String.class);
                            String description = albumSnapshot.child("description").getValue(String.class);
                            String uploadDate = albumSnapshot.child("uploadDate").getValue(String.class);
                            String username = userSnapshot.child("username").getValue(String.class);
                            String userProfilePic = userSnapshot.child("photoprofile").getValue(String.class);
                            Long likesCount = albumSnapshot.child("likes").getChildrenCount();

                            AlbumItem albumItem = new AlbumItem(
                                    title,
                                    url,
                                    description,
                                    uploadDate,
                                    username,
                                    userProfilePic,
                                    likesCount
                            );
                            albumItems.add(albumItem);
                        }
                    }
                }

                adapter.notifyDataSetChanged();

                if (albumItems.isEmpty()) {
                    Toast.makeText(getContext(), "No album items found", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Failed to load album: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }


    // Adapter for the open album search
    public class OpenAlbumSearchAdapter extends RecyclerView.Adapter<OpenAlbumSearchAdapter.AlbumItemViewHolder> {
        private Context context;
        private List<AlbumItem> albumItems;

        public OpenAlbumSearchAdapter(Context context, List<AlbumItem> albumItems) {
            this.context = context;
            this.albumItems = albumItems;
        }

        @NonNull
        @Override
        public AlbumItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_upload, parent, false);
            return new AlbumItemViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull AlbumItemViewHolder holder, int position) {
            AlbumItem item = albumItems.get(position);

            // Set image
            Glide.with(context)
                    .load(item.getImageUrl())
                    .placeholder(R.drawable.error_image)
                    .into(holder.imageView);

            // Set upload date
            holder.uploadDateTextView.setText(item.getUploadDate());

            // Set like count
            holder.likeCount.setText(String.valueOf(item.getLikesCount()));

            // Set title
            holder.textTitle.setText(item.getTitle());

            // Set profile image and username
            Glide.with(context)
                    .load(item.getUserProfilePic())
                    .placeholder(R.drawable.profiledefault)
                    .into(holder.photoProfile);
            holder.usernameUploader.setText(item.getUsername());

            // Set description
            holder.textDescription.setText(item.getDescription());

            // TODO: Implement like, comment, bookmark button functionalities
            // Example:
            // holder.btnLike.setOnClickListener(...);
        }

        @Override
        public int getItemCount() {
            return albumItems.size();
        }

        public class AlbumItemViewHolder extends RecyclerView.ViewHolder {
            ShapeableImageView imageView;
            TextView uploadDateTextView;
            ImageButton btnLike, btnComment, bookmark;
            TextView likeCount;
            TextView textTitle;
            ShapeableImageView photoProfile;
            TextView usernameUploader;
            TextView textDescription;

            public AlbumItemViewHolder(@NonNull View itemView) {
                super(itemView);
                imageView = itemView.findViewById(R.id.imageView);
                uploadDateTextView = itemView.findViewById(R.id.uploadDateTextView);
                btnLike = itemView.findViewById(R.id.btnLike);
                btnComment = itemView.findViewById(R.id.btnComment);
                bookmark = itemView.findViewById(R.id.bookmark);
                likeCount = itemView.findViewById(R.id.likeCount);
                textTitle = itemView.findViewById(R.id.textTitle);
                photoProfile = itemView.findViewById(R.id.photoProfile);
                usernameUploader = itemView.findViewById(R.id.usernameUploader);
                textDescription = itemView.findViewById(R.id.textDescription);
            }
        }
    }
}