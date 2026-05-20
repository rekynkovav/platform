package com.platform.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class EncryptionServiceTest {

    private EncryptionService encryptionService;

    @BeforeEach
    void setUp() {
        encryptionService = new EncryptionService();
        ReflectionTestUtils.setField(encryptionService, "masterKey", "test-master-key-32-bytes-long!!!");
        ReflectionTestUtils.setField(encryptionService, "algorithm", "AES/CBC/PKCS5Padding");
    }

    @Test
    void testGenerateFileKey() {
        byte[] key1 = encryptionService.generateFileKey("user123", "file456");
        byte[] key2 = encryptionService.generateFileKey("user123", "file456");

        assertNotNull(key1);
        assertEquals(32, key1.length);
        assertArrayEquals(key1, key2);
    }

    @Test
    void testGenerateFileKeyDifferentUsers() {
        byte[] key1 = encryptionService.generateFileKey("user123", "file456");
        byte[] key2 = encryptionService.generateFileKey("user789", "file456");

        assertNotNull(key1);
        assertNotNull(key2);
        assertFalse(java.util.Arrays.equals(key1, key2));
    }

    @Test
    void testGenerateIV() {
        String iv1 = encryptionService.generateIV();
        String iv2 = encryptionService.generateIV();

        assertNotNull(iv1);
        assertNotNull(iv2);
        assertNotEquals(iv1, iv2);
    }

    @Test
    void testEncryptAndDecrypt() {
        byte[] originalData = "Hello, World!".getBytes();
        byte[] key = encryptionService.generateFileKey("user123", "file456");
        String iv = encryptionService.generateIV();

        byte[] encrypted = encryptionService.encrypt(originalData, key, iv);
        byte[] decrypted = encryptionService.decrypt(encrypted, key, iv);

        assertNotNull(encrypted);
        assertNotEquals(java.util.Arrays.toString(originalData), java.util.Arrays.toString(encrypted));
        assertArrayEquals(originalData, decrypted);
    }

    @Test
    void testGenerateHLSKey() {
        byte[] hlsKey = encryptionService.generateHLSKey("user123", "file456");

        assertNotNull(hlsKey);
        assertEquals(16, hlsKey.length);
    }
}