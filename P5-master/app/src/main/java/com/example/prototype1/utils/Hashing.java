package com.example.prototype1.utils;

import androidx.annotation.NonNull;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * @author Mo.Msaad
 * Class for hash verification, password/confirmed authenticity
 **/
public class Hashing {
    public Hashing() {
    }

    private final char[] specialChars = {'@', '#', '$', '%', '^', '&', '*', '-', '_', '!', '+', '=', '[', ']', '{', '}', '|', '\\', ':', '\'', ',', '.', '?', '/', '`', '~', '\"', '(', ')', ';'};

    public String mySHA256(@NonNull String input) throws NoSuchAlgorithmException {
        StringBuffer hexString = new StringBuffer();
        MessageDigest hashing = MessageDigest.getInstance("SHA-256");
        byte[] digest = hashing.digest(input.getBytes(StandardCharsets.UTF_8));
        for (int i = 0; i < digest.length; i++) {
            String hex = Integer.toHexString(0xff & digest[i]);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();

    }

    public String bytesToHex(byte[] hash) {
        StringBuffer hexString = new StringBuffer();
        for (int i = 0; i < hash.length; i++) {
            String hex = Integer.toHexString(0xff & hash[i]);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    public boolean hasDigits(@NonNull String input) {
        //todo continue
        boolean has = false;
        char spli[] = input.toCharArray();
        for (int i = 0; i < spli.length; i++) {
            if (Character.isDigit(spli[i]))
                has = true;
        }
        return has;

    }

    public boolean isLongEnough(@NonNull String input) {
        return input.length() >= 10 && input.length() < 32;
    }

    public boolean hasSpecial(@NonNull String input) {
        //todo continue
        boolean has = false;
        char spli[] = input.toCharArray();
        for (int i = 0; i < spli.length; i++) {
            for (int j = 0; j < specialChars.length; j++)
                if (specialChars[j] == (spli[i])) {
                    has = true;
                }
        }
        return has;

    }

    public boolean hasUpperCase(String input) {
        int i;
        boolean has = false;
        //todo continue
        char spli[] = input.toCharArray();
        for (i = 0; i < input.length(); i++)
            if (Character.isUpperCase(spli[i])) {
                has = true;
            }
        return has;
    }

}
