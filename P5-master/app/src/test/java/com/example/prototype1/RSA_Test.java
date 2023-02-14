package com.example.prototype1;

import android.security.keystore.KeyProperties;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.Cipher;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class RSA_Test {

    private static final String RSA_ALGORITHM = KeyProperties.KEY_ALGORITHM_RSA;
    private KeyPairGenerator kpg;
    private KeyFactory kf;


    @Test
    public void RSA_Test() throws Exception {
        kf = KeyFactory.getInstance(RSA_ALGORITHM);

        kpg = KeyPairGenerator.getInstance(RSA_ALGORITHM);
        KeyPairGenerator kpg2 = KeyPairGenerator.getInstance(RSA_ALGORITHM);

        KeyPair pair1 = kpg.generateKeyPair();
        KeyPair pair2 = kpg2.generateKeyPair();

//            firstPair
//        PublicKey ecPublicKey1 =  pair1.getPublic();
//        PrivateKey privateKey1Enc = pair1.getPrivate();
        RSAPublicKey rsaPublicKey1 = (RSAPublicKey) pair1.getPublic();
        RSAPrivateKey rsaPrivateKey1 = (RSAPrivateKey) pair1.getPrivate();

        byte[] pubEnc1 = rsaPublicKey1.getEncoded();
        byte[] privEnc1 = rsaPrivateKey1.getEncoded();

//            secondPair
        //        PublicKey ecPublicKey2 =  pair2.getPublic();
//        PrivateKey privateKey2Enc = pair2.getPrivate();
        RSAPublicKey rsaPublicKey2 = (RSAPublicKey) pair2.getPublic();
        RSAPrivateKey rsaPrivateKey2Enc = (RSAPrivateKey) pair2.getPrivate();

        byte[] pubEnc2 = rsaPublicKey2.getEncoded();
        byte[] privEnc2 = rsaPrivateKey2Enc.getEncoded();

//          secondPair Regeneration
        X509EncodedKeySpec pkSpec1 = new X509EncodedKeySpec(pubEnc1);
        PublicKey otherPublicKey1 = kf.generatePublic(pkSpec1);

//          firstPair Regeneration
        X509EncodedKeySpec pkSpec2 = new X509EncodedKeySpec(pubEnc2);
        PublicKey otherPublicKey2 = kf.generatePublic(pkSpec2);


        PKCS8EncodedKeySpec privateSpec1 = new PKCS8EncodedKeySpec(privEnc1);
        PKCS8EncodedKeySpec privateSpec2 = new PKCS8EncodedKeySpec(privEnc2);

        PrivateKey privateKey1 = kf.generatePrivate(privateSpec1);
        PrivateKey privateKey2 = kf.generatePrivate(privateSpec2);

        String initialData = "testdata";


        Cipher cipherToEncode = Cipher.getInstance(RSA_ALGORITHM);
        cipherToEncode.init(Cipher.ENCRYPT_MODE, rsaPublicKey1);
        byte[] encodedB = cipherToEncode.doFinal(initialData.getBytes(StandardCharsets.UTF_8));

        Cipher cipherToDecode = Cipher.getInstance(RSA_ALGORITHM);
        cipherToDecode.init(Cipher.DECRYPT_MODE, rsaPrivateKey1);
        byte[] decodedB = cipherToDecode.doFinal(encodedB);
        String testResult = new String(decodedB, StandardCharsets.UTF_8);


        assertEquals(initialData, testResult);
        assertArrayEquals(rsaPrivateKey1.getEncoded(), privateKey1.getEncoded());
    }
}