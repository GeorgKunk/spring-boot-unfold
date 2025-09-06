package com.georgk.unfold.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "threads",
        indexes = {
                @Index(name = "idx_threads_type", columnList = "type"),
                @Index(name = "idx_threads_updated_at", columnList = "updatedAt")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_threads_direct_key", columnNames = "directKey")
        })
public class MessageThread {

    @Id
    @GeneratedValue
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ThreadType type;

    @Column(length = 200)
    private String directKey; // normalized "minId:maxId" for DIRECT threads; null for GROUP

    @Column(length = 200)
    private String name; // optional name for GROUP

    @ManyToMany
    @JoinTable(
            name = "thread_participants",
            joinColumns = @JoinColumn(name = "thread_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id"),
            uniqueConstraints = @UniqueConstraint(name = "uk_thread_participant", columnNames = {"thread_id", "user_id"})
    )

    private Set<UserAccount> participants = new LinkedHashSet<>();

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected MessageThread() {
        // JPA
    }

    public MessageThread(ThreadType type, String name, String directKey) {
        this.type = type;
        this.name = name;
        this.directKey = directKey;
    }

    @PrePersist
    public void prePersist() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public ThreadType getType() {
        return type;
    }

    public String getDirectKey() {
        return directKey;
    }

    public String getName() {
        return name;
    }

    public Set<UserAccount> getParticipants() {
        return participants;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setType(ThreadType type) {
        this.type = type;
    }

    public void setDirectKey(String directKey) {
        this.directKey = directKey;
    }

    public void touch() {
        this.updatedAt = Instant.now();
    }
}
