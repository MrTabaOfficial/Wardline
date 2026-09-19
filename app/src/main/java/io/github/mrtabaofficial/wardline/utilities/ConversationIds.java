package io.github.mrtabaofficial.wardline.utilities;

import java.util.Arrays;
import java.util.List;

public final class ConversationIds {

    private static final String SEPARATOR = "_";

    private ConversationIds() {
    }

    public static String idFor(String userA, String userB) {
        List<String> participants = participants(userA, userB);
        return participants.get(0) + SEPARATOR + participants.get(1);
    }

    public static List<String> participants(String userA, String userB) {
        if (userA == null || userB == null || userA.equals(userB)) {
            throw new IllegalArgumentException("A conversation needs two distinct users");
        }
        return userA.compareTo(userB) < 0 ? Arrays.asList(userA, userB) : Arrays.asList(userB, userA);
    }
}
