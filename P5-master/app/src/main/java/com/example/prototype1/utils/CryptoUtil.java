package com.example.prototype1.utils;

import android.content.Context;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.security.keystore.KeyProtection;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.prototype1.R;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import java.security.interfaces.ECPublicKey;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyAgreement;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import static android.security.keystore.KeyProperties.BLOCK_MODE_GCM;
import static android.security.keystore.KeyProperties.DIGEST_SHA256;
import static android.security.keystore.KeyProperties.ENCRYPTION_PADDING_NONE;
import static android.security.keystore.KeyProperties.KEY_ALGORITHM_AES;
import static android.security.keystore.KeyProperties.KEY_ALGORITHM_EC;
import static android.util.Base64.DEFAULT;
import static java.nio.charset.StandardCharsets.UTF_8;

public class CryptoUtil {
    private static final String TAG = "CryptoUtil";
    //"AES/GCM/NoPadding" - is used for setting the algorithm which will be used for encoding.
    public static final String TRANSFORMATION = KEY_ALGORITHM_AES + "/"
            + BLOCK_MODE_GCM + "/" + ENCRYPTION_PADDING_NONE;
    private static final String ANDROID_KEY_STORE = "AndroidKeyStore";
    private static final String SHA256signature = "SHA256withECDSA";
    private static final String SHA_256 = DIGEST_SHA256;
    private static final String SecP256r1 = "secp256r1";
    private static final String ECDH = "ECDH";
    private final MessageDigest sha256 = MessageDigest.getInstance(SHA_256);

    private KeyPairGenerator kpg;
    private KeyPair kp;
    private final KeyFactory kf = KeyFactory.getInstance(KEY_ALGORITHM_EC);
    private final KeyAgreement ka = KeyAgreement.getInstance(ECDH);
    private final ECGenParameterSpec ecGenParameterSpec = new ECGenParameterSpec(SecP256r1);
    private Context mContext;

    // iv is known as Initialization Vector which is an arbitrary number used along with a secret key for encryption.
    private byte[] iv;
    private byte[] aesKeyForMessage;

    private KeyStore keyStore;

    public CryptoUtil(Context context) throws NoSuchAlgorithmException {
        try {
            initKeyStore();
            kpg = keyPGenerator();
            kp = generateECKeyPair();
            mContext = context;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initKeyStore() throws KeyStoreException, CertificateException,
            NoSuchAlgorithmException, IOException {
        keyStore = KeyStore.getInstance(ANDROID_KEY_STORE);
        keyStore.load(null);
    }

    private String encryptText(String string_to_encrypt, String messageAlias) {
        try {
            final byte[] encryptedText = encryptData(string_to_encrypt, messageAlias);
            return Base64.encodeToString(encryptedText, DEFAULT);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "The Message could not be encrypted";
    }

    private byte[] encryptData(final String textToEncrypt, String messageAlias) throws Exception {
        final Cipher cipher = Cipher.getInstance(TRANSFORMATION);

        aesKeyForMessage = createAESKeyToShare();
        cipher.init(Cipher.ENCRYPT_MODE, addAESKeyToAndroidKeyStore(aesKeyForMessage, messageAlias));
        //        cipher.init(Cipher.ENCRYPT_MODE, generateNewSecretKey(messageAlias));


        iv = cipher.getIV();
        byte[] cipherText = cipher.doFinal(textToEncrypt.getBytes(UTF_8));

        ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + cipherText.length);
        byteBuffer.put(iv);
        byteBuffer.put(cipherText);
        return byteBuffer.array();
    }

    private String decryptText(String encrypted, String messageAlias) {
        try {
            return decryptData(encrypted, messageAlias);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "The Message could not be decrypted";
    }

    private String decryptData(String encrypted, String messageAlias)
            throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException,
            BadPaddingException, IllegalBlockSizeException, InvalidAlgorithmParameterException,
            UnrecoverableEntryException, KeyStoreException {

        final Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        byte[] cipherMessage = Base64.decode(encrypted, DEFAULT);
        AlgorithmParameterSpec gcmIv = new GCMParameterSpec(128, cipherMessage, 0, 12);
        cipher.init(Cipher.DECRYPT_MODE, getSecretKeyForDecryption(messageAlias), gcmIv);

        byte[] plainText = cipher.doFinal(cipherMessage, 12, cipherMessage.length - 12);

        return new String(plainText, UTF_8);
    }

    @NonNull
    private SecretKey generateNewSecretKey(String keyAlias) throws NoSuchAlgorithmException,
            NoSuchProviderException, InvalidAlgorithmParameterException {
        final KeyGenerator keyGenerator;

        keyGenerator = KeyGenerator.getInstance(KEY_ALGORITHM_AES, ANDROID_KEY_STORE);
        keyGenerator.init(new KeyGenParameterSpec.Builder(keyAlias,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setKeySize(256)
                .setBlockModes(BLOCK_MODE_GCM)
                .setEncryptionPaddings(ENCRYPTION_PADDING_NONE)
                .setUserAuthenticationRequired(true)
                .setUserAuthenticationValidityDurationSeconds(60 * 60)
                .build());

        return keyGenerator.generateKey();
    }

    public SecretKey getSecretKeyForDecryption(String keyAlias) throws NoSuchAlgorithmException,
            UnrecoverableEntryException, KeyStoreException {

        KeyStore.SecretKeyEntry entry = (KeyStore.SecretKeyEntry) keyStore.getEntry(keyAlias, null);
        if (entry != null) {
            return (entry).getSecretKey();
        } else {
            return null;
        }
    }

    public boolean checkSecretKeyForDecryption(String keyAlias) throws NoSuchAlgorithmException,
            UnrecoverableEntryException, KeyStoreException {

        KeyStore.SecretKeyEntry entry = (KeyStore.SecretKeyEntry) keyStore.getEntry(keyAlias, null);
        return entry == null;
    }

    public List<String> encryptMessage(String content, String messageAlias, String chatIdAlias) {
        List<String> messageAndKey = new ArrayList<>();
        String message = encryptText(content, messageAlias);
        String AESEncryptedWithSessionKey = encryptMessageKeyWithSessionKey(chatIdAlias);

        messageAndKey.add(message);
        messageAndKey.add(AESEncryptedWithSessionKey);

        return messageAndKey;
    }

    public String startDecrypting(String content, String messageAlias) {
        return decryptText(content, messageAlias);
    }

    public static String getCurrentTimestamp(Context mContext) {
        SimpleDateFormat sdf = new SimpleDateFormat(mContext.getString(R.string.timestamp_pattern), Locale.ENGLISH);
        sdf.setTimeZone(TimeZone.getTimeZone("Europe/Copenhagen"));
        return sdf.format(new Date());
    }

    private KeyPair generateECKeyPair() {
        try {
            kp = kpg.generateKeyPair();
        } catch (Exception e) {
            Log.d(TAG, "myPublic: " + e.getCause());
        }
        return kp;
    }

    private KeyPairGenerator keyPGenerator() throws Exception {
        kpg = KeyPairGenerator.getInstance("EC");
        kpg.initialize(ecGenParameterSpec, new SecureRandom());

        return kpg;
    }

    private KeyPairGenerator keyPGeneratorKeyStore(String alias) throws Exception {
        kpg = KeyPairGenerator.getInstance("EC", "AndroidKeyStore");
        kpg.initialize(
                new KeyGenParameterSpec.Builder(
                        alias,
                        KeyProperties.PURPOSE_SIGN | KeyProperties.PURPOSE_VERIFY)
                        .setAlgorithmParameterSpec(ecGenParameterSpec)
                        .setRandomizedEncryptionRequired(true)
                        .setDigests(DIGEST_SHA256,
                                KeyProperties.DIGEST_SHA384,
                                KeyProperties.DIGEST_SHA512)
                        // Only permit the private key to be used if the user authenticated
                        // within the last five minutes.
                        .setUserAuthenticationRequired(true)
                        .setUserAuthenticationValidityDurationSeconds(5 * 60)
                        .build());

        return kpg;
    }

    public byte[] myPublic() {
        ECPublicKey ecPublicKey = (ECPublicKey) kp.getPublic();

        return ecPublicKey.getEncoded();
    }

    public PrivateKey myPrivate() {
        return kp.getPrivate();
    }

    public byte[] agreeOnKey(byte[] otherPk) {
        byte[] sharedSecret = new byte[0];

        try {
            X509EncodedKeySpec pkSpec = new X509EncodedKeySpec(otherPk);
            PublicKey otherPublicKey = kf.generatePublic(pkSpec);

            PrivateKey privateKey = myPrivate();

            ka.init(privateKey);
            ka.doPhase(otherPublicKey, true);
            sharedSecret = ka.generateSecret();

        } catch (Exception e) {
            Log.d(TAG, "agreeOnKey: " + e.getCause());
        }

        return sharedSecret;
    }

    //todo turn this into encrypting aes key with the Secret/Session Key
    private byte[] aesOutOfSharedSecret(String chatIdAlias) throws Exception {
        SecretKey sessionAESKey = getSecretKeyForDecryption(chatIdAlias);

        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, sessionAESKey);
        iv = cipher.getIV();
        byte[] cipherText = cipher.doFinal(aesKeyForMessage);

        //returnItToNull
        aesKeyForMessage = null;

        ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + cipherText.length);
        byteBuffer.put(iv);
        byteBuffer.put(cipherText);
        return byteBuffer.array();
    }

    public String encryptMessageKeyWithSessionKey(String chatIdAlias) {
        try {
            final byte[] encryptedAESKeyWithSessionKey = aesOutOfSharedSecret(chatIdAlias);
            return Base64.encodeToString(encryptedAESKeyWithSessionKey, DEFAULT);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "The Message could not be encrypted";
    }

    public void decryptMessageKeFromSessionKey(String encryptedAESKey, String chatIdAlias, String messageAlias) {
        try {
            retrieveAESKey(encryptedAESKey, chatIdAlias, messageAlias);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void retrieveAESKey(String encrypted, String chatIdAlias, String messageAlias) throws Exception {

        final Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        byte[] cipherMessage = Base64.decode(encrypted, DEFAULT);

        AlgorithmParameterSpec gcmIv = new GCMParameterSpec(128, cipherMessage, 0, 12);

        cipher.init(Cipher.DECRYPT_MODE, getSecretKeyForDecryption(chatIdAlias), gcmIv);

        byte[] retrievedAES = cipher.doFinal(cipherMessage, 12, cipherMessage.length - 12);

        addAESKeyToAndroidKeyStore(retrievedAES, messageAlias );
    }

    public String signData(String myPubKey) {
        String signString = null;
        try {
            PKCS8EncodedKeySpec privateSpec = new PKCS8EncodedKeySpec(myPrivate().getEncoded());
            PrivateKey privateKey = kf.generatePrivate(privateSpec);

            //at sender's end
            Signature ecdsaSign = Signature.getInstance(SHA256signature);
            ecdsaSign.initSign(privateKey);
            ecdsaSign.update(myPubKey.getBytes(UTF_8));

            byte[] signature = ecdsaSign.sign();
            signString = Base64.encodeToString(signature, DEFAULT);

        } catch (Exception e) {
            Log.d(TAG, "agreeOnKey: " + e.getCause());
        }
        return signString;
    }

    public boolean verifySigned(String otherPk, String signature) {
        try {
            // at receiver's end
            Signature ecdsaVerify = Signature.getInstance(SHA256signature);

            X509EncodedKeySpec pkSpec = new X509EncodedKeySpec(Base64.decode(otherPk, DEFAULT));
            PublicKey publicKey = KeyFactory.getInstance("EC").generatePublic(pkSpec);

            ecdsaVerify.initVerify(publicKey);
            //using otherPerson's PubKey as text for signature as instructed in Pub Key Exchange.
            ecdsaVerify.update(otherPk.getBytes(UTF_8));
            return ecdsaVerify.verify(Base64.decode(signature, DEFAULT));
        } catch (Exception e) {
            Log.d(TAG, "agreeOnKey: " + e.getCause());
        }

        return false;
    }

    public void saveSharedKey(@NonNull byte[] otherPublicKey, @NonNull String alias) throws Exception {
        byte[] sk = agreeOnKey(otherPublicKey); //creating shared secret key
        // derive a key out of the digest value of the shared secret

        SecretKey secretKey = new SecretKeySpec(sk, KEY_ALGORITHM_AES);

        //saving key to keystore with alias
        KeyStore.SecretKeyEntry skEntry = new KeyStore.SecretKeyEntry(secretKey);
        KeyProtection protectionParameter = new KeyProtection.Builder(KeyProperties.PURPOSE_ENCRYPT
                | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setUserAuthenticationRequired(true)
                .setUserAuthenticationValidityDurationSeconds(365 * 24 * 60 * 60)
                .build();


        // try with null and see for testing prototype only. Later will be used with biometric authentication
        keyStore.setEntry(alias, skEntry, protectionParameter);
        Log.d(TAG, "saveSharedKey: keyEntry exists: " + keyStore.isKeyEntry(alias));
    }

    public SecretKey addAESKeyToAndroidKeyStore(@NonNull byte[] aesKey, @NonNull String messageAlias) throws Exception {
        SecretKey secretKey = new SecretKeySpec(aesKey, KEY_ALGORITHM_AES);

        //saving key to keystore with messageAlias
        KeyStore.SecretKeyEntry skEntry = new KeyStore.SecretKeyEntry(secretKey);
        KeyProtection protectionParameter = new KeyProtection.Builder(KeyProperties.PURPOSE_ENCRYPT
                | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setUserAuthenticationRequired(true)
                .setUserAuthenticationValidityDurationSeconds(365 * 24 * 60 * 60)
                .build();

        keyStore.setEntry(messageAlias, skEntry, protectionParameter);

        return secretKey;
    }


    public byte[] createAESKeyToShare() throws Exception {
        KeyGenerator kg = KeyGenerator.getInstance(KEY_ALGORITHM_AES);
        kg.init(256, new SecureRandom());
        SecretKey createAESKey = kg.generateKey();
        return createAESKey.getEncoded();
    }

}
