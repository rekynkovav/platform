package com.platform.model;

public enum BroadcastStatus {
    IDLE("idle"),
    LIVE("live"),
    ENDED("ended");
    
    private final String value;
    
    BroadcastStatus(String value) {
        this.value = value;
    }
    
    public String getValue() {
        return value;
    }
    
    @Override
    public String toString() {
        return value;
    }
}
