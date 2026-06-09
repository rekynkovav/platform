package com.platform.service;

import com.platform.model.MediaFile;
import com.platform.util.FFmpegExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class HLSStreamingService {

    @Value("${media.hls.segment-duration:6}")
    private int segmentDuration;

    @Value("${media.hls.enabled:true}")
    private boolean hlsEnabled;

    @Value("${app.media.upload-path:/tmp/platform-media}")
    private String tempPath;

    private final EncryptionService encryptionService;
    private final StorageService storageService;
    private final FFmpegExecutor ffmpegExecutor;

    /**
     * Конвертация видео в HLS формат с шифрованием
     */
    public HLSResult convertToHLS(File inputVideo, String userId, String fileId) {
        if (!hlsEnabled) {
            log.warn("HLS streaming is disabled");
            return null;
        }

        try {
            Path hlsDir = Paths.get(tempPath, "hls", fileId);
            Files.createDirectories(hlsDir);

            byte[] encryptionKey = encryptionService.generateHLSKey(userId, fileId);

            Path keyFile = hlsDir.resolve("key.bin");
            Files.write(keyFile, encryptionKey);

            Path keyInfoFile = hlsDir.resolve("keyinfo.txt");
            String keyUrl = String.format("/api/media/key/%s", fileId);
            byte[] iv = new byte[16];
            new SecureRandom().nextBytes(iv);
            String keyInfoContent = String.format("%s\n%s\n%s",
                keyUrl,
                keyFile.toAbsolutePath(),
                bytesToHex(iv)
            );
            Files.writeString(keyInfoFile, keyInfoContent);

            Path playlistPath = hlsDir.resolve("playlist.m3u8");
            Path segmentPattern = hlsDir.resolve("segment%d.ts");

            List<String> command = new ArrayList<>();
            command.add("ffmpeg");
            command.add("-i");
            command.add(inputVideo.getAbsolutePath());
            command.add("-c:v");
            command.add("libx264");
            command.add("-preset");
            command.add("fast");
            command.add("-crf");
            command.add("23");
            command.add("-c:a");
            command.add("aac");
            command.add("-b:a");
            command.add("128k");
            command.add("-hls_time");
            command.add(String.valueOf(segmentDuration));
            command.add("-hls_playlist_type");
            command.add("vod");
            command.add("-hls_segment_filename");
            command.add(segmentPattern.toString());
            command.add("-hls_key_info_file");
            command.add(keyInfoFile.toString());
            command.add("-y");
            command.add(playlistPath.toString());

            log.info("Starting HLS conversion for file: {}", fileId);

            ffmpegExecutor.execute(command);

            int segmentCount = countSegments(hlsDir);
            String playlistS3Key;
            try {
                playlistS3Key = uploadHLSFiles(hlsDir, fileId);
            } finally {
                deleteDirectory(hlsDir.toFile());
            }

            log.info("HLS conversion completed for file: {}", fileId);

            return HLSResult.builder()
                .playlistUrl(playlistS3Key)
                .segmentCount(segmentCount)
                .duration(ffmpegExecutor.probeDuration(inputVideo))
                .build();

        } catch (Exception e) {
            log.error("Failed to convert video to HLS", e);
            throw new RuntimeException("HLS conversion failed: " + e.getMessage());
        }
    }

    private String uploadHLSFiles(Path hlsDir, String fileId) throws Exception {
        String playlistS3Key = null;
        File[] files = hlsDir.toFile().listFiles();

        if (files != null) {
            for (File file : files) {
                byte[] content = Files.readAllBytes(file.toPath());
                String path = "hls/" + fileId + "/" + file.getName();

                if (file.getName().endsWith(".m3u8")) {
                    playlistS3Key = storageService.upload(content, path, "application/vnd.apple.mpegurl");
                } else if (file.getName().endsWith(".ts")) {
                    storageService.upload(content, path, "video/mp2t");
                }
            }
        }

        return playlistS3Key;
    }

    private int countSegments(Path hlsDir) {
        File[] files = hlsDir.toFile().listFiles((dir, name) -> name.endsWith(".ts"));
        return files != null ? files.length : 0;
    }

    private void deleteDirectory(File directory) {
        if (directory.exists()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file);
                    } else {
                        file.delete();
                    }
                }
            }
            directory.delete();
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    @lombok.Data
    @lombok.Builder
    public static class HLSResult {
        private String playlistUrl;
        private int segmentCount;
        private int duration;
    }
}
