package com.georgk.unfold.web.model;

import org.springframework.hateoas.RepresentationModel;

import java.time.Instant;
import java.util.UUID;

public class MessageModel extends RepresentationModel<MessageModel> {
    private UUID id;
    private UUID threadId;
    private UUID senderId;
    private String content;
    private Instant createdAt;

    public MessageModel(UUID id, UUID threadId, UUID senderId, String content, Instant createdAt) {
        this.id = id;
        this.threadId = threadId;
        this.senderId = senderId;
        this.content = content;
        this.createdAt = createdAt;
    }

    public MessageModel() {}

    public UUID getId() {
        return id;
    }

    public UUID getThreadId() {
        return threadId;
    }

    public UUID getSenderId() {
        return senderId;
    }

    public String getContent() {
        return content;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
