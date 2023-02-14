package com.example.prototype1.crypto;

import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class Encrypto {
    private static final String TAG = "Encrypto";

    public Encrypto() throws Exception {
    }

    //recommended algo AES/GCM/NoPadding
    private static Key keyBytes() throws NoSuchAlgorithmException {
        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        keyGenerator.init(256);
//      SecretKey key = keyGenerator.generateKey();
//      Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
        return keyGenerator.generateKey();
    }


    private static Cipher myCipher() throws Exception {
        return Cipher.getInstance("AES/GCM/NoPadding");
    }

    public byte[] encryptData(byte[] input) throws Exception {
        Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
        byte[] iv = new byte[16];
        new SecureRandom().nextBytes(iv);
        c.init(Cipher.ENCRYPT_MODE, keyBytes(), new IvParameterSpec(iv));
        return myCipher().doFinal(input);
    }

    public byte[] decryptData(byte[] input) throws Exception {
        String encryptionKeyString = "thisisa128bitkey";
        byte[] encryptionKeyBytes = encryptionKeyString.getBytes();
        SecretKey secretKey = new SecretKeySpec(encryptionKeyBytes, "AES");
        Cipher c = Cipher.getInstance("AES/ECB/PKCS5Padding");
        byte[] iv = new byte[16];
        new SecureRandom().nextBytes(iv);
        c.init(Cipher.DECRYPT_MODE, secretKey);
        return c.doFinal(input);
    }

    public byte[] enc(String message) throws Exception {

        String encryptionKeyString = "thisisa128bitkey";
        byte[] encryptionKeyBytes = encryptionKeyString.getBytes();
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        SecretKey secretKey = new SecretKeySpec(encryptionKeyBytes, "AES");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);

        byte[] encryptedMessageBytes = cipher.doFinal(message.getBytes());

        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        return encryptedMessageBytes;
    }


}
