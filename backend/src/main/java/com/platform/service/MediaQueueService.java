package com.platform.service;

import com.platform.util.NamedThreadFactory;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class MediaQueueService {

    private final BlockingQueue<MediaTask> queue = new LinkedBlockingQueue<>(100);
    private final ExecutorService worker = Executors.newSingleThreadExecutor(new NamedThreadFactory("media-queue"));

    public MediaQueueService() {
        worker.submit(this::processQueue);
    }

    public void submitTask(MediaTask task) {
        try {
            queue.put(task);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Failed to submit media task", e);
        }
    }

    @PreDestroy
    public void shutdown() {
        worker.shutdown();
        try {
            if (!worker.awaitTermination(30, TimeUnit.SECONDS)) {
                worker.shutdownNow();
            }
        } catch (InterruptedException e) {
            worker.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private void processQueue() {
        while (true) {
            try {
                MediaTask task = queue.take();
                task.process();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    public static class MediaTask {
        private final String inputPath;
        private final String outputPath;
        private final String type;

        public MediaTask(String inputPath, String outputPath, String type) {
            this.inputPath = inputPath;
            this.outputPath = outputPath;
            this.type = type;
        }

        public void process() {
            try {
                ProcessBuilder pb = new ProcessBuilder(
                    "ffmpeg",
                    "-i", inputPath,
                    "-c:v", "libx264",
                    "-preset", "fast",
                    "-crf", "23",
                    "-c:a", "aac",
                    "-b:a", "128k",
                    outputPath
                );
                pb.redirectErrorStream(true);
                Process process = pb.start();

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    while (reader.readLine() != null) {
                        // consume stream to prevent deadlock
                    }
                }

                int exitCode;
                try {
                    exitCode = process.waitFor(30, TimeUnit.MINUTES) ? process.exitValue() : -1;
                } finally {
                    if (process.isAlive()) {
                        process.destroyForcibly();
                    }
                }

                if (exitCode != 0) {
                    throw new RuntimeException("FFmpeg conversion failed with exit code: " + exitCode);
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to process media task", e);
            }
        }
    }
}
