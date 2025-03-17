package com.example.imaginate;

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class Login extends AppCompatActivity {

    private EditText etUsername, etPassword;
    private TextView btnRegister, btnLogin;
    private Dialog loadingDialog;
    private DatabaseReference database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Periksa status login terlebih dahulu sebelum menampilkan layout
        if (checkLoginStatus()) {
            navigateToMainActivity();
            return; // Penting untuk menghentikan eksekusi method onCreate
        }

        setContentView(R.layout.activity_login);

        btnRegister = findViewById(R.id.btnRegister);
        btnLogin = findViewById(R.id.btnLogin);
        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);

        loadingDialog = new Dialog(Login.this);
        loadingDialog.setContentView(R.layout.dialog_loading_small);
        loadingDialog.setCancelable(false);
        loadingDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent register = new Intent(getApplicationContext(), Register.class);
                startActivity(register);
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                finish(); // Tutup Login agar tidak bisa kembali dengan tombol back
            }
        });

        // Add this code inside your onCreate method, after the initialization of other views
        findViewById(R.id.seePassword).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Get the current cursor position
                int cursorPosition = etPassword.getSelectionStart();

                // Toggle password visibility
                if (etPassword.getInputType() == (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD)) {
                    // Show password
                    etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                    ((ImageView) view).setImageResource(R.drawable.crossed_eye_24); // Change to crossed eye icon
                } else {
                    // Hide password
                    etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                    ((ImageView) view).setImageResource(R.drawable.eye_24); // Change back to normal eye icon
                }

                // Restore font
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    etPassword.setTypeface(Typeface.create(getResources().getFont(R.font.gentium), Typeface.NORMAL));
                }

                // Restore cursor position
                etPassword.setSelection(cursorPosition);
            }
        });

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String email = etUsername.getText().toString();
                String password = etPassword.getText().toString();

                database = FirebaseDatabase.getInstance().getReference("users");

                if (email.isEmpty() || password.isEmpty()) {
                    Toast.makeText(getApplicationContext(), "Email atau Password tidak boleh kosong", Toast.LENGTH_SHORT).show();
                } else {
                    loadingDialog.show();
                    loginUser(email, password);
                }
            }
        });

        if (isDarkTheme()) {
            setStatusBarForDarkTheme();
        } else {
            setStatusBarForLightTheme();
        }
    }

    // Metode baru untuk login user - membuat kode lebih rapi dan terorganisir
    private void loginUser(String email, String password) {
        database.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                boolean isAuthenticated = false;
                String userId = null;
                String username = null;

                for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                    String storedEmail = userSnapshot.child("email").getValue(String.class);
                    String storedPassword = userSnapshot.child("password").getValue(String.class);

                    if (email.equals(storedEmail) && password.equals(storedPassword)) {
                        isAuthenticated = true;
                        userId = userSnapshot.getKey();
                        username = userSnapshot.child("username").getValue(String.class);
                        break;
                    }
                }

                loadingDialog.dismiss();

                if (isAuthenticated) {
                    saveUserSession(userId, username);
                    Toast.makeText(getApplicationContext(), "Login Berhasil", Toast.LENGTH_SHORT).show();
                    navigateToMainActivity();
                } else {
                    Toast.makeText(getApplicationContext(), "Email atau Password salah", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                loadingDialog.dismiss();
                Toast.makeText(getApplicationContext(), "Gagal terhubung ke database: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Metode untuk menyimpan data sesi pengguna
    private void saveUserSession(String userId, String username) {
        SharedPreferences sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("is_logged_in", true); // Gunakan key yang konsisten dengan SplashScreenActivity
        editor.putString("userId", userId);
        if (username != null) {
            editor.putString("username", username);
        }
        editor.apply();
    }

    // Metode untuk navigasi ke MainActivity
    private void navigateToMainActivity() {
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        finish();
    }

    // Metode untuk memeriksa status login
    private boolean checkLoginStatus() {
        SharedPreferences sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE);
        return sharedPreferences.getBoolean("is_logged_in", false);
    }

    private void setStatusBarForLightTheme() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getWindow().setStatusBarColor(getResources().getColor(R.color.black, null));
            WindowInsetsControllerCompat windowInsetsController =
                    WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
            windowInsetsController.setAppearanceLightStatusBars(true);
        }
    }

    private void setStatusBarForDarkTheme() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getWindow().setStatusBarColor(getResources().getColor(R.color.black, null));
            WindowInsetsControllerCompat windowInsetsController =
                    WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
            windowInsetsController.setAppearanceLightStatusBars(false);
        }
    }

    private boolean isDarkTheme() {
        // Implement your logic to check if the dark theme is enabled
        return (getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
                android.content.res.Configuration.UI_MODE_NIGHT_YES;
    }
}

    // Metode onStart tidak diperlukan lagi karena pemeriksaan status login sudah dilakukan di awal onCreate
    // sehingga dihapus untuk menghindari duplikasi kode