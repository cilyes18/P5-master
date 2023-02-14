package com.example.prototype1.Db;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class DbSingleton {

    private static final String TAG = "DbSingleton";

    private static final FirebaseDatabase mDatabase = FirebaseDatabase.getInstance();
    public static final DatabaseReference myRef = mDatabase.getReference();

    public DatabaseReference getMyRef() {
        return myRef;
    }

    private static final DatabaseReference users_ref = mDatabase.getReference("users"),
            phone_reg_ref = mDatabase.getReference("Phone_Reg"),
            mac_add_ref = mDatabase.getReference("mac_add"),
            message_ref = mDatabase.getReference("messages"),
            chat_ref = mDatabase.getReference("chats"),
            approved_ref = mDatabase.getReference("approved_users"),
            dh_ref = mDatabase.getReference("dh_node"),
            log_ref = mDatabase.getReference("logs");


    private DbSingleton() {
    }

    public static FirebaseDatabase getmDatabase() {
        return mDatabase;
    }

    public static DbSingleton getInstance() {
        if (mDatabase != null) {
            return new DbSingleton();
        } else return null;
    }

    public FirebaseDatabase getDbInstance() {
        return mDatabase;
    }

    public DatabaseReference getPhone_reg_ref() {
        phone_reg_ref.keepSynced(false);
        return phone_reg_ref;
    }

    public DatabaseReference getMac_add_ref() {
        mac_add_ref.keepSynced(false);
        return mac_add_ref;
    }

    public DatabaseReference getApproved_ref() {
        approved_ref.keepSynced(false);
        return approved_ref;
    }

    public DatabaseReference getMessage_ref() {
        message_ref.keepSynced(true);
        return message_ref;
    }

    public DatabaseReference getChat_ref() {
        chat_ref.keepSynced(true);
        return chat_ref;
    }

    public DatabaseReference getUsers_ref() {
        users_ref.keepSynced(false);
        return users_ref;
    }

    public DatabaseReference getLog_ref() {
        log_ref.keepSynced(false);
        return log_ref;
    }

    public DatabaseReference getDh_ref() {
        dh_ref.keepSynced(false);
        return dh_ref;
    }
}
