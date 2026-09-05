package io.github.mrtabaofficial.wardline.utilities;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;

public class EncryptionUtil {

    private static final String AES_ALGORITHM = "AES";
    private static final String AES_TRANSFORMATION = "AES/ECB/PKCS5Padding";
    private static final String AES_KEY = "REDACTED";
    private static final String MESSAGE_DELETED = "Message Deleted";

    public static String encrypt(String input, Date timestamp) throws Exception {
        if (MESSAGE_DELETED.equals(input)) {
            return input;
        }
        SecretKey secretKey = generateKey(timestamp);
        Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        byte[] encryptedBytes = cipher.doFinal(input.getBytes());
        return Base64.encodeToString(encryptedBytes, Base64.DEFAULT);
    }

    public static String decrypt(String encryptedInput, Date timestamp) {
        if (MESSAGE_DELETED.equals(encryptedInput)) {
            return encryptedInput;
        }
        try {
            SecretKey secretKey = generateKey(timestamp);
            Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            byte[] decryptedBytes = cipher.doFinal(Base64.decode(encryptedInput, Base64.DEFAULT));
            return new String(decryptedBytes);
        } catch (Exception e) {
            e.printStackTrace();
            return "Error decrypting message: " + e.getMessage();
        }
    }

    private static SecretKey generateKey(Date timestamp) throws Exception {
        Calendar cal = Calendar.getInstance();
        cal.setTime(timestamp);
        int minute = cal.get(Calendar.MINUTE);
        int second = cal.get(Calendar.SECOND);

        // Derive key based on the minute and second of the timestamp
        String timeKey = AES_KEY + minute + second;

        // Hash the key to ensure it is of proper length (16 bytes for AES-128)
        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        byte[] key = sha.digest(timeKey.getBytes(StandardCharsets.UTF_8));
        key = Arrays.copyOf(key, 16); // Use only the first 16 bytes

        return new SecretKeySpec(key, AES_ALGORITHM);
    }
}