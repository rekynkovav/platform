package com.platform.model;

public final class BroadcastStatus {

    private BroadcastStatus() {}

    public static final String IDLE = "idle";
    public static final String PREPARING = "preparing";
    public static final String LIVE = "live";
    public static final String PAUSED = "paused";
    public static final String ENDED = "ended";
    public static final String FAILED = "failed";

    public static boolean isActive(String status) {
        return LIVE.equals(status) || PREPARING.equals(status);
    }

    public static boolean isFinished(String status) {
        return ENDED.equals(status) || FAILED.equals(status);
    }
}