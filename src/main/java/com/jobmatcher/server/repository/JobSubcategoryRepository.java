package com.jobmatcher.server.repository;

import com.jobmatcher.server.domain.JobSubcategory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobSubcategoryRepository extends JpaRepository<JobSubcategory, Long> {
}
