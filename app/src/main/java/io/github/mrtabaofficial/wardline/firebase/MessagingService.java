package io.github.mrtabaofficial.wardline.firebase;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import io.github.mrtabaofficial.wardline.R;
import io.github.mrtabaofficial.wardline.activities.ChatActivity;
import io.github.mrtabaofficial.wardline.crypto.MessageCipher;
import io.github.mrtabaofficial.wardline.models.User;
import io.github.mrtabaofficial.wardline.utilities.Constants;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.security.GeneralSecurityException;
import java.util.Map;

public class MessagingService extends FirebaseMessagingService {

    private static final String TAG = "MessagingService";
    private static final String CHANNEL_ID = "chat_message";

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            PushTokens.save(user.getUid(), token)
                    .addOnFailureListener(e -> Log.w(TAG, "Unable to store the new FCM token", e));
        }
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        Map<String, String> data = remoteMessage.getData();
        String senderId = data.get(Constants.REMOTE_MSG_SENDER_ID);
        if (currentUser == null || senderId == null || senderId.equals(currentUser.getUid())) {
            return;
        }

        User sender = new User();
        sender.id = senderId;
        sender.name = data.get(Constants.REMOTE_MSG_SENDER_NAME);

        Intent intent = new Intent(this, ChatActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra(Constants.KEY_USER, sender);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, senderId.hashCode(), intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String text = decryptPreview(currentUser.getUid(), senderId, data.get(Constants.REMOTE_MSG_CIPHERTEXT));
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notifications)
                .setContentTitle(sender.name)
                .setContentText(text)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(text))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                getString(R.string.notification_channel_name), NotificationManager.IMPORTANCE_DEFAULT);
        channel.setDescription(getString(R.string.notification_channel_description));
        getSystemService(NotificationManager.class).createNotificationChannel(channel);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {
            NotificationManagerCompat.from(this).notify(senderId.hashCode(), builder.build());
        }
    }

    private String decryptPreview(String currentUserId, String senderId, String ciphertext) {
        if (ciphertext != null) {
            try {
                return MessageCipher.forApp().decrypt(ciphertext,
                        MessageCipher.context(senderId, currentUserId));
            } catch (GeneralSecurityException e) {
                Log.w(TAG, "Unable to decrypt the notification preview", e);
            }
        }
        return getString(R.string.new_encrypted_message);
    }
}
