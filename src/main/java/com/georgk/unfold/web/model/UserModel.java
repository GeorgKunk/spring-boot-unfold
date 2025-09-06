package com.georgk.unfold.web.model;

import org.springframework.hateoas.RepresentationModel;

import java.time.Instant;
import java.util.UUID;

public class UserModel extends RepresentationModel<UserModel> {
    private UUID id;
    private String username;
    private Instant createdAt;

    public UserModel(UUID id, String username, Instant createdAt) {
        this.id = id;
        this.username = username;
        this.createdAt = createdAt;
    }

    public UserModel() {}

    public UUID getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
