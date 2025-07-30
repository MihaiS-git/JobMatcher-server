package com.jobmatcher.server.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.*;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Inheritance(strategy = InheritanceType.JOINED)
public class PublicProfile extends Auditable {

    @Id
    @GeneratedValue
    private UUID id;

    @OneToOne
    private User user;

    @Min(value = 2)
    @Column(length = 100, unique = true, nullable = false)
    private String username;

    @Lob
    private String about;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name="public_profile_languages",
            joinColumns = @JoinColumn(name="profile_id"),
            inverseJoinColumns=@JoinColumn(name="language_id")
    )
    private Set<Language> languages = new HashSet<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "public_profile_social_media", joinColumns = @JoinColumn(name = "profile_id"))
    @Column(name = "social_media_url")
    private Set<String> socialMedia = new HashSet<>();

    @Column(name = "website_url")
    private String websiteUrl;

    @Min(value = 0)
    @Max(value = 5)
    @NotNull
    private Double rating = 0.0;

}
