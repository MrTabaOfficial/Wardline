package io.github.mrtabaofficial.wardline.models;

import java.util.Date;
import java.util.Objects;

public class ChatMessage {
    public String id, senderId, receiverId, message, dateTime;
    public Date dataObject;
    public boolean deleted;
    public String conversionId, conversionName, conversionImage;

    public boolean hasSameContent(ChatMessage other) {
        return deleted == other.deleted
                && Objects.equals(message, other.message)
                && Objects.equals(dateTime, other.dateTime)
                && Objects.equals(conversionName, other.conversionName)
                && Objects.equals(conversionImage, other.conversionImage);
    }
}
