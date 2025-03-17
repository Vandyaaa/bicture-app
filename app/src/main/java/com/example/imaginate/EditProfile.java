package com.example.imaginate;

import android.Manifest;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.yalantis.ucrop.UCrop;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class EditProfile extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private static final String SAMPLE_CROPPED_IMAGE_NAME = "Profile_Picture.jpg";

    private ShapeableImageView editPhotoProfile;
    private EditText editUsername, editBio, locationTxt;
    private ImageButton btnSave;
    private ImageButton btnDeletePhoto, btnUploadPhoto;
    private ProgressBar progressBar;
    private ProgressDialog progressDialog;
    private ImageButton btnBack;
    private Dialog loadingDialog;
    private DatabaseReference databaseRef;
    private StorageReference storageRef;
    private FusedLocationProviderClient fusedLocationClient;
    private Uri photoUri;
    private String userId;
    private EditText editTagName;
    private TextView tagNameCount, locationSearch;
    private boolean isTagNameAvailable = false;
    private SharedPreferences sharedPreferences;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.edit_profile);
        TextView usernameCount = findViewById(R.id.UsernameCount);
        usernameCount.setText("0/20");
        TextView descriptionCount = findViewById(R.id.descriptionCount);
        descriptionCount.setText("0/500");
        editTagName = findViewById(R.id.editntagname);
        tagNameCount = findViewById(R.id.tagNameCount);
        tagNameCount.setText("0/20");


        initLoadingDialog();

        // Inisialisasi UI
        btnUploadPhoto = findViewById(R.id.btnUploadPhoto);
        editPhotoProfile = findViewById(R.id.editPhotoProfile);
        editUsername = findViewById(R.id.editUsername);
        editBio = findViewById(R.id.editbio);
        locationTxt = findViewById(R.id.locationtxt);
        locationSearch = findViewById(R.id.locationsearch);
        btnSave = findViewById(R.id.save);
        btnBack = findViewById(R.id.back); // Inisialisasi tombol "Back"
        btnDeletePhoto = findViewById(R.id.btnDeletePhoto);
        progressBar = findViewById(R.id.editProgressBar);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        sharedPreferences = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        boolean isDarkMode = sharedPreferences.getBoolean("dark_mode", false);
        applyTheme(isDarkMode);

        // Inisialisasi Firebase
        SharedPreferences sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE);
        userId = sharedPreferences.getString("userId", null);

        if (userId == null) {
            Toast.makeText(this, "User tidak ditemukan. Silakan login ulang.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        databaseRef = FirebaseDatabase.getInstance().getReference("users").child(userId);
        storageRef = FirebaseStorage.getInstance().getReference("profile_pictures").child(userId);

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Loading...");
        progressDialog.setCancelable(false);

        // Event listeners
        btnUploadPhoto.setOnClickListener(v -> startImageSelection());
        btnSave.setOnClickListener(v -> saveChanges());
        btnDeletePhoto.setOnClickListener(v -> deleteProfilePhoto());
        btnBack.setOnClickListener(v -> onBackPressed());
        locationSearch.setOnClickListener(v -> getCurrentLocation());
        loadUserData();

        editUsername.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Not needed
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Check for spaces in the input
                String input = s.toString();
                if (input.contains(" ")) {
                    // Remove all spaces from the input
                    String trimmedInput = input.replace(" ", "");

                    // Update the EditText with the trimmed input
                    editUsername.setText(trimmedInput);

                    // Move cursor to the end
                    editUsername.setSelection(trimmedInput.length());

                    // Show alert to user
                    Toast.makeText(EditProfile.this,
                            "Username tidak boleh mengandung spasi",
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                int length = s.length();
                usernameCount.setText(length + "/20");

                // If text length is greater than 20, truncate it
                if (length > 20) {
                    s.delete(20, length);
                }
            }
        });

        editUsername.setFilters(new InputFilter[] {
                new InputFilter.LengthFilter(20)
        });

        editBio.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Not needed
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Not needed
            }

            @Override
            public void afterTextChanged(Editable s) {
                int length = s.length();
                descriptionCount.setText(length + "/500");

                // If text length is greater than 500, truncate it
                if (length > 500) {
                    s.delete(500, length);
                }
            }
        });

// Also add this to editBio
        editBio.setFilters(new InputFilter[] {
                new InputFilter.LengthFilter(500)
        });

        editTagName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Not needed
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String input = s.toString();

                // Remove spaces if present
                if (input.contains(" ")) {
                    String trimmedInput = input.replace(" ", "");
                    editTagName.setText(trimmedInput);
                    editTagName.setSelection(trimmedInput.length());
                    Toast.makeText(EditProfile.this,
                            "Tag name tidak boleh mengandung spasi",
                            Toast.LENGTH_SHORT).show();
                }

                // Ensure it starts with @
                if (!input.isEmpty() && !input.startsWith("@")) {
                    editTagName.setText("@" + input);
                    editTagName.setSelection(editTagName.length());
                }

                // Check tagName availability
                if (input.length() > 1) { // Only check if there's content after @
                    checkTagNameAvailability(input);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                int length = s.length();
                tagNameCount.setText(length + "/20");

                // If text length is greater than 20, truncate it
                if (length > 20) {
                    s.delete(20, length);
                }
            }
        });

// Add length filter to tagName
        editTagName.setFilters(new InputFilter[] {
                new InputFilter.LengthFilter(20)
        });
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
            getWindow().setStatusBarColor(getResources().getColor(R.color.black, null));
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
            getWindow().setStatusBarColor(getResources().getColor(R.color.black, null));
            // Mengatur ikon status bar menjadi putih
            WindowInsetsControllerCompat windowInsetsController =
                    WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
            windowInsetsController.setAppearanceLightStatusBars(false);
        }
    }

    private void checkTagNameAvailability(String tagName) {
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");
        usersRef.orderByChild("tagName").equalTo(tagName).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    // Cek apakah tagName dimiliki oleh pengguna saat ini
                    boolean isOwnTagName = false;
                    for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                        String snapshotUserId = userSnapshot.getKey();
                        if (snapshotUserId != null && snapshotUserId.equals(userId)) {
                            isOwnTagName = true;
                            break;
                        }
                    }

                    if (isOwnTagName) {
                        // Ini adalah tagName pengguna sendiri, jadi diperbolehkan
                        isTagNameAvailable = true;
                        editTagName.setError(null);
                    } else {
                        // TagName digunakan oleh pengguna lain
                        isTagNameAvailable = false;
                        editTagName.setError("Tag name sudah digunakan pengguna lain");
                    }
                } else {
                    // TagName belum digunakan siapapun
                    isTagNameAvailable = true;
                    editTagName.setError(null);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(EditProfile.this,
                        "Gagal memeriksa ketersediaan tag name",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void startImageSelection() {

        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    private void startCrop(Uri sourceUri) {
        Uri destinationUri = Uri.fromFile(new File(getCacheDir(), SAMPLE_CROPPED_IMAGE_NAME));
        UCrop.Options options = new UCrop.Options();
        // Basic settings
        options.setCompressionQuality(80);
        options.setCircleDimmedLayer(true); // Make crop area circular for profile picture
        options.setShowCropFrame(true); // Show crop frame
        options.setShowCropGrid(true); // Show crop grid

        // Customize colors
        options.setStatusBarColor(ContextCompat.getColor(this, R.color.black));
        options.setToolbarColor(ContextCompat.getColor(this, R.color.black));
        options.setToolbarWidgetColor(ContextCompat.getColor(this, R.color.white));
        options.setActiveControlsWidgetColor(ContextCompat.getColor(this, R.color.white));
        options.setDimmedLayerColor(ContextCompat.getColor(this, R.color.black));
        options.setCropGridColor(ContextCompat.getColor(this, R.color.white));
        options.setCropFrameColor(ContextCompat.getColor(this, R.color.white));
        options.setDimmedLayerColor(ContextCompat.getColor(this, R.color.black));

        // Customize the crop grid
        options.setCropGridColumnCount(2);
        options.setCropGridRowCount(2);
        options.setCropGridStrokeWidth(1);
        options.setCropGridColor(ContextCompat.getColor(this, R.color.white));

        // Customize toolbar
        options.setToolbarTitle("");
        options.setHideBottomControls(true); // Set true untuk menghilangkan kontrol bawah
        options.setFreeStyleCropEnabled(false);

        // Additional settings
        options.setMaxScaleMultiplier(10.0f);
        options.setImageToCropBoundsAnimDuration(300);

        // Kustomisasi ikon

        UCrop.of(sourceUri, destinationUri)
                .withAspectRatio(1, 1)
                .withMaxResultSize(1080, 1080)
                .withOptions(options)
                .start(this);
    }



    private void initLoadingDialog() {
        loadingDialog = new Dialog(this);
        loadingDialog.setContentView(R.layout.dialog_loading_small);
        loadingDialog.setCancelable(false);
        loadingDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
    }

    private void showLoading(String message) {
        if (!loadingDialog.isShowing()) {
            loadingDialog.show();
        }
    }

    private void hideLoading() {
        if (loadingDialog.isShowing()) {
            loadingDialog.dismiss();
        }
    }

    private void loadUserData() {


        databaseRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // Load username
                String username = snapshot.child("username").getValue(String.class);
                editUsername.setText(username);

                String bio = snapshot.child("bio").getValue(String.class);
                if (bio != null) {
                    editBio.setText(bio);
                }

                // Load location
                String location = snapshot.child("location").getValue(String.class);
                if (location != null) {
                    locationTxt.setText(location);
                }

                // Load photo
                String photoUrl = snapshot.child("photoprofile").getValue(String.class);
                if (photoUrl != null && !photoUrl.isEmpty()) {
                    Glide.with(EditProfile.this)
                            .load(photoUrl)
                            .placeholder(R.drawable.default_profile_bct_v1)
                            .into(editPhotoProfile);
                }

                String tagName = snapshot.child("tagName").getValue(String.class);
                if (tagName != null) {
                    editTagName.setText(tagName);
                }

                hideLoading();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                hideLoading();
                Toast.makeText(EditProfile.this, "Gagal memuat data.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void getCurrentLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        getAddressFromLocation(location);
                    } else {
                        Toast.makeText(EditProfile.this, "Tidak dapat mendapatkan lokasi.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(EditProfile.this, "Gagal mendapatkan lokasi.", Toast.LENGTH_SHORT).show());
    }

    private void getAddressFromLocation(Location location) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(
                    location.getLatitude(),
                    location.getLongitude(),
                    1);
            if (!addresses.isEmpty()) {
                Address address = addresses.get(0);
                // Menggunakan getSubAdminArea() untuk mendapatkan nama kota
                String city = address.getSubAdminArea();
                // Mengambil nama provinsi
                String state = address.getAdminArea();
                // Mengambil nama negara
                String country = address.getCountryName();

                // Memastikan semua komponen ada sebelum menggabungkan
                StringBuilder locationString = new StringBuilder();
                if (city != null) {
                    locationString.append(city);
                }
                if (state != null) {
                    if (locationString.length() > 0) locationString.append(", ");
                    locationString.append(state);
                }
                if (country != null) {
                    if (locationString.length() > 0) locationString.append(", ");
                    locationString.append(country);
                }

                locationTxt.setText(locationString.toString());
            } else {
                Toast.makeText(this, "Tidak dapat menemukan alamat untuk lokasi ini.", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            Toast.makeText(this, "Gagal mendapatkan alamat.", Toast.LENGTH_SHORT).show();
        }
    }

    private String getLocalCountryName(String countryCode) {
        // Mapping kode negara dengan Locale yang sesuai
        if (countryCode == null) return null;

        switch (countryCode.toUpperCase()) {
            case "DE": // Jerman
                return new Locale("de", "DE").getDisplayCountry(new Locale("de"));
            case "FR": // Prancis
                return new Locale("fr", "FR").getDisplayCountry(new Locale("fr"));
            case "IT": // Italia
                return new Locale("it", "IT").getDisplayCountry(new Locale("it"));
            case "ES": // Spanyol
                return new Locale("es", "ES").getDisplayCountry(new Locale("es"));
            case "JP": // Jepang
                return new Locale("ja", "JP").getDisplayCountry(new Locale("ja"));
            case "CN":
                return new Locale("zh", "CN").getDisplayCountry(new Locale("zh"));
            case "KR": // Korea Selatan
                return new Locale("ko", "KR").getDisplayCountry(new Locale("ko"));
            case "NL": // Belanda
                return new Locale("nl", "NL").getDisplayCountry(new Locale("nl"));
            case "PT": // Portugal
                return new Locale("pt", "PT").getDisplayCountry(new Locale("pt"));
            case "RU": // Rusia
                return new Locale("ru", "RU").getDisplayCountry(new Locale("ru"));
            case "ID": // Indonesia
                return "Indonesia"; // Menggunakan nama Indonesia karena sudah sama
            default:
                // Untuk negara lain, gunakan nama default dari Locale sistem
                return new Locale("", countryCode).getDisplayCountry(Locale.getDefault());
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation();
            } else {
                Toast.makeText(this, "Izin lokasi diperlukan untuk fitur ini.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            Uri sourceUri = data.getData();
            if (sourceUri != null) {
                // Clear any previous image cache before starting new crop
                Glide.with(this).clear(editPhotoProfile);
                startCrop(sourceUri);
            }
        }
        else if (requestCode == UCrop.REQUEST_CROP && resultCode == RESULT_OK) {
            Uri croppedUri = UCrop.getOutput(data);
            if (croppedUri != null) {
                // Update photoUri with the new cropped image URI
                photoUri = croppedUri;

                // Invalidate the cache before loading the new image
                Glide.get(this).clearMemory();

                // Use Glide with skipMemoryCache and diskCacheStrategy to avoid caching issues
                Glide.with(this)
                        .load(photoUri)
                        .skipMemoryCache(true)
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .placeholder(R.drawable.default_profile_bct_v1)
                        .into(editPhotoProfile);

                // Log untuk debugging
                Log.d("EditProfile", "Loaded cropped image: " + photoUri.toString());
            } else {
                Log.e("EditProfile", "Cropped image URI is null");
            }
        }
        else if (resultCode == UCrop.RESULT_ERROR) {
            final Throwable cropError = UCrop.getError(data);
            if (cropError != null) {
                Log.e("EditProfile", "Crop error: " + cropError.getMessage());
                Toast.makeText(this, "Error: " + cropError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }



    private void uploadProfilePhoto(String newUsername, Dialog saveDialog) {
        if (photoUri == null) {
            saveDialog.dismiss();
            Toast.makeText(this, "Tidak ada foto yang dipilih", Toast.LENGTH_SHORT).show();
            return;
        }

        // Membuat referensi dengan timestamp untuk menghindari nama file yang sama
        StorageReference photoRef = storageRef.child("cropped_" + System.currentTimeMillis() + ".jpg");

        photoRef.putFile(photoUri)
                .addOnSuccessListener(taskSnapshot ->
                        photoRef.getDownloadUrl().addOnSuccessListener(uri -> {
                            String photoUrl = uri.toString();
                            databaseRef.child("photoprofile").setValue(photoUrl)
                                    .addOnSuccessListener(aVoid -> {
                                        updateUsername(newUsername, saveDialog);
                                    })
                                    .addOnFailureListener(e -> {
                                        saveDialog.dismiss();
                                        Toast.makeText(EditProfile.this, "Gagal menyimpan URL foto.", Toast.LENGTH_SHORT).show();
                                    });
                        }))
                .addOnFailureListener(e -> {
                    saveDialog.dismiss();
                    Toast.makeText(EditProfile.this, "Gagal mengupload foto.", Toast.LENGTH_SHORT).show();
                });
    }

    private void updateUsername(String newUsername, Dialog saveDialog) {
        databaseRef.child("username").setValue(newUsername).addOnCompleteListener(task -> {
            saveDialog.dismiss();
            if (task.isSuccessful()) {
                Toast.makeText(EditProfile.this, "Perubahan disimpan.", Toast.LENGTH_SHORT).show();
                refreshCollectionFragment();
                switchToCollectionFragment(); // Replace finish() with this
            } else {
                Toast.makeText(EditProfile.this, "Gagal menyimpan perubahan.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void refreshCollectionFragment() {
        Intent intent = new Intent("refresh_collection");
        sendBroadcast(intent);
    }

    private void deleteProfilePhoto() {
        // Create and show loading dialog
        Dialog deleteDialog = new Dialog(this);
        deleteDialog.setContentView(R.layout.dialog_loading_small);
        deleteDialog.setCancelable(false);
        deleteDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);


        // Show the dialog
        deleteDialog.show();

        // Set default photo URL in the database
        String defaultPhotoUrl = ""; // Empty string or you can use a default URL if available

        databaseRef.child("photoprofile").setValue(defaultPhotoUrl).addOnSuccessListener(aVoid -> {
            // Update UI to show default profile image
            editPhotoProfile.setImageResource(R.drawable.default_profile_bct_v1);

            // Reset photoUri to null so we know there's no custom photo to upload
            photoUri = null;

            // Dismiss loading dialog
            deleteDialog.dismiss();

            // Show success message
            Toast.makeText(EditProfile.this, "Foto profil berhasil dihapus", Toast.LENGTH_SHORT).show();
        }).addOnFailureListener(e -> {
            // Dismiss loading dialog
            deleteDialog.dismiss();

            // Show error message
            Toast.makeText(EditProfile.this, "Gagal mengatur foto profil default", Toast.LENGTH_SHORT).show();
        });
    }

    private void saveChanges() {
        String newUsername = editUsername.getText().toString().trim();
        String newTagName = editTagName.getText().toString().trim();
        String newBio = editBio.getText().toString().trim();
        String newLocation = (locationTxt != null) ? locationTxt.getText().toString().trim() : "";

        if (newUsername.isEmpty() || newTagName.isEmpty()) {
            Toast.makeText(this, "Username and Tag Name cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if tagName is available before saving
        if (!isTagNameAvailable && !newTagName.isEmpty()) {
            Toast.makeText(this, "Tag name is already in use", Toast.LENGTH_SHORT).show();
            return;
        }

        Dialog saveDialog = new Dialog(this);
        saveDialog.setContentView(R.layout.dialog_loading_small);
        saveDialog.setCancelable(false);
        saveDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        saveDialog.show();

        // Update database
        databaseRef.child("username").setValue(newUsername);
        databaseRef.child("tagName").setValue(newTagName);
        databaseRef.child("bio").setValue(newBio);
        databaseRef.child("location").setValue(newLocation);

        if (photoUri != null) {
            uploadProfilePhoto(newUsername, saveDialog);
        } else {
            saveDialog.dismiss();
            Toast.makeText(this, "Profile updated", Toast.LENGTH_SHORT).show();
            refreshCollectionFragment();
            switchToCollectionFragment();
        }
    }


    private void switchToCollectionFragment() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("fragment", "CollectionFragment");
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); // Clear the activity stack
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        // Aksi saat tombol back ditekan
        super.onBackPressed();
        finish(); // Menutup aktivitas saat ini
    }
}
