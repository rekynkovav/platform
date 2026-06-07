package com.platform.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CreateBroadcastRequestTest {

    @Test
    void createBroadcastRequest_SettersAndGetters_Work() {
        CreateBroadcastRequest req = new CreateBroadcastRequest();
        req.setTitle("Live Q&A Session");
        req.setDescription("Ask me anything about nutrition");
        req.setStreamType("video");

        assertEquals("Live Q&A Session", req.getTitle());
        assertEquals("Ask me anything about nutrition", req.getDescription());
        assertEquals("video", req.getStreamType());
    }

    @Test
    void createBroadcastRequest_DefaultStreamType_IsVideo() {
        CreateBroadcastRequest req = new CreateBroadcastRequest();
        assertEquals("video", req.getStreamType());
    }

    @Test
    void createBroadcastRequest_NullFields_AreAllowed() {
        CreateBroadcastRequest req = new CreateBroadcastRequest();
        assertNull(req.getTitle());
        assertNull(req.getDescription());
    }
}
