package com.jobmatcher.server.service;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

import com.jobmatcher.server.domain.*;
import com.jobmatcher.server.exception.ResourceNotFoundException;
import com.jobmatcher.server.exception.UploadFileException;
import com.jobmatcher.server.mapper.PortfolioItemMapper;
import com.jobmatcher.server.model.*;
import com.jobmatcher.server.repository.*;
import com.jobmatcher.server.util.SanitizationUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.util.*;

import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class PortfolioItemServiceImplTest {
    @Mock private PortfolioItemRepository repository;
    @Mock private PortfolioItemMapper portfolioItemMapper;
    @Mock private JobCategoryRepository jobCategoryRepository;
    @Mock private JobSubcategoryRepository jobSubcategoryRepository;
    @Mock private FreelancerProfileRepository freelancerProfileRepository;

    @InjectMocks private PortfolioItemServiceImpl service;

    private UUID portfolioItemId;
    private UUID freelancerProfileId;
    private Long categoryId;
    private Set<Long> subcategoryIds;

    @BeforeEach
    void setUp() {
        portfolioItemId = UUID.randomUUID();
        freelancerProfileId = UUID.randomUUID();
        categoryId = 1L;
        subcategoryIds = Set.of(1L, 2L);
    }

    // --- Positive tests ---

    @Test
    void getPortfolioItemById_found() {
        PortfolioItem item = new PortfolioItem();
        PortfolioItemDetailDTO dto = PortfolioItemDetailDTO.builder()
                .id(portfolioItemId)
                .build();
        when(repository.findById(portfolioItemId)).thenReturn(Optional.of(item));
        when(portfolioItemMapper.toDetailDto(item)).thenReturn(dto);

        PortfolioItemDetailDTO result = service.getPortfolioItemById(portfolioItemId);
        assertSame(dto, result);
    }

    @Test
    void getPortfolioItemsByFreelancerProfileId_returnsSummarySet() {
        PortfolioItem item = new PortfolioItem();
        PortfolioItemSummaryDTO dto = PortfolioItemSummaryDTO.builder()
                .id(portfolioItemId)
                .build();
        when(repository.findByFreelancerProfileId(freelancerProfileId)).thenReturn( new HashSet<>(List.of(item)));
        when(portfolioItemMapper.toSummaryDto(item)).thenReturn(dto);

        Set<PortfolioItemSummaryDTO> result = service.getPortfolioItemsByFreelancerProfileId(freelancerProfileId);
        assertEquals(1, result.size());
        assertTrue(result.contains(dto));
    }

    @Test
    void savePortfolioItem_success_existingFreelancerProfile() {
        PortfolioItemRequestDTO request = PortfolioItemRequestDTO.builder()
                .freelancerProfileId(freelancerProfileId)
                .categoryId(categoryId)
                .subcategoryIds(new HashSet<>(subcategoryIds))
                .title("Title")
                .description("Desc")
                .clientName("Client")
                .demoUrl("http://demo.com")
                .sourceUrl("http://source.com")
                .build();

        JobCategory category = new JobCategory();
        Set<JobSubcategory> subcategories = Set.of(new JobSubcategory(), new JobSubcategory());
        FreelancerProfile freelancerProfile = new FreelancerProfile();
        PortfolioItem entity = new PortfolioItem();
        PortfolioItem savedItem = new PortfolioItem();
        PortfolioItemDetailDTO detailDTO = PortfolioItemDetailDTO.builder()
                .id(portfolioItemId)
                .build();

        when(jobCategoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(jobSubcategoryRepository.findAllById(subcategoryIds)).thenReturn(new ArrayList<>(subcategories));
        when(freelancerProfileRepository.findById(freelancerProfileId)).thenReturn(Optional.of(freelancerProfile));
        when(portfolioItemMapper.toEntity(any(), eq(category), eq(subcategories))).thenReturn(entity);
        when(repository.save(entity)).thenReturn(savedItem);
        when(portfolioItemMapper.toDetailDto(savedItem)).thenReturn(detailDTO);

        PortfolioItemDetailDTO result = service.savePortfolioItem(request);
        assertSame(detailDTO, result);
        assertTrue(freelancerProfile.getPortfolioItems().contains(savedItem));
        verify(freelancerProfileRepository).save(freelancerProfile);
    }

    @Test
    void savePortfolioItem_nullOrBlankFields_becomeNull() {
        PortfolioItemRequestDTO request = PortfolioItemRequestDTO.builder()
                .title(null)
                .description("   ")
                .clientName("")
                .demoUrl(null)
                .sourceUrl(" ")
                .categoryId(categoryId)
                .subcategoryIds(subcategoryIds)
                .freelancerProfileId(null)
                .build();

        JobCategory category = new JobCategory();
        Set<JobSubcategory> subcategories = new HashSet<>();
        PortfolioItem entity = new PortfolioItem();
        PortfolioItem savedItem = new PortfolioItem();
        PortfolioItemDetailDTO dto = PortfolioItemDetailDTO.builder().id(portfolioItemId).build();
        FreelancerProfile newProfile = new FreelancerProfile();

        when(jobCategoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(jobSubcategoryRepository.findAllById(subcategoryIds)).thenReturn(new ArrayList<>(subcategories));
        when(freelancerProfileRepository.save(any(FreelancerProfile.class))).thenReturn(newProfile);
        when(portfolioItemMapper.toEntity(any(), eq(category), eq(subcategories))).thenReturn(entity);
        when(repository.save(entity)).thenReturn(savedItem);
        when(portfolioItemMapper.toDetailDto(savedItem)).thenReturn(dto);

        PortfolioItemDetailDTO result = service.savePortfolioItem(request);

        assertSame(dto, result);
        assertNull(entity.getTitle());
        assertNull(entity.getDescription());
        assertNull(entity.getClientName());
        assertNull(entity.getDemoUrl());
        assertNull(entity.getSourceUrl());
    }

    @Test
    void savePortfolioItem_invalidFields_throwsIllegalArgumentException() {
        PortfolioItemRequestDTO request = PortfolioItemRequestDTO.builder()
                .title("badTitle")
                .description("badDescription")
                .demoUrl("badDemo")
                .sourceUrl("badSource")
                .categoryId(categoryId)
                .subcategoryIds(subcategoryIds)
                .freelancerProfileId(null)
                .build();

        JobCategory category = new JobCategory();
        Set<JobSubcategory> subcategories = new HashSet<>();

        when(jobCategoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(jobSubcategoryRepository.findAllById(subcategoryIds)).thenReturn(new ArrayList<>(subcategories));

        // Mock static SanitizationUtil
        try (MockedStatic<SanitizationUtil> util = mockStatic(SanitizationUtil.class)) {
            util.when(() -> SanitizationUtil.sanitizeText("badTitle")).thenReturn(null);
            util.when(() -> SanitizationUtil.sanitizeText("badDescription")).thenReturn(null);
            util.when(() -> SanitizationUtil.sanitizeUrl("badDemo")).thenReturn(null);
            util.when(() -> SanitizationUtil.sanitizeUrl("badSource")).thenReturn(null);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.savePortfolioItem(request));
            assertTrue(ex.getMessage().contains("Title") || ex.getMessage().contains("Description") ||
                    ex.getMessage().contains("Demo URL") || ex.getMessage().contains("Source URL"));
        }
    }


    @Test
    void updatePortfolioItem_success_updateFields() {
        PortfolioItem existing = new PortfolioItem();
        existing.setSubcategories(new HashSet<>());
        PortfolioItemRequestDTO request = PortfolioItemRequestDTO.builder()
                .categoryId(categoryId)
                .subcategoryIds(subcategoryIds)
                .title("Title")
                .description("Desc")
                .clientName("Client")
                .demoUrl("http://demo.com")
                .sourceUrl("http://source.com")
                .build();
        JobCategory category = new JobCategory();
        Set<JobSubcategory> subcategories = Set.of(new JobSubcategory(), new JobSubcategory());
        PortfolioItemDetailDTO dto = PortfolioItemDetailDTO.builder()
                .id(portfolioItemId)
                .build();

        when(repository.findById(portfolioItemId)).thenReturn(Optional.of(existing));
        when(jobCategoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(jobSubcategoryRepository.findAllById(subcategoryIds)).thenReturn(new ArrayList<>(subcategories));
        when(repository.save(existing)).thenReturn(existing);
        when(portfolioItemMapper.toDetailDto(existing)).thenReturn(dto);

        PortfolioItemDetailDTO result = service.updatePortfolioItem(portfolioItemId, request);
        assertSame(dto, result);
        assertEquals(category, existing.getCategory());
        assertEquals(subcategories, existing.getSubcategories());
        assertEquals("Title", existing.getTitle());
        assertEquals("Desc", existing.getDescription());
        assertEquals("Client", existing.getClientName());
        assertEquals("http://demo.com", existing.getDemoUrl());
        assertEquals("http://source.com", existing.getSourceUrl());
    }

    @Test
    void deletePortfolioItem_success() {
        PortfolioItem existing = new PortfolioItem();
        when(repository.findById(portfolioItemId)).thenReturn(Optional.of(existing));
        service.deletePortfolioItem(portfolioItemId);
        verify(repository).delete(existing);
    }

    @Test
    void uploadPortfolioItemImages_success() {
        PortfolioItem item = new PortfolioItem();
        item.setImageUrls(new HashSet<>());
        List<String> urls = List.of("http://img1.com", "http://img2.com");
        when(repository.findById(portfolioItemId)).thenReturn(Optional.of(item));
        try (MockedStatic<SanitizationUtil> util = mockStatic(SanitizationUtil.class)) {
            util.when(() -> SanitizationUtil.sanitizeUrl(any())).thenAnswer(i -> i.getArgument(0));
            service.uploadPortfolioItemImages(portfolioItemId, urls);
            assertTrue(item.getImageUrls().containsAll(urls));
            verify(repository).save(item);
        }
    }

    @Test
    void deletePortfolioItemImage_existingUrl_removesAndSaves() {
        PortfolioItem item = new PortfolioItem();
        item.setImageUrls(new HashSet<>(Set.of("http://img.com")));
        when(repository.findById(portfolioItemId)).thenReturn(Optional.of(item));

        service.deletePortfolioItemImage(portfolioItemId, "http://img.com");
        assertFalse(item.getImageUrls().contains("http://img.com"));
        verify(repository).save(item);
    }

    @Test
    void deletePortfolioItemImage_nonExistingUrl_doesNothing() {
        PortfolioItem item = new PortfolioItem();
        item.setImageUrls(new HashSet<>());
        when(repository.findById(portfolioItemId)).thenReturn(Optional.of(item));

        service.deletePortfolioItemImage(portfolioItemId, "http://nonexist.com");
        verify(repository, never()).save(any());
    }

    // --- Negative / exception tests ---

    @Test
    void getPortfolioItemById_notFound_throws() {
        when(repository.findById(portfolioItemId)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.getPortfolioItemById(portfolioItemId));
    }

    @Test
    void savePortfolioItem_categoryNotFound_throws() {
        PortfolioItemRequestDTO request = PortfolioItemRequestDTO.builder().categoryId(categoryId).build();
        when(jobCategoryRepository.findById(categoryId)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.savePortfolioItem(request));
    }

    @Test
    void savePortfolioItem_freelancerProfileNotFound_throws() {
        PortfolioItemRequestDTO request = PortfolioItemRequestDTO.builder().categoryId(categoryId).freelancerProfileId(freelancerProfileId).build();
        when(jobCategoryRepository.findById(categoryId)).thenReturn(Optional.of(new JobCategory()));
        when(jobSubcategoryRepository.findAllById(any())).thenReturn(new ArrayList<>());
        when(freelancerProfileRepository.findById(freelancerProfileId)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.savePortfolioItem(request));
    }

    @Test
    void savePortfolioItem_nullFreelancerProfile_createsNewProfile() {
        PortfolioItemRequestDTO request = PortfolioItemRequestDTO.builder()
                .categoryId(categoryId)
                .subcategoryIds(subcategoryIds)
                .freelancerProfileId(null) // null -> new profile created
                .build();

        JobCategory category = new JobCategory();
        Set<JobSubcategory> subcategories = Set.of(new JobSubcategory(), new JobSubcategory());
        PortfolioItem entity = new PortfolioItem();
        PortfolioItem savedItem = new PortfolioItem();
        PortfolioItemDetailDTO detailDTO = PortfolioItemDetailDTO.builder().id(portfolioItemId).build();
        FreelancerProfile newProfile = new FreelancerProfile();
        newProfile.setPortfolioItems(new HashSet<>());

        // --- Mocks ---
        when(jobCategoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(jobSubcategoryRepository.findAllById(subcategoryIds)).thenReturn(new ArrayList<>(subcategories));
        when(freelancerProfileRepository.save(any(FreelancerProfile.class))).thenReturn(newProfile);
        when(portfolioItemMapper.toEntity(any(), eq(category), eq(subcategories))).thenReturn(entity);
        when(repository.save(entity)).thenReturn(savedItem);
        when(portfolioItemMapper.toDetailDto(savedItem)).thenReturn(detailDTO);

        // --- Execute ---
        PortfolioItemDetailDTO result = service.savePortfolioItem(request);

        // --- Assertions ---
        assertSame(detailDTO, result);
        assertTrue(newProfile.getPortfolioItems().contains(savedItem));

        // --- Verify saves ---
        verify(freelancerProfileRepository, times(2)).save(any(FreelancerProfile.class));
        verify(repository).save(entity);
        verify(portfolioItemMapper).toDetailDto(savedItem);
    }

    @Test
    void savePortfolioItem_invalidUrl_throwsIllegalArgumentException() {
        PortfolioItemRequestDTO request = PortfolioItemRequestDTO.builder()
                .categoryId(categoryId)
                .subcategoryIds(subcategoryIds)
                .demoUrl("http://some-invalid-url") // will trigger sanitization failure
                .build();

        JobCategory category = new JobCategory();
        FreelancerProfile profile = new FreelancerProfile();

        PortfolioItemRepository repo = mock(PortfolioItemRepository.class);
        PortfolioItemMapper mapper = mock(PortfolioItemMapper.class);
        JobCategoryRepository jobCatRepo = mock(JobCategoryRepository.class);
        JobSubcategoryRepository jobSubRepo = mock(JobSubcategoryRepository.class);
        FreelancerProfileRepository profileRepo = mock(FreelancerProfileRepository.class);

        when(jobCatRepo.findById(categoryId)).thenReturn(Optional.of(category));
        when(jobSubRepo.findAllById(subcategoryIds)).thenReturn(new ArrayList<>());
        when(profileRepo.save(any())).thenReturn(profile);

        PortfolioItemServiceImpl service = new PortfolioItemServiceImpl(
                repo, mapper, jobCatRepo, jobSubRepo, profileRepo
        );

        // Mock the static SanitizationUtil.sanitizeUrl to return null
        try (MockedStatic<SanitizationUtil> mockedStatic = mockStatic(SanitizationUtil.class)) {
            mockedStatic.when(() -> SanitizationUtil.sanitizeUrl(anyString())).thenReturn(null);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.savePortfolioItem(request));

            assertTrue(ex.getMessage().contains("Demo URL contains invalid characters"));
        }
    }

    @Test
    void updatePortfolioItem_itemNotFound_throws() {
        PortfolioItemRequestDTO request = PortfolioItemRequestDTO.builder()
                .categoryId(categoryId)
                .subcategoryIds(new HashSet<>(List.of(1L, 2L)))
                .clientName("Client")
                .title("Title")
                .description("Desc")
                .demoUrl("http://demo.com")
                .sourceUrl("http://source.com")
                .freelancerProfileId(freelancerProfileId)
                .build();
        when(repository.findById(portfolioItemId)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.updatePortfolioItem(portfolioItemId, request));
    }

    @Test
    void updatePortfolioItem_nullCategory_clearsCategory() {
        PortfolioItem existing = new PortfolioItem();
        existing.setSubcategories(new HashSet<>());

        PortfolioItemRequestDTO request = PortfolioItemRequestDTO.builder()
                .categoryId(null) // should clear category
                .subcategoryIds(subcategoryIds)
                .build();

        Set<JobSubcategory> subcategories = Set.of(new JobSubcategory(), new JobSubcategory());
        PortfolioItemDetailDTO dto = PortfolioItemDetailDTO.builder().id(portfolioItemId).build();

        when(repository.findById(portfolioItemId)).thenReturn(Optional.of(existing));
        when(jobSubcategoryRepository.findAllById(subcategoryIds)).thenReturn(new ArrayList<>(subcategories));
        when(repository.save(existing)).thenReturn(existing);
        when(portfolioItemMapper.toDetailDto(existing)).thenReturn(dto);

        PortfolioItemDetailDTO result = service.updatePortfolioItem(portfolioItemId, request);

        assertSame(dto, result);
        assertNull(existing.getCategory());
        assertEquals(subcategories, existing.getSubcategories());
    }

    @Test
    void updatePortfolioItem_nullSubcategories_clearsSubcategories() {
        PortfolioItem existing = new PortfolioItem();
        existing.setSubcategories(new HashSet<>(Set.of(new JobSubcategory())));

        PortfolioItemRequestDTO request = PortfolioItemRequestDTO.builder()
                .subcategoryIds(null) // should clear subcategories
                .build();

        PortfolioItemDetailDTO dto = PortfolioItemDetailDTO.builder().id(portfolioItemId).build();

        when(repository.findById(portfolioItemId)).thenReturn(Optional.of(existing));
        when(repository.save(existing)).thenReturn(existing);
        when(portfolioItemMapper.toDetailDto(existing)).thenReturn(dto);

        PortfolioItemDetailDTO result = service.updatePortfolioItem(portfolioItemId, request);

        assertSame(dto, result);
        assertTrue(existing.getSubcategories().isEmpty());
    }

    @Test
    void updatePortfolioItem_categoryNotFound_throwsResourceNotFoundException() {
        PortfolioItemRequestDTO request = PortfolioItemRequestDTO.builder()
                .categoryId(categoryId)
                .build();

        PortfolioItem existing = new PortfolioItem();
        when(repository.findById(portfolioItemId)).thenReturn(Optional.of(existing));
        when(jobCategoryRepository.findById(request.getCategoryId())).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> service.updatePortfolioItem(portfolioItemId, request));
        assertEquals("Job category not found.", ex.getMessage());
    }

    @Test
    void updatePortfolioItem_subcategoriesNull_clearsExistingSubcategories() {
        PortfolioItemRequestDTO request = PortfolioItemRequestDTO.builder()
                .categoryId(categoryId)
                .subcategoryIds(null) // triggers .clear()
                .build();

        PortfolioItem existing = new PortfolioItem();
        existing.setSubcategories(new HashSet<>(List.of(new JobSubcategory())));

        when(repository.findById(portfolioItemId)).thenReturn(Optional.of(existing));
        when(jobCategoryRepository.findById(categoryId)).thenReturn(Optional.of(new JobCategory()));
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(portfolioItemMapper.toDetailDto(any())).thenReturn(PortfolioItemDetailDTO.builder().build());

        PortfolioItemDetailDTO result = service.updatePortfolioItem(portfolioItemId, request);

        assertTrue(existing.getSubcategories().isEmpty());
    }

    @Test
    void updatePortfolioItem_textFieldsBlank_setNull() {
        PortfolioItemRequestDTO request = PortfolioItemRequestDTO.builder()
                .title("   ")
                .description("")
                .clientName(null)
                .demoUrl("   ")
                .sourceUrl("")
                .categoryId(categoryId)
                .subcategoryIds(subcategoryIds)
                .build();

        PortfolioItem existing = new PortfolioItem();
        existing.setSubcategories(new HashSet<>());
        when(repository.findById(portfolioItemId)).thenReturn(Optional.of(existing));
        when(jobCategoryRepository.findById(categoryId)).thenReturn(Optional.of(new JobCategory()));
        when(jobSubcategoryRepository.findAllById(subcategoryIds)).thenReturn(new ArrayList<>());
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(portfolioItemMapper.toDetailDto(any())).thenReturn(PortfolioItemDetailDTO.builder().build());

        PortfolioItemDetailDTO result = service.updatePortfolioItem(portfolioItemId, request);

        assertNull(existing.getTitle());
        assertNull(existing.getDescription());
        assertNull(existing.getClientName());
        assertNull(existing.getDemoUrl());
        assertNull(existing.getSourceUrl());
    }

    @Test
    void uploadPortfolioItemImages_nonExistingItem_throwsResourceNotFoundException() {
        UUID portfolioItemId = UUID.randomUUID();
        List<String> imageUrls = List.of("http://example.com/image1.jpg");

        // Mock repository to return empty
        when(repository.findById(portfolioItemId)).thenReturn(Optional.empty());

        // Assert that ResourceNotFoundException is thrown
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () ->
                service.uploadPortfolioItemImages(portfolioItemId, imageUrls)
        );

        assertEquals("Portfolio item not found.", exception.getMessage());
    }


    @Test
    void uploadPortfolioItemImages_nullOrEmpty_throws() {
        PortfolioItem item = new PortfolioItem();
        when(repository.findById(portfolioItemId)).thenReturn(Optional.of(item));
        assertThrows(UploadFileException.class, () -> service.uploadPortfolioItemImages(portfolioItemId, null));
        assertThrows(UploadFileException.class, () -> service.uploadPortfolioItemImages(portfolioItemId, Collections.emptyList()));
    }

    @Test
    void uploadPortfolioItemImages_allSanitizedNull_throws() {
        PortfolioItem item = new PortfolioItem();
        when(repository.findById(portfolioItemId)).thenReturn(Optional.of(item));
        try (MockedStatic<SanitizationUtil> util = mockStatic(SanitizationUtil.class)) {
            util.when(() -> SanitizationUtil.sanitizeUrl(any())).thenReturn(null);
            assertThrows(UploadFileException.class, () -> service.uploadPortfolioItemImages(portfolioItemId, List.of("http://img.com")));
        }
    }

    @Test
    void deletePortfolioItem_notFound_throws() {
        when(repository.findById(portfolioItemId)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.deletePortfolioItem(portfolioItemId));
    }

    @Test
    void deletePortfolioItemImage_nonExistingItem_throwsResourceNotFoundException() {
        UUID portfolioItemId = UUID.randomUUID();
        String imageUrl = "http://example.com/image.jpg";

        when(repository.findById(portfolioItemId)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () ->
                service.deletePortfolioItemImage(portfolioItemId, imageUrl)
        );

        assertEquals("Portfolio item not found", exception.getMessage());
    }


}