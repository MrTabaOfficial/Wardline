package com.example.myapplication.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.example.myapplication.R;
import com.example.myapplication.utilities.Constants;
import com.example.myapplication.utilities.LocaleHelper;
import com.example.myapplication.utilities.PreferenceManager;
import com.google.android.material.switchmaterial.SwitchMaterial;

public class SettingsActivity extends AppCompatActivity {

    private Spinner languageSpinner;
    private Button buttonSave;
    private String selectedLanguageCode;
    private SwitchMaterial darkModeSwitch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        darkModeSwitch = findViewById(R.id.darkModeSwitch);
        languageSpinner = findViewById(R.id.languageSpinner);
        buttonSave = findViewById(R.id.buttonSavePassword);

        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.languages_array, android.R.layout.simple_spinner_item);

        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // Apply the adapter to the spinner
        languageSpinner.setAdapter(adapter);

        // Set the selected language based on saved preferences
        SharedPreferences prefs = getSharedPreferences("Settings", MODE_PRIVATE);
        String currentLanguage = prefs.getString("My_Lang", "en");
        selectedLanguageCode = currentLanguage;
        int spinnerPosition = adapter.getPosition(getLanguageName(currentLanguage));
        languageSpinner.setSelection(spinnerPosition);

        languageSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                String selectedLanguage = (String) parentView.getItemAtPosition(position);
                selectedLanguageCode = getLanguageCode(selectedLanguage);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                // Do nothing
            }
        });
        boolean isDarkMode = prefs.getBoolean("Dark_Mode", false);
        darkModeSwitch.setChecked(isDarkMode);

        darkModeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            AppCompatDelegate.setDefaultNightMode(isChecked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
            SharedPreferences.Editor editor = getSharedPreferences("Settings", MODE_PRIVATE).edit();
            editor.putBoolean("Dark_Mode", isChecked);
            editor.apply();
        });

        buttonSave.setOnClickListener(v -> {
            setLocale(selectedLanguageCode);
        });
    }

    private void setLocale(String langCode) {
        LocaleHelper.setLocale(SettingsActivity.this, langCode);

        // Determine if the user is logged in
        PreferenceManager preferenceManager = new PreferenceManager(getApplicationContext());
        boolean isLoggedIn = preferenceManager.getBoolean(Constants.KEY_IS_SIGNED_IN);

        // Restart the appropriate activity based on login status
        Intent intent;
        if (isLoggedIn) {
            intent = new Intent(SettingsActivity.this, MainActivity.class);
        } else {
            intent = new Intent(SettingsActivity.this, SignInActivity.class);
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }


    private String getLanguageName(String langCode) {
        switch (langCode) {
            case "en":
                return "English";
            case "ka":
                return "Georgian";
            case "ru":
                return "Russian";
            default:
                return "English";
        }
    }

    private String getLanguageCode(String langName) {
        switch (langName) {
            case "English":
                return "en";
            case "Georgian":
                return "ka";
            case "Russian":
                return "ru";
            default:
                return "en";
        }
    }
}
