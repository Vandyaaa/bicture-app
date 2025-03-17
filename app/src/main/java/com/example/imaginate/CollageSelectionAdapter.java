// Create a new file CollageSelectionAdapter.java
package com.example.imaginate;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.imaginate.models.CollageItem;

import java.util.ArrayList;

public class CollageSelectionAdapter extends RecyclerView.Adapter<CollageSelectionAdapter.CollageViewHolder> {
    private final ArrayList<CollageItem> collageList;
    private final OnCollageSelectedListener listener;

    public interface OnCollageSelectedListener {
        void onCollageSelected(String collageId, String collageName);
    }

    public CollageSelectionAdapter(ArrayList<CollageItem> collageList, OnCollageSelectedListener listener) {
        this.collageList = collageList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CollageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_collage_selection, parent, false);
        return new CollageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CollageViewHolder holder, int position) {
        CollageItem collage = collageList.get(position);
        holder.collageName.setText(collage.getName());
        holder.itemView.setOnClickListener(v ->
                listener.onCollageSelected(collage.getId(), collage.getName()));
    }

    @Override
    public int getItemCount() {
        return collageList.size();
    }

    public class CollageViewHolder extends RecyclerView.ViewHolder {
        TextView collageName;

        public CollageViewHolder(@NonNull View itemView) {
            super(itemView);
            collageName = itemView.findViewById(R.id.collageName);
        }
    }
}