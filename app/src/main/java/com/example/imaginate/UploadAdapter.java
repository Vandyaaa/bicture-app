package com.example.imaginate;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.List;

public class UploadAdapter extends RecyclerView.Adapter<UploadAdapter.UploadViewHolder> {

    private List<Upload> uploadList;

    public UploadAdapter(List<Upload> uploadList) {
        this.uploadList = uploadList;
    }

    @NonNull
    @Override
    public UploadViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_upload, parent, false);
        return new UploadViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UploadViewHolder holder, int position) {
        Upload upload = uploadList.get(position);
        holder.usernameTextView.setText(upload.getUsername());
        holder.titleTextView.setText(upload.getTitle());
        holder.descriptionTextView.setText(upload.getDescription());
        holder.likeCountTextView.setText(String.valueOf(upload.getLikes()));

        // Load image with Glide
        Glide.with(holder.itemView.getContext())
                .load(upload.getImageUrl())
                .into(holder.imageView);

        // Handle like button click
        holder.btnLike.setOnClickListener(v -> {
            // Tambahkan log untuk cek apakah tombol ditekan
            Log.d("UploadAdapter", "btnLike diklik pada posisi: " + position);

            String username = upload.getUsername();
            String title = upload.getTitle();

            DatabaseReference likeRef = FirebaseDatabase.getInstance()
                    .getReference(username + "/album/" + title + "/likes");

            // Ambil jumlah likes dari Firebase
            likeRef.get().addOnSuccessListener(snapshot -> {
                int currentLikes = snapshot.exists() ? snapshot.getValue(Integer.class) : 0;
                Log.d("UploadAdapter", "Current likes: " + currentLikes);

                int updatedLikes = currentLikes + 1;
                Log.d("UploadAdapter", "Updated likes: " + updatedLikes);

                // Simpan kembali ke Firebase dan update UI
                likeRef.setValue(updatedLikes).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        holder.likeCountTextView.setText(String.valueOf(updatedLikes));
                        Log.d("UploadAdapter", "Like count updated successfully");
                    } else {
                        Log.e("UploadAdapter", "Failed to update like count", task.getException());
                    }
                });
            }).addOnFailureListener(e -> {
                Log.e("UploadAdapter", "Failed to retrieve likes", e);
            });
        });


    }

    @Override
    public int getItemCount() {
        return uploadList.size();
    }

    public static class UploadViewHolder extends RecyclerView.ViewHolder {
        TextView usernameTextView, titleTextView, descriptionTextView, likeCountTextView;
        ImageView imageView;
        Button btnLike; // Pastikan ini Button, bukan ImageButton

        public UploadViewHolder(@NonNull View itemView) {
            super(itemView);
            usernameTextView = itemView.findViewById(R.id.usernameUploader);
            titleTextView = itemView.findViewById(R.id.textTitle);
            descriptionTextView = itemView.findViewById(R.id.textDescription);
            likeCountTextView = itemView.findViewById(R.id.likeCount);
            imageView = itemView.findViewById(R.id.imageView);
            btnLike = itemView.findViewById(R.id.btnLike); // Button di sini
        }
    }

}
