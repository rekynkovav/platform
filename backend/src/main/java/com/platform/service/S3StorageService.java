package com.platform.service;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.SdkClientException;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import io.micrometer.core.annotation.Timed;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.time.Instant;
import java.util.Date;

@Slf4j
@Service
@Profile("prod")
public class S3StorageService implements StorageService {

    @Value("${yandex.s3.access-key:}")
    private String accessKey;

    @Value("${yandex.s3.secret-key:}")
    private String secretKey;

    @Value("${yandex.s3.bucket:platform-media}")
    private String bucket;

    @Value("${yandex.s3.endpoint:https://storage.yandexcloud.net}")
    private String endpoint;

    @Value("${yandex.s3.region:ru-central1}")
    private String region;

    private AmazonS3 s3Client;

    @PostConstruct
    public void init() {
        BasicAWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);
        s3Client = AmazonS3ClientBuilder.standard()
            .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(endpoint, region))
            .withCredentials(new AWSStaticCredentialsProvider(credentials))
            .withPathStyleAccessEnabled(true)
            .build();
    }

    @PreDestroy
    public void shutdown() {
        if (s3Client != null) {
            s3Client.shutdown();
        }
    }

    @Timed(value = "s3.upload", description = "Time to upload to S3")
    @Override
    public String upload(byte[] data, String path, String contentType) {
        try {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(data.length);
            metadata.setContentType(contentType != null ? contentType : "application/octet-stream");

            ByteArrayInputStream inputStream = new ByteArrayInputStream(data);
            PutObjectRequest request = new PutObjectRequest(bucket, path, inputStream, metadata);
            s3Client.putObject(request);

            log.info("File uploaded to S3: {}", path);
            return path;
        } catch (Exception e) {
            log.error("Failed to upload file to S3: {}", path, e);
            throw new RuntimeException("Failed to upload file to S3", e);
        }
    }

    @Timed(value = "s3.download", description = "Time to download from S3")
    @Override
    public byte[] download(String path) {
        try (S3Object object = s3Client.getObject(bucket, path);
             ByteArrayOutputStream baos = new ByteArrayOutputStream(8192)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            try (InputStream is = object.getObjectContent()) {
                while ((bytesRead = is.read(buffer)) != -1) {
                    baos.write(buffer, 0, bytesRead);
                }
            }
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Failed to download file from S3: {}", path, e);
            throw new RuntimeException("Failed to download file from S3", e);
        }
    }

    @Timed(value = "s3.delete", description = "Time to delete from S3")
    @Override
    public boolean delete(String path) {
        try {
            s3Client.deleteObject(bucket, path);
            log.info("File deleted from S3: {}", path);
            return true;
        } catch (Exception e) {
            log.error("Failed to delete file from S3: {}", path, e);
            return false;
        }
    }

    @Timed(value = "s3.generate_presigned_url", description = "Time to generate presigned URL")
    @Override
    public String generatePresignedUrl(String path, int expirationSeconds) {
        try {
            Date expiration = Date.from(Instant.now().plusSeconds(expirationSeconds));
            GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(bucket, path)
                .withExpiration(expiration);
            URL url = s3Client.generatePresignedUrl(request);
            return url.toString();
        } catch (Exception e) {
            log.error("Failed to generate presigned URL for: {}", path, e);
            throw new RuntimeException("Failed to generate presigned URL", e);
        }
    }

    @Timed(value = "s3.exists", description = "Time to check existence in S3")
    @Override
    public boolean exists(String path) {
        try {
            return s3Client.doesObjectExist(bucket, path);
        } catch (Exception e) {
            log.error("Failed to check existence: {}", path, e);
            return false;
        }
    }

    @Timed(value = "s3.upload_stream", description = "Time to upload stream to S3")
    @Override
    public String upload(InputStream inputStream, String path, String contentType, long contentLength) {
        try {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(contentLength);
            metadata.setContentType(contentType != null ? contentType : "application/octet-stream");

            PutObjectRequest request = new PutObjectRequest(bucket, path, inputStream, metadata);
            s3Client.putObject(request);

            log.info("File uploaded to S3 via stream: {}", path);
            return path;
        } catch (Exception e) {
            log.error("Failed to upload file to S3 via stream: {}", path, e);
            throw new RuntimeException("Failed to upload file to S3", e);
        }
    }

    /**
     * Проверка, что файл успешно загружен и доступен
     */
    public boolean verifyUpload(String key, long expectedSize) {
        try {
            ObjectMetadata metadata = s3Client.getObjectMetadata(bucket, key);
            return metadata.getContentLength() == expectedSize;
        } catch (SdkClientException e) {
            log.error("File not found in S3: {}", key, e);
            return false;
        }
    }

    /**
     * Проверка, что файл является валидным медиа-файлом
     */
    public boolean isValidMediaFile(String key) {
        try {
            return s3Client.doesObjectExist(bucket, key);
        } catch (Exception e) {
            log.error("Failed to verify media file: {}", key, e);
            return false;
        }
    }
}
