package com.example.unifyu2;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class UserDetailsActivity extends AppCompatActivity {
    private TextInputEditText rollNumberEditText;
    private AutoCompleteTextView semesterSpinner;
    private ProgressDialog progressDialog;
    private FirebaseAuth firebaseAuth;
    private DatabaseReference databaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_details);

        // Initialize Firebase
        firebaseAuth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference();

        // Initialize views
        rollNumberEditText = findViewById(R.id.rollNumberEditText);
        semesterSpinner = findViewById(R.id.semesterSpinner);
        MaterialButton submitButton = findViewById(R.id.submitButton);

        // Setup progress dialog
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Saving details...");
        progressDialog.setCancelable(false);

        // Setup semester spinner
        String[] semesters = new String[]{"1st Semester", "2nd Semester", "3rd Semester", 
                                        "4th Semester", "5th Semester", "6th Semester", 
                                        "7th Semester", "8th Semester"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                semesters
        );
        semesterSpinner.setAdapter(adapter);

        // Setup click listener
        submitButton.setOnClickListener(v -> saveUserDetails());
    }

    private void saveUserDetails() {
        String rollNumber = rollNumberEditText.getText().toString().trim().toUpperCase();
        String semester = semesterSpinner.getText().toString().trim();

        // Validate input
        if (TextUtils.isEmpty(rollNumber)) {
            rollNumberEditText.setError("Roll number is required");
            return;
        }

        if (rollNumber.length() != 10) {
            rollNumberEditText.setError("Roll number must be 10 characters");
            return;
        }

        if (TextUtils.isEmpty(semester)) {
            Toast.makeText(this, "Please select your semester", Toast.LENGTH_SHORT).show();
            return;
        }

        progressDialog.show();

        // Get current user ID
        String userId = firebaseAuth.getCurrentUser().getUid();

        // Create user details map
        Map<String, Object> userDetails = new HashMap<>();
        userDetails.put("rollNumber", rollNumber);
        userDetails.put("semester", semester);
        userDetails.put("email", firebaseAuth.getCurrentUser().getEmail());
        userDetails.put("displayName", firebaseAuth.getCurrentUser().getDisplayName());

        // Save to database
        databaseReference.child("users").child(userId).setValue(userDetails)
                .addOnCompleteListener(task -> {
                    progressDialog.dismiss();
                    if (task.isSuccessful()) {
                        // Navigate to MainActivity
                        startActivity(new Intent(UserDetailsActivity.this, MainActivity.class));
                        finish();
                    } else {
                        Toast.makeText(UserDetailsActivity.this,
                                "Failed to save details. Please try again.",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }
} 