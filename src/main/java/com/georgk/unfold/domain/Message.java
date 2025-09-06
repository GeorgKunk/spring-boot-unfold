package com.georgk.unfold.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "messages",
        indexes = {
                @Index(name = "idx_messages_thread", columnList = "thread_id"),
                @Index(name = "idx_messages_created_at", columnList = "createdAt")
        })
public class Message {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "thread_id", nullable = false)
    private MessageThread thread;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private UserAccount sender;

    @Column(nullable = false, length = 4000)
    private String content;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected Message() {
        // JPA
    }

    public Message(MessageThread thread, UserAccount sender, String content) {
        this.thread = thread;
        this.sender = sender;
        this.content = content;
    }

    @PrePersist
    public void prePersist() {
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public MessageThread getThread() {
        return thread;
    }

    public UserAccount getSender() {
        return sender;
    }

    public String getContent() {
        return content;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
