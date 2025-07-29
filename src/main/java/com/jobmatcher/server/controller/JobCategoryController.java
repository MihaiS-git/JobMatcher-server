package com.jobmatcher.server.controller;

import com.jobmatcher.server.model.JobCategoryDTO;
import com.jobmatcher.server.service.IJobCategoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.jobmatcher.server.model.ApiConstants.API_VERSION;

@RestController
@RequestMapping(path=API_VERSION + "/job_categories")
public class JobCategoryController {

    private final IJobCategoryService categoryService;

    public JobCategoryController(IJobCategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public ResponseEntity<List<JobCategoryDTO>> getAllJobCategories(){
        List<JobCategoryDTO> categories = categoryService.getAllJobCategories();
        return ResponseEntity.ok(categories);
    }

    @GetMapping(path = "{id}")
    public ResponseEntity<JobCategoryDTO> getJobCategoryById(@PathVariable Long id){
        JobCategoryDTO category = categoryService.getJobCategoryById(id);
        return ResponseEntity.ok(category);
    }
}
