package com.platform.service;

import com.platform.exception.EncryptionException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

@Slf4j
@Service
public class EncryptionService {

    @Value("${app.media.encryption.master-key}")
    private String masterKey;

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final String HLS_ALGORITHM = "AES/CBC/PKCS5Padding";
    private static final String KEY_ALGORITHM = "AES";
    private static final int KEY_SIZE = 32;
    private static final int IV_SIZE = 12;
    private static final int GCM_TAG_LENGTH = 128;
    private static final String CBC_ALGORITHM = "AES/CBC/PKCS5Padding";
    private static final int CBC_IV_SIZE = 16;

    @PostConstruct
    public void validateConfig() {
        if (masterKey == null || masterKey.isBlank()) {
            throw new IllegalStateException("MEDIA_ENCRYPTION_KEY is not configured. Set media.encryption.master-key property");
        }
    }

    public byte[] generateFileKey(String userId, String fileId) {
        try {
            String combined = masterKey + ":" + userId + ":" + fileId;
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(combined.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.error("Failed to generate file key", e);
            throw new EncryptionException("Failed to generate file key", e);
        }
    }

    public String generateIV() {
        SecureRandom random = new SecureRandom();
        byte[] iv = new byte[IV_SIZE];
        random.nextBytes(iv);
        return "02|" + Base64.getEncoder().encodeToString(iv);
    }

    public byte[] encrypt(byte[] data, byte[] key, String ivBase64) {
        try {
            SecretKeySpec secretKey = new SecretKeySpec(key, KEY_ALGORITHM);

            if (ivBase64.startsWith("02|")) {
                String actualIv = ivBase64.substring(3);
                byte[] iv = Base64.getDecoder().decode(actualIv);
                GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
                Cipher cipher = Cipher.getInstance(ALGORITHM);
                cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec);
                return cipher.doFinal(data);
            } else {
                byte[] iv = Base64.getDecoder().decode(ivBase64);
                IvParameterSpec ivSpec = new IvParameterSpec(iv);
                Cipher cipher = Cipher.getInstance(CBC_ALGORITHM);
                cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec);
                return cipher.doFinal(data);
            }
        } catch (Exception e) {
            log.error("Encryption failed", e);
            throw new EncryptionException("Failed to encrypt data", e);
        }
    }

    public byte[] decrypt(byte[] encryptedData, byte[] key, String ivBase64) {
        try {
            SecretKeySpec secretKey = new SecretKeySpec(key, KEY_ALGORITHM);

            if (ivBase64.startsWith("02|")) {
                String actualIv = ivBase64.substring(3);
                byte[] iv = Base64.getDecoder().decode(actualIv);
                GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
                Cipher cipher = Cipher.getInstance(ALGORITHM);
                cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec);
                return cipher.doFinal(encryptedData);
            } else {
                byte[] iv = Base64.getDecoder().decode(ivBase64);
                IvParameterSpec ivSpec = new IvParameterSpec(iv);
                Cipher cipher = Cipher.getInstance(CBC_ALGORITHM);
                cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec);
                return cipher.doFinal(encryptedData);
            }
        } catch (javax.crypto.AEADBadTagException e) {
            log.error("Decryption failed - invalid tag or key", e);
            throw new EncryptionException("Failed to decrypt data: invalid tag or key", e);
        } catch (Exception e) {
            log.error("Decryption failed", e);
            throw new EncryptionException("Failed to decrypt data", e);
        }
    }

    public byte[] generateHLSKey(String userId, String fileId) {
        byte[] fullKey = generateFileKey(userId, fileId);
        byte[] hlsKey = new byte[16];
        System.arraycopy(fullKey, 0, hlsKey, 0, 16);
        return hlsKey;
    }

    public Cipher getCipher(int mode, byte[] key, String iv) throws Exception {
        SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
        if (iv.startsWith("02|")) {
            String actualIv = iv.substring(3);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, Base64.getDecoder().decode(actualIv));
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(mode, keySpec, gcmSpec);
            return cipher;
        } else {
            IvParameterSpec ivSpec = new IvParameterSpec(Base64.getDecoder().decode(iv));
            Cipher cipher = Cipher.getInstance(CBC_ALGORITHM);
            cipher.init(mode, keySpec, ivSpec);
            return cipher;
        }
    }

    public Cipher getCipherForHLS(int mode, byte[] key, String iv) throws Exception {
        SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
        IvParameterSpec ivSpec = new IvParameterSpec(Base64.getDecoder().decode(iv));
        Cipher cipher = Cipher.getInstance(HLS_ALGORITHM);
        cipher.init(mode, keySpec, ivSpec);
        return cipher;
    }
}
