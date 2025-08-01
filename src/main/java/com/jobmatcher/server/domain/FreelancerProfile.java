package com.jobmatcher.server.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@PrimaryKeyJoinColumn(name = "id")
public class FreelancerProfile extends PublicProfile{

    private String headline;
    private Double hourlyRate;
    private Boolean availableForHire;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "public_profile_job_subcategories",
            joinColumns = @JoinColumn(name = "profile_id"),
            inverseJoinColumns = @JoinColumn(name = "subcategory_id")
    )
    private Set<JobSubcategory> jobSubcategories = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "public_profile_skills",
            joinColumns = @JoinColumn(name = "profile_id"),
            inverseJoinColumns = @JoinColumn(name = "skill_id")
    )
    private Set<Skill> skills = new HashSet<>();

    @Enumerated(EnumType.STRING)
    private ExperienceLevel experienceLevel;
}
