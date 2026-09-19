package io.github.mrtabaofficial.wardline.utilities;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import org.junit.Test;

import java.util.Arrays;

public class ConversationIdsTest {

    @Test
    public void idFor_isIndependentOfArgumentOrder() {
        assertEquals(ConversationIds.idFor("userB", "userA"), ConversationIds.idFor("userA", "userB"));
        assertEquals("userA_userB", ConversationIds.idFor("userB", "userA"));
    }

    @Test
    public void participants_areSorted() {
        assertEquals(Arrays.asList("a1", "z9"), ConversationIds.participants("z9", "a1"));
    }

    @Test
    public void sameUserTwice_isRejected() {
        assertThrows(IllegalArgumentException.class, () -> ConversationIds.idFor("userA", "userA"));
    }

    @Test
    public void missingUser_isRejected() {
        assertThrows(IllegalArgumentException.class, () -> ConversationIds.idFor(null, "userA"));
    }
}
