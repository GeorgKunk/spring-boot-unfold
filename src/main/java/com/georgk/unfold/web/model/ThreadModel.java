package com.georgk.unfold.web.model;

import com.georgk.unfold.domain.ThreadType;
import org.springframework.hateoas.RepresentationModel;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class ThreadModel extends RepresentationModel<ThreadModel> {
    private UUID id;
    private ThreadType type;
    private String name;
    private List<UUID> participantIds;
    private Instant createdAt;
    private Instant updatedAt;

    public ThreadModel(UUID id, ThreadType type, String name, List<UUID> participantIds, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.type = type;
        this.name = name;
        this.participantIds = participantIds;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public ThreadModel() {}

    public UUID getId() {
        return id;
    }

    public ThreadType getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public List<UUID> getParticipantIds() {
        return participantIds;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
