package com.example.myapplication;

import android.app.Application;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

import com.example.myapplication.utilities.LocaleHelper;

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        LocaleHelper.applySavedLocale(this);
        applyDarkMode();
    }
    private void applyDarkMode() {
        SharedPreferences prefs = getSharedPreferences("Settings", MODE_PRIVATE);
        boolean isDarkMode = prefs.getBoolean("Dark_Mode", false);
        AppCompatDelegate.setDefaultNightMode(isDarkMode ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
    }
}
