package com.example.imaginate;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.imaginate.models.Album;

import java.util.ArrayList;


public class AlbumAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 0; // View type untuk header
    private static final int TYPE_ITEM = 1;   // View type untuk item biasa

    private ArrayList<Album> albumItems;
    private Context context;

    public AlbumAdapter(ArrayList<Album> albumItems) {
        this.albumItems = albumItems;
        this.context = context;
    }

    @Override
    public int getItemViewType(int position) {
        // Posisi 0 adalah header
        return (position == 0) ? TYPE_HEADER : TYPE_ITEM;
    }



    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_HEADER) {
            // Inflate layout untuk header
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_header, parent, false);
            return new HeaderViewHolder(view);
        } else {
            // Inflate layout untuk item biasa
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_upload, parent, false);
            return new ItemViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (getItemViewType(position) == TYPE_HEADER) {
            // Bind data untuk header
            HeaderViewHolder headerHolder = (HeaderViewHolder) holder;
            // Contoh konten header
        } else {
            // Bind data untuk item biasa
            int itemPosition = position - 1; // Kurangi 1 karena posisi 0 adalah header
            Album album = albumItems.get(itemPosition);

            ItemViewHolder itemHolder = (ItemViewHolder) holder;
            itemHolder.titleTextView.setText(album.getTitle());
            itemHolder.descriptionTextView.setText(album.getDescription());
            Glide.with(itemHolder.itemView.getContext()) // Gunakan konteks dari view
                    .load(album.getUrl())
                    .placeholder(R.drawable.bicture) // Tambahkan placeholder
                    .error(R.drawable.error_image)       // Tambahkan gambar error
                    .into(itemHolder.imageView);

            // Menampilkan tanggal upload (jika tersedia)
            itemHolder.uploadDateTextView.setText(album.getUploadDate());
        }
    }



    @Override
    public int getItemCount() {
        return albumItems.size() + 1; // Tambahkan 1 untuk header
    }

    // ViewHolder untuk header
    public static class HeaderViewHolder extends RecyclerView.ViewHolder {
        ConstraintLayout headerTextView;

        public HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            headerTextView = itemView.findViewById(R.id.header_title); // Pastikan ID ini ada di item_header.xml
        }
    }

    // ViewHolder untuk item biasa
    public static class ItemViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        TextView titleTextView, descriptionTextView, uploadDateTextView;

        public ItemViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageView);
            titleTextView = itemView.findViewById(R.id.textTitle);
            descriptionTextView = itemView.findViewById(R.id.textDescription);
            uploadDateTextView = itemView.findViewById(R.id.uploadDateTextView); // Pastikan ID ini ada di item_upload.xml
        }
    }
}
