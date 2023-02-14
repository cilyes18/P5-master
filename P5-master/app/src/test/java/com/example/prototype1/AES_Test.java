package com.example.prototype1;

import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import org.junit.Test;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.spec.AlgorithmParameterSpec;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

import static android.security.keystore.KeyProperties.BLOCK_MODE_GCM;
import static android.security.keystore.KeyProperties.DIGEST_SHA256;
import static android.security.keystore.KeyProperties.ENCRYPTION_PADDING_NONE;
import static android.security.keystore.KeyProperties.KEY_ALGORITHM_AES;
import static org.junit.Assert.*;


public class AES_Test {
    public static final String TRANSFORMATION = KEY_ALGORITHM_AES + "/"
            + BLOCK_MODE_GCM + "/" + ENCRYPTION_PADDING_NONE;
    private KeyGenerator keyGenerator;

    public byte[] iv;

    @Test
    public void AES_GCM_Test() throws Exception {
        String textToEncrypt = "hello";

        keyGenerator = KeyGenerator.getInstance(KEY_ALGORITHM_AES);
        keyGenerator.init(256);

        SecretKey secretKeyToUse = keyGenerator.generateKey();

        final Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, secretKeyToUse);
        iv = cipher.getIV();
        byte[] cipherText = cipher.doFinal(textToEncrypt.getBytes(StandardCharsets.UTF_8));

        ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + cipherText.length);
        byteBuffer.put(iv);
        byteBuffer.put(cipherText);
        byte[] arrayToTest = byteBuffer.array();
        //done Encrypting

        //decrypting
        final Cipher decipher = Cipher.getInstance(TRANSFORMATION);
        AlgorithmParameterSpec gcmIv = new GCMParameterSpec(128, arrayToTest, 0, 12);
        decipher.init(Cipher.DECRYPT_MODE, secretKeyToUse, gcmIv);

        byte[] plainText = decipher.doFinal(arrayToTest, 12, arrayToTest.length - 12);

        String endResult = new String(plainText, StandardCharsets.UTF_8);
        assertEquals(endResult, textToEncrypt);
    }
}