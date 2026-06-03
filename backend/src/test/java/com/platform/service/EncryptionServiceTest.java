package com.platform.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.Cipher;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class EncryptionServiceTest {

    private EncryptionService encryptionService;

    @BeforeEach
    void setUp() {
        encryptionService = new EncryptionService();
        ReflectionTestUtils.setField(encryptionService, "algorithm", "AES/CBC/PKCS5Padding");
    }

    // ==================== generateIV ====================

    @Test
    void generateIV_ReturnsBase64Encoded16Bytes() {
        String iv = encryptionService.generateIV();

        assertNotNull(iv);
        byte[] decoded = Base64.getDecoder().decode(iv);
        assertEquals(16, decoded.length);
    }

    @Test
    void generateIV_EachCallProducesUniqueValue() {
        String iv1 = encryptionService.generateIV();
        String iv2 = encryptionService.generateIV();

        assertNotEquals(iv1, iv2);
    }

    // ==================== encrypt / decrypt roundtrip ====================

    @Test
    void encryptDecrypt_Roundtrip_ReturnsOriginalData() {
        byte[] key = new byte[32]; // 256-bit AES key
        for (int i = 0; i < 32; i++) key[i] = (byte) i;

        String iv = encryptionService.generateIV();
        byte[] plaintext = "Hello, encrypted world!".getBytes(StandardCharsets.UTF_8);

        byte[] encrypted = encryptionService.encrypt(plaintext, key, iv);
        assertNotNull(encrypted);
        assertFalse(java.util.Arrays.equals(plaintext, encrypted));

        byte[] decrypted = encryptionService.decrypt(encrypted, key, iv);
        assertArrayEquals(plaintext, decrypted);
    }

    @Test
    void encrypt_DifferentIV_ProducesDifferentCiphertext() {
        byte[] key = new byte[32];
        byte[] plaintext = "Same data".getBytes(StandardCharsets.UTF_8);

        String iv1 = encryptionService.generateIV();
        String iv2 = encryptionService.generateIV();

        byte[] enc1 = encryptionService.encrypt(plaintext, key, iv1);
        byte[] enc2 = encryptionService.encrypt(plaintext, key, iv2);

        assertFalse(java.util.Arrays.equals(enc1, enc2));
    }

    @Test
    void decrypt_WrongKey_ThrowsException() {
        byte[] key1 = new byte[32];
        byte[] key2 = new byte[32];
        key2[0] = 1; // different key

        String iv = encryptionService.generateIV();
        byte[] plaintext = "Secret".getBytes(StandardCharsets.UTF_8);

        byte[] encrypted = encryptionService.encrypt(plaintext, key1, iv);

        assertThrows(RuntimeException.class, () ->
                encryptionService.decrypt(encrypted, key2, iv));
    }

    @Test
    void decrypt_CorruptedData_ThrowsException() {
        byte[] key = new byte[32];
        String iv = encryptionService.generateIV();
        byte[] corrupted = new byte[]{1, 2, 3, 4, 5};

        assertThrows(RuntimeException.class, () ->
                encryptionService.decrypt(corrupted, key, iv));
    }

    // ==================== generateFileKey / generateHLSKey ====================

    @Test
    void generateFileKey_ReturnsDeterministicKey() {
        byte[] key1 = encryptionService.generateFileKey("user-1", "file-1");
        byte[] key2 = encryptionService.generateFileKey("user-1", "file-1");

        assertNotNull(key1);
        assertArrayEquals(key1, key2);
    }

    @Test
    void generateFileKey_DifferentInputs_ProduceDifferentKeys() {
        byte[] key1 = encryptionService.generateFileKey("user-1", "file-1");
        byte[] key2 = encryptionService.generateFileKey("user-2", "file-1");

        assertFalse(java.util.Arrays.equals(key1, key2));
    }

    @Test
    void generateHLSKey_Returns16Bytes() {
        byte[] hlsKey = encryptionService.generateHLSKey("user-1", "file-1");

        assertNotNull(hlsKey);
        assertEquals(16, hlsKey.length);
    }

    // ==================== getCipher ====================

    @Test
    void getCipher_EncryptMode_ReturnsInitializedCipher() throws Exception {
        byte[] key = new byte[16];
        String iv = "0123456789abcdef"; // exactly 16 chars for raw IV bytes

        Cipher cipher = encryptionService.getCipher(Cipher.ENCRYPT_MODE, key, iv);

        assertNotNull(cipher);
        // Verify cipher is initialized by checking it can process data
        assertNotNull(cipher.getIV());
    }
}
