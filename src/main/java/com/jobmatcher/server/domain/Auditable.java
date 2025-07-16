package com.jobmatcher.server.domain;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.Getter;

import java.time.OffsetDateTime;

@Getter
@MappedSuperclass
public abstract class Auditable {
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "last_update", nullable = false)
    private OffsetDateTime lastUpdate;

    @PrePersist
    @PreUpdate
    private void updateTimeStamps() {
        OffsetDateTime now = OffsetDateTime.now();
        if (createdAt == null) createdAt = now;
        lastUpdate = now;
    }
}

