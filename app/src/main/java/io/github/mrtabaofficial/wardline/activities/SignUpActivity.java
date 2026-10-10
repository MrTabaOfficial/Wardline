package io.github.mrtabaofficial.wardline.activities;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import io.github.mrtabaofficial.wardline.R;
import io.github.mrtabaofficial.wardline.databinding.ActivitySignUpBinding;
import io.github.mrtabaofficial.wardline.utilities.Constants;
import io.github.mrtabaofficial.wardline.utilities.ImageUtils;
import io.github.mrtabaofficial.wardline.utilities.SystemBars;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.IOException;
import java.util.HashMap;

public class SignUpActivity extends AppCompatActivity {

    private static final String TAG = "SignUpActivity";

    private ActivitySignUpBinding binding;
    private String encodedImage;

    private final ActivityResultLauncher<PickVisualMediaRequest> pickImage = registerForActivityResult(
            new ActivityResultContracts.PickVisualMedia(), this::onImagePicked);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySignUpBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        SystemBars.applyEdgeToEdge(this, binding.getRoot());
        setListeners();
    }

    private void setListeners() {
        binding.textSignIn.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        binding.buttonSignUp.setOnClickListener(v -> {
            if (isValidSignUpDetails()) {
                signUp();
            }
        });
        binding.layoutImage.setOnClickListener(v -> pickImage.launch(new PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                .build()));
    }

    private void onImagePicked(Uri imageUri) {
        if (imageUri == null) {
            return;
        }
        try {
            Bitmap bitmap = ImageUtils.load(getContentResolver(), imageUri);
            binding.imageProfile.setImageBitmap(bitmap);
            binding.textAddImage.setVisibility(View.GONE);
            encodedImage = ImageUtils.encode(bitmap);
        } catch (IOException e) {
            Log.w(TAG, "Unable to load the selected image", e);
            showToast(getString(R.string.error_load_image));
        }
    }

    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    private void signUp() {
        loading(true);
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        String email = binding.inputEmail.getText().toString().trim();
        String password = binding.inputPassword.getText().toString();

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    FirebaseUser firebaseUser = mAuth.getCurrentUser();
                    if (task.isSuccessful() && firebaseUser != null) {
                        storeUserInFirestore(firebaseUser);
                    } else {
                        loading(false);
                        showToast(getString(R.string.error_sign_up));
                    }
                });
    }

    private void storeUserInFirestore(FirebaseUser firebaseUser) {
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        HashMap<String, Object> user = new HashMap<>();
        user.put(Constants.KEY_NAME, binding.inputName.getText().toString().trim());
        user.put(Constants.KEY_EMAIL, firebaseUser.getEmail());
        user.put(Constants.KEY_IMAGE, encodedImage);

        database.collection(Constants.KEY_COLLECTION_USERS)
                .document(firebaseUser.getUid())
                .set(user)
                .addOnSuccessListener(aVoid -> sendVerificationEmail(firebaseUser))
                .addOnFailureListener(exception -> {
                    loading(false);
                    showToast(getString(R.string.error_sign_up));
                });
    }

    private void sendVerificationEmail(FirebaseUser firebaseUser) {
        firebaseUser.sendEmailVerification().addOnCompleteListener(task -> {
            String email = firebaseUser.getEmail();
            FirebaseAuth.getInstance().signOut();
            loading(false);
            showVerificationDialog(email, task.isSuccessful());
        });
    }

    private void showVerificationDialog(String email, boolean emailSent) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.email_verification_title)
                .setMessage(emailSent
                        ? getString(R.string.email_verification_message, email)
                        : getString(R.string.error_verification_email))
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    dialog.dismiss();
                    finish();
                })
                .setCancelable(false)
                .show();
    }

    private Boolean isValidSignUpDetails() {
        String email = binding.inputEmail.getText().toString().trim();
        String password = binding.inputPassword.getText().toString();
        if (encodedImage == null) {
            showToast(getString(R.string.error_select_image));
            return false;
        } else if (binding.inputName.getText().toString().trim().isEmpty()) {
            showToast(getString(R.string.error_enter_name));
            return false;
        } else if (email.isEmpty()) {
            showToast(getString(R.string.error_enter_email));
            return false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showToast(getString(R.string.error_invalid_email));
            return false;
        } else if (password.length() < Constants.MIN_PASSWORD_LENGTH) {
            showToast(getResources().getQuantityString(R.plurals.error_password_too_short, Constants.MIN_PASSWORD_LENGTH, Constants.MIN_PASSWORD_LENGTH));
            return false;
        } else if (!password.equals(binding.inputConfirmPassword.getText().toString())) {
            showToast(getString(R.string.error_password_mismatch));
            return false;
        } else {
            return true;
        }
    }

    private void loading(boolean isLoading) {
        if (isLoading) {
            binding.buttonSignUp.setVisibility(View.INVISIBLE);
            binding.progressBar.setVisibility(View.VISIBLE);
        } else {
            binding.progressBar.setVisibility(View.INVISIBLE);
            binding.buttonSignUp.setVisibility(View.VISIBLE);
        }
    }
}
