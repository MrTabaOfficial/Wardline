package com.example.myapplication.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.databinding.ActivityVerifyCodeBinding;
import com.example.myapplication.utilities.Constants;
import com.example.myapplication.utilities.PreferenceManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class VerifyCodeActivity extends AppCompatActivity {

    private ActivityVerifyCodeBinding binding;
    private PreferenceManager preferenceManager;
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore database;
    private String email;
    private String correctCode;
    private static final String TAG = "VerifyCodeActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityVerifyCodeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        preferenceManager = new PreferenceManager(getApplicationContext());
        firebaseAuth = FirebaseAuth.getInstance();
        database = FirebaseFirestore.getInstance();

        email = getIntent().getStringExtra("email");
        correctCode = getIntent().getStringExtra("code");

        setListeners();
    }

    private void setListeners() {
        binding.buttonVerify.setOnClickListener(v -> {
            if (isValidCode()) {
                verifyCode();
            }
        });
    }

    private void verifyCode() {
        String enteredCode = binding.inputCode.getText().toString().trim();
        if (enteredCode.equals(correctCode)) {
            // Code is correct, log in the user
            FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
            if (firebaseUser != null) {
                database.collection(Constants.KEY_COLLECTION_USERS)
                        .document(firebaseUser.getUid())
                        .get()
                        .addOnSuccessListener(documentSnapshot -> {
                            preferenceManager.putBoolean(Constants.KEY_IS_SIGNED_IN, true);
                            preferenceManager.putString(Constants.KEY_USER_ID, documentSnapshot.getId());
                            preferenceManager.putString(Constants.KEY_NAME, documentSnapshot.getString(Constants.KEY_NAME));
                            preferenceManager.putString(Constants.KEY_IMAGE, documentSnapshot.getString(Constants.KEY_IMAGE));
                            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Failed to get user data", e);
                            showToast("Unable to log in");
                        });
            } else {
                Log.e(TAG, "firebaseUser is null");
                showToast("Unable to log in");
            }
        } else {
            showToast("Incorrect verification code");
        }
    }

    private boolean isValidCode() {
        if (binding.inputCode.getText().toString().trim().isEmpty()) {
            showToast("Enter verification code");
            return false;
        } else {
            return true;
        }
    }

    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }
}
