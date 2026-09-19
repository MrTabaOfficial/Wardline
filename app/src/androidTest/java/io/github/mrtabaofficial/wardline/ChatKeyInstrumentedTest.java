package io.github.mrtabaofficial.wardline;

import static org.junit.Assert.assertEquals;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import io.github.mrtabaofficial.wardline.crypto.MessageCipher;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class ChatKeyInstrumentedTest {

    @Test
    public void configuredKey_encryptsAndDecryptsOnDevice() throws Exception {
        MessageCipher cipher = MessageCipher.forApp();
        byte[] context = MessageCipher.context("sender", "receiver");

        String ciphertext = cipher.encrypt("device round trip", context);

        assertEquals("device round trip", cipher.decrypt(ciphertext, context));
    }
}
