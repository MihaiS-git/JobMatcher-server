package com.jobmatcher.server.service;

import com.jobmatcher.server.domain.JobCategory;
import com.jobmatcher.server.exception.ResourceNotFoundException;
import com.jobmatcher.server.mapper.JobCategoryMapper;
import com.jobmatcher.server.model.JobCategoryDTO;
import com.jobmatcher.server.repository.JobCategoryRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class JobCategoryServiceImpl implements IJobCategoryService{
    private final JobCategoryRepository categoryRepository;
    private final JobCategoryMapper jobCategoryMapper;

    public JobCategoryServiceImpl(JobCategoryRepository categoryRepository, JobCategoryMapper jobCategoryMapper) {
        this.categoryRepository = categoryRepository;
        this.jobCategoryMapper = jobCategoryMapper;
    }

    @Override
    public List<JobCategoryDTO> getAllJobCategories() {
        List<JobCategoryDTO> categories = categoryRepository.findAll().stream().map(jobCategoryMapper::toDto).toList();
        if(categories.isEmpty()){
            throw new ResourceNotFoundException("Job categories not found.");
        }
        return categories;
    }

    @Override
    public JobCategoryDTO getJobCategoryById(Long id) {
        JobCategory category = categoryRepository.findById(id).orElseThrow(() ->
                new ResourceNotFoundException("Category not found."));
        return jobCategoryMapper.toDto(category);
    }
}
