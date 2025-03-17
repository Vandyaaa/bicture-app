package com.example.imaginate;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

public class Settings extends AppCompatActivity {
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Inisialisasi SharedPreferences
        sharedPreferences = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        int themeMode = sharedPreferences.getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);

        // Terapkan tema
        applyTheme(themeMode);

        setContentView(R.layout.settings);

        ImageButton closeButton = findViewById(R.id.close_button);
        closeButton.setOnClickListener(v -> finish());

        RadioGroup radioGroupTheme = findViewById(R.id.radioGroupTheme);
        RadioButton radioLight = findViewById(R.id.radioLight);
        RadioButton radioDark = findViewById(R.id.radioDark);
        RadioButton radioSystem = findViewById(R.id.radioSystem);

        // Set the current selection based on the saved theme mode
        switch (themeMode) {
            case AppCompatDelegate.MODE_NIGHT_NO:
                radioLight.setChecked(true);
                break;
            case AppCompatDelegate.MODE_NIGHT_YES:
                radioDark.setChecked(true);
                break;
            case AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM:
                radioSystem.setChecked(true);
                break;
        }

        // Handle theme change
        radioGroupTheme.setOnCheckedChangeListener((group, checkedId) -> {
            int selectedMode;
            boolean isDarkMode;

            if (checkedId == R.id.radioLight) {
                selectedMode = AppCompatDelegate.MODE_NIGHT_NO;
                isDarkMode = false;
            } else if (checkedId == R.id.radioDark) {
                selectedMode = AppCompatDelegate.MODE_NIGHT_YES;
                isDarkMode = true;
            } else {
                selectedMode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
                // Untuk mode sistem, tentukan isDarkMode berdasarkan sistem
                int currentNightMode = getResources().getConfiguration().uiMode
                        & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
                isDarkMode = currentNightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES;
            }

            // Simpan kedua preferensi (untuk backward compatibility)
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putInt("theme_mode", selectedMode);
            editor.putBoolean("dark_mode", isDarkMode);
            editor.apply();

            // Terapkan tema baru
            setApplicationTheme(selectedMode);
        });

        ImageButton btnLogout = findViewById(R.id.btnLogout);
        btnLogout.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(Settings.this);
            View logoutDialogView = LayoutInflater.from(Settings.this).inflate(R.layout.dialog_confirm_logout, null);

            TextView cancelButton = logoutDialogView.findViewById(R.id.cancel);
            TextView logoutButton = logoutDialogView.findViewById(R.id.logout);

            builder.setView(logoutDialogView);
            AlertDialog dialog = builder.create();
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

            cancelButton.setOnClickListener(v1 -> dialog.dismiss());

            logoutButton.setOnClickListener(v1 -> {
                SharedPreferences preferences = getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = preferences.edit();
                editor.clear(); // Atau editor.putBoolean("is_logged_in", false);
                editor.apply();

                Intent intent = new Intent(Settings.this, Login.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
                dialog.dismiss();
            });

            dialog.show();
        });
    }

    // Metode untuk mengubah tema aplikasi
    private void setApplicationTheme(int themeMode) {
        // Terapkan tema
        applyTheme(themeMode);

        // Restart activity untuk menerapkan tema baru
        new android.os.Handler().post(() -> {
            Intent intent = new Intent(Settings.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });
    }

    // Metode untuk menerapkan tema dan menyesuaikan status bar
    private void applyTheme(int themeMode) {
        AppCompatDelegate.setDefaultNightMode(themeMode);

        // Periksa mode malam saat ini setelah penerapan
        boolean isDarkTheme = (themeMode == AppCompatDelegate.MODE_NIGHT_YES) ||
                (themeMode == AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM &&
                        (getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
                                android.content.res.Configuration.UI_MODE_NIGHT_YES);

        if (isDarkTheme) {
            setStatusBarForDarkTheme();
        } else {
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
}