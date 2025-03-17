package com.example.imaginate;

import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

public class BaseActivity extends AppCompatActivity {

    protected SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Inisialisasi SharedPreferences
        sharedPreferences = getSharedPreferences("AppPrefs", MODE_PRIVATE);

        // Terapkan tema berdasarkan preferensi pengguna
        applyThemeFromPreferences();
    }

    protected void applyThemeFromPreferences() {
        boolean isDarkMode = sharedPreferences.getBoolean("dark_mode", false);
        applyTheme(isDarkMode);
    }

    protected void applyTheme(boolean isDarkMode) {
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            setStatusBarForDarkTheme();
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            setStatusBarForLightTheme();
        }
    }

    // Mengatur status bar untuk tema terang (ikon hitam)
    protected void setStatusBarForLightTheme() {
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
    protected void setStatusBarForDarkTheme() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Membuat status bar transparan
            getWindow().setStatusBarColor(getResources().getColor(R.color.transparan, null));
            // Mengatur ikon status bar menjadi putih
            WindowInsetsControllerCompat windowInsetsController =
                    WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
            windowInsetsController.setAppearanceLightStatusBars(false);
        }
    }
}