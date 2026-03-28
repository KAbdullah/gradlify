/**
 *
 * Provides AES encryption and decryption methods for secure storage of sensitive information,
 * such as AI API keys or personal data.
 *
 * Uses a symmetric key (AES-128) loaded from `application.properties` via:
 *     gradify.encryption.key=your-secret-16+ character key
 *
 * - The key must be at least 16 characters (128-bit AES).
 * - This utility is static and globally accessible after Spring injects the key.
 *
 *
 */

package com.example.backend.auth.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

@Component
public class EncryptionUtil {

    private static String key;

    @Value("${gradify.encryption.key:GradifyDefaultEncKey2026}")
    public void setKey(String keyValue) {
        // Ensure 16/24/32 bytes
        if (keyValue.length() < 16) {
            throw new IllegalArgumentException("Encryption key must be at least 16 characters long.");
        }
        key = keyValue.substring(0, 16);
    }

    public static String encrypt(String value) {
        try {
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key.getBytes(), "AES"));
            return Base64.getEncoder().encodeToString(cipher.doFinal(value.getBytes()));
        } catch (Exception e) {
            throw new RuntimeException("Encryption error", e);
        }
    }

    public static String decrypt(String encrypted) {
        try {
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key.getBytes(), "AES"));
            return new String(cipher.doFinal(Base64.getDecoder().decode(encrypted)));
        } catch (Exception e) {
            throw new RuntimeException("Decryption error", e);
        }
    }
}
