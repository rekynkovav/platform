package com.platform.exception;

public class FFmpegException extends RuntimeException {

    private final int exitCode;

    public FFmpegException(String message, int exitCode) {
        super(message);
        this.exitCode = exitCode;
    }

    public FFmpegException(String message, int exitCode, Throwable cause) {
        super(message, cause);
        this.exitCode = exitCode;
    }

    public FFmpegException(String message, Throwable cause) {
        super(message, cause);
        this.exitCode = -1;
    }

    public int getExitCode() {
        return exitCode;
    }
}
