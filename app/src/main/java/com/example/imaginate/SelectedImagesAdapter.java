package com.example.imaginate;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class SelectedImagesAdapter extends RecyclerView.Adapter<SelectedImagesAdapter.ImageViewHolder> {
    private List<Uri> imageUris;
    private OnImageRemoveClickListener removeClickListener;

    public interface OnImageRemoveClickListener {
        void onRemoveClick(Uri uri);
    }

    public SelectedImagesAdapter(List<Uri> imageUris, OnImageRemoveClickListener listener) {
        this.imageUris = imageUris;
        this.removeClickListener = listener;
    }

    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_selected_image, parent, false);
        return new ImageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
        Uri imageUri = imageUris.get(position);

        Glide.with(holder.itemView.getContext())
                .load(imageUri)
                .into(holder.imageView);

        holder.removeButton.setOnClickListener(v -> removeClickListener.onRemoveClick(imageUri));
    }

    @Override
    public int getItemCount() {
        return imageUris.size();
    }

    static class ImageViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        ImageButton removeButton;

        ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.selectedImageView);
            removeButton = itemView.findViewById(R.id.removeImageButton);
        }
    }
}