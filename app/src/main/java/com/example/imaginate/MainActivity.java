package com.example.imaginate;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.imaginate.databinding.ActivityMainBinding;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;

    // Simpan semua fragment agar tidak dibuat ulang
    private Fragment activeFragment;
    private Fragment homeFragment;
    private Fragment searchFragment;
    private Fragment uploadFragment;
    private Fragment collectionFragment;
    private Fragment bookmarkFragment;

    // Konstanta untuk animasi
    private static final int ANIM_NONE = 0;
    private static final int ANIM_SLIDE = 1;
    private static final int ANIM_FADE = 2;

    // ID menu yang aktif saat ini
    private int currentActiveItemId = R.id.home;

    // Flag untuk pelacakan apakah activity sedang dalam proses restart
    private static boolean isRecreating = false;

    // SharedPreferences untuk tema
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Inisialisasi SharedPreferences dan terapkan tema
        sharedPreferences = getSharedPreferences("AppPrefs", MODE_PRIVATE);

        // Baca pengaturan tema dari SharedPreferences
        int themeMode = sharedPreferences.getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);

        // Terapkan tema
        applyTheme(themeMode);

        // Inflate layout hanya sekali
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Hindari inisialisasi fragment jika activity sedang di-recreate
        if (savedInstanceState == null && !isRecreating) {
            // Buat instance baru fragment hanya jika savedInstanceState null
            homeFragment = new HomeFragment();
            searchFragment = new SearchFragment();
            uploadFragment = new UploadFragment();
            collectionFragment = new CollectionFragment();
            bookmarkFragment = new BookmarkFragment();

            // Inisialisasi semua fragment
            initializeFragments();
        } else {
            // Dapatkan fragment yang sudah ada dari FragmentManager
            FragmentManager fm = getSupportFragmentManager();
            homeFragment = fm.findFragmentByTag("1");
            searchFragment = fm.findFragmentByTag("2");
            uploadFragment = fm.findFragmentByTag("3");
            collectionFragment = fm.findFragmentByTag("4");
            bookmarkFragment = fm.findFragmentByTag("5");

            // Memastikan semua fragment tidak null sebelum mengecek visibility
            if (homeFragment != null && homeFragment.isVisible()) {
                activeFragment = homeFragment;
                currentActiveItemId = R.id.home;
            } else if (searchFragment != null && searchFragment.isVisible()) {
                activeFragment = searchFragment;
                currentActiveItemId = R.id.search;
            } else if (uploadFragment != null && uploadFragment.isVisible()) {
                activeFragment = uploadFragment;
                currentActiveItemId = R.id.upload;
            } else if (collectionFragment != null && collectionFragment.isVisible()) {
                activeFragment = collectionFragment;
                currentActiveItemId = R.id.colection;
            } else if (bookmarkFragment != null && bookmarkFragment.isVisible()) {
                activeFragment = bookmarkFragment;
                currentActiveItemId = R.id.profile;
            } else {
                // Fallback jika tidak ada fragment yang visible
                if (homeFragment == null) {
                    homeFragment = new HomeFragment();
                    searchFragment = new SearchFragment();
                    uploadFragment = new UploadFragment();
                    collectionFragment = new CollectionFragment();
                    bookmarkFragment = new BookmarkFragment();

                    initializeFragments();
                } else {
                    activeFragment = homeFragment;
                    currentActiveItemId = R.id.home;
                }
            }
        }

        isRecreating = false;

        // Menyembunyikan background bottom navigation
        binding.bottomNavigationView.setBackground(null);

        // Set listener untuk navigasi
        setupBottomNavigation();

        // Set ikon aktif sesuai fragment yang aktif
        updateIcons(currentActiveItemId);
        binding.bottomNavigationView.setSelectedItemId(currentActiveItemId);

        // Check if we need to navigate to a specific fragment from intent
        if (savedInstanceState == null) {
            checkIntentForNavigation();
        }
    }

    private void applyTheme(int themeMode) {
        // Terapkan tema sesuai mode yang dipilih
        AppCompatDelegate.setDefaultNightMode(themeMode);

        // Periksa mode malam saat ini setelah penerapan
        boolean isDarkTheme = (themeMode == AppCompatDelegate.MODE_NIGHT_YES) ||
                (themeMode == AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM &&
                        (getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
                                android.content.res.Configuration.UI_MODE_NIGHT_YES);

        // Perbarui flag dark_mode di SharedPreferences
        sharedPreferences.edit().putBoolean("dark_mode", isDarkTheme).apply();

        if (isDarkTheme) {
            setStatusBarForDarkTheme();
        } else {
            setStatusBarForLightTheme();
        }
    }

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
    protected void onResume() {
        super.onResume();
        // Memeriksa dan menerapkan tema saat activity dilanjutkan
        int themeMode = sharedPreferences.getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        applyTheme(themeMode);
    }

    public void setApplicationTheme(int themeMode) {
        // Simpan pengaturan tema
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("theme_mode", themeMode);

        // Perbarui flag dark_mode untuk kompatibilitas
        boolean isDarkMode = (themeMode == AppCompatDelegate.MODE_NIGHT_YES) ||
                (themeMode == AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM &&
                        (getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
                                android.content.res.Configuration.UI_MODE_NIGHT_YES);
        editor.putBoolean("dark_mode", isDarkMode);
        editor.apply();

        // Set flag bahwa activity sedang dalam proses recreate
        isRecreating = true;

        // Terapkan tema
        applyTheme(themeMode);

        // Biarkan sistem yang menangani perubahan konfigurasi
        AppCompatDelegate.setDefaultNightMode(themeMode);
    }


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // Simpan ID item navigasi aktif
        outState.putInt("currentActiveItemId", currentActiveItemId);
    }

    private void checkIntentForNavigation() {
        if (getIntent() != null && getIntent().hasExtra("fragment")) {
            String fragmentName = getIntent().getStringExtra("fragment");
            if ("CollectionFragment".equals(fragmentName)) {
                // Use switchToFragment safely with isAdded check
                if (collectionFragment != null && !collectionFragment.isAdded()) {
                    switchToFragment(collectionFragment, ANIM_NONE);
                } else {
                    // If already added, just show it
                    showFragment(collectionFragment);
                }
                binding.bottomNavigationView.setSelectedItemId(R.id.colection);
                currentActiveItemId = R.id.colection;
                updateIcons(R.id.colection);
            }
        }
    }

    private void initializeFragments() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();

        // Add fragments only if they're not already added
        if (homeFragment != null && !homeFragment.isAdded()) {
            transaction.add(R.id.frame_layout, homeFragment, "1");
        }
        if (searchFragment != null && !searchFragment.isAdded()) {
            transaction.add(R.id.frame_layout, searchFragment, "2").hide(searchFragment);
        }
        if (uploadFragment != null && !uploadFragment.isAdded()) {
            transaction.add(R.id.frame_layout, uploadFragment, "3").hide(uploadFragment);
        }
        if (collectionFragment != null && !collectionFragment.isAdded()) {
            transaction.add(R.id.frame_layout, collectionFragment, "4").hide(collectionFragment);
        }
        if (bookmarkFragment != null && !bookmarkFragment.isAdded()) {
            transaction.add(R.id.frame_layout, bookmarkFragment, "5").hide(bookmarkFragment);
        }

        transaction.commitNow();
        activeFragment = homeFragment;
    }

    private void setupBottomNavigation() {
        binding.bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            // Clear ProfileShowFragment when navigation happens
            clearProfileShowFragment(true);
            clearSavedAlbumsFragment(true);
            if (id == currentActiveItemId) {
                if (id == R.id.home) {
                    refreshHomeFragment();
                    return true;
                }
                return true;
            }

            currentActiveItemId = id;

            if (id == R.id.home) {
                switchToFragment(homeFragment, ANIM_SLIDE);
            } else if (id == R.id.search) {
                switchToFragment(searchFragment, ANIM_SLIDE);
            } else if (id == R.id.upload) {
                switchToFragment(uploadFragment, ANIM_SLIDE);
            } else if (id == R.id.colection) {
                switchToFragment(collectionFragment, ANIM_SLIDE);
            } else if (id == R.id.profile) {
                switchToFragment(bookmarkFragment, ANIM_SLIDE);
            }

            updateIcons(id);
            return true;
        });
    }

    private void clearProfileShowFragment(boolean useAnimation) {
        // Implementasi yang sama seperti sebelumnya
        FragmentManager fragmentManager = getSupportFragmentManager();
        Fragment profileShowFragment = fragmentManager.findFragmentByTag("ProfileShowFragment");

        if (profileShowFragment != null) {
            FragmentTransaction transaction = fragmentManager.beginTransaction();

            if (useAnimation) {
                transaction.setCustomAnimations(
                        R.anim.slide_in_right,
                        R.anim.slide_in_right
                );
            }

            transaction.remove(profileShowFragment);
            transaction.commit();

            for (int i = 0; i < fragmentManager.getBackStackEntryCount(); i++) {
                String backStackName = fragmentManager.getBackStackEntryAt(i).getName();
                if (backStackName != null && backStackName.startsWith("ProfileShowFragment_")) {
                    fragmentManager.popBackStack(backStackName, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                    break;
                }
            }
        }
    }

    private void clearSearchFragment(boolean useAnimation) {
        // Implementasi yang sama seperti sebelumnya
        FragmentManager fragmentManager = getSupportFragmentManager();
        Fragment searchFragment = fragmentManager.findFragmentByTag("SearchFragment");

        if (searchFragment != null) {
            FragmentTransaction transaction = fragmentManager.beginTransaction();

            if (useAnimation) {
                transaction.setCustomAnimations(
                        R.anim.slide_in_right,
                        R.anim.slide_in_right
                );
            }

            transaction.remove(searchFragment);
            transaction.commit();

            for (int i = 0; i < fragmentManager.getBackStackEntryCount(); i++) {
                String backStackName = fragmentManager.getBackStackEntryAt(i).getName();
                if (backStackName != null && backStackName.startsWith("ProfileShowFragment_")) {
                    fragmentManager.popBackStack(backStackName, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                    break;
                }
            }
        }
    }

    private void clearSavedAlbumsFragment(boolean useAnimation) {
        // Implementasi yang sama seperti sebelumnya
        FragmentManager fragmentManager = getSupportFragmentManager();
        Fragment savedAlbumsFragment = fragmentManager.findFragmentByTag("SavedAlbumsFragment");

        if (savedAlbumsFragment != null) {
            FragmentTransaction transaction = fragmentManager.beginTransaction();

            if (useAnimation) {
                transaction.setCustomAnimations(
                        R.anim.slide_in_right,
                        R.anim.slide_in_right
                );
            }

            transaction.remove(savedAlbumsFragment);
            transaction.commit();

            for (int i = 0; i < fragmentManager.getBackStackEntryCount(); i++) {
                String backStackName = fragmentManager.getBackStackEntryAt(i).getName();
                if (backStackName != null && backStackName.startsWith("SavedAlbumsFragment_")) {
                    fragmentManager.popBackStack(backStackName, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                    break;
                }
            }
        }
    }

    private void refreshHomeFragment() {
        vibrateDevice();

        // Cek apakah homeFragment null
        if (homeFragment == null) {
            homeFragment = new HomeFragment();
        }

        // Cek apakah homeFragment memiliki interface Refreshable
        if (homeFragment instanceof Refreshable) {
            ((Refreshable) homeFragment).onRefresh();
            return;
        }

        // Cek apakah activeFragment null
        if (activeFragment == null) {
            activeFragment = homeFragment;
        }

        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();

        // Buat fragment home baru
        Fragment newHomeFragment = new HomeFragment();

        // Hati-hati dengan transaksi fragment pada recreate
        try {
            // First hide the active fragment
            transaction.hide(activeFragment);

            // Remove old home fragment if it exists and is added
            if (homeFragment != null && homeFragment.isAdded()) {
                transaction.remove(homeFragment);
            }

            // Add new home fragment and show it
            transaction.add(R.id.frame_layout, newHomeFragment, "1")
                    .show(newHomeFragment)
                    .commitNow();

            // Perbarui referensi
            homeFragment = newHomeFragment;
            activeFragment = newHomeFragment;
        } catch (Exception e) {
            // Handle exception, misalnya fragment sudah tidak attached
            // Inisialisasi ulang fragment
            homeFragment = newHomeFragment;
            activeFragment = newHomeFragment;

            FragmentTransaction newTransaction = fragmentManager.beginTransaction();
            newTransaction.add(R.id.frame_layout, newHomeFragment, "1").commitNow();
        }
    }

    // Helper method to just show a fragment that's already added
    private void showFragment(Fragment fragment) {
        if (fragment == null || !fragment.isAdded() || activeFragment == null) {
            return;
        }

        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.hide(activeFragment).show(fragment).commitNow();
        activeFragment = fragment;
    }

    private void switchToFragment(Fragment fragment, int animationType) {
        // Cek apakah fragment null
        if (fragment == null) {
            return;
        }

        if (activeFragment == fragment) {
            return;
        }

        // Cek apakah activeFragment null
        if (activeFragment == null) {
            activeFragment = fragment;
            return;
        }

        // Clear any ProfileShowFragment before switching tabs
        clearProfileShowFragment(true);
        clearSavedAlbumsFragment(true);

        FragmentManager fragmentManager = getSupportFragmentManager();

        // Safety check - make sure we're not in the middle of another transaction
        fragmentManager.executePendingTransactions();

        FragmentTransaction transaction = fragmentManager.beginTransaction();

        if (animationType == ANIM_SLIDE) {
            transaction.setCustomAnimations(
                    R.anim.slide_in_right,
                    R.anim.slide_out_left
            );
        } else if (animationType == ANIM_FADE) {
            transaction.setCustomAnimations(
                    R.anim.slide_in_right,
                    R.anim.slide_in_right
            );
        }

        // Hide current active fragment
        transaction.hide(activeFragment);

        // Check if fragment is already added to avoid "Fragment already added" exception
        if (!fragment.isAdded()) {
            try {
                transaction.add(R.id.frame_layout, fragment, getTagForFragment(fragment));
            } catch (IllegalStateException e) {
                // Fragment might already be added but we missed it
                // Just show it instead
                transaction = fragmentManager.beginTransaction();
                transaction.hide(activeFragment).show(fragment);
            }
        } else {
            // Just show it if it's already added
            transaction.show(fragment);
        }

        // Use commitNow to execute immediately and avoid race conditions
        transaction.commitNow();

        activeFragment = fragment;
    }

    // Helper method untuk mendapatkan tag fragment
    private String getTagForFragment(Fragment fragment) {
        if (fragment == homeFragment) return "1";
        if (fragment == searchFragment) return "2";
        if (fragment == uploadFragment) return "3";
        if (fragment == collectionFragment) return "4";
        if (fragment == bookmarkFragment) return "5";
        return "unknown";
    }

    private void updateIcons(int activeItemId) {
        // Implementasi yang sama seperti sebelumnya
        BottomNavigationView bottomNavigationView = binding.bottomNavigationView;
        ImageView uploadButton = binding.uploadButton;

        bottomNavigationView.getMenu().findItem(R.id.home).setIcon(R.drawable.home_24);
        bottomNavigationView.getMenu().findItem(R.id.search).setIcon(R.drawable.search_24);
        bottomNavigationView.getMenu().findItem(R.id.upload).setIcon(R.drawable.add_24);
        bottomNavigationView.getMenu().findItem(R.id.colection).setIcon(R.drawable.user_24);
        bottomNavigationView.getMenu().findItem(R.id.profile).setIcon(R.drawable.bookmark_24);

        uploadButton.setImageTintList(ColorStateList.valueOf(getResources().getColor(R.color.blacksmooth4)));

        if (activeItemId == R.id.home) {
            bottomNavigationView.getMenu().findItem(R.id.home).setIcon(R.drawable.home_24_filled);
        } else if (activeItemId == R.id.search) {
            bottomNavigationView.getMenu().findItem(R.id.search).setIcon(R.drawable.search_24filled);
        } else if (activeItemId == R.id.upload) {
            bottomNavigationView.getMenu().findItem(R.id.upload).setIcon(R.drawable.add_24_filled);
            uploadButton.setImageTintList(ColorStateList.valueOf(getResources().getColor(R.color.white)));
            vibrateDevice();
        } else if (activeItemId == R.id.colection) {
            bottomNavigationView.getMenu().findItem(R.id.colection).setIcon(R.drawable.user_24_filled);
        } else if (activeItemId == R.id.profile) {
            bottomNavigationView.getMenu().findItem(R.id.profile).setIcon(R.drawable.bookmark_24filled_ic);
        }
    }

    private void vibrateDevice() {
        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(80, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(80);
            }
        }
    }

    @Override
    public void onBackPressed() {
        // Implementasi yang sama seperti sebelumnya
        FragmentManager fragmentManager = getSupportFragmentManager();

        if (fragmentManager.getBackStackEntryCount() > 0) {
            String fragmentTag = fragmentManager.getBackStackEntryAt(
                    fragmentManager.getBackStackEntryCount() - 1).getName();

            if (fragmentTag != null &&
                    (fragmentTag.startsWith("ProfileShowFragment_") || fragmentTag.startsWith("SearchFragment_") || fragmentTag.startsWith("SavedAlbumsFragment_"))) {

                int currentNavItemId = binding.bottomNavigationView.getSelectedItemId();
                String parentFragmentTag = fragmentTag.contains("ProfileShowFragment_") ?
                        fragmentTag.substring("ProfileShowFragment_".length()):
                        fragmentTag.substring("SearchFragment_".length());
                        fragmentTag.substring("SavedAlbumsFragment_".length());

                boolean onSameTab = false;

                if ((currentNavItemId == R.id.home && "1".equals(parentFragmentTag)) ||
                        (currentNavItemId == R.id.search && "2".equals(parentFragmentTag)) ||
                        (currentNavItemId == R.id.upload && "3".equals(parentFragmentTag)) ||
                        (currentNavItemId == R.id.colection && "4".equals(parentFragmentTag)) ||
                        (currentNavItemId == R.id.profile && "5".equals(parentFragmentTag))) {
                    onSameTab = true;
                }

                if (onSameTab) {
                    fragmentManager.popBackStack();
                } else {
                    String fragmentToRemove = fragmentTag.contains("ProfileShowFragment") ?
                            "ProfileShowFragment" : "SavedAlbumsFragment";

                    Fragment fragment = fragmentManager.findFragmentByTag(fragmentToRemove);
                    if (fragment != null) {
                        FragmentTransaction transaction = fragmentManager.beginTransaction();
                        transaction.setCustomAnimations(
                                R.anim.slide_out_left,
                                R.anim.slide_in_right
                        );
                        transaction.remove(fragment);
                        transaction.commit();

                        fragmentManager.popBackStack(fragmentTag, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                    }
                }
                return;
            }
        }
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }

    public interface Refreshable {
        void onRefresh();
    }

    // Perbaikan pada metode setDarkMode
    @Deprecated
    public void setDarkMode(boolean darkMode) {
        int themeMode = darkMode ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO;
        setApplicationTheme(themeMode);
    }
}