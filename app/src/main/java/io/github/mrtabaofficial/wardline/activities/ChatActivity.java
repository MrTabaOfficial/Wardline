package io.github.mrtabaofficial.wardline.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.core.content.IntentCompat;

import io.github.mrtabaofficial.wardline.R;
import io.github.mrtabaofficial.wardline.adapters.ChatAdapter;
import io.github.mrtabaofficial.wardline.crypto.MessageCipher;
import io.github.mrtabaofficial.wardline.databinding.ActivityChatBinding;
import io.github.mrtabaofficial.wardline.models.ChatMessage;
import io.github.mrtabaofficial.wardline.models.User;
import io.github.mrtabaofficial.wardline.utilities.Constants;
import io.github.mrtabaofficial.wardline.utilities.ConversationIds;
import io.github.mrtabaofficial.wardline.utilities.ImageUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;

import java.security.GeneralSecurityException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class ChatActivity extends BaseActivity {
    private static final String TAG = "ChatActivity";

    private ActivityChatBinding binding;
    private User receiverUser;
    private final List<ChatMessage> chatMessages = new ArrayList<>();
    private ChatAdapter chatAdapter;
    private FirebaseFirestore database;
    private DocumentReference conversationReference;
    private String currentUserId;
    private MessageCipher cipher;
    private String receiverImage;
    private ListenerRegistration messagesRegistration;
    private ListenerRegistration receiverRegistration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (isFinishing()) {
            return;
        }
        binding = ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        receiverUser = IntentCompat.getSerializableExtra(getIntent(), Constants.KEY_USER, User.class);
        currentUserId = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
        if (receiverUser == null || receiverUser.id == null || receiverUser.id.equals(currentUserId)) {
            finish();
            return;
        }
        cipher = MessageCipher.forApp();
        database = FirebaseFirestore.getInstance();
        conversationReference = database.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                .document(ConversationIds.idFor(currentUserId, receiverUser.id));
        receiverImage = receiverUser.image;

        binding.textName.setText(receiverUser.name);
        chatAdapter = new ChatAdapter(ImageUtils.decode(receiverImage), currentUserId);
        chatAdapter.setOnMessageClickListener(this::deleteMessage);
        binding.chatRecyclerView.setAdapter(chatAdapter);
        setListeners();
    }

    @Override
    protected void onStart() {
        super.onStart();
        listenMessages();
        listenReceiver();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (messagesRegistration != null) {
            messagesRegistration.remove();
            messagesRegistration = null;
        }
        if (receiverRegistration != null) {
            receiverRegistration.remove();
            receiverRegistration = null;
        }
    }

    private void setListeners() {
        binding.imageBack.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        binding.layoutSend.setOnClickListener(v -> sendMessage());
        binding.btninfo.setOnClickListener(v -> viewUserProfile());
    }

    private void sendMessage() {
        String messageText = binding.inputMessage.getText().toString().trim();
        if (messageText.isEmpty()) {
            return;
        }

        String ciphertext;
        try {
            ciphertext = cipher.encrypt(messageText, MessageCipher.context(currentUserId, receiverUser.id));
        } catch (GeneralSecurityException e) {
            Log.e(TAG, "Unable to encrypt the message", e);
            showToast(getString(R.string.error_send_message));
            return;
        }

        DocumentReference messageReference = conversationReference.collection(Constants.KEY_COLLECTION_MESSAGES).document();
        Map<String, Object> message = new HashMap<>();
        message.put(Constants.KEY_SENDER_ID, currentUserId);
        message.put(Constants.KEY_RECEIVER_ID, receiverUser.id);
        message.put(Constants.KEY_CIPHERTEXT, ciphertext);
        message.put(Constants.KEY_DELETED, false);
        message.put(Constants.KEY_TIMESTAMP, FieldValue.serverTimestamp());

        Map<String, Object> conversation = new HashMap<>();
        conversation.put(Constants.KEY_PARTICIPANTS, ConversationIds.participants(currentUserId, receiverUser.id));
        conversation.put(Constants.KEY_LAST_MESSAGE, ciphertext);
        conversation.put(Constants.KEY_LAST_SENDER_ID, currentUserId);
        conversation.put(Constants.KEY_TIMESTAMP, FieldValue.serverTimestamp());

        WriteBatch batch = database.batch();
        batch.set(messageReference, message);
        batch.set(conversationReference, conversation, SetOptions.merge());
        batch.commit().addOnFailureListener(e -> {
            Log.w(TAG, "Unable to send the message", e);
            showToast(getString(R.string.error_send_message));
        });
        binding.inputMessage.setText(null);
    }

    private void deleteMessage(ChatMessage chatMessage) {
        boolean isLastMessage = !chatMessages.isEmpty()
                && chatMessages.get(chatMessages.size() - 1).id.equals(chatMessage.id);

        Map<String, Object> messageUpdate = new HashMap<>();
        messageUpdate.put(Constants.KEY_DELETED, true);
        messageUpdate.put(Constants.KEY_CIPHERTEXT, "");

        WriteBatch batch = database.batch();
        batch.update(conversationReference.collection(Constants.KEY_COLLECTION_MESSAGES).document(chatMessage.id), messageUpdate);
        if (isLastMessage) {
            batch.update(conversationReference, Constants.KEY_LAST_MESSAGE, "");
        }
        batch.commit()
                .addOnSuccessListener(unused -> showToast(getString(R.string.message_deleted)))
                .addOnFailureListener(e -> showToast(getString(R.string.error_delete_message)));
    }

    private void listenMessages() {
        chatMessages.clear();
        messagesRegistration = conversationReference.collection(Constants.KEY_COLLECTION_MESSAGES)
                .orderBy(Constants.KEY_TIMESTAMP, Query.Direction.ASCENDING)
                .addSnapshotListener((value, error) -> {
                    binding.progressBar.setVisibility(View.GONE);
                    if (error != null || value == null) {
                        if (error != null) {
                            Log.w(TAG, "Unable to listen for messages", error);
                        }
                        return;
                    }
                    boolean messageAdded = false;
                    for (DocumentChange change : value.getDocumentChanges()) {
                        switch (change.getType()) {
                            case ADDED:
                                chatMessages.add(change.getNewIndex(), toChatMessage(change.getDocument()));
                                messageAdded = true;
                                break;
                            case MODIFIED:
                                chatMessages.remove(change.getOldIndex());
                                chatMessages.add(change.getNewIndex(), toChatMessage(change.getDocument()));
                                break;
                            case REMOVED:
                                chatMessages.remove(change.getOldIndex());
                                break;
                        }
                    }
                    boolean scrollToEnd = messageAdded && !chatMessages.isEmpty();
                    chatAdapter.submitList(new ArrayList<>(chatMessages), () -> {
                        if (scrollToEnd) {
                            binding.chatRecyclerView.smoothScrollToPosition(chatMessages.size() - 1);
                        }
                    });
                    binding.chatRecyclerView.setVisibility(View.VISIBLE);
                });
    }

    private ChatMessage toChatMessage(DocumentSnapshot document) {
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.id = document.getId();
        chatMessage.senderId = document.getString(Constants.KEY_SENDER_ID);
        chatMessage.receiverId = document.getString(Constants.KEY_RECEIVER_ID);
        chatMessage.deleted = Boolean.TRUE.equals(document.getBoolean(Constants.KEY_DELETED));
        Date date = document.getDate(Constants.KEY_TIMESTAMP, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE);
        chatMessage.dataObject = date;
        chatMessage.dateTime = date != null ? getReadableDateTime(date) : "";
        if (chatMessage.deleted) {
            chatMessage.message = getString(R.string.message_deleted);
        } else {
            chatMessage.message = decryptMessage(document.getString(Constants.KEY_CIPHERTEXT),
                    chatMessage.senderId, chatMessage.receiverId);
        }
        return chatMessage;
    }

    private String decryptMessage(String ciphertext, String senderId, String receiverId) {
        if (ciphertext != null && senderId != null && receiverId != null) {
            try {
                return cipher.decrypt(ciphertext, MessageCipher.context(senderId, receiverId));
            } catch (GeneralSecurityException e) {
                Log.w(TAG, "Unable to decrypt a message", e);
            }
        }
        return getString(R.string.message_unavailable);
    }

    private void listenReceiver() {
        receiverRegistration = database.collection(Constants.KEY_COLLECTION_USERS)
                .document(receiverUser.id)
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null) {
                        return;
                    }
                    Long availability = value.getLong(Constants.KEY_AVAILABILITY);
                    boolean isReceiverAvailable = availability != null && availability == 1;
                    binding.textAvailability.setVisibility(isReceiverAvailable ? View.VISIBLE : View.GONE);
                    String image = value.getString(Constants.KEY_IMAGE);
                    if (image != null && !image.equals(receiverImage)) {
                        receiverImage = image;
                        chatAdapter.setReceiverProfileImage(ImageUtils.decode(image));
                    }
                });
    }

    private void viewUserProfile() {
        Intent intent = new Intent(this, ProfileActivity.class);
        intent.putExtra(Constants.KEY_USER, receiverUser);
        startActivity(intent);
    }

    private String getReadableDateTime(Date date) {
        return new SimpleDateFormat("MMMM dd, yyyy - hh:mm a", Locale.getDefault()).format(date);
    }

    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }
}
