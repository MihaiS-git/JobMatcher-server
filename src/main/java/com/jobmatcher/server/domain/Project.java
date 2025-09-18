package com.jobmatcher.server.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "projects")
public class Project extends Auditable{

    @Id
    @GeneratedValue
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private CustomerProfile customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "freelancer_id")
    private FreelancerProfile freelancer;

    @NotBlank
    @Size(max=255)
    @Column(name = "title", nullable = false)
    private String title;

    @NotBlank
    @Size(max=2000)
    @Column(name = "description", nullable = false, length = 2000)
    private String description;

    @NotNull
    @Enumerated(EnumType.STRING)
    private ProjectStatus status = ProjectStatus.OPEN;

    @NotNull
    @DecimalMin("0.0")
    @Column(name = "budget", nullable = false)
    private BigDecimal budget;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_type", nullable = false)
    private PaymentType paymentType;

    @NotNull
    @Future
    @Column(name = "deadline", nullable = false)
    private LocalDate deadline;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private JobCategory category;

    @NotEmpty
    @Size(min = 1, max = 5)
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "project_job_subcategories",
            joinColumns = @JoinColumn(name = "project_id"),
            inverseJoinColumns = @JoinColumn(name = "subcategory_id")
    )
    @OrderBy("name ASC")
    private Set<JobSubcategory> subcategories;

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Proposal> proposals = new HashSet<>();

}
