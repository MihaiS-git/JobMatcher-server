package com.jobmatcher.server.controller;

import com.jobmatcher.server.controller.config.AbstractIntegrationTest;
import com.jobmatcher.server.domain.JobCategory;
import com.jobmatcher.server.repository.JobCategoryRepository;
import com.jobmatcher.server.repository.JobSubcategoryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import static org.hamcrest.Matchers.is;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
public class JobCategoryControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JobCategoryRepository categoryRepository;

    @Autowired
    private JobSubcategoryRepository subcategoryRepository;

    @Test
    @WithMockUser(username = "test", roles = {"STAFF"})
    void shouldReturnJobCategories() throws Exception {
        var categories = categoryRepository.findAll();

        mockMvc.perform(get("/api/v0/job_categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(categories.size()))
                .andExpect(jsonPath("$[0].name").value(categories.getFirst().getName()))
                .andExpect(jsonPath("$[1].name").value(categories.get(1).getName()));
    }

    @Test
    @WithMockUser(username = "test", roles = {"STAFF"})
    void shouldReturnJobCategoryById() throws Exception {
        JobCategory category = categoryRepository.findAll().getFirst();

        mockMvc.perform(get("/api/v0/job_categories/{id}", category.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is(category.getName())))
                .andExpect(jsonPath("$.description", is(category.getDescription())));
    }

    @Test
    @WithMockUser(username = "test", roles = {"STAFF"})
    void shouldReturn404ForNonExistingJobCategory() throws Exception {
        mockMvc.perform(get("/api/v0/job_categories/{id}", 9999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Category not found."));
    }
}
