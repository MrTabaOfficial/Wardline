package io.github.mrtabaofficial.wardline.crypto;

import io.github.mrtabaofficial.wardline.BuildConfig;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public final class MessageCipher {

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int KEY_LENGTH_BYTES = 32;
    private static final int IV_LENGTH_BYTES = 12;
    private static final int TAG_LENGTH_BITS = 128;
    private static final String CONTEXT_PREFIX = "message/v1|";

    private static MessageCipher appCipher;

    private final SecretKey key;
    private final SecureRandom random = new SecureRandom();

    public MessageCipher(String base64Key) {
        byte[] keyBytes;
        try {
            keyBytes = Base64.getDecoder().decode(base64Key);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("The chat key must be Base64", e);
        }
        if (keyBytes.length != KEY_LENGTH_BYTES) {
            throw new IllegalArgumentException("The chat key must be 32 bytes");
        }
        key = new SecretKeySpec(keyBytes, "AES");
    }

    public static synchronized MessageCipher forApp() {
        if (appCipher == null) {
            appCipher = new MessageCipher(BuildConfig.CHAT_AES_KEY);
        }
        return appCipher;
    }

    public static byte[] context(String senderId, String receiverId) {
        return (CONTEXT_PREFIX + senderId + "|" + receiverId).getBytes(StandardCharsets.UTF_8);
    }

    public String encrypt(String plaintext, byte[] context) throws GeneralSecurityException {
        byte[] iv = new byte[IV_LENGTH_BYTES];
        random.nextBytes(iv);
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
        cipher.updateAAD(context);
        byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(ByteBuffer.allocate(iv.length + ciphertext.length)
                .put(iv)
                .put(ciphertext)
                .array());
    }

    public String decrypt(String encoded, byte[] context) throws GeneralSecurityException {
        byte[] data;
        try {
            data = Base64.getDecoder().decode(encoded);
        } catch (IllegalArgumentException e) {
            throw new GeneralSecurityException("Ciphertext is not valid Base64", e);
        }
        if (data.length <= IV_LENGTH_BYTES) {
            throw new GeneralSecurityException("Ciphertext is too short");
        }
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, data, 0, IV_LENGTH_BYTES));
        cipher.updateAAD(context);
        byte[] plaintext = cipher.doFinal(data, IV_LENGTH_BYTES, data.length - IV_LENGTH_BYTES);
        return new String(plaintext, StandardCharsets.UTF_8);
    }
}
