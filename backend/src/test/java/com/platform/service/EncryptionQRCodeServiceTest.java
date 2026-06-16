package com.platform.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class EncryptionQRCodeServiceTest {

    // ==================== EncryptionService ====================

    private EncryptionService encryptionService;

    @BeforeEach
    void setUp() {
        encryptionService = new EncryptionService();
        ReflectionTestUtils.setField(encryptionService, "masterKey", "test-master-key-123456789012345678");
    }

    @Test
    void encryption_GenerateIV_ReturnsBase64String() {
        String iv = encryptionService.generateIV();
        assertNotNull(iv);
        assertTrue(iv.startsWith("02|"), "IV should start with version prefix 02|");
        byte[] decoded = Base64.getDecoder().decode(iv.substring(3));
        assertEquals(12, decoded.length);
    }

    @Test
    void encryption_GenerateIV_UniqueEachCall() {
        String iv1 = encryptionService.generateIV();
        String iv2 = encryptionService.generateIV();
        assertNotEquals(iv1, iv2);
    }

    @Test
    void encryption_EncryptDecrypt_RoundTrip() {
        byte[] key = new byte[32]; // AES-256 key
        for (int i = 0; i < 32; i++) key[i] = (byte) i;
        String iv = encryptionService.generateIV();
        byte[] plaintext = "Hello, encrypted world!".getBytes();

        byte[] encrypted = encryptionService.encrypt(plaintext, key, iv);
        assertNotNull(encrypted);
        assertNotEquals(new String(plaintext), new String(encrypted));

        byte[] decrypted = encryptionService.decrypt(encrypted, key, iv);
        assertArrayEquals(plaintext, decrypted);
    }

    @Test
    void encryption_EncryptDecrypt_EmptyData() {
        byte[] key = new byte[32];
        String iv = encryptionService.generateIV();
        byte[] plaintext = new byte[0];

        byte[] encrypted = encryptionService.encrypt(plaintext, key, iv);
        byte[] decrypted = encryptionService.decrypt(encrypted, key, iv);
        assertArrayEquals(plaintext, decrypted);
    }

    @Test
    void encryption_Decrypt_WrongKey_ThrowsException() {
        byte[] key1 = new byte[32];
        byte[] key2 = new byte[32];
        key2[0] = 1;
        String iv = encryptionService.generateIV();
        byte[] plaintext = "secret data".getBytes();

        byte[] encrypted = encryptionService.encrypt(plaintext, key1, iv);
        assertThrows(RuntimeException.class, () -> encryptionService.decrypt(encrypted, key2, iv));
    }

    @Test
    void encryption_GenerateFileKey_ReturnsNonNull() {
        byte[] fileKey = encryptionService.generateFileKey("user-1", "file-1");
        assertNotNull(fileKey);
        assertTrue(fileKey.length > 0);
    }

    @Test
    void encryption_GenerateHLSKey_Returns16Bytes() {
        byte[] hlsKey = encryptionService.generateHLSKey("user-1", "file-1");
        assertNotNull(hlsKey);
        assertEquals(16, hlsKey.length);
    }

    // ==================== QRCodeService ====================

    @Test
    void qrCode_GenerateBase64_ReturnsValidBase64() {
        QRCodeService service = new QRCodeService();
        String base64 = service.generateQRCodeBase64("https://example.com");
        assertNotNull(base64);
        assertFalse(base64.isEmpty());
        // Verify it's valid base64 by decoding
        byte[] decoded = Base64.getDecoder().decode(base64);
        assertTrue(decoded.length > 0);
    }

    @Test
    void qrCode_GeneratePaymentQRData_ContainsRequiredFields() {
        QRCodeService service = new QRCodeService();
        ReflectionTestUtils.setField(service, "recipientName", "ИП Иванов");
        ReflectionTestUtils.setField(service, "account", "40703810512000002359");
        ReflectionTestUtils.setField(service, "bank", "ПАО Сбербанк");
        ReflectionTestUtils.setField(service, "bik", "041909644");
        ReflectionTestUtils.setField(service, "corrAccount", "30101810400000000225");
        ReflectionTestUtils.setField(service, "inn", "9715470089");
        ReflectionTestUtils.setField(service, "kpp", "773601001");
        String qrData = service.generatePaymentQRData(
                new BigDecimal("990.00"), "pay-123", "john");

        assertNotNull(qrData);
        assertTrue(qrData.startsWith("ST00012|"));
        assertTrue(qrData.contains("PersonalAcc=40703810512000002359"));
        assertTrue(qrData.contains("BIC=041909644"));
        assertTrue(qrData.contains("Sum=99000")); // 990.00 * 100
        assertTrue(qrData.contains("Purpose=Подписка pay-123"));
        assertTrue(qrData.contains("PayeeINN=9715470089"));
    }

    @Test
    void qrCode_GeneratePaymentQRData_DifferentAmounts() {
        QRCodeService service = new QRCodeService();
        ReflectionTestUtils.setField(service, "recipientName", "Test");
        ReflectionTestUtils.setField(service, "account", "ACC");
        ReflectionTestUtils.setField(service, "bank", "Bank");
        ReflectionTestUtils.setField(service, "bik", "BIK");
        ReflectionTestUtils.setField(service, "corrAccount", "CORR");
        ReflectionTestUtils.setField(service, "inn", "INN");
        ReflectionTestUtils.setField(service, "kpp", "KPP");
        String qr1 = service.generatePaymentQRData(new BigDecimal("100.00"), "p1", "u1");
        String qr2 = service.generatePaymentQRData(new BigDecimal("500.50"), "p2", "u2");

        assertTrue(qr1.contains("Sum=10000"));
        assertTrue(qr2.contains("Sum=50050"));
    }
}
