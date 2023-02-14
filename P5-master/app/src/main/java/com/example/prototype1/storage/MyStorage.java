package com.example.prototype1.storage;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class MyStorage {
    private static final String TAG = "MyStorage";

    private final static FirebaseStorage fs = FirebaseStorage.getInstance();
    private final StorageReference user_storage = fs.getReference("users_prof");
    private final StorageReference dual_chat_storage = fs.getReference("indivi_chat");
    private final StorageReference multi_chat_storage = fs.getReference("group_chat");

    private MyStorage() {
    }

    public static FirebaseStorage getFs() {
        return fs;
    }

    public StorageReference getUser_storage() {
        return user_storage;
    }

    public StorageReference getDual_chat_storage() {
        return dual_chat_storage;
    }

    public StorageReference getMulti_chat_storage() {
        return multi_chat_storage;
    }

    public static MyStorage getInstance() {
        return new MyStorage();
    }

}
