package com.platform.service;

import org.springframework.stereotype.Service;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@Slf4j
public class StreamingFileService {

    private static final int BUFFER_SIZE = 8192; // 8KB
    private static final long MAX_FILE_SIZE = 1024 * 1024 * 1024; // 1GB
    private static final String BASE_UPLOAD_DIR = System.getenv("UPLOAD_DIR") != null
            ? System.getenv("UPLOAD_DIR")
            : "/app/uploads";

    private Path validateAndResolvePath(String filePath) throws IOException {
        Path basePath = Paths.get(BASE_UPLOAD_DIR).toRealPath();
        Path resolvedPath = basePath.resolve(filePath).normalize().toRealPath();
        if (!resolvedPath.startsWith(basePath)) {
            throw new SecurityException("Path traversal detected: " + filePath);
        }
        return resolvedPath;
    }

    public void streamFile(String filePath, HttpServletResponse response) throws IOException {
        Path path = validateAndResolvePath(filePath);
        File file = path.toFile();

        if (!file.exists()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "File not found");
            return;
        }

        if (file.length() > MAX_FILE_SIZE) {
            response.sendError(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE, "File too large");
            return;
        }

        String contentType = Files.probeContentType(path);
        if (contentType == null) {
            contentType = "application/octet-stream";
        }

        response.setContentType(contentType);
        response.setContentLengthLong(file.length());
        response.setHeader("Content-Disposition", "inline; filename=\"" + file.getName() + "\"");
        response.setHeader("Accept-Ranges", "bytes");

        // Поддержка частичной загрузки (Range)
        String rangeHeader = response.getHeader("Range");
        long start = 0;
        long end = file.length() - 1;

        if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
            String[] ranges = rangeHeader.substring(6).split("-");
            try {
                start = Long.parseLong(ranges[0]);
                if (ranges.length > 1 && !ranges[1].isEmpty()) {
                    end = Long.parseLong(ranges[1]);
                }
                response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
                response.setHeader("Content-Range", "bytes " + start + "-" + end + "/" + file.length());
                response.setContentLengthLong(end - start + 1);
            } catch (NumberFormatException e) {
                log.warn("Invalid range header: {}", rangeHeader);
            }
        }

        try (InputStream inputStream = new FileInputStream(file);
             OutputStream outputStream = response.getOutputStream()) {

            inputStream.skip(start);
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;
            long remaining = end - start + 1;

            while (remaining > 0 && (bytesRead = inputStream.read(buffer, 0, (int) Math.min(buffer.length, remaining))) != -1) {
                outputStream.write(buffer, 0, bytesRead);
                remaining -= bytesRead;
            }
            outputStream.flush();
        } catch (IOException e) {
            log.error("Error streaming file: {}", e.getMessage());
            if (!response.isCommitted()) {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        }
    }

    public void streamZipArchive(File[] files, HttpServletResponse response) throws IOException {
        response.setContentType("application/zip");
        response.setHeader("Content-Disposition", "attachment; filename=\"archive.zip\"");

        try (ZipOutputStream zos = new ZipOutputStream(response.getOutputStream())) {
            for (File file : files) {
                Path validatedPath = validateAndResolvePath(file.getAbsolutePath());
                File validatedFile = validatedPath.toFile();
                if (validatedFile.exists() && validatedFile.isFile()) {
                    ZipEntry entry = new ZipEntry(validatedFile.getName());
                    zos.putNextEntry(entry);

                    try (FileInputStream fis = new FileInputStream(validatedFile)) {
                        byte[] buffer = new byte[BUFFER_SIZE];
                        int length;
                        while ((length = fis.read(buffer)) >= 0) {
                            zos.write(buffer, 0, length);
                        }
                    }
                    zos.closeEntry();
                }
            }
            zos.finish();
        }
    }

    public void streamLargeFile(InputStream inputStream, OutputStream outputStream, long size) throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        int bytesRead;
        long totalRead = 0;

        while (totalRead < size && (bytesRead = inputStream.read(buffer, 0, (int) Math.min(BUFFER_SIZE, size - totalRead))) != -1) {
            outputStream.write(buffer, 0, bytesRead);
            totalRead += bytesRead;
        }
        outputStream.flush();
    }
}
