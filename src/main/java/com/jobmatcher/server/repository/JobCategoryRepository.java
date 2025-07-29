package com.jobmatcher.server.repository;

import com.jobmatcher.server.domain.JobCategory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobCategoryRepository extends JpaRepository<JobCategory, Long> {
}
