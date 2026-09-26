package io.github.mrtabaofficial.wardline.firebase;

import io.github.mrtabaofficial.wardline.utilities.Constants;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Collections;

public final class PushTokens {

    private PushTokens() {
    }

    private static DocumentReference reference(String userId) {
        return FirebaseFirestore.getInstance()
                .collection(Constants.KEY_COLLECTION_USERS)
                .document(userId)
                .collection(Constants.KEY_COLLECTION_PRIVATE)
                .document(Constants.KEY_DOCUMENT_MESSAGING);
    }

    public static Task<Void> save(String userId, String token) {
        return reference(userId).set(Collections.singletonMap(Constants.KEY_FCM_TOKEN, token));
    }

    public static Task<Void> delete(String userId) {
        return reference(userId).delete();
    }
}
