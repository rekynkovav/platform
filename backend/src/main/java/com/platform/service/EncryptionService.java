package com.platform.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

@Slf4j
@Service
public class EncryptionService {

    // @Value("${media.encryption.master-key}")
    private String masterKey = "test-master-key-1234567890";

    @Value("${media.encryption.algorithm:AES/CBC/PKCS5Padding}")
    private String algorithm;

    private static final String KEY_ALGORITHM = "AES";
    private static final int KEY_SIZE = 32;
    private static final int IV_SIZE = 16;

    public byte[] generateFileKey(String userId, String fileId) {
        try {
            String keySource = masterKey + ":" + userId + ":" + fileId;
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(keySource.getBytes("UTF-8"));
        } catch (Exception e) {
            log.error("Failed to generate file key", e);
            throw new RuntimeException("Failed to generate encryption key", e);
        }
    }

    public String generateIV() {
        SecureRandom random = new SecureRandom();
        byte[] iv = new byte[IV_SIZE];
        random.nextBytes(iv);
        return Base64.getEncoder().encodeToString(iv);
    }

    public byte[] encrypt(byte[] data, byte[] key, String ivBase64) {
        try {
            SecretKeySpec secretKey = new SecretKeySpec(key, KEY_ALGORITHM);
            byte[] iv = Base64.getDecoder().decode(ivBase64);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);

            Cipher cipher = Cipher.getInstance(algorithm);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec);

            return cipher.doFinal(data);
        } catch (Exception e) {
            log.error("Encryption failed", e);
            throw new RuntimeException("Failed to encrypt data", e);
        }
    }

    public byte[] decrypt(byte[] encryptedData, byte[] key, String ivBase64) {
        try {
            SecretKeySpec secretKey = new SecretKeySpec(key, KEY_ALGORITHM);
            byte[] iv = Base64.getDecoder().decode(ivBase64);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);

            Cipher cipher = Cipher.getInstance(algorithm);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec);

            return cipher.doFinal(encryptedData);
        } catch (Exception e) {
            log.error("Decryption failed", e);
            throw new RuntimeException("Failed to decrypt data", e);
        }
    }

    public byte[] generateHLSKey(String userId, String fileId) {
        byte[] fullKey = generateFileKey(userId, fileId);
        byte[] hlsKey = new byte[16];
        System.arraycopy(fullKey, 0, hlsKey, 0, 16);
        return hlsKey;
    }

    public Cipher getCipher(int mode, byte[] key, String ivBase64) throws Exception {
        SecretKeySpec secretKey = new SecretKeySpec(key, KEY_ALGORITHM);
        byte[] iv = Base64.getDecoder().decode(ivBase64);
        IvParameterSpec ivSpec = new IvParameterSpec(iv);

        Cipher cipher = Cipher.getInstance(algorithm);
        cipher.init(mode, secretKey, ivSpec);
        return cipher;
    }
}