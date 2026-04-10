package com.platform.service;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.*;
import com.platform.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Service
public class S3StorageService {

    @Value("${yandex.s3.access-key}")
    private String accessKey;

    @Value("${yandex.s3.secret-key}")
    private String secretKey;

    @Value("${yandex.s3.bucket}")
    private String bucket;

    @Value("${yandex.s3.endpoint}")
    private String endpoint;

    @Value("${yandex.s3.region}")
    private String region;

    private AmazonS3 s3Client;

    private AmazonS3 getS3Client() {
        if (s3Client == null) {
            BasicAWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);

            s3Client = AmazonS3ClientBuilder.standard()
                .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(endpoint, region))
                .withCredentials(new AWSStaticCredentialsProvider(credentials))
                .withPathStyleAccessEnabled(true)
                .build();
        }
        return s3Client;
    }

    /**
     * Загрузка зашифрованного файла в S3
     */
    public String uploadEncryptedFile(byte[] encryptedData, String fileType, String originalFilename) {
        try {
            String key = generateS3Key(fileType, originalFilename);

            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(encryptedData.length);
            metadata.setContentType("application/octet-stream");
            metadata.addUserMetadata("encrypted", "true");
            metadata.addUserMetadata("original-filename", originalFilename);

            InputStream inputStream = new ByteArrayInputStream(encryptedData);

            PutObjectRequest request = new PutObjectRequest(bucket, key, inputStream, metadata);
            getS3Client().putObject(request);

            log.info("Encrypted file uploaded to S3: {}", key);
            return key;

        } catch (Exception e) {
            log.error("Failed to upload file to S3", e);
            throw new BusinessException("Failed to upload file: " + e.getMessage());
        }
    }

    /**
     * Загрузка обычного файла
     */
    public String uploadFile(MultipartFile file, String fileType) {
        try {
            String key = generateS3Key(fileType, file.getOriginalFilename());

            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(file.getSize());
            metadata.setContentType(file.getContentType());

            PutObjectRequest request = new PutObjectRequest(bucket, key, file.getInputStream(), metadata);
            getS3Client().putObject(request);

            log.info("File uploaded to S3: {}", key);
            return key;

        } catch (IOException e) {
            log.error("Failed to upload file to S3", e);
            throw new BusinessException("Failed to upload file: " + e.getMessage());
        }
    }

    /**
     * Скачивание файла из S3
     */
    public byte[] downloadFile(String s3Key) {
        try {
            S3Object object = getS3Client().getObject(bucket, s3Key);
            return object.getObjectContent().readAllBytes();
        } catch (Exception e) {
            log.error("Failed to download file from S3: {}", s3Key, e);
            throw new BusinessException("Failed to download file: " + e.getMessage());
        }
    }

    /**
     * Генерация presigned URL для временного доступа
     */
    public String generatePresignedUrl(String s3Key, int expirationSeconds) {
        try {
            Date expiration = Date.from(Instant.now().plusSeconds(expirationSeconds));

            GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(bucket, s3Key)
                .withMethod(com.amazonaws.HttpMethod.GET)
                .withExpiration(expiration);

            URL url = getS3Client().generatePresignedUrl(request);

            log.debug("Generated presigned URL for: {} (expires in {}s)", s3Key, expirationSeconds);
            return url.toString();

        } catch (Exception e) {
            log.error("Failed to generate presigned URL for: {}", s3Key, e);
            throw new BusinessException("Failed to generate access URL: " + e.getMessage());
        }
    }

    /**
     * Удаление файла из S3
     */
    public void deleteFile(String s3Key) {
        try {
            getS3Client().deleteObject(bucket, s3Key);
            log.info("File deleted from S3: {}", s3Key);
        } catch (Exception e) {
            log.error("Failed to delete file from S3: {}", s3Key, e);
            throw new BusinessException("Failed to delete file: " + e.getMessage());
        }
    }

    /**
     * Проверка существования файла
     */
    public boolean fileExists(String s3Key) {
        try {
            return getS3Client().doesObjectExist(bucket, s3Key);
        } catch (Exception e) {
            log.error("Failed to check file existence: {}", s3Key, e);
            return false;
        }
    }

    /**
     * Получение метаданных файла
     */
    public ObjectMetadata getFileMetadata(String s3Key) {
        try {
            return getS3Client().getObjectMetadata(bucket, s3Key);
        } catch (Exception e) {
            log.error("Failed to get file metadata: {}", s3Key, e);
            throw new BusinessException("Failed to get file metadata: " + e.getMessage());
        }
    }

    /**
     * Генерация уникального ключа для S3
     */
    private String generateS3Key(String fileType, String originalFilename) {
        String uuid = UUID.randomUUID().toString();
        String timestamp = String.valueOf(System.currentTimeMillis());
        String sanitizedFilename = originalFilename.replaceAll("[^a-zA-Z0-9._-]", "_");

        return String.format("%s/%s_%s_%s", fileType, uuid, timestamp, sanitizedFilename);
    }
}
