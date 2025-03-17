
package com.example.imaginate;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class HeaderItem extends Fragment {
    private static final int PICK_IMAGE_REQUEST = 1;
    private TextView profileUsername, profileEmail;
    private ImageView photoProfile;
    private Button btnLogout;
    private Uri imageUri;
    private DatabaseReference databaseRef;
    private StorageReference storageRef;
    private String currentUsername;


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.layout_header, container, false);

        profileUsername = view.findViewById(R.id.profile_username);
        profileEmail = view.findViewById(R.id.profile_email);
        photoProfile = view.findViewById(R.id.photoProfile);
        btnLogout = view.findViewById(R.id.btnLogout);
        Button editProfileButton = view.findViewById(R.id.EditProfile);

        SharedPreferences sharedPreferences = requireActivity().getSharedPreferences("user_prefs", getContext().MODE_PRIVATE);
        String userId = sharedPreferences.getString("userId", null); // Ambil userId

        if (userId != null) {
            databaseRef = FirebaseDatabase.getInstance().getReference("users").child(userId); // Gunakan userId
            storageRef = FirebaseStorage.getInstance().getReference("uploads").child(userId).child("profile"); // Gunakan userId
            loadProfileData();

            btnLogout.setOnClickListener(v -> {
                sharedPreferences.edit().putBoolean("isLoggedIn", false).apply();
                Intent intent = new Intent(getActivity(), Login.class);
                startActivity(intent);
                requireActivity().finish();
            });

            editProfileButton.setOnClickListener(v -> {
                Intent intent = new Intent(getActivity(), EditProfile.class);
                startActivity(intent);
            });
        } else {
            Toast.makeText(getContext(), "User tidak ditemukan, silakan login ulang", Toast.LENGTH_SHORT).show();
        }

        return view;
    }
    private void loadProfileData() {
        databaseRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String username = snapshot.child("username").getValue(String.class);
                String email = snapshot.child("email").getValue(String.class);
                String photoUrl = snapshot.child("photoprofile").getValue(String.class);

                if (username != null && email != null) {
                    profileUsername.setText(username);
                    profileEmail.setText(email);
                } else {
                    Toast.makeText(getContext(), "Gagal mengambil data profil", Toast.LENGTH_SHORT).show();
                }

                if (photoUrl != null && !photoUrl.isEmpty()) {
                    Glide.with(getContext()).load(photoUrl).into(photoProfile);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

}