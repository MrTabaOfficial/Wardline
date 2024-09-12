package com.example.myapplication.firebase;

import android.content.Context;

import com.example.myapplication.utilities.PreferenceManager;
import com.google.firebase.firestore.FirebaseFirestore;

public class FirestoreUtil {
    private static FirebaseFirestore database;
    private static PreferenceManager preferenceManager;

    public static FirebaseFirestore getDatabaseInstance() {
        if (database == null) {
            database = FirebaseFirestore.getInstance();
        }
        return database;
    }

    public static PreferenceManager getPreferenceManager(Context context) {
        if (preferenceManager == null) {
            preferenceManager = new PreferenceManager(context);
        }
        return preferenceManager;
    }
}