package com.jobmatcher.server.service;

import com.jobmatcher.server.domain.FreelancerProfile;
import com.jobmatcher.server.domain.JobCategory;
import com.jobmatcher.server.domain.JobSubcategory;
import com.jobmatcher.server.domain.PortfolioItem;
import com.jobmatcher.server.exception.ResourceNotFoundException;
import com.jobmatcher.server.mapper.PortfolioItemMapper;
import com.jobmatcher.server.model.PortfolioItemDetailDTO;
import com.jobmatcher.server.model.PortfolioItemRequestDTO;
import com.jobmatcher.server.model.PortfolioItemSummaryDTO;
import com.jobmatcher.server.repository.FreelancerProfileRepository;
import com.jobmatcher.server.repository.JobCategoryRepository;
import com.jobmatcher.server.repository.JobSubcategoryRepository;
import com.jobmatcher.server.repository.PortfolioItemRepository;
import com.jobmatcher.server.util.SanitizationUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(rollbackFor = Exception.class)
public class PortfolioItemServiceImpl implements IPortfolioItemService {

    private final PortfolioItemRepository repository;
    private final PortfolioItemMapper portfolioItemMapper;
    private final JobCategoryRepository jobCategoryRepository;
    private final JobSubcategoryRepository jobSubcategoryRepository;
    private final FreelancerProfileRepository freelancerProfileRepository;

    public PortfolioItemServiceImpl(
            PortfolioItemRepository repository,
            PortfolioItemMapper portfolioItemMapper,
            JobCategoryRepository jobCategoryRepository,
            JobSubcategoryRepository jobSubcategoryRepository, FreelancerProfileRepository freelancerProfileRepository
    ) {
        this.repository = repository;
        this.portfolioItemMapper = portfolioItemMapper;
        this.jobCategoryRepository = jobCategoryRepository;
        this.jobSubcategoryRepository = jobSubcategoryRepository;
        this.freelancerProfileRepository = freelancerProfileRepository;
    }

    @Override
    public PortfolioItemDetailDTO getPortfolioItemById(UUID id) {
        PortfolioItem item = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio item not found."));
        return portfolioItemMapper.toDetailDto(item);
    }

    @Override
    public Set<PortfolioItemSummaryDTO> getPortfolioItemsByFreelancerProfileId(UUID freelancerProfileId) {
        return repository.findByFreelancerProfileId(freelancerProfileId).stream()
                .map(portfolioItemMapper::toSummaryDto)
                .collect(Collectors.toSet());
    }

    @Override
    public PortfolioItemDetailDTO savePortfolioItem(PortfolioItemRequestDTO requestItem) {
        JobCategory category = jobCategoryRepository.findById(requestItem.getCategoryId()).orElseThrow(() ->
                new ResourceNotFoundException("Job category not found."));
        Set<JobSubcategory> subcategories = new HashSet<>(jobSubcategoryRepository.findAllById(requestItem.getSubcategoryIds()));
        FreelancerProfile freelancerProfile;
        if (requestItem.getFreelancerProfileId() == null) {
            freelancerProfile = freelancerProfileRepository.save(new FreelancerProfile());
        } else {
            freelancerProfile = freelancerProfileRepository.findById(requestItem.getFreelancerProfileId())
                    .orElseThrow(() -> new ResourceNotFoundException("Freelancer profile not found."));
        }

        PortfolioItemRequestDTO sanitizedRequestItem = sanitizePortfolioItemRequest(requestItem);

        PortfolioItem entity = portfolioItemMapper.toEntity(sanitizedRequestItem, category, subcategories);
        entity.setFreelancerProfile(freelancerProfile);
        PortfolioItem savedItem = repository.save(entity);

        freelancerProfile.getPortfolioItems().add(savedItem);
        freelancerProfileRepository.save(freelancerProfile);

        return portfolioItemMapper.toDetailDto(savedItem);
    }

    @Override
    public PortfolioItemDetailDTO updatePortfolioItem(UUID id, PortfolioItemRequestDTO requestItem) {
        PortfolioItem existingItem = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio item not found."));

        JobCategory category = null;
        if (requestItem.getCategoryId() != null) {
            category = jobCategoryRepository.findById(requestItem.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Job category not found."));
        }

        Set<JobSubcategory> subcategories = requestItem.getSubcategoryIds() != null
                ? new HashSet<>(jobSubcategoryRepository.findAllById(requestItem.getSubcategoryIds()))
                : new HashSet<>();

        PortfolioItemRequestDTO sanitizedRequestItem = sanitizePortfolioItemRequest(requestItem);

        existingItem.setCategory(category);
        existingItem.setSubcategories(subcategories);
        existingItem.setTitle(sanitizedRequestItem.getTitle());
        existingItem.setDescription(sanitizedRequestItem.getDescription());
        existingItem.setClientName(sanitizedRequestItem.getClientName());
        existingItem.setDemoUrl(sanitizedRequestItem.getDemoUrl());
        existingItem.setSourceUrl(sanitizedRequestItem.getSourceUrl());
        existingItem.setImageUrls(sanitizedRequestItem.getImageUrls());

        PortfolioItem updatedItem = repository.save(existingItem);

        return portfolioItemMapper.toDetailDto(updatedItem);
    }

    @Override
    public void deletePortfolioItem(UUID id) {
        PortfolioItem existingItem = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio item not found."));
        repository.delete(existingItem);
    }


    private static PortfolioItemRequestDTO sanitizePortfolioItemRequest(PortfolioItemRequestDTO requestItem) {
        String title = sanitizeOptionalText(requestItem.getTitle(), "Title");
        String description = sanitizeOptionalText(requestItem.getDescription(), "Description");
        String clientName = sanitizeOptionalText(requestItem.getClientName(), "Client name");
        String demoUrl = sanitizeOptionalUrl(requestItem.getDemoUrl(), "Demo URL");
        String sourceUrl = sanitizeOptionalUrl(requestItem.getSourceUrl(), "Source URL");
        Set<String> imageUrls = (requestItem.getImageUrls() == null ? Collections.<String>emptyList() : requestItem.getImageUrls())
                .stream()
                .map(SanitizationUtil::sanitizeUrl)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (imageUrls.isEmpty() && requestItem.getImageUrls() != null && !requestItem.getImageUrls().isEmpty()) {
            throw new IllegalArgumentException("Image URLs contain invalid characters.");
        }

        return PortfolioItemRequestDTO.builder()
                .title(title)
                .description(description)
                .categoryId(requestItem.getCategoryId())
                .subcategoryIds(requestItem.getSubcategoryIds())
                .demoUrl(demoUrl)
                .sourceUrl(sourceUrl)
                .imageUrls(imageUrls)
                .clientName(clientName)
                .build();
    }

    private static String sanitizeOptionalText(String input, String fieldName) {
        if (input == null) return null;
        String sanitized = SanitizationUtil.sanitizeText(input);
        if (sanitized == null) throw new IllegalArgumentException(fieldName + " contains invalid characters.");
        return sanitized;
    }

    private static String sanitizeOptionalUrl(String input, String fieldName) {
        if (input == null) return null;
        String sanitized = SanitizationUtil.sanitizeUrl(input);
        if (sanitized == null) throw new IllegalArgumentException(fieldName + " contains invalid characters.");
        return sanitized;
    }
}
