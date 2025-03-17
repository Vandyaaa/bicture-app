package com.example.imaginate;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.example.imaginate.models.LikeData;

import java.util.ArrayList;

public class UserLikesAdapter extends RecyclerView.Adapter<UserLikesAdapter.ViewHolder> {
    private ArrayList<LikeData> userList;
    private final Context context;

    public UserLikesAdapter(ArrayList<LikeData> userList, Context context) {
        this.userList = userList;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_likes, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        LikeData user = userList.get(position);
        holder.username.setText(user.getUsername());

        // Tambahkan ini jika ada TextView untuk tagName
        if (holder.tagName != null) {
            holder.tagName.setText(user.getTagName());
        }

        Context contextToUse = context != null ? context : holder.itemView.getContext();

        Glide.with(contextToUse)
                .load(user.getPhotoProfile())
                .placeholder(R.drawable.ppdf2)
                .error(R.drawable.ppdf2)
                .listener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                        Log.e("GlideError", "Gagal memuat gambar: " + e.getMessage());
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                        Log.d("GlideSuccess", "Gambar berhasil dimuat");
                        return false;
                    }
                })
                .into(holder.photoProfileImageView);

        holder.userProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (contextToUse != null) {
                    navigateToUserProfile(user.getUserId(), (AppCompatActivity) contextToUse);
                }
            }
        });

    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    private void navigateToUserProfile(String userId, AppCompatActivity activity) {
        ProfileShowFragment profileFragment = new ProfileShowFragment();

        // Mengirim userId ke Fragment
        Bundle args = new Bundle();
        args.putString("userId", userId);
        profileFragment.setArguments(args);

        // Menampilkan Fragment
        FragmentManager fragmentManager = activity.getSupportFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.frame_layout, profileFragment) // Ganti dengan ID container Fragment
                .addToBackStack(null)
                .commit();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView username, tagName;
        ImageView photoProfileImageView;
        ConstraintLayout userProfile;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            username = itemView.findViewById(R.id.textUsername);
            tagName = itemView.findViewById(R.id.tagNameTextView);
            photoProfileImageView = itemView.findViewById(R.id.photoProfileImageView);
            userProfile = itemView.findViewById(R.id.userprofile);
        }
    }
}
