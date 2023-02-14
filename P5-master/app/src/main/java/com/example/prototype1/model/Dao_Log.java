package com.example.prototype1.model;

import android.util.Pair;

import java.util.List;

public class Dao_Log {
    private String eventId,
            senderId,
            userId,
            performedAction,
            timestamp;
    private boolean joined, left;
    private List<Pair<User, Boolean>> receivers;

    //https://firebase.google.com/docs/functions/writing-and-viewing-logs - official doc
    //https://proandroiddev.com/remote-logging-with-timber-and-firebase-realtime-database-a9dfbe66284c - timber + firebase db
    public Dao_Log() {
    }

    public Dao_Log(String eventId, String senderId, List<Pair<User, Boolean>> receivers, String timestamp) {
        this.eventId = eventId;
        this.senderId = senderId;
        this.receivers = receivers;
        this.timestamp = timestamp;
    }

    public Dao_Log(String eventId, String userId, boolean joined, boolean left, String timestamp) {
        this.eventId = eventId;
        this.userId = userId;
        this.joined = joined;
        this.left = left;
        this.timestamp = timestamp;
    }

    public Dao_Log(String eventId, String senderId, String userId, String performedAction, String timestamp) {
        this.eventId = eventId;
        this.userId = userId;
        this.senderId = senderId;
        this.timestamp = timestamp;
        this.performedAction = performedAction;
    }

    public Dao_Log(String eventId, String userId, String performedAction, String timestamp) {
        this.eventId = eventId;
        this.userId = userId;
        this.timestamp = timestamp;
        this.performedAction = performedAction;
    }

    public String getEventId() {
        return eventId;
    }

    public String getSenderId() {
        return senderId;
    }

    public String getUserId() {
        return userId;
    }

    public List<Pair<User, Boolean>> getReceivers() {
        return receivers;
    }

    public boolean isJoined() {
        return joined;
    }

    public boolean isLeft() {
        return left;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String toStringSendMessage() {
        return "Log nr: " + eventId +
                " the User " + senderId +
                " has " + performedAction +
                " to User " + userId +
                " at " + timestamp;
    }

    public String toStringDeleteOrUpdateMessage() {
        return "Log nr: " + eventId +
                " the User " + userId +
                " has " + performedAction +
                " from User " + userId +
                " at " + timestamp;
    }
}
