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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
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

    @Override
    public String upload(byte[] data, String path, String contentType) {
        try {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(data.length);
            metadata.setContentType(contentType != null ? contentType : "application/octet-stream");

            ByteArrayInputStream inputStream = new ByteArrayInputStream(data);
            PutObjectRequest request = new PutObjectRequest(bucket, path, inputStream, metadata);
            getS3Client().putObject(request);

            log.info("File uploaded to S3: {}", path);
            return path;
        } catch (Exception e) {
            log.error("Failed to upload file to S3: {}", path, e);
            throw new RuntimeException("Failed to upload file to S3", e);
        }
    }

    @Override
    public byte[] download(String path) {
        try {
            S3Object object = getS3Client().getObject(bucket, path);
            return object.getObjectContent().readAllBytes();
        } catch (Exception e) {
            log.error("Failed to download file from S3: {}", path, e);
            throw new RuntimeException("Failed to download file from S3", e);
        }
    }

    @Override
    public boolean delete(String path) {
        try {
            getS3Client().deleteObject(bucket, path);
            log.info("File deleted from S3: {}", path);
            return true;
        } catch (Exception e) {
            log.error("Failed to delete file from S3: {}", path, e);
            return false;
        }
    }

    @Override
    public String generatePresignedUrl(String path, int expirationSeconds) {
        try {
            Date expiration = Date.from(Instant.now().plusSeconds(expirationSeconds));
            GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(bucket, path)
                .withExpiration(expiration);
            URL url = getS3Client().generatePresignedUrl(request);
            return url.toString();
        } catch (Exception e) {
            log.error("Failed to generate presigned URL for: {}", path, e);
            throw new RuntimeException("Failed to generate presigned URL", e);
        }
    }

    @Override
    public boolean exists(String path) {
        try {
            return getS3Client().doesObjectExist(bucket, path);
        } catch (Exception e) {
            log.error("Failed to check existence: {}", path, e);
            return false;
        }
    }

    @Override
    public String upload(InputStream inputStream, String path, String contentType, long contentLength) {
        try {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(contentLength);
            metadata.setContentType(contentType != null ? contentType : "application/octet-stream");

            PutObjectRequest request = new PutObjectRequest(bucket, path, inputStream, metadata);
            getS3Client().putObject(request);

            log.info("File uploaded to S3 via stream: {}", path);
            return path;
        } catch (Exception e) {
            log.error("Failed to upload file to S3 via stream: {}", path, e);
            throw new RuntimeException("Failed to upload file to S3", e);
        }
    }
}
