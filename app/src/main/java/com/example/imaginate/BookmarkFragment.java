
package com.example.imaginate;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.imaginate.models.CollageItem;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

// BookmarkFragment.java
public class BookmarkFragment extends Fragment {
    private RecyclerView collageRecyclerView;
    private ArrayList<CollageItem> collageList;
    private CollageAdapter collageAdapter;
    private String currentUserId;
    private DatabaseReference databaseReference;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_bookmark, container, false);

        SharedPreferences sharedPreferences = requireActivity().getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        currentUserId = sharedPreferences.getString("userId", null);

        // Initialize RecyclerView for collages
        collageRecyclerView = view.findViewById(R.id.collageRecyclerView);
        collageRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
        collageList = new ArrayList<>();
        collageAdapter = new CollageAdapter();
        collageRecyclerView.setAdapter(collageAdapter);

        // Initialize Firebase Reference
        databaseReference = FirebaseDatabase.getInstance().getReference()
                .child("users").child(currentUserId).child("bookmark");

        // Add Collage Button
        LinearLayout addCollageButton = view.findViewById(R.id.addCollage);
        addCollageButton.setOnClickListener(v -> showAddCollageDialog());

        // Load collages
        loadCollages();

        return view;
    }

    private void loadCollages() {
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                collageList.clear();

                // Add default "allCollage" if it doesn't exist
                if (!snapshot.hasChild("allCollage")) {
                    DatabaseReference allCollageRef = databaseReference.child("allCollage");
                    allCollageRef.child("name").setValue("All Collage");
                    allCollageRef.child("timestamp").setValue(ServerValue.TIMESTAMP);
                }

                // Load all collages
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
                Toast.makeText(getContext(), "Failed to load collages", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showAddCollageDialog() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(requireContext(), R.style.BottomSheetDialogTheme);
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_comment, null);

        // Initialize views
        EditText collageNameInput = dialogView.findViewById(R.id.collageNameInput);
        TextView cancelButton = dialogView.findViewById(R.id.cancel);
        TextView addButton = dialogView.findViewById(R.id.add);

        // Set click listeners
        cancelButton.setOnClickListener(v -> bottomSheetDialog.dismiss());

        addButton.setOnClickListener(v -> {
            String collageName = collageNameInput.getText().toString().trim();
            if (!collageName.isEmpty()) {
                String collageId = databaseReference.push().getKey();
                if (collageId != null) {
                    DatabaseReference newCollageRef = databaseReference.child(collageId);
                    newCollageRef.child("name").setValue(collageName);
                    newCollageRef.child("timestamp").setValue(ServerValue.TIMESTAMP);
                }
                bottomSheetDialog.dismiss();
            } else {
                collageNameInput.setError("Please enter a collection name");
            }
        });

        bottomSheetDialog.setContentView(dialogView);
        bottomSheetDialog.show();
    }

    private class CollageAdapter extends RecyclerView.Adapter<CollageAdapter.CollageViewHolder> {
        @NonNull
        @Override
        public CollageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_collage, parent, false);
            return new CollageViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull CollageViewHolder holder, int position) {
            CollageItem collage = collageList.get(position);
            holder.collageName.setText(collage.getName());

//            ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) holder.itemView.getLayoutParams();
//            if (position % 2 != 0) { // Check if position is odd
//                layoutParams.topMargin = (int) (30 * holder.itemView.getResources().getDisplayMetrics().density); // Convert dp to pixels
//            } else {
//                layoutParams.topMargin =  (int) (30 * holder.itemView.getResources().getDisplayMetrics().density);
//            }

            ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) holder.itemView.getLayoutParams();
            if (position == 0 || position == 1) {
                layoutParams.topMargin = (int) (85 * holder.itemView.getResources().getDisplayMetrics().density); // Convert dp to pixels
            } else {
                layoutParams.topMargin = 0;
            }
            holder.itemView.setLayoutParams(layoutParams);



            // Ambil thumbnail dari album pertama di dalam kolase
            databaseReference.child(collage.getId()).addListenerForSingleValueEvent(new ValueEventListener() {
                @SuppressLint("ResourceAsColor")
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    // Hitung jumlah item dalam kolase
                    long itemCount = snapshot.getChildrenCount() - 2; // Kurangi "name" dan "timestamp"
                    holder.itemCount.setText(itemCount + " items");

                    String thumbnailUrl = null;

                    for (DataSnapshot albumSnapshot : snapshot.getChildren()) {
                        // Lewati node "name" dan "timestamp"
                        if (!albumSnapshot.getKey().equals("name") && !albumSnapshot.getKey().equals("timestamp")) {
                            thumbnailUrl = albumSnapshot.child("imageUrl").getValue(String.class);
                            if (thumbnailUrl != null) {
                                break; // Ambil hanya gambar dari album pertama
                            }
                        }
                    }

                    // Jika URL tersedia, load dengan Glide
                    if (thumbnailUrl != null) {
                        Glide.with(holder.itemView.getContext())
                                .load(thumbnailUrl)
                                .placeholder(R.drawable.blacksmooth2_drawable)
                                .into(holder.thumbnailImage);
                    } else {
                        // Jika tidak ada gambar, gunakan placeholder default
                        holder.thumbnailImage.setImageResource(R.drawable.blacksmooth2_drawable);
                    }
                }

                @SuppressLint("ResourceAsColor")
                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    holder.itemCount.setText("0 items");
                    holder.thumbnailImage.setImageResource(R.drawable.blacksmooth2_drawable);
                }
            });

            holder.itemView.setOnClickListener(v -> {
                Bundle bundle = new Bundle();
                bundle.putString("collageId", collage.getId());
                bundle.putString("collageName", collage.getName());
                SavedAlbumsFragment savedAlbumsFragment = new SavedAlbumsFragment();
                savedAlbumsFragment.setArguments(bundle);
                getParentFragmentManager()
                        .beginTransaction()
                        .replace(R.id.frame_layout, savedAlbumsFragment)
                        .addToBackStack(null)
                        .commit();
            });




            holder.itemView.setOnClickListener(v -> {
                Bundle bundle = new Bundle();
                bundle.putString("collageId", collage.getId());
                bundle.putString("collageName", collage.getName());
                SavedAlbumsFragment savedAlbumsFragment = new SavedAlbumsFragment();
                savedAlbumsFragment.setArguments(bundle);

                // Use custom tag format for proper back stack handling
                // Use custom tag format for proper back stack handling
                String backStackTag = "SavedAlbumsFragment_5"; // 5 is for profile tab

                getParentFragmentManager()
                        .beginTransaction()
                        .add(R.id.frame_layout, savedAlbumsFragment, "SavedAlbumsFragment")
                        .addToBackStack(backStackTag)
                        .commit();
            });
        }



        @Override
        public int getItemCount() {
            return collageList.size();
        }

        class CollageViewHolder extends RecyclerView.ViewHolder {
            TextView collageName;
            TextView itemCount;
            ImageView thumbnailImage;

            CollageViewHolder(@NonNull View itemView) {
                super(itemView);
                collageName = itemView.findViewById(R.id.collageName);
                itemCount = itemView.findViewById(R.id.itemCount);
                thumbnailImage = itemView.findViewById(R.id.thumbnailImage);
            }
        }
    }
}

