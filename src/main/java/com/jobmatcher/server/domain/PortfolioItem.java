package com.jobmatcher.server.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "portfolio_items")
public class PortfolioItem extends Auditable {

    @Id
    @GeneratedValue
    private UUID id;

    private String title;
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private JobCategory category;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "portfolio_item_job_subcategories",
            joinColumns = @JoinColumn(name = "portfolio_item_id"),
            inverseJoinColumns = @JoinColumn(name = "subcategory_id")
    )
    private Set<JobSubcategory> subcategories = new HashSet<>();

    @Column(name="demo_url")
    private String demoUrl;

    @Column(name="source_url")
    private String sourceUrl;

    @ElementCollection
    @CollectionTable(name = "portfolio_item_images", joinColumns = @JoinColumn(name = "portfolio_item_id"))
    private Set<String> imageUrls = new HashSet<>();

    @Column(name = "client_name")
    private String clientName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "freelancer_profile_id", nullable = false)
    private FreelancerProfile freelancerProfile;
}
