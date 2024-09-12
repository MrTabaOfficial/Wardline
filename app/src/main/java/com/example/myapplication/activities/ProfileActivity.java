package com.example.myapplication.activities;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.R;
import com.example.myapplication.models.User;
import com.example.myapplication.utilities.Constants;
import com.example.myapplication.utilities.PreferenceManager;

public class ProfileActivity extends AppCompatActivity {
    private ImageView imageProfile;
    private TextView textName, textEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        imageProfile = findViewById(R.id.imageProfile);
        textName = findViewById(R.id.textName);
        textEmail = findViewById(R.id.textEmail);
        Button btnBack = findViewById(R.id.btnback);

        btnBack.setOnClickListener(view -> finish());

        loadUserProfile();
    }

    private void loadUserProfile() {
        User user = (User) getIntent().getSerializableExtra(Constants.KEY_USER);
        if (user != null) {
            textName.setText(user.name);
            textEmail.setText(user.email);

            // If the image is a URL, load it with Picasso
            if (user.image != null && !user.image.isEmpty()) {
                byte[] decodedString = Base64.decode(user.image, Base64.DEFAULT);
                Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                imageProfile.setImageBitmap(decodedByte);
            }
        }
    }
}
