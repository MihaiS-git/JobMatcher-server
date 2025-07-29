package com.jobmatcher.server.service;

import com.jobmatcher.server.model.JobCategoryDTO;

import java.util.List;

public interface IJobCategoryService {
    List<JobCategoryDTO> getAllJobCategories();
    JobCategoryDTO getJobCategoryById(Long id);
}
