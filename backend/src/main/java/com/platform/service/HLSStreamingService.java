package com.platform.service;

import com.platform.model.MediaFile;
import com.platform.service.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
            String keyInfoContent = String.format("%s\n%s\n%s",
                keyUrl,
                keyFile.toAbsolutePath(),
                bytesToHex(encryptionKey).substring(0, 32)
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

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                    log.debug("FFmpeg: {}", line);
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                log.error("FFmpeg failed with exit code: {}. Output: {}", exitCode, output);
                throw new RuntimeException("HLS conversion failed");
            }

            String playlistS3Key = uploadHLSFiles(hlsDir, fileId);
            deleteDirectory(hlsDir.toFile());

            log.info("HLS conversion completed for file: {}", fileId);

            return HLSResult.builder()
                .playlistUrl(playlistS3Key)
                .segmentCount(countSegments(hlsDir))
                .duration(getVideoDuration(inputVideo))
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

    private int getVideoDuration(File videoFile) {
        try {
            List<String> command = new ArrayList<>();
            command.add("ffprobe");
            command.add("-v");
            command.add("error");
            command.add("-show_entries");
            command.add("format=duration");
            command.add("-of");
            command.add("default=noprint_wrappers=1:nokey=1");
            command.add(videoFile.getAbsolutePath());

            ProcessBuilder pb = new ProcessBuilder(command);
            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String duration = reader.readLine();
                return (int) Double.parseDouble(duration);
            }
        } catch (Exception e) {
            log.error("Failed to get video duration", e);
            return 0;
        }
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
