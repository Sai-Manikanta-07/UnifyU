package com.example.unifyu2.utils;

import android.content.Context;

import com.example.unifyu2.R;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.database.DatabaseException;

public class FirebaseErrorUtils {
    public static String getErrorMessage(Context context, Exception exception) {
        if (exception instanceof FirebaseAuthInvalidUserException) {
            return context.getString(R.string.error_invalid_user);
        } else if (exception instanceof FirebaseAuthInvalidCredentialsException) {
            return context.getString(R.string.error_invalid_credentials);
        } else if (exception instanceof FirebaseAuthWeakPasswordException) {
            return context.getString(R.string.error_weak_password);
        } else if (exception instanceof FirebaseNetworkException) {
            return context.getString(R.string.error_network);
        } else if (exception instanceof DatabaseException) {
            return context.getString(R.string.error_database);
        }
        return exception.getMessage() != null ? 
            exception.getMessage() : 
            context.getString(R.string.error_unknown);
    }
} 