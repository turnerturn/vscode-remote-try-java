package com.mycompany.app;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.stereotype.Component;

import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;

@Component
public class Cryptor {

    private static final String ALGORITHM = "AES";

    private SecretKeySpec getKey(String secret) throws Exception {
        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        byte[] key = secret.getBytes("UTF-8");
        key = sha.digest(key);
        key = Arrays.copyOf(key, 16); // Use only the first 16 bytes (128 bits)
        return new SecretKeySpec(key, ALGORITHM);
    }

    public String encode(String data, String secret) throws Exception {
        SecretKeySpec key = getKey(secret);
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, key);
        byte[] encrypted = cipher.doFinal(data.getBytes());
        return Base64.getEncoder().encodeToString(encrypted);
    }

    public String decode(String encryptedData, String secret) throws Exception {
        try {
            SecretKeySpec key = getKey(secret);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, key);
            byte[] decodedBytes = Base64.getDecoder().decode(encryptedData);
            byte[] decrypted = cipher.doFinal(decodedBytes);
            return new String(decrypted);
        } catch (javax.crypto.BadPaddingException e) {
            // Handle the exception gracefully
            throw new IllegalArgumentException("Invalid decryption key or corrupted data", e);
        }
    }
}