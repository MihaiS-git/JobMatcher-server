package com.jobmatcher.server.mapper;

import com.jobmatcher.server.domain.JobCategory;
import com.jobmatcher.server.domain.JobSubcategory;
import com.jobmatcher.server.domain.PortfolioItem;
import com.jobmatcher.server.model.*;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class PortfolioItemMapper {

    private final JobCategoryMapper jobCategoryMapper;
    private final JobSubcategoryMapper jobSubcategoryMapper;

    public PortfolioItemMapper(JobCategoryMapper jobCategoryMapper, JobSubcategoryMapper jobSubcategoryMapper) {
        this.jobCategoryMapper = jobCategoryMapper;
        this.jobSubcategoryMapper = jobSubcategoryMapper;
    }

    public PortfolioItemDetailDTO toDetailDto(PortfolioItem entity) {
        if (entity == null) {
            return null;
        }

        return PortfolioItemDetailDTO.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .category(jobCategoryMapper.toDto(entity.getCategory()))
                .subcategories(entity.getSubcategories().stream()
                        .map(jobSubcategoryMapper::toDto)
                        .collect(Collectors.toSet()))
                .demoUrl(entity.getDemoUrl())
                .sourceUrl(entity.getSourceUrl())
                .imageUrls(new HashSet<>(entity.getImageUrls()))
                .clientName(entity.getClientName())
                .freelancerProfileId(entity.getFreelancerProfile().getId())
                .build();
    }

    public PortfolioItemSummaryDTO toSummaryDto(PortfolioItem entity) {
        if (entity == null) {
            return null;
        }

        return PortfolioItemSummaryDTO.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .category(jobCategoryMapper.toDto(entity.getCategory()))
                .subcategories(entity.getSubcategories().stream()
                        .map(jobSubcategoryMapper::toDto)
                        .collect(Collectors.toSet()))
                .build();
    }

    public PortfolioItem toEntity(
            PortfolioItemRequestDTO dto,
            JobCategory category,
            Set<JobSubcategory> subcategories
            ) {
        if (dto == null) {
            return null;
        }

        PortfolioItem entity = new PortfolioItem();
        entity.setTitle(dto.getTitle());
        entity.setDescription(dto.getDescription());
        entity.setCategory(category);
        entity.setSubcategories(subcategories);
        entity.setDemoUrl(dto.getDemoUrl());
        entity.setSourceUrl(dto.getSourceUrl());
        entity.setImageUrls(new HashSet<>(dto.getImageUrls()));
        entity.setClientName(dto.getClientName());

        return entity;
    }
}
