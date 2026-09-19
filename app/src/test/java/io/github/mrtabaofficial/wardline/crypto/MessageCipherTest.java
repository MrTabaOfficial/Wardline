package io.github.mrtabaofficial.wardline.crypto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThrows;

import org.junit.Test;

import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Base64;

public class MessageCipherTest {

    private static final String KEY = Base64.getEncoder().encodeToString(new byte[32]);
    private static final String OTHER_KEY = Base64.getEncoder().encodeToString(filled(32, (byte) 7));
    private static final String ALICE = "aliceUid";
    private static final String BOB = "bobUid";

    private final MessageCipher cipher = new MessageCipher(KEY);

    @Test
    public void encryptThenDecrypt_returnsOriginalText() throws GeneralSecurityException {
        byte[] context = MessageCipher.context(ALICE, BOB);
        String ciphertext = cipher.encrypt("Hello, Bob! გამარჯობა", context);

        assertEquals("Hello, Bob! გამარჯობა", cipher.decrypt(ciphertext, context));
    }

    @Test
    public void ciphertext_doesNotContainPlaintextAndIsRandomised() throws GeneralSecurityException {
        byte[] context = MessageCipher.context(ALICE, BOB);
        String first = cipher.encrypt("same text", context);
        String second = cipher.encrypt("same text", context);

        assertNotEquals(first, second);
        assertFalse(new String(Base64.getDecoder().decode(first)).contains("same text"));
    }

    @Test
    public void decryptWithAnotherKey_fails() throws GeneralSecurityException {
        byte[] context = MessageCipher.context(ALICE, BOB);
        String ciphertext = cipher.encrypt("hello", context);

        assertThrows(GeneralSecurityException.class, () -> new MessageCipher(OTHER_KEY).decrypt(ciphertext, context));
    }

    @Test
    public void decryptWithDifferentConversationContext_fails() throws GeneralSecurityException {
        String ciphertext = cipher.encrypt("hello", MessageCipher.context(ALICE, BOB));

        assertThrows(GeneralSecurityException.class, () ->
                cipher.decrypt(ciphertext, MessageCipher.context("malloryUid", BOB)));
    }

    @Test
    public void decryptTamperedCiphertext_fails() throws GeneralSecurityException {
        byte[] context = MessageCipher.context(ALICE, BOB);
        byte[] bytes = Base64.getDecoder().decode(cipher.encrypt("hello", context));
        bytes[bytes.length - 1] ^= 0x01;
        String tampered = Base64.getEncoder().encodeToString(bytes);

        assertThrows(GeneralSecurityException.class, () -> cipher.decrypt(tampered, context));
    }

    @Test
    public void decryptInvalidInput_fails() {
        byte[] context = MessageCipher.context(ALICE, BOB);

        assertThrows(GeneralSecurityException.class, () -> cipher.decrypt("not base64 !!", context));
        assertThrows(GeneralSecurityException.class, () -> cipher.decrypt("c2hvcnQ=", context));
    }

    @Test
    public void keyOfWrongLength_isRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                new MessageCipher(Base64.getEncoder().encodeToString(new byte[16])));
        assertThrows(IllegalArgumentException.class, () -> new MessageCipher("not a key"));
    }

    private static byte[] filled(int length, byte value) {
        byte[] bytes = new byte[length];
        Arrays.fill(bytes, value);
        return bytes;
    }
}
