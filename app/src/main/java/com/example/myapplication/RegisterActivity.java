package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private EditText etName, etEmail, etPassword;
    private Button btnRegister;
    private TextView tvLogin;
    private ProgressBar progressBar;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseAnalytics analytics;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        analytics = FirebaseAnalytics.getInstance(this);

        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnRegister = findViewById(R.id.btnRegister);
        tvLogin = findViewById(R.id.tvLogin);
        progressBar = findViewById(R.id.progressBar);

        btnRegister.setOnClickListener(v -> register());
        tvLogin.setOnClickListener(v -> finish());
    }

    private void register() {
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, getString(R.string.error_empty_fields), Toast.LENGTH_SHORT).show();
            return;
        }
        if (password.length() < 6) {
            etPassword.setError("Mật khẩu phải ít nhất 6 ký tự");
            return;
        }

        setLoading(true);
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    String uid = authResult.getUser().getUid();

                    // Lưu thông tin user vào Firestore
                    Map<String, Object> userData = new HashMap<>();
                    userData.put("name", name);
                    userData.put("email", email);
                    userData.put("createdAt", System.currentTimeMillis());

                    db.collection("users").document(uid).set(userData)
                            .addOnCompleteListener(task -> {
                                // Lưu FCM token
                                FirebaseMessaging.getInstance().getToken()
                                        .addOnSuccessListener(token -> {
                                            db.collection("users").document(uid)
                                                    .update("fcmToken", token);
                                        });

                                // Log analytics event đăng ký
                                Bundle bundle = new Bundle();
                                bundle.putString(FirebaseAnalytics.Param.METHOD, "email");
                                analytics.logEvent(FirebaseAnalytics.Event.SIGN_UP, bundle);

                                Toast.makeText(this, "Đăng ký thành công!", Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(this, MainActivity.class));
                                finishAffinity();
                            });
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this, getString(R.string.error_register_failed) + ": " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnRegister.setEnabled(!loading);
    }
}
