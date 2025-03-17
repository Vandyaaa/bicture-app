package com.example.imaginate;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class Register extends AppCompatActivity {

    private EditText etUsername, etEmail, etPassword;
    private TextView btnRegister, btnLogin;
    private TextView countUsernameText;
    private static final int MAX_USERNAME_LENGTH = 20;
    private DatabaseReference database;
    private boolean spaceWarningShown = false;
    private Dialog loadingDialog;


    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        etUsername = findViewById(R.id.etUsername);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnRegister = findViewById(R.id.btnRegister);
        btnLogin = findViewById(R.id.btnLogin);
        countUsernameText = findViewById(R.id.countUsernameText);

        countUsernameText.setText("0/" + MAX_USERNAME_LENGTH);

        // Inisialisasi loading dialog
        loadingDialog = new Dialog(Register.this);
        loadingDialog.setContentView(R.layout.dialog_loading_small);
        loadingDialog.setCancelable(false);
        loadingDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        if (isDarkTheme()) {
            setStatusBarForDarkTheme();
        } else {
            setStatusBarForLightTheme();
        }

        findViewById(R.id.seePassword).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Get the current cursor position
                int cursorPosition = etPassword.getSelectionStart();

                // Toggle password visibility
                if (etPassword.getInputType() == (android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD)) {
                    // Show password
                    etPassword.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                    ((android.widget.ImageView) view).setImageResource(R.drawable.crossed_eye_24); // Change to crossed eye icon
                } else {
                    // Hide password
                    etPassword.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
                    ((android.widget.ImageView) view).setImageResource(R.drawable.eye_24); // Change back to normal eye icon
                }

                // Restore font
                etPassword.setTypeface(android.graphics.Typeface.create(getResources().getFont(R.font.gentium), android.graphics.Typeface.NORMAL));

                // Restore cursor position
                etPassword.setSelection(cursorPosition);
            }
        });

        etUsername.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Reset the warning flag when user starts typing
                spaceWarningShown = false;
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String currentText = s.toString();

                // Check if the new text contains spaces
                if (currentText.contains(" ") && !spaceWarningShown) {
                    Toast.makeText(getApplicationContext(), "Username tidak boleh menggunakan spasi!", Toast.LENGTH_SHORT).show();
                    spaceWarningShown = true;

                    // Remove spaces and update the EditText
                    String textWithoutSpaces = currentText.replace(" ", "");
                    etUsername.setText(textWithoutSpaces);
                    etUsername.setSelection(textWithoutSpaces.length());
                    return;
                }

                // Update character count
                String currentCount = s.length() + "/" + MAX_USERNAME_LENGTH;
                countUsernameText.setText(currentCount);

                // If text length exceeds maximum, truncate it
                if (s.length() > MAX_USERNAME_LENGTH) {
                    etUsername.setText(s.subSequence(0, MAX_USERNAME_LENGTH));
                    etUsername.setSelection(MAX_USERNAME_LENGTH);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Not needed
            }
        });

        database = FirebaseDatabase.getInstance().getReferenceFromUrl("https://imageinate-a3d4b-default-rtdb.firebaseio.com/");

        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String username = etUsername.getText().toString();
                String email = etEmail.getText().toString();
                String password = etPassword.getText().toString();

                if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
                    Toast.makeText(getApplicationContext(), "Ada Data Yang Masih Kosong!!", Toast.LENGTH_SHORT).show();
                } else if (!isValidEmail(email)) {
                    Toast.makeText(getApplicationContext(), "Format Email Tidak Valid!", Toast.LENGTH_SHORT).show();
                } else {
                    loadingDialog.show();
                    // Cek username terhadap tagName yang sudah ada terlebih dahulu
                    checkIfUsernameExistsAsTagName(username, email, password);
                }
            }
        });

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Register.this, Login.class);
                startActivity(intent);
                // Animasi slide saat kembali ke halaman login
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            }
        });
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

    private void checkIfEmailExists(String email, String username, String password) {
        DatabaseReference emailRef = FirebaseDatabase.getInstance().getReference("users");

        emailRef.orderByChild("email").equalTo(email).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    // Jika email sudah terdaftar
                    loadingDialog.dismiss(); // Tutup dialog loading
                    Toast.makeText(getApplicationContext(), "Email sudah terdaftar!", Toast.LENGTH_SHORT).show();
                } else {
                    // Jika email belum terdaftar, daftarkan pengguna baru
                    registerUser(username, email, password);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                loadingDialog.dismiss(); // Tutup dialog loading jika terjadi error
                Toast.makeText(getApplicationContext(), "Error: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void registerUser(String username, String email, String password) {
        // Generate userId dalam format int(11)
        long timestamp = System.currentTimeMillis();
        String userId = String.valueOf(timestamp).substring(0, 11); // Ambil 11 digit pertama

        database = FirebaseDatabase.getInstance().getReference("users");

        // Simpan data pengguna
        database.child(userId).child("username").setValue(username);
        database.child(userId).child("email").setValue(email);
        database.child(userId).child("password").setValue(password);
        database.child(userId).child("photoprofile").setValue("");

        // Tambahkan tagName dengan nilai yang sama seperti username
        String tagName = "@" + username;
        database.child(userId).child("tagName").setValue(tagName);


        // Tampilkan loading selama beberapa saat (misalnya 2 detik) sebelum pindah ke halaman login
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                // Tutup dialog loading
                loadingDialog.dismiss();

                // Beri notifikasi pengguna dan arahkan ke halaman login
                Toast.makeText(getApplicationContext(), "Register Berhasil", Toast.LENGTH_SHORT).show();
                Intent register = new Intent(getApplicationContext(), Login.class);
                startActivity(register);
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                finish(); // Tutup activity register
            }
        }, 2000); // Delay 2 detik
    }

    // Fungsi untuk memvalidasi format email
    private boolean isValidEmail(String email) {
        String emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+";
        return email.matches(emailPattern);
    }

    private void checkIfUsernameExistsAsTagName(String username, String email, String password) {
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");

        // Buat tagName dari username untuk pengecekan (tambahkan @ di depan)
        String tagNameToCheck = "@" + username;

        usersRef.orderByChild("tagName").equalTo(tagNameToCheck).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    // Username sudah digunakan sebagai tagName oleh pengguna lain
                    loadingDialog.dismiss();
                    Toast.makeText(getApplicationContext(),
                            "Username tidak dapat digunakan karena sudah terdaftar sebagai tag name pengguna lain!",
                            Toast.LENGTH_LONG).show();
                } else {
                    // Username aman digunakan, lanjutkan ke pengecekan email
                    checkIfEmailExists(email, username, password);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                loadingDialog.dismiss();
                Toast.makeText(getApplicationContext(),
                        "Error: " + databaseError.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Pastikan dialog ditutup saat activity dihancurkan
        if (loadingDialog != null && loadingDialog.isShowing()) {
            loadingDialog.dismiss();
        }
    }
}