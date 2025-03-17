package com.example.imaginate;

import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;

public class ScaleImageActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.scale_image);

        Log.d("ScaleImageActivity", "Activity created!");


        ImageView imageView = findViewById(R.id.imageView3);
        TextView textTitle = findViewById(R.id.textTitle);
        ImageView backButton = findViewById(R.id.imageButton);

        String imageUrl = getIntent().getStringExtra("image_url");
        String title = getIntent().getStringExtra("title");

        Glide.with(this).load(imageUrl).into(imageView);
        textTitle.setText(title);

        if (imageUrl != null) {
            Log.d("ScaleImageActivity", "Image URL: " + imageUrl);
            Glide.with(this).load(imageUrl).into(imageView);
        } else {
            Log.e("ScaleImageActivity", "No image URL found!");
        }

        backButton.setOnClickListener(v -> finish());

    }
}
