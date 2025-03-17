package com.example.imaginate;

import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.yalantis.ucrop.UCrop;

import java.io.File;

public class CustomUCropActivity extends AppCompatActivity {
    private ImageView btnRotate, btnCrop;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_custom_ucrop);

        btnRotate = findViewById(R.id.custom_btn_rotate);
        btnCrop = findViewById(R.id.custom_btn_crop);

        Uri sourceUri = getIntent().getParcelableExtra("SOURCE_URI");
        Uri destinationUri = Uri.fromFile(new File(getCacheDir(), "cropped_image.jpg"));

        // Setup UCrop
        UCrop uCrop = UCrop.of(sourceUri, destinationUri)
                .withAspectRatio(1, 1)
                .withMaxResultSize(1080, 1080)
                .withOptions(getUCropOptions());

        uCrop.start(this);

        btnRotate.setOnClickListener(v -> rotateImage());
        btnCrop.setOnClickListener(v -> finishCropping());
    }

    private UCrop.Options getUCropOptions() {
        UCrop.Options options = new UCrop.Options();
        options.setCompressionQuality(80);
        options.setCircleDimmedLayer(true);
        options.setStatusBarColor(Color.BLACK);
        options.setToolbarColor(Color.BLACK);
        options.setToolbarWidgetColor(Color.WHITE);
        options.setActiveControlsWidgetColor(Color.WHITE);
        options.setCropFrameColor(Color.WHITE);
        options.setCropGridColor(Color.WHITE);
        options.setDimmedLayerColor(Color.BLACK);
        return options;
    }

    private void rotateImage() {
        // Implementasi rotate image
    }

    private void finishCropping() {
        setResult(RESULT_OK);
        finish();
    }
}
