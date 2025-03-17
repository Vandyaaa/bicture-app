package com.example.imaginate;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

public class SplashScreenActivity extends AppCompatActivity {

    private static final long SPLASH_DELAY = 2000;
    private SharedPreferences sharedPreferences;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sharedPreferences = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        boolean isDarkMode = sharedPreferences.getBoolean("dark_mode", false);
        applyTheme(isDarkMode);
        // Tunggu sebentar sebelum menampilkan layout dengan animasi
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                // Set layout splashscreen yang memiliki animasi
                setContentView(R.layout.splashscreen);

                // Inisialisasi views
                ImageView logoImageView = findViewById(R.id.logoImageView);

                // Membuat animasi
                Animation fadeIn = AnimationUtils.loadAnimation(SplashScreenActivity.this, android.R.anim.fade_in);
                Animation slideUp = AnimationUtils.loadAnimation(SplashScreenActivity.this, R.anim.slide_up);

                // Menerapkan animasi
                logoImageView.startAnimation(fadeIn);

                // Pindah ke Login atau MainActivity setelah animasi selesai
                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        // Cek status login
                        SharedPreferences preferences = getSharedPreferences("user_prefs", MODE_PRIVATE);
                        boolean isLoggedIn = preferences.getBoolean("is_logged_in", false);

                        Intent intent;
                        if (isLoggedIn) {
                            // Jika sudah login, langsung ke MainActivity
                            intent = new Intent(SplashScreenActivity.this, MainActivity.class);
                        } else {
                            // Jika belum login, arahkan ke Login
                            intent = new Intent(SplashScreenActivity.this, Login.class);
                        }

                        startActivity(intent);
                        finish();
                    }
                }, SPLASH_DELAY);
            }
        }, 1000); // Delay 1 detik sebelum menampilkan animasi
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

    @Override
    public void onBackPressed() {
        // Mencegah tombol back ditekan saat splash screen
        // Tidak perlu memanggil super disini untuk benar-benar mencegah back
        super.onBackPressed();
    }
}