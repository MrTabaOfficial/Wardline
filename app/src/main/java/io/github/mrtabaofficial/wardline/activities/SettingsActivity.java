package io.github.mrtabaofficial.wardline.activities;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ArrayAdapter;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;

import io.github.mrtabaofficial.wardline.R;
import io.github.mrtabaofficial.wardline.databinding.ActivitySettingsBinding;
import io.github.mrtabaofficial.wardline.utilities.Constants;

import java.util.Arrays;
import java.util.Locale;

public class SettingsActivity extends AppCompatActivity {

    private static final String[] LANGUAGE_CODES = {"en", "ka", "ru"};

    private ActivitySettingsBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.languages_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.languageSpinner.setAdapter(adapter);
        binding.languageSpinner.setSelection(Math.max(0, Arrays.asList(LANGUAGE_CODES).indexOf(currentLanguage())));

        SharedPreferences settings = getSharedPreferences(Constants.KEY_SETTINGS_PREFERENCE, MODE_PRIVATE);
        binding.darkModeSwitch.setChecked(settings.getBoolean(Constants.KEY_DARK_MODE, false));
        binding.darkModeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            settings.edit().putBoolean(Constants.KEY_DARK_MODE, isChecked).apply();
            AppCompatDelegate.setDefaultNightMode(isChecked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
        });

        binding.buttonSavePassword.setOnClickListener(v -> {
            String languageCode = LANGUAGE_CODES[binding.languageSpinner.getSelectedItemPosition()];
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(languageCode));
            finish();
        });
    }

    private String currentLanguage() {
        LocaleListCompat locales = AppCompatDelegate.getApplicationLocales();
        Locale locale = locales.isEmpty() ? Locale.getDefault() : locales.get(0);
        return locale != null ? locale.getLanguage() : LANGUAGE_CODES[0];
    }
}
