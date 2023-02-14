package com.example.prototype1.model;

import java.io.Serializable;
import java.util.HashMap;

/**
 * @author Mohamed Msaad. 23/11/2020
 * class object of a DH key exchange between two users.
 **/

public class DHModel implements Serializable {

    private String node_Id, admin_uid;
    private HashMap<String, String> publicKeys; // string for uid, string for public key
    private HashMap<String, String> aesMessageWithKey; // string for uid, string for result key
    private HashMap<String, String> signature; // string for uid, string for signature key
    private HashMap<String, Boolean> members; //list for members, string for uid and boolean for admin


    public HashMap<String, String> getPublicKeys() {
        return publicKeys;
    }

    public String getAdmin_uid() {
        return admin_uid;
    }

    public DHModel(String node_Id, String admin_uid, HashMap<String, String> publicKeys,
                   HashMap<String, Boolean> members,  HashMap<String,
            String> signature, HashMap<String, String> aesMessageWithKey) {
        this.node_Id = node_Id;
        this.admin_uid = admin_uid;
        this.publicKeys = publicKeys;
        this.members = members;
        this.signature = signature;
        this.aesMessageWithKey = aesMessageWithKey;
    }

    public DHModel() {
    }

    public String getNode_Id() {
        return node_Id;
    }

    public HashMap<String, Boolean> getMembers() {
        return members;
    }

    public HashMap<String, String> getSignature() {
        return signature;
    }

    public HashMap<String, String> getAesMessageWithKey() {
        return aesMessageWithKey;
    }
}
