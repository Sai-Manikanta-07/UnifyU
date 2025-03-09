package com.example.unifyu2;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.example.unifyu2.utils.FirebaseErrorUtils;

public class RegisterActivity extends AppCompatActivity {
    private TextInputEditText usernameEditText, emailEditText, passwordEditText, confirmPasswordEditText;
    private FirebaseAuth firebaseAuth;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Initialize Firebase Auth
        firebaseAuth = FirebaseAuth.getInstance();

        // Initialize views
        usernameEditText = findViewById(R.id.usernameEditText);
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        confirmPasswordEditText = findViewById(R.id.confirmPasswordEditText);
        MaterialButton registerButton = findViewById(R.id.registerButton);

        // Setup progress dialog
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage(getString(R.string.please_wait));
        progressDialog.setCancelable(false);

        // Setup click listeners
        registerButton.setOnClickListener(v -> attemptRegistration());
        findViewById(R.id.loginText).setOnClickListener(v -> {
            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
            finish();
        });

        setupPasswordValidation();
    }

    private void attemptRegistration() {
        String username = usernameEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString();
        String confirmPassword = confirmPasswordEditText.getText().toString();

        // Validate input
        if (TextUtils.isEmpty(username)) {
            usernameEditText.setError("Username is required");
            return;
        }

        if (TextUtils.isEmpty(email)) {
            emailEditText.setError("Email is required");
            return;
        }

        if (TextUtils.isEmpty(password)) {
            passwordEditText.setError("Password is required");
            return;
        }

        if (password.length() < 6) {
            passwordEditText.setError(getString(R.string.password_too_short));
            return;
        }

        if (!password.equals(confirmPassword)) {
            confirmPasswordEditText.setError(getString(R.string.passwords_not_matching));
            return;
        }

        progressDialog.show();

        // Create user with Firebase
        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Update profile with username
                        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                .setDisplayName(username)
                                .build();

                        firebaseAuth.getCurrentUser().updateProfile(profileUpdates)
                                .addOnCompleteListener(profileTask -> {
                                    progressDialog.dismiss();
                                    if (profileTask.isSuccessful()) {
                                        Toast.makeText(RegisterActivity.this,
                                                getString(R.string.registration_success),
                                                Toast.LENGTH_SHORT).show();
                                        startActivity(new Intent(RegisterActivity.this, UserDetailsActivity.class));
                                        finish();
                                    }
                                });
                    } else {
                        progressDialog.dismiss();
                        String errorMessage = FirebaseErrorUtils.getErrorMessage(
                            this, 
                            task.getException()
                        );
                        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void setupPasswordValidation() {
        TextInputLayout confirmPasswordLayout = findViewById(R.id.confirmPasswordLayout);
        TextInputEditText passwordEdit = findViewById(R.id.passwordEditText);
        TextInputEditText confirmPasswordEdit = findViewById(R.id.confirmPasswordEditText);

        confirmPasswordEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String password = passwordEdit.getText().toString();
                String confirmPassword = s.toString();
                
                if (!confirmPassword.isEmpty() && !confirmPassword.equals(password)) {
                    confirmPasswordLayout.setEndIconMode(TextInputLayout.END_ICON_CLEAR_TEXT);
                    confirmPasswordLayout.setError(getString(R.string.passwords_not_matching));
                } else {
                    confirmPasswordLayout.setError(null);
                    confirmPasswordLayout.setEndIconMode(TextInputLayout.END_ICON_PASSWORD_TOGGLE);
                }
            }
        });
    }
} 