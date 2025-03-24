package com.example.unifyu2;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;

public class ResetPasswordActivity extends AppCompatActivity {
    private TextInputEditText emailEditText;
    private FirebaseAuth firebaseAuth;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);
        
        // Initialize Firebase Auth
        firebaseAuth = FirebaseAuth.getInstance();
        
        // Initialize views
        emailEditText = findViewById(R.id.emailEditText);
        MaterialButton resetButton = findViewById(R.id.resetButton);
        
        // Setup progress dialog
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Sending reset email...");
        progressDialog.setCancelable(false);

        // Setup click listeners
        resetButton.setOnClickListener(v -> attemptPasswordReset());
        findViewById(R.id.backToLoginText).setOnClickListener(v -> {
            startActivity(new Intent(ResetPasswordActivity.this, LoginActivity.class));
            finish();
        });
    }

    private void attemptPasswordReset() {
        String email = emailEditText.getText().toString().trim();

        // Validate input
        if (TextUtils.isEmpty(email)) {
            emailEditText.setError("Email is required");
            return;
        }

        progressDialog.show();

        // Send password reset email
        firebaseAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    progressDialog.dismiss();
                    if (task.isSuccessful()) {
                        Toast.makeText(ResetPasswordActivity.this, 
                                "Password reset email sent. Check your inbox.", 
                                Toast.LENGTH_LONG).show();
                        
                        // Return to login screen
                        startActivity(new Intent(ResetPasswordActivity.this, LoginActivity.class));
                        finish();
                    } else {
                        String errorMessage = task.getException() != null ? 
                                task.getException().getMessage() : 
                                "Failed to send reset email";
                        Toast.makeText(ResetPasswordActivity.this, 
                                "Error: " + errorMessage, 
                                Toast.LENGTH_LONG).show();
                    }
                });
    }
} 