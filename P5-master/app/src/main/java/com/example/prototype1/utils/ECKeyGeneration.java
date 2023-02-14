package com.example.prototype1.utils;

import android.os.Build;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.crypto.KeyAgreement;

/**
 * @author Mohamed Msaad
 * Class used for Key exchange on firebase DB
 */

@RequiresApi(api = Build.VERSION_CODES.O)
public class ECKeyGeneration {
    private static final String TAG = "DiffieHilmanKeyProcess";
    private final static String AES = "AES";
    private final static String SHA_256 = "SHA-256";
    public final static int MY_DEFAULT = Base64.DEFAULT;

    private final MessageDigest hash = MessageDigest.getInstance(SHA_256);
    private KeyPairGenerator kpg;
    //    private KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC", "AndroidKeyStore");
    private KeyPair kp;
    private KeyAgreement ka;

    private static final String SecP256r1 = "secp256r1";

    private byte[] sharedSecret, pubKey;
    private PrivateKey privKey;

    public ECKeyGeneration() throws Exception {

    }

    private byte[] getSharedSecret() {
        return sharedSecret;
    }

    private void setSharedSecret(byte[] sharedSecret) {
        this.sharedSecret = sharedSecret;
    }

    private byte[] getPubKey() {
        return pubKey;
    }

    private void setPubKey(byte[] pubKey) {
        this.pubKey = pubKey;
    }

    private PrivateKey getPrivKey() {
        return privKey;
    }

    private void setPrivKey(PrivateKey privKey) {
        this.privKey = privKey;
    }

    /**
     * first generate key pair for user.
     * second read other party's public key
     * third agree on key
     * verify integrity
     */


    public static byte[] encodedBase64(byte[] input) {
        return Base64.encode(input, MY_DEFAULT);
    }

    public static byte[] decodedBase64(byte[] input) {
        return Base64.decode(input, MY_DEFAULT);
    }


    /**
     * @return publicKey for the user
     **/
    public byte[] myPublic() {

        try {
            ECGenParameterSpec ecGenParameterSpec = new ECGenParameterSpec(SecP256r1);
            kpg = KeyPairGenerator.getInstance("EC", "AndroidKeyStore");
            kpg.initialize(
                    new KeyGenParameterSpec.Builder(
                            "chatId",
                            KeyProperties.PURPOSE_SIGN | KeyProperties.PURPOSE_VERIFY)
                            .setAlgorithmParameterSpec(ecGenParameterSpec)
                            .setDigests(KeyProperties.DIGEST_SHA256,
                                    KeyProperties.DIGEST_SHA384,
                                    KeyProperties.DIGEST_SHA512)
                            // Only permit the private key to be used if the user authenticated
                            // within the last five minutes.
//                            .setUserAuthenticationRequired(true)
//                            .setUserAuthenticationValidityDurationSeconds(5 * 60)
                            .build());

        } catch (Exception e) {
            Log.d(TAG, "myPublic: " + e.getCause());
        }


        kp = kpg.generateKeyPair();


//                        kpg.initialize(256);
        return kp.getPublic().getEncoded();
    }


    //todo 1st person encrypt with both public and private;
    //todo
    public byte[] myPrivate() {

//        kpg.initialize(256);

        PrivateKey privatePk = kp.getPrivate();
        setPrivKey(privatePk);
        return privatePk.getEncoded();
    }

    //todo agree
    public byte[] agreeOnKey(byte[] otherPk) throws InvalidKeyException, NoSuchAlgorithmException, InvalidKeySpecException {
        try {
            KeyFactory kf = KeyFactory.getInstance("EC");
            X509EncodedKeySpec pkSpec = new X509EncodedKeySpec(otherPk);
            PublicKey otherPublicKey = kf.generatePublic(pkSpec);

            PKCS8EncodedKeySpec privateSpec = new PKCS8EncodedKeySpec(myPrivate());
            PrivateKey privateKey = kf.generatePrivate(privateSpec);

            ka = KeyAgreement.getInstance("ECDH");

            ka.init(privateKey);
            ka.doPhase(otherPublicKey, true);
            byte[] sharedSecret = ka.generateSecret();
            setSharedSecret(sharedSecret);
        } catch (Exception e) {
            Log.d(TAG, "agreeOnKey: " + e.getCause());
        }

        return sharedSecret;
    }

    public byte[] messageDigest(@NonNull byte[] ourPk, @NonNull byte[] otherPk) {
        // Read shared secret
        byte[] sharedSecret = getSharedSecret();
        // Derive a key from the shared secret and both public keys
        hash.update(sharedSecret);
        // Simple ordering
        List<ByteBuffer> keys = Arrays.asList(ByteBuffer.wrap(ourPk), ByteBuffer.wrap(otherPk));
        Collections.sort(keys);
        hash.update(keys.get(0));
        hash.update(keys.get(1));

        return hash.digest();
    }

//     TODO: 11/15/20 now simulate this on database


}