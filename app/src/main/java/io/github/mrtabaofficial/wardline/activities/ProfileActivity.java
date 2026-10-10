package io.github.mrtabaofficial.wardline.activities;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.IntentCompat;

import io.github.mrtabaofficial.wardline.databinding.ActivityProfileBinding;
import io.github.mrtabaofficial.wardline.models.User;
import io.github.mrtabaofficial.wardline.utilities.Constants;
import io.github.mrtabaofficial.wardline.utilities.ImageUtils;
import io.github.mrtabaofficial.wardline.utilities.SystemBars;

public class ProfileActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityProfileBinding binding = ActivityProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        SystemBars.applyEdgeToEdge(this, binding.getRoot());
        binding.btnback.setOnClickListener(view -> finish());

        User user = IntentCompat.getSerializableExtra(getIntent(), Constants.KEY_USER, User.class);
        if (user != null) {
            binding.textName.setText(user.name);
            binding.textEmail.setText(user.email);
            binding.imageProfile.setImageBitmap(ImageUtils.decode(user.image));
        }
    }
}
