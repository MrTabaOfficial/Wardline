package io.github.mrtabaofficial.wardline;

import android.app.Application;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

import io.github.mrtabaofficial.wardline.utilities.Constants;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        if (BuildConfig.DEBUG && !BuildConfig.FIREBASE_EMULATOR_HOST.isEmpty()) {
            FirebaseAuth.getInstance().useEmulator(BuildConfig.FIREBASE_EMULATOR_HOST, 9099);
            FirebaseFirestore.getInstance().useEmulator(BuildConfig.FIREBASE_EMULATOR_HOST, 8080);
        }
        SharedPreferences settings = getSharedPreferences(Constants.KEY_SETTINGS_PREFERENCE, MODE_PRIVATE);
        AppCompatDelegate.setDefaultNightMode(settings.getBoolean(Constants.KEY_DARK_MODE, false)
                ? AppCompatDelegate.MODE_NIGHT_YES
                : AppCompatDelegate.MODE_NIGHT_NO);
    }
}
