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

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Button btnLogin, btnGoogleSignIn;
    private TextView tvRegister;
    private ProgressBar progressBar;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseAnalytics analytics;
    private GoogleSignInClient googleSignInClient;
    private boolean googleSignInAvailable = false;

    private final ActivityResultLauncher<Intent> googleSignInLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                try {
                    GoogleSignInAccount account = task.getResult(ApiException.class);
                    firebaseAuthWithGoogle(account.getIdToken());
                } catch (ApiException e) {
                    Toast.makeText(this, "Google sign in thất bại: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    setLoading(false);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAuth = FirebaseAuth.getInstance();

        // Nếu đã đăng nhập thì chuyển thẳng vào app
        if (mAuth.getCurrentUser() != null) {
            goToMain();
            return;
        }

        setContentView(R.layout.activity_login);

        db = FirebaseFirestore.getInstance();
        analytics = FirebaseAnalytics.getInstance(this);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnGoogleSignIn = findViewById(R.id.btnGoogleSignIn);
        tvRegister = findViewById(R.id.tvRegister);
        progressBar = findViewById(R.id.progressBar);

        // Cấu hình Google Sign In
        // default_web_client_id được tự động tạo bởi plugin google-services
        // khi google-services.json có oauth_client đã cấu hình
        try {
            int resId = getResources().getIdentifier("default_web_client_id", "string", getPackageName());
            if (resId != 0) {
                String webClientId = getString(resId);
                GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestIdToken(webClientId)
                        .requestEmail()
                        .build();
                googleSignInClient = GoogleSignIn.getClient(this, gso);
                googleSignInAvailable = true;
            } else {
                // OAuth chưa được cấu hình trong Firebase Console
                btnGoogleSignIn.setEnabled(false);
                btnGoogleSignIn.setAlpha(0.5f);
                btnGoogleSignIn.setText("Google Sign In (chưa cấu hình)");
            }
        } catch (Exception e) {
            btnGoogleSignIn.setEnabled(false);
            btnGoogleSignIn.setAlpha(0.5f);
        }

        btnLogin.setOnClickListener(v -> loginWithEmail());
        btnGoogleSignIn.setOnClickListener(v -> {
            if (googleSignInAvailable) {
                loginWithGoogle();
            } else {
                Toast.makeText(this,
                        "Vui lòng bật Google Sign In trong Firebase Console và cập nhật google-services.json",
                        Toast.LENGTH_LONG).show();
            }
        });
        tvRegister.setOnClickListener(v -> startActivity(new Intent(this, RegisterActivity.class)));
    }

    private void loginWithEmail() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, getString(R.string.error_empty_fields), Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    // Log analytics
                    Bundle bundle = new Bundle();
                    bundle.putString(FirebaseAnalytics.Param.METHOD, "email");
                    analytics.logEvent(FirebaseAnalytics.Event.LOGIN, bundle);

                    saveFcmToken(authResult.getUser().getUid());
                    goToMain();
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this, getString(R.string.error_login_failed) + ": " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    private void loginWithGoogle() {
        setLoading(true);
        Intent signInIntent = googleSignInClient.getSignInIntent();
        googleSignInLauncher.launch(signInIntent);
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnSuccessListener(authResult -> {
                    // Tạo/cập nhật user document trong Firestore
                    String uid = authResult.getUser().getUid();
                    Map<String, Object> userData = new HashMap<>();
                    userData.put("name", authResult.getUser().getDisplayName());
                    userData.put("email", authResult.getUser().getEmail());
                    db.collection("users").document(uid).set(userData);

                    // Log analytics
                    Bundle bundle = new Bundle();
                    bundle.putString(FirebaseAnalytics.Param.METHOD, "google");
                    analytics.logEvent(FirebaseAnalytics.Event.LOGIN, bundle);

                    saveFcmToken(uid);
                    goToMain();
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this, "Xác thực Google thất bại: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    private void saveFcmToken(String userId) {
        FirebaseMessaging.getInstance().getToken()
                .addOnSuccessListener(token -> {
                    Map<String, Object> data = new HashMap<>();
                    data.put("fcmToken", token);
                    db.collection("users").document(userId).update(data);
                });
    }

    private void goToMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnLogin.setEnabled(!loading);
        if (googleSignInAvailable) {
            btnGoogleSignIn.setEnabled(!loading);
        }
    }
}
