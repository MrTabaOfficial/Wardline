package io.github.mrtabaofficial.wardline.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;

import io.github.mrtabaofficial.wardline.R;
import io.github.mrtabaofficial.wardline.databinding.ActivitySignInBinding;
import io.github.mrtabaofficial.wardline.utilities.Constants;
import io.github.mrtabaofficial.wardline.utilities.PreferenceManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class SignInActivity extends AppCompatActivity {

    private static final String TAG = "SignInActivity";
    private static final String LEGACY_LANGUAGE_KEY = "My_Lang";

    private ActivitySignInBinding binding;
    private PreferenceManager preferenceManager;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        migrateLegacyLanguage();

        preferenceManager = new PreferenceManager(getApplicationContext());
        auth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null && currentUser.isEmailVerified()
                && preferenceManager.getBoolean(Constants.KEY_IS_SIGNED_IN)) {
            startActivity(new Intent(getApplicationContext(), MainActivity.class));
            finish();
            return;
        }
        binding = ActivitySignInBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setListeners();
    }

    private void migrateLegacyLanguage() {
        SharedPreferences settings = getSharedPreferences(Constants.KEY_SETTINGS_PREFERENCE, MODE_PRIVATE);
        String legacyLanguage = settings.getString(LEGACY_LANGUAGE_KEY, null);
        if (legacyLanguage == null) {
            return;
        }
        settings.edit().remove(LEGACY_LANGUAGE_KEY).apply();
        if (AppCompatDelegate.getApplicationLocales().isEmpty()) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(legacyLanguage));
        }
    }

    private void setListeners() {
        binding.textCreateNewAccount.setOnClickListener(v ->
                startActivity(new Intent(getApplicationContext(), SignUpActivity.class)));
        binding.btnSettings.setOnClickListener(v -> startActivity(new Intent(getApplicationContext(), SettingsActivity.class)));
        binding.buttonSignIn.setOnClickListener(v -> {
            if (isValidSignInDetails()) {
                signIn();
            }
        });
        binding.textForgotPassword.setOnClickListener(v -> showForgotPasswordDialog());
    }

    private void signIn() {
        loading(true);
        String email = binding.inputEmail.getText().toString().trim();
        String password = binding.inputPassword.getText().toString();

        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    FirebaseUser user = auth.getCurrentUser();
                    if (!task.isSuccessful() || user == null) {
                        loading(false);
                        Exception exception = task.getException();
                        if (exception != null) {
                            Log.w(TAG, "signInWithEmailAndPassword failed", exception);
                        }
                        showToast(getString(R.string.error_invalid_credentials));
                        return;
                    }
                    if (!user.isEmailVerified()) {
                        loading(false);
                        showEmailNotVerifiedDialog(user);
                        return;
                    }
                    fetchUserDetails(user);
                });
    }

    private void fetchUserDetails(FirebaseUser user) {
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        database.collection(Constants.KEY_COLLECTION_USERS)
                .document(user.getUid())
                .get()
                .addOnCompleteListener(task -> {
                    loading(false);
                    DocumentSnapshot documentSnapshot = task.getResult();
                    if (!task.isSuccessful() || documentSnapshot == null || !documentSnapshot.exists()) {
                        auth.signOut();
                        showToast(getString(R.string.error_fetch_user));
                        return;
                    }
                    if (user.getEmail() != null && !user.getEmail().equals(documentSnapshot.getString(Constants.KEY_EMAIL))) {
                        documentSnapshot.getReference().update(Constants.KEY_EMAIL, user.getEmail());
                    }
                    preferenceManager.putBoolean(Constants.KEY_IS_SIGNED_IN, true);
                    preferenceManager.putString(Constants.KEY_USER_ID, documentSnapshot.getId());
                    preferenceManager.putString(Constants.KEY_NAME, documentSnapshot.getString(Constants.KEY_NAME));
                    preferenceManager.putString(Constants.KEY_IMAGE, documentSnapshot.getString(Constants.KEY_IMAGE));
                    Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                });
    }

    private void showEmailNotVerifiedDialog(FirebaseUser user) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.email_not_verified_title)
                .setMessage(getString(R.string.email_not_verified_message, user.getEmail()))
                .setPositiveButton(R.string.resend_email, (dialog, which) ->
                        user.sendEmailVerification().addOnCompleteListener(task -> {
                            auth.signOut();
                            showToast(getString(task.isSuccessful()
                                    ? R.string.verification_email_sent
                                    : R.string.error_verification_email));
                        }))
                .setNegativeButton(android.R.string.cancel, (dialog, which) -> auth.signOut())
                .setCancelable(false)
                .show();
    }

    private void showForgotPasswordDialog() {
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_forgot_password, null);
        EditText emailInput = dialogView.findViewById(R.id.inputEmail);

        new AlertDialog.Builder(this)
                .setTitle(R.string.forgot_password)
                .setView(dialogView)
                .setPositiveButton(R.string.send, (dialog, which) -> {
                    String email = emailInput.getText().toString().trim();
                    if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                        showToast(getString(R.string.error_invalid_email));
                        return;
                    }
                    sendPasswordResetEmail(email);
                })
                .setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void sendPasswordResetEmail(String email) {
        auth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> showToast(getString(R.string.password_reset_sent)));
    }

    private void loading(boolean isLoading) {
        if (isLoading) {
            binding.buttonSignIn.setVisibility(View.INVISIBLE);
            binding.progressBar.setVisibility(View.VISIBLE);
        } else {
            binding.progressBar.setVisibility(View.INVISIBLE);
            binding.buttonSignIn.setVisibility(View.VISIBLE);
        }
    }

    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    private Boolean isValidSignInDetails() {
        String email = binding.inputEmail.getText().toString().trim();
        if (email.isEmpty()) {
            showToast(getString(R.string.error_enter_email));
            return false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showToast(getString(R.string.error_invalid_email));
            return false;
        } else if (binding.inputPassword.getText().toString().isEmpty()) {
            showToast(getString(R.string.error_enter_password));
            return false;
        } else {
            return true;
        }
    }
}
