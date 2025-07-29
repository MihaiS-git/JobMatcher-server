package com.jobmatcher.server.service;

import com.jobmatcher.server.domain.JobCategory;
import com.jobmatcher.server.exception.ResourceNotFoundException;
import com.jobmatcher.server.mapper.JobCategoryMapper;
import com.jobmatcher.server.model.JobCategoryDTO;
import com.jobmatcher.server.repository.JobCategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobCategoryServiceImplTest {

    @Mock
    private JobCategoryRepository categoryRepository;

    @Mock
    private JobCategoryMapper categoryMapper;

    @InjectMocks
    private JobCategoryServiceImpl categoryService;

    @BeforeEach
    void setup(){
    }

    @Test
    void getAllJobCategories_returnsList_whenCategoriesExist() {
        JobCategory category = new JobCategory("Digital", "Creative work");
        JobCategoryDTO dto = JobCategoryDTO.builder().id(1L).name("Digital").description("Creative work").build();

        when(categoryRepository.findAll()).thenReturn(List.of(category));
        when(categoryMapper.toDto(category)).thenReturn(dto);

        List<JobCategoryDTO> result = categoryService.getAllJobCategories();

        assertEquals(1, result.size());
        assertEquals("Digital", result.get(0).getName());
        verify(categoryRepository, times(1)).findAll();
        verify(categoryMapper, times(1)).toDto(category);
    }

    @Test
    void getAllJobCategories_throws_whenNoCategoriesExist() {
        when(categoryRepository.findAll()).thenReturn(List.of());

        assertThrows(ResourceNotFoundException.class, () -> categoryService.getAllJobCategories());
        verify(categoryRepository).findAll();
    }

    @Test
    void getJobCategoryById_returnsCategory_whenExists() {
        JobCategory category = new JobCategory("Tech", "Advanced jobs");
        JobCategoryDTO dto = JobCategoryDTO.builder().id(2L).name("Tech").description("Advanced jobs").build();

        when(categoryRepository.findById(2L)).thenReturn(Optional.of(category));
        when(categoryMapper.toDto(category)).thenReturn(dto);

        JobCategoryDTO result = categoryService.getJobCategoryById(2L);

        assertNotNull(result);
        assertEquals("Tech", result.getName());
        verify(categoryRepository).findById(2L);
        verify(categoryMapper).toDto(category);
    }

    @Test
    void getJobCategoryById_throws_whenNotFound() {
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> categoryService.getJobCategoryById(99L));
        verify(categoryRepository).findById(99L);
    }

}