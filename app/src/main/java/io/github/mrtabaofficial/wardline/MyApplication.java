package io.github.mrtabaofficial.wardline;

import android.app.Application;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

import io.github.mrtabaofficial.wardline.utilities.Constants;

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        SharedPreferences settings = getSharedPreferences(Constants.KEY_SETTINGS_PREFERENCE, MODE_PRIVATE);
        AppCompatDelegate.setDefaultNightMode(settings.getBoolean(Constants.KEY_DARK_MODE, false)
                ? AppCompatDelegate.MODE_NIGHT_YES
                : AppCompatDelegate.MODE_NIGHT_NO);
    }
}
