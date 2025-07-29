package com.jobmatcher.server.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name="job_subcategories")
public class JobSubcategory extends Auditable{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String description;

    @ManyToOne
    @JoinColumn(name = "category_id")
    private JobCategory category;

    public JobSubcategory(String name, String description, JobCategory category) {
        super();
        this.name = name;
        this.description = description;
        this.category=category;
    }
}
