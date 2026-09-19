package io.github.mrtabaofficial.wardline.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import io.github.mrtabaofficial.wardline.R;
import io.github.mrtabaofficial.wardline.adapters.RecentConversationsAdapter;
import io.github.mrtabaofficial.wardline.crypto.MessageCipher;
import io.github.mrtabaofficial.wardline.databinding.ActivityMainBinding;
import io.github.mrtabaofficial.wardline.listeners.ConversionListener;
import io.github.mrtabaofficial.wardline.models.ChatMessage;
import io.github.mrtabaofficial.wardline.models.User;
import io.github.mrtabaofficial.wardline.utilities.Constants;
import io.github.mrtabaofficial.wardline.utilities.ImageUtils;
import io.github.mrtabaofficial.wardline.utilities.PreferenceManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.messaging.FirebaseMessaging;

import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class MainActivity extends BaseActivity implements ConversionListener {
    private static final String TAG = "MainActivity";

    private ActivityMainBinding binding;
    private PreferenceManager preferenceManager;
    private RecentConversationsAdapter conversationsAdapter;
    private FirebaseFirestore database;
    private String currentUserId;
    private MessageCipher cipher;
    private ListenerRegistration conversationsRegistration;
    private List<DocumentSnapshot> conversationDocuments = new ArrayList<>();
    private final Map<String, User> userCache = new HashMap<>();
    private final Set<String> pendingUserLoads = new HashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (isFinishing()) {
            return;
        }
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        preferenceManager = new PreferenceManager(getApplicationContext());
        database = FirebaseFirestore.getInstance();
        currentUserId = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
        cipher = MessageCipher.forApp();

        conversationsAdapter = new RecentConversationsAdapter(this);
        binding.conversationsRecyclerView.setAdapter(conversationsAdapter);
        loadUserDetails();
        getToken();
        setListeners();
    }

    @Override
    protected void onStart() {
        super.onStart();
        listenConversations();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (conversationsRegistration != null) {
            conversationsRegistration.remove();
            conversationsRegistration = null;
        }
    }

    private void setListeners() {
        binding.btnSettings.setOnClickListener(v -> startActivity(new Intent(getApplicationContext(), SettingsActivity.class)));
        binding.fabNewChat.setOnClickListener(v -> startActivity(new Intent(getApplicationContext(), UsersActivity.class)));
        binding.imageProfile.setOnClickListener(v -> startActivity(new Intent(getApplicationContext(), UserProfileActivity.class)));
    }

    private void loadUserDetails() {
        binding.textName.setText(preferenceManager.getString(Constants.KEY_NAME));
        binding.imageProfile.setImageBitmap(ImageUtils.decode(preferenceManager.getString(Constants.KEY_IMAGE)));
    }

    private void listenConversations() {
        conversationsRegistration = database.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                .whereArrayContains(Constants.KEY_PARTICIPANTS, currentUserId)
                .addSnapshotListener((value, error) -> {
                    binding.progressBar.setVisibility(View.GONE);
                    if (error != null || value == null) {
                        if (error != null) {
                            Log.w(TAG, "Unable to listen for conversations", error);
                        }
                        return;
                    }
                    conversationDocuments = value.getDocuments();
                    renderConversations();
                });
    }

    private void renderConversations() {
        List<ChatMessage> conversations = new ArrayList<>();
        for (DocumentSnapshot document : conversationDocuments) {
            ChatMessage conversation = toConversation(document);
            if (conversation != null) {
                conversations.add(conversation);
            }
        }
        Collections.sort(conversations, (obj1, obj2) -> obj2.dataObject.compareTo(obj1.dataObject));
        conversationsAdapter.submitList(conversations);
        binding.conversationsRecyclerView.setVisibility(View.VISIBLE);
    }

    private ChatMessage toConversation(DocumentSnapshot document) {
        List<?> participants = (List<?>) document.get(Constants.KEY_PARTICIPANTS);
        if (participants == null || participants.size() != 2) {
            return null;
        }
        String otherUserId = String.valueOf(currentUserId.equals(participants.get(0)) ? participants.get(1) : participants.get(0));
        User otherUser = userCache.get(otherUserId);
        if (otherUser == null) {
            loadUser(otherUserId);
        }

        ChatMessage conversation = new ChatMessage();
        conversation.conversionId = otherUserId;
        conversation.conversionName = otherUser != null ? otherUser.name : "";
        conversation.conversionImage = otherUser != null ? otherUser.image : null;
        Date date = document.getDate(Constants.KEY_TIMESTAMP, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE);
        conversation.dataObject = date != null ? date : new Date(0);
        conversation.message = decryptLastMessage(document, otherUserId);
        return conversation;
    }

    private String decryptLastMessage(DocumentSnapshot document, String otherUserId) {
        String ciphertext = document.getString(Constants.KEY_LAST_MESSAGE);
        if (ciphertext == null || ciphertext.isEmpty()) {
            return getString(R.string.message_deleted);
        }
        String senderId = document.getString(Constants.KEY_LAST_SENDER_ID);
        if (senderId != null) {
            String receiverId = senderId.equals(currentUserId) ? otherUserId : currentUserId;
            try {
                return cipher.decrypt(ciphertext, MessageCipher.context(senderId, receiverId));
            } catch (GeneralSecurityException e) {
                Log.w(TAG, "Unable to decrypt the last message", e);
            }
        }
        return getString(R.string.message_unavailable);
    }

    private void loadUser(String userId) {
        if (!pendingUserLoads.add(userId)) {
            return;
        }
        database.collection(Constants.KEY_COLLECTION_USERS).document(userId).get()
                .addOnCompleteListener(task -> {
                    pendingUserLoads.remove(userId);
                    DocumentSnapshot document = task.getResult();
                    if (task.isSuccessful() && document != null && document.exists()) {
                        User user = new User();
                        user.id = userId;
                        user.name = document.getString(Constants.KEY_NAME);
                        user.email = document.getString(Constants.KEY_EMAIL);
                        user.image = document.getString(Constants.KEY_IMAGE);
                        userCache.put(userId, user);
                        renderConversations();
                    }
                });
    }

    private void getToken() {
        FirebaseMessaging.getInstance().getToken().addOnSuccessListener(this::updateToken);
    }

    private void updateToken(String token) {
        database.collection(Constants.KEY_COLLECTION_USERS).document(currentUserId)
                .update(Constants.KEY_FCM_TOKEN, token)
                .addOnFailureListener(e -> Log.w(TAG, "Unable to update the FCM token", e));
    }

    @Override
    public void onConversionClicked(User user) {
        Intent intent = new Intent(getApplicationContext(), ChatActivity.class);
        intent.putExtra(Constants.KEY_USER, user);
        startActivity(intent);
    }
}
