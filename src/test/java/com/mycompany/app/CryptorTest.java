package com.mycompany.app;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class CryptorTest {

    private Cryptor cryptor;

    @BeforeEach
    public void setUp() {
        cryptor = new Cryptor();
    }

    @Test
    public void testEncodeDecode() throws Exception {
        String secret = "mySecretPassword";
        String dataToEncrypt = "Hello, World!";

        String encryptedData = cryptor.encode(dataToEncrypt, secret);
        String decryptedData = cryptor.decode(encryptedData, secret);

        assertEquals(dataToEncrypt, decryptedData);
    }

    @Test
    public void testEncodeDecodeWithDifferentData() throws Exception {
        String secret = "anotherSecretPassword";
        String dataToEncrypt = "Different Data";

        String encryptedData = cryptor.encode(dataToEncrypt, secret);
        String decryptedData = cryptor.decode(encryptedData, secret);

        assertEquals(dataToEncrypt, decryptedData);
    }

    @Test
    public void testEncodeDecodeWithEmptyData() throws Exception {
        String secret = "emptyDataSecret";
        String dataToEncrypt = "";

        String encryptedData = cryptor.encode(dataToEncrypt, secret);
        String decryptedData = cryptor.decode(encryptedData, secret);

        assertEquals(dataToEncrypt, decryptedData);
    }

    @Test
    public void testEncodeDecodeWithSpecialCharacters() throws Exception {
        String secret = "specialCharSecret";
        String dataToEncrypt = "!@#$%^&*()_+{}|:\"<>?";

        String encryptedData = cryptor.encode(dataToEncrypt, secret);
        String decryptedData = cryptor.decode(encryptedData, secret);

        assertEquals(dataToEncrypt, decryptedData);
    }

    @Test
    public void testEncodeDecodeWithDifferentSecret() throws Exception {
        String secret = "mySecretPassword";
        String differentSecret = "differentSecretPassword";
        String dataToEncrypt = "Hello, World!";

        String encryptedData = cryptor.encode(dataToEncrypt, secret);
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            cryptor.decode(encryptedData, differentSecret);
        });

        String expectedMessage = "Invalid decryption key or corrupted data";
        String actualMessage = exception.getMessage();

        assertTrue(actualMessage.contains(expectedMessage));
    }
}