package com.example.prototype1.model;

import java.util.HashMap;
import java.util.List;

public class Chat {

    private String chatId,
            chatName,
            adminId;
    private HashMap<String, Boolean> members;
    private List<String> messagesInChat;
    private boolean isGroup;

    public Chat() {
    }

    public Chat(String chatId, String adminId, HashMap<String, Boolean> members, List<String> messagesInChat, boolean isGroup, String chatName) {
        this.chatId = chatId;
        this.adminId = adminId;
        this.members = members;
        this.messagesInChat = messagesInChat;
        this.isGroup = isGroup;
        this.chatName = chatName;
    }

    public String getChatId() {
        return chatId;
    }

    public String getAdminId() {
        return adminId;
    }

    public HashMap<String, Boolean> getMembers() {
        return members;
    }

    public List<String> getMessagesInChat() {
        return messagesInChat;
    }

    public boolean isGroup() {
        return isGroup;
    }

    public String getChatName() {
        return chatName;
    }
}
