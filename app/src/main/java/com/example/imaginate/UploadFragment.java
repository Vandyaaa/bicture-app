package com.example.imaginate;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.InputFilter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class UploadFragment extends Fragment {
    private static final int PICK_IMAGE_REQUEST = 3;
    private EditText etTitle, etDescription;
    private ImageView photoProfileUpload;
    private LinearLayout uploadImage;
    private TextView usernameUploader;
    private TextView uploadButton;
    private ImageView iyakan;
    private Uri imageUri;
    private DatabaseReference databaseRef;
    private StorageReference storageRef;
    private LinearLayout progressBar;
    private static final int MAX_TITLE_LENGTH = 30;
    private static final int MAX_DESCRIPTION_LENGTH = 7000;
    private LinearLayout infotext;
    private ValueEventListener userDataListener;
    private ValueEventListener postCountListener;
    private DatabaseReference userRef;
    private String userId;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_upload, container, false);

        etTitle = view.findViewById(R.id.etTitle);
        etDescription = view.findViewById(R.id.etDescription);
        uploadImage = view.findViewById(R.id.uploadImage);
        uploadButton = view.findViewById(R.id.uploadButton);
        photoProfileUpload = view.findViewById(R.id.photoProfileUpload);
        usernameUploader = view.findViewById(R.id.usernameUploader);
        iyakan = view.findViewById(R.id.iyakan);
        progressBar = view.findViewById(R.id.progressBar);
        progressBar.setVisibility(View.INVISIBLE);
        infotext = view.findViewById(R.id.infotext);

        etTitle.setFilters(new InputFilter[] {
                new InputFilter.LengthFilter(MAX_TITLE_LENGTH)
        });

        etDescription.setFilters(new InputFilter[] {
                new InputFilter.LengthFilter(MAX_DESCRIPTION_LENGTH)
        });

        etTitle.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                int remainingChars = MAX_TITLE_LENGTH - s.length();
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });

        etDescription.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                int remainingChars = MAX_DESCRIPTION_LENGTH - s.length();
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });

        setupFirebaseReferences();

        uploadImage.setOnClickListener(v -> openFileChooser());

        uploadButton.setOnClickListener(v -> {
            if (imageUri != null) {
                uploadImage();
            } else {
                showToast("Pilih gambar terlebih dahulu");
            }
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadUserData();
    }

    @Override
    public void onPause() {
        super.onPause();
        detachListeners();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        detachListeners();
    }

    private void setupFirebaseReferences() {
        // Get userId safely with null checks
        if (!isAdded()) return;

        try {
            SharedPreferences sharedPreferences = requireActivity().getSharedPreferences("user_prefs", getContext().MODE_PRIVATE);
            userId = sharedPreferences.getString("userId", null);

            if (userId != null) {
                // Set up Firebase references
                userRef = FirebaseDatabase.getInstance().getReference("users").child(userId);
                databaseRef = userRef.child("album");
                storageRef = FirebaseStorage.getInstance().getReference("uploads").child(userId);
            } else {
                showToast("User tidak ditemukan, silakan login ulang");
            }
        } catch (Exception e) {
            if (isAdded()) {
                showToast("Error setting up Firebase: " + e.getMessage());
            }
        }
    }

    private void loadUserData() {
        if (!isAdded() || userRef == null) return;

        // Create the listener
        if (userDataListener == null) {
            userDataListener = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (!isAdded()) return;

                    String fetchedUsername = snapshot.child("username").getValue(String.class);
                    String fetchedPhotoProfile = snapshot.child("photoprofile").getValue(String.class);

                    // Set username uploader
                    if (fetchedUsername != null) {
                        usernameUploader.setText(fetchedUsername);
                    } else {
                        usernameUploader.setText("Unknown User");
                    }

                    // Set foto profil uploader
                    if (fetchedPhotoProfile != null && !fetchedPhotoProfile.isEmpty() && isAdded()) {
                        try {
                            Glide.with(requireContext())
                                    .load(fetchedPhotoProfile)
                                    .placeholder(R.drawable.ikonprofildfl)
                                    .error(R.drawable.ikonprofildfl)
                                    .into(photoProfileUpload);
                        } catch (Exception e) {
                            if (isAdded()) {
                                photoProfileUpload.setImageResource(R.drawable.ikonprofildfl);
                            }
                        }
                    } else if (isAdded()) {
                        photoProfileUpload.setImageResource(R.drawable.ikonprofildfl);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    if (isAdded()) {
                        showToast("Gagal memuat data pengguna: " + error.getMessage());
                    }
                }
            };
        }

        // Attach the listener
        try {
            userRef.addListenerForSingleValueEvent(userDataListener);
        } catch (Exception e) {
            if (isAdded()) {
                showToast("Error loading user data: " + e.getMessage());
            }
        }
    }

    private void detachListeners() {
        // Only need to detach active listeners
        if (postCountListener != null && userRef != null) {
            userRef.child("post").removeEventListener(postCountListener);
            postCountListener = null;
        }
    }

    private void openFileChooser() {
        if (!isAdded()) return;

        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (!isAdded()) return;

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == getActivity().RESULT_OK && data != null && data.getData() != null) {
            imageUri = data.getData();
            iyakan.setImageURI(imageUri);
            infotext.setVisibility(View.GONE); // Hide infotext when image is selected
        }
    }

    private void uploadImage() {
        if (!isAdded()) return;

        // Validate title
        String title = etTitle.getText().toString().trim();
        if (title.isEmpty()) {
            etTitle.setError("Title is required");
            etTitle.requestFocus();
            return;
        }

        // Validate description
        String description = etDescription.getText().toString().trim();
        if (description.isEmpty()) {
            etDescription.setError("Description is required");
            etDescription.requestFocus();
            return;
        }

        // Validate image
        if (imageUri == null) {
            showToast("Please select an image first");
            uploadImage.requestFocus();
            return;
        }

        if (userId == null) {
            showToast("User not found. Please login again");
            return;
        }

        // Show progress bar
        progressBar.setVisibility(View.VISIBLE);
        String timestamp = String.valueOf(System.currentTimeMillis());
        String humanReadableDate = new SimpleDateFormat("dd MMMM yyyy", Locale.ENGLISH).format(new Date());

        // Generate 11-digit album ID
        int albumId = generateUniqueAlbumId();

        // Upload process
        final StorageReference fileReference = storageRef.child("image_" + timestamp + ".jpg");

        fileReference.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> {
                    if (!isAdded()) {
                        return; // Fragment is no longer attached
                    }

                    fileReference.getDownloadUrl().addOnSuccessListener(uri -> {
                        if (!isAdded()) {
                            return; // Fragment is no longer attached
                        }

                        String imageUrl = uri.toString();

                        // Save album data to Firebase Database with albumId as Integer
                        DatabaseReference newAlbumRef = databaseRef.child(String.valueOf(albumId));
                        newAlbumRef.child("id").setValue(albumId);
                        newAlbumRef.child("url").setValue(imageUrl);
                        newAlbumRef.child("description").setValue(description);
                        newAlbumRef.child("timestamp").setValue(Long.parseLong(timestamp));
                        newAlbumRef.child("humanReadableDate").setValue(humanReadableDate);
                        newAlbumRef.child("title").setValue(title);

                        // Update post count with a safe listener
                        updatePostCount();

                        progressBar.setVisibility(View.INVISIBLE);
                        showToast("Upload successful");
                        resetFields();
                    });
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) {
                        return; // Fragment is no longer attached
                    }

                    progressBar.setVisibility(View.INVISIBLE);
                    showToast("Upload failed: " + e.getMessage());
                });
    }

    private void updatePostCount() {
        if (!isAdded() || userId == null) return;

        DatabaseReference postRef = FirebaseDatabase.getInstance().getReference("users").child(userId).child("post");

        // Create a new listener for the post count update
        postCountListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;

                Integer currentPostCount = snapshot.getValue(Integer.class);
                if (currentPostCount == null) {
                    currentPostCount = 0;
                }
                postRef.setValue(currentPostCount + 1);

                // Remove listener after use
                postRef.removeEventListener(this);
                postCountListener = null;
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (!isAdded()) return;

                showToast("Failed to update post count: " + error.getMessage());

                // Remove listener on cancel
                postRef.removeEventListener(this);
                postCountListener = null;
            }
        };

        // Attach the listener
        postRef.addListenerForSingleValueEvent(postCountListener);
    }

    private int generateUniqueAlbumId() {
        // Generate a random 11-digit number
        int min = 1000000000; // Smallest 11-digit number
        int max = Integer.MAX_VALUE; // Largest possible integer
        return (int) (Math.random() * (max - min + 1)) + min;
    }

    private void resetFields() {
        if (!isAdded()) return;

        iyakan.setImageResource(0);
        etTitle.setText("");
        etDescription.setText("");
        imageUri = null;
        infotext.setVisibility(View.VISIBLE); // Show infotext when form is reset
    }

    private void showToast(String message) {
        if (isAdded() && getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
    }
}