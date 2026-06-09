package com.platform.util;

import com.platform.exception.FFmpegException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class FFmpegExecutor {

    private static final long DEFAULT_TIMEOUT_MINUTES = 30;

    public int execute(List<String> command) throws FFmpegException {
        return execute(command, DEFAULT_TIMEOUT_MINUTES);
    }

    public int execute(List<String> command, long timeoutMinutes) throws FFmpegException {
        log.debug("Executing FFmpeg command: {}", String.join(" ", command));

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);

        Process process;
        try {
            process = pb.start();
        } catch (Exception e) {
            throw new FFmpegException("Failed to start FFmpeg process: " + e.getMessage(), e);
        }

        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
                log.debug("FFmpeg: {}", line);
            }
        } catch (Exception e) {
            throw new FFmpegException("Failed to read FFmpeg output", e);
        }

        int exitCode;
        try {
            exitCode = process.waitFor(timeoutMinutes, TimeUnit.MINUTES) ? process.exitValue() : -1;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new FFmpegException("FFmpeg process interrupted", e);
        } finally {
            if (process.isAlive()) {
                process.destroyForcibly();
            }
        }

        if (exitCode != 0) {
            log.error("FFmpeg failed with exit code: {}. Output: {}", exitCode, output);
            throw new FFmpegException("FFmpeg failed with exit code: " + exitCode, exitCode);
        }

        return exitCode;
    }

    public File executeToFile(List<String> command, File outputFile) throws FFmpegException {
        execute(command);
        if (!outputFile.exists() || outputFile.length() == 0) {
            throw new FFmpegException("FFmpeg produced empty output file", -1);
        }
        return outputFile;
    }

    public int probeDuration(File mediaFile) {
        try {
            List<String> command = List.of(
                "ffprobe", "-v", "error",
                "-show_entries", "format=duration",
                "-of", "default=noprint_wrappers=1:nokey=1",
                mediaFile.getAbsolutePath()
            );
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String duration = reader.readLine();
                process.waitFor(10, TimeUnit.SECONDS);
                return duration != null ? (int) Double.parseDouble(duration.trim()) : 0;
            } finally {
                if (process.isAlive()) {
                    process.destroyForcibly();
                }
            }
        } catch (Exception e) {
            log.error("Failed to probe media duration", e);
            return 0;
        }
    }

    public void checkAvailability() {
        try {
            ProcessBuilder pb = new ProcessBuilder("ffmpeg", "-version");
            pb.redirectErrorStream(true);
            Process process = pb.start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                while (reader.readLine() != null) {}
            }
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                log.error("FFMPEG NOT AVAILABLE! Install: apt-get install ffmpeg");
            } else {
                log.info("FFmpeg is available");
            }
        } catch (Exception e) {
            log.error("FFMPEG NOT FOUND! Watermark and HLS will not work. Error: {}", e.getMessage());
        }
    }
}
