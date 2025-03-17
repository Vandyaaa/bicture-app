package com.example.imaginate;

import static androidx.core.graphics.drawable.DrawableCompat.applyTheme;

import android.Manifest;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.VelocityTracker;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.dynamicanimation.animation.DynamicAnimation;
import androidx.dynamicanimation.animation.SpringAnimation;
import androidx.dynamicanimation.animation.SpringForce;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.github.chrisbanes.photoview.PhotoView;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.imageview.ShapeableImageView;

import java.io.File;
public class FullscreenImageActivity extends AppCompatActivity {
    private PhotoView fullscreenImage;
    private View backgroundView;
    private View headerLayout;
    private ImageView downloadButton;
    private ProgressBar downloadProgress;
    private long downloadId;
    private DownloadManager downloadManager;
    private static final int PERMISSION_REQUEST_CODE = 1001;

    private VelocityTracker velocityTracker;

    private ShapeableImageView photoProfile;
    private TextView usernameUploader;

    private BottomSheetBehavior<LinearLayout> bottomSheetBehavior;
    private TextView imageTitle;
    private TextView imageDescription;

    private ScaleGestureDetector scaleGestureDetector;
    private float scaleFactor = 1.0f;
    // Touch modes
    private static final int NONE = 0;

    private final float[] SCALE_LEVELS = {1.0f, 1.5f, 2.0f, 3.0f, 4.0f, 5.0f};
    private int currentScaleIndex = 0;

    private GestureDetector gestureDetector;
    private float lastX, lastY;
    private float posX = 0, posY = 0;

    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fullscreen_image);
        photoProfile = findViewById(R.id.photoProfile);
        fullscreenImage = findViewById(R.id.fullscreen_image);
        backgroundView = findViewById(R.id.background_view);
        headerLayout = findViewById(R.id.headerLayout);
        ImageView closeButton = findViewById(R.id.close_button);
        downloadButton = findViewById(R.id.Download);

        usernameUploader = findViewById(R.id.usernameUploader);

        sharedPreferences = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        boolean isDarkMode = sharedPreferences.getBoolean("dark_mode", false);
        applyTheme(isDarkMode);

        fullscreenImage.setScaleType(ImageView.ScaleType.FIT_CENTER);

        String imageUrl = getIntent().getStringExtra("imageUrl");
        setupBlurredBackground(imageUrl);
        String userPhotoUrl = getIntent().getStringExtra("userPhotoUrl");
        String username = getIntent().getStringExtra("username");
        Glide.with(this)
                .load(imageUrl)
                .into(fullscreenImage);

        if (username != null && !username.isEmpty()) {
            usernameUploader.setText(username);
        } else {
            // Fallback to a default username if no username is provided
            usernameUploader.setText("Unknown User");
        }




        if (userPhotoUrl != null && !userPhotoUrl.isEmpty()) {
            Glide.with(this)
                    .load(userPhotoUrl)
                    .placeholder(R.drawable.profilaselie)
                    .error(R.drawable.profilaselie)
                    .circleCrop()
                    .into(photoProfile);
        }

        if (username != null && !username.isEmpty()) {
            usernameUploader.setText(username);
        } else {
            usernameUploader.setText("Unknown User");
        }

        // Initialize gesture detectors

        startEnterAnimation();
        closeButton.setOnClickListener(v -> finishWithExitAnimation());

        downloadProgress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        ConstraintLayout.LayoutParams params = new ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.MATCH_PARENT,
                8 // Set fixed height for progress bar
        );
        params.setMargins(50, 0, 50, 0);
        downloadProgress.setLayoutParams(params);
        downloadProgress.setVisibility(View.GONE);
        downloadProgress.setProgressDrawable(ContextCompat.getDrawable(this, R.drawable.progress_bar));
        ((ConstraintLayout) findViewById(R.id.headerLayout)).addView(downloadProgress);

        // Initialize DownloadManager
        downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);

        // Register broadcast receiver for download completion
        registerReceiver(downloadReceiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));

        initializeViews();
        setupBottomSheet();
        loadImage();

        SpringForce springForce = new SpringForce()
                .setFinalPosition(1.0f)
                .setDampingRatio(SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY) // Efek elastis
                .setStiffness(SpringForce.STIFFNESS_LOW); // Lebih smooth

        SpringAnimation springAnimX = new SpringAnimation(fullscreenImage, DynamicAnimation.SCALE_X);
        SpringAnimation springAnimY = new SpringAnimation(fullscreenImage, DynamicAnimation.SCALE_Y);
        springAnimX.setSpring(springForce);
        springAnimY.setSpring(springForce);


        fullscreenImage.setOnMatrixChangeListener(rect -> {
            float scale = fullscreenImage.getScale();
            if (scale < 1.0f) {
                springAnimX.start();
                springAnimY.start();
            }
        });



//        fullscreenImage.setOnSingleFlingListener((e1, e2, velocityX, velocityY) -> {
//            if (e1.getY() < e2.getY()) { // Swipe ke bawah
//                finishWithExitAnimation();
//                return true;
//            }
//            return false;
//        });
        fullscreenImage.setMaximumScale(10.0f); // Atur batas zoom maksimum (misalnya 10x)
        fullscreenImage.setMinimumScale(1.0f);  // Pastikan bisa kembali ke ukuran normal
        fullscreenImage.setScale(1.0f, true);   // Pastikan mulai dari skala normal
        fullscreenImage.setAdjustViewBounds(true);
        fullscreenImage.setScaleType(ImageView.ScaleType.FIT_CENTER);


    }

    private void applyTheme(boolean isDarkMode) {
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            setStatusBarForDarkTheme();
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            setStatusBarForLightTheme();
        }
    }

    // Mengatur status bar untuk tema terang (ikon hitam)
    private void setStatusBarForLightTheme() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Membuat status bar transparan
            getWindow().setStatusBarColor(getResources().getColor(R.color.transparan, null));
            // Mengatur ikon status bar menjadi hitam
            WindowInsetsControllerCompat windowInsetsController =
                    WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
            windowInsetsController.setAppearanceLightStatusBars(true);
        }
    }

    // Mengatur status bar untuk tema gelap (ikon putih)
    private void setStatusBarForDarkTheme() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Membuat status bar transparan
            getWindow().setStatusBarColor(getResources().getColor(R.color.transparan, null));
            // Mengatur ikon status bar menjadi putih
            WindowInsetsControllerCompat windowInsetsController =
                    WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
            windowInsetsController.setAppearanceLightStatusBars(false);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP) {
            if (velocityTracker != null) {
                velocityTracker.recycle();
                velocityTracker = null;
            }
        }
        return super.onTouchEvent(event);
    }

    private void checkAndAnimateBounds() {
        if (scaleFactor > 1.0f) {
            float imageWidth = fullscreenImage.getWidth() * scaleFactor;
            float imageHeight = fullscreenImage.getHeight() * scaleFactor;
            float maxX = (imageWidth - fullscreenImage.getWidth()) / 2;
            float maxY = (imageHeight - fullscreenImage.getHeight()) / 2;

            float targetX = Math.max(-maxX, Math.min(maxX, posX));
            float targetY = Math.max(-maxY, Math.min(maxY, posY));

            if (targetX != posX || targetY != posY) {
                fullscreenImage.animate()
                        .translationX(targetX)
                        .translationY(targetY)
                        .setDuration(200)
                        .setInterpolator(new DecelerateInterpolator())
                        .start();

                posX = targetX;
                posY = targetY;
            }
        }
    }



    private void setupBlurredBackground(String imageUrl) {
        ImageView backgroundView = findViewById(R.id.background_view);

        // Load the same image into the background with blur effect
        Glide.with(this)
                .asBitmap()
                .load(imageUrl)
                .into(new CustomTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                        // Create a scaled down version for better performance
                        Bitmap scaledBitmap = Bitmap.createScaledBitmap(resource,
                                resource.getWidth() / 2,
                                resource.getHeight() / 2,
                                true);

                        // Apply blur effect
                        Bitmap blurredBitmap = blurBitmap(scaledBitmap);

                        // Set the blurred bitmap as background
                        backgroundView.setImageBitmap(blurredBitmap);

                        // Cleanup
                        scaledBitmap.recycle();
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {
                        // Handle cleanup
                    }
                });
    }

    // Add this method to create the blur effect
    private Bitmap blurBitmap(Bitmap bitmap) {
        // Create another bitmap that will hold the results
        Bitmap outputBitmap = Bitmap.createBitmap(bitmap);

        // Create the Renderscript context
        RenderScript renderScript = RenderScript.create(this);

        // Create an Allocation for the input
        Allocation tmpIn = Allocation.createFromBitmap(renderScript, bitmap);

        // Create an Allocation for the output
        Allocation tmpOut = Allocation.createFromBitmap(renderScript, outputBitmap);

        // Create the Intrinsic Blur Script
        ScriptIntrinsicBlur blurScript = ScriptIntrinsicBlur.create(renderScript, Element.U8_4(renderScript));

        // Set the blur radius (1 to 25)
        blurScript.setRadius(10f);

        // Set the input
        blurScript.setInput(tmpIn);

        // Run the script to blur
        blurScript.forEach(tmpOut);

        // Copy the output to the output bitmap
        tmpOut.copyTo(outputBitmap);

        // Clean up
        renderScript.destroy();
        tmpIn.destroy();
        tmpOut.destroy();
        blurScript.destroy();

        return outputBitmap;
    }


    private void initializeViews() {
        fullscreenImage = findViewById(R.id.fullscreen_image);
        backgroundView = findViewById(R.id.background_view);
        headerLayout = findViewById(R.id.headerLayout);
        ImageView closeButton = findViewById(R.id.close_button);
        downloadButton = findViewById(R.id.Download);
        usernameUploader = findViewById(R.id.usernameUploader);

        // Set up your existing click listeners
        closeButton.setOnClickListener(v -> finishWithExitAnimation());
        downloadButton.setOnClickListener(v -> checkPermissionAndDownload());
    }

    private void setupBottomSheet() {
        // Initialize bottom sheet views
        LinearLayout bottomSheet = findViewById(R.id.bottom_sheet);
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);

        View bottomSheetHandle = findViewById(R.id.drag_handle);
        bottomSheetHandle.setOnClickListener(v -> toggleBottomSheet());
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);

        imageTitle = findViewById(R.id.album_title);
        imageDescription = findViewById(R.id.album_description);

        // Get album details from intent
        String title = getIntent().getStringExtra("imageTitle");
        String description = getIntent().getStringExtra("imageDescription");

        imageTitle.setText(title != null ? title : "Untitled");
        imageDescription.setText(description != null ? description : "No description available");

        // Set up bottom sheet callback
        bottomSheetBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                    // Handle expanded state
                    headerLayout.animate().alpha(0f).setDuration(200);
                } else if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
                    // Handle collapsed state
                    headerLayout.animate().alpha(1f).setDuration(200);
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                // Optional: Add animation effects during sliding
                headerLayout.setAlpha(1 - slideOffset);
            }
        });

        // Set initial state
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
    }

    // Fungsi untuk toggle bottom sheet
    private void toggleBottomSheet() {
        if (bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_COLLAPSED) {
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        } else {
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        }
    }

    private void loadImage() {
        String imageUrl = getIntent().getStringExtra("imageUrl");
        String username = getIntent().getStringExtra("username");

        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(this)
                    .load(imageUrl)
                    .into(fullscreenImage);
        }

        if (username != null && !username.isEmpty()) {
            usernameUploader.setText(username);
        } else {
            usernameUploader.setText("Unknown User");
        }
    }

    private void checkPermissionAndDownload() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    PERMISSION_REQUEST_CODE);
        } else {
            startDownload();
        }
    }

    private void startDownload() {
        String imageUrl = getIntent().getStringExtra("imageUrl");
        if (imageUrl == null || imageUrl.isEmpty()) {
            Toast.makeText(this, "Invalid image URL", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create download directory if it doesn't exist
        File directory = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "Imaginate");
        if (!directory.exists()) {
            directory.mkdirs();
        }

        // Create download request
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(imageUrl))
                .setTitle("Downloading Image")
                .setDescription("Downloading image from Imaginate")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_PICTURES,
                        "Imaginate/IMG_" + System.currentTimeMillis() + ".jpg");

        // Start download
        downloadId = downloadManager.enqueue(request);
        downloadProgress.setVisibility(View.VISIBLE);
        downloadButton.setEnabled(false);

        // Start progress tracking
        startProgressTracking();
    }

    private void startProgressTracking() {
        new Thread(() -> {
            boolean downloading = true;
            while (downloading) {
                DownloadManager.Query q = new DownloadManager.Query();
                q.setFilterById(downloadId);

                android.database.Cursor cursor = downloadManager.query(q);
                if (cursor != null && cursor.moveToFirst()) {
                    try {
                        int bytesDownloadedIndex = cursor.getColumnIndex(
                                DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR);
                        int bytesTotalIndex = cursor.getColumnIndex(
                                DownloadManager.COLUMN_TOTAL_SIZE_BYTES);
                        int statusIndex = cursor.getColumnIndex(
                                DownloadManager.COLUMN_STATUS);

                        if (bytesDownloadedIndex >= 0 && bytesTotalIndex >= 0) {
                            int bytes_downloaded = cursor.getInt(bytesDownloadedIndex);
                            int bytes_total = cursor.getInt(bytesTotalIndex);

                            if (bytes_total > 0) {
                                final int progress = (int) ((bytes_downloaded * 100L) / bytes_total);
                                runOnUiThread(() -> downloadProgress.setProgress(progress));
                            }
                        }

                        if (statusIndex >= 0 &&
                                cursor.getInt(statusIndex) == DownloadManager.STATUS_SUCCESSFUL) {
                            downloading = false;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        downloading = false;
                    }
                } else {
                    downloading = false;
                }

                if (cursor != null) {
                    cursor.close();
                }

                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }).start();
    }

    private final BroadcastReceiver downloadReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
            if (id == downloadId) {
                runOnUiThread(() -> {
                    downloadProgress.setVisibility(View.GONE);
                    downloadButton.setEnabled(true);
                    Toast.makeText(context, "Download completed", Toast.LENGTH_SHORT).show();
                });
            }
        }
    };

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startDownload();
            } else {
                Toast.makeText(this, "Storage permission required to download images",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(downloadReceiver);
    }



    private void startEnterAnimation() {
        // Animasi fade-in untuk gambar
        AlphaAnimation imageFadeIn = new AlphaAnimation(0f, 1f);
        imageFadeIn.setDuration(300);
        imageFadeIn.setFillAfter(true);
        fullscreenImage.startAnimation(imageFadeIn);

        // Animasi fade-in untuk latar belakang
        AlphaAnimation backgroundFadeIn = new AlphaAnimation(0f, 1f);
        backgroundFadeIn.setDuration(300);
        backgroundFadeIn.setFillAfter(true);
        backgroundView.startAnimation(backgroundFadeIn);
    }

    private void finishWithExitAnimation() {
        // Animasi fade-out untuk latar belakang
        AlphaAnimation backgroundFadeOut = new AlphaAnimation(1f, 0f);
        backgroundFadeOut.setDuration(300);
        backgroundFadeOut.setFillAfter(true);

        // Animasi fade-out untuk gambar
        AlphaAnimation imageFadeOut = new AlphaAnimation(1f, 0f);
        imageFadeOut.setDuration(300);
        imageFadeOut.setFillAfter(true);

        // Jalankan animasi fade-out
        backgroundView.startAnimation(backgroundFadeOut);
        fullscreenImage.startAnimation(imageFadeOut);

        // Akhiri aktivitas setelah animasi selesai
        backgroundFadeOut.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {}

            @Override
            public void onAnimationEnd(Animation animation) {
                finish();
                overridePendingTransition(0, 0); // Tanpa transisi bawaan
            }

            @Override
            public void onAnimationRepeat(Animation animation) {}
        });
    }

//    private void toggleHeaderVisibility() {
//        if (headerLayout.getVisibility() == View.VISIBLE) {
//            headerLayout.animate()
//                    .alpha(0f)
//                    .setDuration(200)
//                    .setListener(new AnimatorListenerAdapter() {
//                        @Override
//                        public void onAnimationEnd(Animator animation) {
//                            headerLayout.setVisibility(View.GONE);
//                        }
//                    });
//        } else {
//            headerLayout.setVisibility(View.VISIBLE);
//            headerLayout.animate()
//                    .alpha(1f)
//                    .setDuration(200)
//                    .setListener(null);
//        }
//    }

}