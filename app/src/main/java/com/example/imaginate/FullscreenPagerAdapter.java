package com.example.imaginate;

import android.content.Context;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.imaginate.models.Album;

import java.util.ArrayList;

public class FullscreenPagerAdapter extends RecyclerView.Adapter<FullscreenPagerAdapter.FullscreenViewHolder> {
    private final Context context;
    private final ArrayList<Album> albumList;

    public FullscreenPagerAdapter(Context context, ArrayList<Album> albumList) {
        this.context = context;
        this.albumList = albumList;
    }

    @NonNull
    @Override
    public FullscreenViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ImageView imageView = new ImageView(context);
        imageView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);

        return new FullscreenViewHolder(imageView);
    }

    @Override
    public void onBindViewHolder(@NonNull FullscreenViewHolder holder, int position) {
        Album album = albumList.get(position);

        Glide.with(context)
                .load(album.getUrl())
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(holder.imageView);
    }

    @Override
    public int getItemCount() {
        return albumList.size();
    }

    static class FullscreenViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;

        FullscreenViewHolder(ImageView itemView) {
            super(itemView);
            this.imageView = itemView;
        }
    }
}
