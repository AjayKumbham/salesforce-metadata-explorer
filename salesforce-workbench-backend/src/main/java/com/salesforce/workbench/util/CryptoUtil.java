package com.salesforce.workbench.util;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

public class CryptoUtil {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int IV_LENGTH_BYTES = 12;
    private static final int GCM_TAG_LENGTH_BITS = 128;

    private CryptoUtil() {
    }

    public static String encrypt(String rawValue, String secretKey) {
        if (rawValue == null) return null;
        try {
            byte[] iv = new byte[IV_LENGTH_BYTES];
            new SecureRandom().nextBytes(iv);

            SecretKeySpec key = generateKey(secretKey);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            byte[] encryptedBytes = cipher.doFinal(rawValue.getBytes(StandardCharsets.UTF_8));

            // Prepend IV to ciphertext: [IV (12 bytes)][ciphertext]
            byte[] combined = ByteBuffer.allocate(iv.length + encryptedBytes.length)
                    .put(iv)
                    .put(encryptedBytes)
                    .array();

            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new RuntimeException("Error encrypting value", e);
        }
    }

    public static String decrypt(String encryptedValue, String secretKey) {
        if (encryptedValue == null) return null;
        try {
            byte[] combined = Base64.getDecoder().decode(encryptedValue);

            // Extract IV from the beginning of the payload
            ByteBuffer byteBuffer = ByteBuffer.wrap(combined);
            byte[] iv = new byte[IV_LENGTH_BYTES];
            byteBuffer.get(iv);
            byte[] cipherText = new byte[byteBuffer.remaining()];
            byteBuffer.get(cipherText);

            SecretKeySpec key = generateKey(secretKey);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            byte[] decryptedBytes = cipher.doFinal(cipherText);

            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Error decrypting value", e);
        }
    }

    private static SecretKeySpec generateKey(String secretKey) {
        byte[] providedBytes = secretKey.getBytes(StandardCharsets.UTF_8);
        if (providedBytes.length < 32) {
            throw new IllegalArgumentException(
                "Encryption key must be at least 32 characters for AES-256 (got " + providedBytes.length + ")");
        }
        // Use exactly 32 bytes for AES-256
        byte[] keyBytes = new byte[32];
        System.arraycopy(providedBytes, 0, keyBytes, 0, 32);
        return new SecretKeySpec(keyBytes, "AES");
    }
}
