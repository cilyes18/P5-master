package com.example.prototype1.model;

import android.os.Parcel;
import android.os.Parcelable;

import org.jetbrains.annotations.NotNull;

public class Message implements Parcelable {
    private String messageId,
            senderId,
            content,
            timestamp,
            urlAttached,
            encryptedAESKey;
    private boolean replyOrForward;

    public Message() {
    }

    public Message(String messageId, String senderId, String content, String timestamp, String urlAttached, String encryptedAESKey) {
        this.messageId = messageId;
        this.senderId = senderId;
        this.content = content;
        this.timestamp = timestamp;
        this.urlAttached = urlAttached;
        this.encryptedAESKey = encryptedAESKey;
    }

    protected Message(Parcel in) {
        messageId = in.readString();
        senderId = in.readString();
        content = in.readString();
        timestamp = in.readString();
        urlAttached = in.readString();
        encryptedAESKey = in.readString();
    }

    public static final Creator<Message> CREATOR = new Creator<Message>() {
        @Override
        public Message createFromParcel(Parcel in) {
            return new Message(in);
        }

        @Override
        public Message[] newArray(int size) {
            return new Message[size];
        }
    };

    @NotNull
    public String getSenderId() {
        return senderId;
    }

    @NotNull
    public String getContent() {
        return content;
    }

    @NotNull
    public String getTimestamp() {
        return timestamp;
    }

    @NotNull
    public String getUrlAttached() {
        return urlAttached;
    }

    @NotNull
    public String getMessageId() {
        return messageId;
    }

    @NotNull
    public String getEncryptedAESKey() {
        return encryptedAESKey;
    }

    public boolean isIncoming(@NotNull String senderId, @NotNull String currentUserId) {
        return senderId.equals(currentUserId);
    }

    public void useAsReply(boolean replyMessage) {
        this.replyOrForward = replyMessage;
    }

    //todo add both message and boolean to Message FBase think of similar thing to Forward
    public boolean isReplyOrForward() {
        return replyOrForward;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(messageId);
        dest.writeString(senderId);
        dest.writeString(content);
        dest.writeString(timestamp);
        dest.writeString(urlAttached);
        dest.writeString(encryptedAESKey);
    }

}
