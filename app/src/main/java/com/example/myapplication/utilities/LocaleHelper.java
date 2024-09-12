package com.example.myapplication.utilities;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;

import java.util.Locale;

public class LocaleHelper {

    private static final String PREFERENCE_NAME = "Settings";
    private static final String LANGUAGE_KEY = "My_Lang";

    public static void setLocale(Context context, String languageCode) {
        saveLanguagePreference(context, languageCode);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            setSystemLocale(context, languageCode);
        } else {
            setLegacyLocale(context, languageCode);
        }
    }

    @TargetApi(Build.VERSION_CODES.N)
    private static void setSystemLocale(Context context, String languageCode) {
        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.setLocale(locale);
        context.getResources().updateConfiguration(config, context.getResources().getDisplayMetrics());
    }

    @SuppressWarnings("deprecation")
    private static void setLegacyLocale(Context context, String languageCode) {
        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);
        Resources resources = context.getResources();
        Configuration config = resources.getConfiguration();
        config.locale = locale;
        resources.updateConfiguration(config, resources.getDisplayMetrics());
    }

    private static void saveLanguagePreference(Context context, String languageCode) {
        SharedPreferences.Editor editor = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE).edit();
        editor.putString(LANGUAGE_KEY, languageCode);
        editor.apply();
    }

    public static void applySavedLocale(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE);
        String langCode = prefs.getString(LANGUAGE_KEY, "en"); // Default to English if no language is set
        setLocale(context, langCode);
    }
}
