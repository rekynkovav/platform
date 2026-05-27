package com.platform.service;

import org.springframework.stereotype.Service;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class MediaQueueService {

    private final BlockingQueue<MediaTask> queue = new LinkedBlockingQueue<>(100);
    private final ExecutorService worker = Executors.newSingleThreadExecutor();

    public MediaQueueService() {
        worker.submit(this::processQueue);
    }

    public void submitTask(MediaTask task) {
        queue.offer(task);
    }

    private void processQueue() {
        while (true) {
            try {
                MediaTask task = queue.take();
                // Обработка FFmpeg
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
            // FFmpeg конвертация
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
                Process process = pb.start();
                int exitCode = process.waitFor();
                if (exitCode != 0) {
                    throw new RuntimeException("FFmpeg conversion failed with exit code: " + exitCode);
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to process media task", e);
            }
        }
    }
}