package com.jobmatcher.server.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.UUID;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "skills", uniqueConstraints = @UniqueConstraint(columnNames = "name"))
public class Skill extends Auditable {

    @Id
    @GeneratedValue
    private UUID id;

    @Size(min = 1)
    @Column(nullable = false, unique = true)
    private String name;

    public Skill(String name) {
        super();
        this.name = name;
    }
}
