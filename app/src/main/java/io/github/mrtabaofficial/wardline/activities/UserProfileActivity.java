package io.github.mrtabaofficial.wardline.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;

import io.github.mrtabaofficial.wardline.R;
import io.github.mrtabaofficial.wardline.databinding.ActivityUserProfileBinding;
import io.github.mrtabaofficial.wardline.firebase.PushTokens;
import io.github.mrtabaofficial.wardline.utilities.Constants;
import io.github.mrtabaofficial.wardline.utilities.ImageUtils;
import io.github.mrtabaofficial.wardline.utilities.PreferenceManager;
import io.github.mrtabaofficial.wardline.utilities.SystemBars;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;

import java.io.IOException;

public class UserProfileActivity extends BaseActivity {

    private static final String TAG = "UserProfileActivity";

    private ActivityUserProfileBinding binding;
    private PreferenceManager preferenceManager;
    private FirebaseUser user;
    private DocumentReference userDocument;

    private final ActivityResultLauncher<PickVisualMediaRequest> pickImage = registerForActivityResult(
            new ActivityResultContracts.PickVisualMedia(), this::onImagePicked);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (isFinishing()) {
            return;
        }
        binding = ActivityUserProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        SystemBars.applyEdgeToEdge(this, binding.getRoot());

        preferenceManager = new PreferenceManager(getApplicationContext());
        user = FirebaseAuth.getInstance().getCurrentUser();
        userDocument = FirebaseFirestore.getInstance()
                .collection(Constants.KEY_COLLECTION_USERS)
                .document(user.getUid());

        loadUserDetails();

        binding.imageBack.setOnClickListener(v -> finish());
        binding.btnChangePassword.setOnClickListener(v -> showChangePasswordDialog());
        binding.btnLogout.setOnClickListener(v -> signOut());
        binding.btnChangeProfilePicture.setOnClickListener(v -> pickImage.launch(new PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                .build()));
        binding.btnChangeEmail.setOnClickListener(v -> showChangeEmailDialog());
    }

    private void loadUserDetails() {
        binding.textName.setText(preferenceManager.getString(Constants.KEY_NAME));
        binding.textEmail.setText(user.getEmail());
        binding.imageProfile.setImageBitmap(ImageUtils.decode(preferenceManager.getString(Constants.KEY_IMAGE)));
    }

    private void showChangePasswordDialog() {
        View view = getLayoutInflater().inflate(R.layout.dialog_change_password, null);
        EditText editOldPassword = view.findViewById(R.id.editOldPassword);
        EditText editNewPassword = view.findViewById(R.id.editNewPassword);
        EditText editConfirmPassword = view.findViewById(R.id.editConfirmPassword);

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.change_password)
                .setView(view)
                .setPositiveButton(R.string.change_password, (dialog, which) -> {
                    String oldPassword = editOldPassword.getText().toString();
                    String newPassword = editNewPassword.getText().toString();
                    if (newPassword.length() < Constants.MIN_PASSWORD_LENGTH) {
                        showToast(getResources().getQuantityString(R.plurals.error_password_too_short, Constants.MIN_PASSWORD_LENGTH, Constants.MIN_PASSWORD_LENGTH));
                    } else if (!newPassword.equals(editConfirmPassword.getText().toString())) {
                        showToast(getString(R.string.error_password_mismatch));
                    } else {
                        changePassword(oldPassword, newPassword);
                    }
                })
                .setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.dismiss())
                .show();
    }

    private Task<Void> reauthenticate(String password) {
        AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), password);
        return user.reauthenticate(credential);
    }

    private void changePassword(String oldPassword, String newPassword) {
        reauthenticate(oldPassword)
                .onSuccessTask(unused -> user.updatePassword(newPassword))
                .addOnCompleteListener(task -> showToast(getString(task.isSuccessful()
                        ? R.string.password_changed
                        : R.string.error_change_password)));
    }

    private void onImagePicked(Uri imageUri) {
        if (imageUri == null) {
            return;
        }
        try {
            Bitmap bitmap = ImageUtils.load(getContentResolver(), imageUri);
            uploadProfilePicture(bitmap);
        } catch (IOException e) {
            Log.w(TAG, "Unable to load the selected image", e);
            showToast(getString(R.string.error_load_image));
        }
    }

    private void uploadProfilePicture(Bitmap bitmap) {
        String encodedImage = ImageUtils.encode(bitmap);
        userDocument.update(Constants.KEY_IMAGE, encodedImage)
                .addOnSuccessListener(unused -> {
                    preferenceManager.putString(Constants.KEY_IMAGE, encodedImage);
                    binding.imageProfile.setImageBitmap(ImageUtils.decode(encodedImage));
                    showToast(getString(R.string.profile_picture_updated));
                })
                .addOnFailureListener(e -> showToast(getString(R.string.error_update_picture)));
    }

    private void showChangeEmailDialog() {
        View view = getLayoutInflater().inflate(R.layout.dialog_change_email, null);
        EditText editCurrentPassword = view.findViewById(R.id.editCurrentPassword);
        EditText editNewEmail = view.findViewById(R.id.editNewEmail);

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.change_email)
                .setView(view)
                .setPositiveButton(R.string.change_email, (dialog, which) -> {
                    String newEmail = editNewEmail.getText().toString().trim();
                    if (!Patterns.EMAIL_ADDRESS.matcher(newEmail).matches()) {
                        showToast(getString(R.string.error_invalid_email));
                        return;
                    }
                    changeEmail(editCurrentPassword.getText().toString(), newEmail);
                })
                .setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void changeEmail(String currentPassword, String newEmail) {
        reauthenticate(currentPassword)
                .onSuccessTask(unused -> user.verifyBeforeUpdateEmail(newEmail))
                .addOnCompleteListener(task -> showToast(task.isSuccessful()
                        ? getString(R.string.email_change_link_sent, newEmail)
                        : getString(R.string.error_change_email)));
    }

    private void signOut() {
        Task<Void> offline = userDocument.update(Constants.KEY_AVAILABILITY, 0);
        Tasks.whenAllComplete(offline, PushTokens.delete(user.getUid()),
                        FirebaseMessaging.getInstance().unregister())
                .addOnCompleteListener(task -> finishSignOut());
    }

    private void finishSignOut() {
        preferenceManager.clear();
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(getApplicationContext(), SignInActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }
}
