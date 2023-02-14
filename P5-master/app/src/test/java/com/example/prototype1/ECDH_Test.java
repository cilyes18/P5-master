package com.example.prototype1;

import android.security.keystore.KeyProperties;

import org.junit.Test;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.KeyAgreement;

import static org.junit.Assert.assertArrayEquals;

public class ECDH_Test {

    private KeyAgreement ka, ka1;
    private KeyPairGenerator kpg;
    private ECGenParameterSpec ecGenParameterSpec = new ECGenParameterSpec(SecP256r1);
    private KeyFactory kf;

    private static final String SecP256r1 = "secp256r1";
    private static final String SHA_256 = "SHA-256";

    @Test
    public void privateKeyTest() throws Exception {
        byte[] shared1;
        byte[] shared2;
        ka = KeyAgreement.getInstance("ECDH");
        ka1 = KeyAgreement.getInstance("ECDH");
        kf = KeyFactory.getInstance("EC");

        kpg = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC);
        KeyPairGenerator kpg2 = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC);

        kpg.initialize(ecGenParameterSpec, new SecureRandom());
        kpg2.initialize(ecGenParameterSpec, new SecureRandom());

        KeyPair pair1 = kpg.generateKeyPair();
        KeyPair pair2 = kpg2.generateKeyPair();

//            firstPair
        ECPublicKey ecPublicKey1 = (ECPublicKey) pair1.getPublic();
        PrivateKey privateKey1Enc = pair1.getPrivate();

        byte[] pubEnc1 = ecPublicKey1.getEncoded();
        byte[] privEnc1 = privateKey1Enc.getEncoded();

//            secondPair
        ECPublicKey ecPublicKey2 = (ECPublicKey) pair2.getPublic();
        PrivateKey privateKey2Enc = pair2.getPrivate();

        byte[] pubEnc2 = ecPublicKey2.getEncoded();
        byte[] privEnc2 = privateKey2Enc.getEncoded();

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

        ka.init(privateKey1);
        ka.doPhase(otherPublicKey2, true);
        shared1 = ka.generateSecret();

        ka1.init(privateKey2);
        ka1.doPhase(otherPublicKey1, true);
        shared2 = ka1.generateSecret();

        assertArrayEquals(shared1, shared2);
    }

    @Test
    public void ecPrivKeyTest() throws Exception {
        byte[] shared1;
        byte[] shared2;
        ka = KeyAgreement.getInstance("ECDH");
        ka1 = KeyAgreement.getInstance("ECDH");
        kf = KeyFactory.getInstance("EC");

        kpg = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC);
        KeyPairGenerator kpg2 = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC);

        kpg.initialize(ecGenParameterSpec, new SecureRandom());
        kpg2.initialize(ecGenParameterSpec, new SecureRandom());

        KeyPair pair1 = kpg.generateKeyPair();
        KeyPair pair2 = kpg2.generateKeyPair();

//            firstPair
        ECPublicKey ecPublicKey1 = (ECPublicKey) pair1.getPublic();
        ECPrivateKey ecPrivateKey1 = (ECPrivateKey) pair1.getPrivate();

        byte[] pubEnc1 = ecPublicKey1.getEncoded();

//            secondPair
        ECPublicKey ecPublicKey2 = (ECPublicKey) pair2.getPublic();
        ECPrivateKey ecPrivateKey2 = (ECPrivateKey) pair2.getPrivate();

        byte[] pubEnc2 = ecPublicKey2.getEncoded();

//          secondPair Regeneration
        X509EncodedKeySpec pkSpec1 = new X509EncodedKeySpec(pubEnc1);
        PublicKey otherPublicKey1 = kf.generatePublic(pkSpec1);

//          firstPair Regeneration
        X509EncodedKeySpec pkSpec2 = new X509EncodedKeySpec(pubEnc2);
        PublicKey otherPublicKey2 = kf.generatePublic(pkSpec2);

        ka.init(ecPrivateKey1);
        ka.doPhase(otherPublicKey2, true);
        shared1 = ka.generateSecret();

        ka1.init(ecPrivateKey2);
        ka1.doPhase(otherPublicKey1, true);
        shared2 = ka1.generateSecret();

        MessageDigest sha256 = MessageDigest.getInstance(SHA_256);
        byte[] digestedSharedKey1 = sha256.digest(shared1);
        byte[] digestedSharedKey2 = sha256.digest(shared2);

        assertArrayEquals(shared1, shared2);
        assertArrayEquals(digestedSharedKey1, digestedSharedKey2);
    }

   /*
    @Test
    public void testEncryptDecrypt() throws Exception {

        final String name = "secp256r1";

        // NOTE just "EC" also seems to work here
        final KeyPairGenerator kpg = KeyPairGenerator.getInstance("ECDH", BouncyCastleProvider.PROVIDER_NAME);
        kpg.initialize(new ECGenParameterSpec(name));

        // Key pair to store public and private key
        final KeyPair keyPair = kpg.generateKeyPair();

        // Message to encrypt
        byte[] message = "hello".getBytes(StandardCharsets.UTF_8);

        // Encrypt
        final BigInteger r = EciesEncryption.generateR();
        byte[] encrypted = EciesEncryption.encrypt(message, r, keyPair.getPublic());

        // Decrypt
        byte[] decrypted = EciesEncryption.decrypt(encrypted, keyPair.getPrivate());
        System.out.println("Decrypted message: " + new String(decrypted));

        assertArrayEquals(message, decrypted);

    }*/


        /*

        public void test() {
        ECPrivateKeyParameters privKeyA = new ECPrivateKeyParameters(
                new BigInteger(1, HexUtil.decode("8653b44d4acebec2cd64a015b2e509c70c9049a692e71b08fe7f52cc1fa5595f")), CURVE);
        ECDHBasicAgreement agreement = new ECDHBasicAgreement();
        agreement.init(privKeyA);
        ECPublicKeyParameters pubKeyB = new ECPublicKeyParameters(
                CURVE.getCurve().decodePoint(HexUtil.decode("02fd82681e79fbe293aef1a48c6c9b1252591340bb46de1444ad5de400ff84a433")), CURVE);
        BigInteger result = agreement.calculateAgreement(pubKeyB);
        byte[] sharedSecret = BigIntegers.asUnsignedByteArray(agreement.getFieldSize(), result);
        System.out.println(HexUtil.encode(sharedSecret));
        // sharedSecret hex string: 692c40fdbe605b9966beee978ab290e7a35056dffe9ed092a87e62fce468791d
    }*/


    /*
     * @Test public void testRS256() throws Exception {
     * <p>
     * KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
     * keyGen.initialize(2048);
     * KeyPair pair = keyGen.generateKeyPair();
     * PrivateKey priv = pair.getPrivate();
     * PublicKey pub = pair.getPublic();
     * <p>
     * String jwsToken = Jwts.builder().setSubject("Leonard McCoy").signWith(SignatureAlgorithm.RS256, priv).compact();
     * Settings settings = Settings.builder().put("signing_key", "-----BEGIN PUBLIC KEY-----\n" + BaseEncoding.base64().encode(pub.getEncoded()) + "-----END PUBLIC KEY-----").build();
     * <p>
     * HTTPJwtAuthenticator jwtAuth = new HTTPJwtAuthenticator(settings, null);
     * Map<String, String> headers = new HashMap<String, String>();
     * headers.put("Authorization", "Bearer " + jwsToken);
     * <p>
     * AuthCredentials creds = jwtAuth.extractCredentials(new FakeRestRequest(headers, new HashMap<String, String>()), null);
     * Assert.assertNotNull(creds);
     * Assert.assertEquals("Leonard McCoy", creds.getUsername());
     * Assert.assertEquals(0, creds.getBackendRoles().size());
     * }
     */

}
