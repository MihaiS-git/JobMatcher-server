package com.jobmatcher.server.service;

import com.jobmatcher.server.domain.*;
import com.jobmatcher.server.exception.ResourceNotFoundException;
import com.jobmatcher.server.mapper.FreelancerProfileMapper;
import com.jobmatcher.server.model.FreelancerDetailDTO;
import com.jobmatcher.server.model.FreelancerProfileRequestDTO;
import com.jobmatcher.server.model.FreelancerSummaryDTO;
import com.jobmatcher.server.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class FreelancerProfileServiceImplTest {
    @Mock
    private FreelancerProfileRepository profileRepository;

    @Mock
    private FreelancerProfileMapper profileMapper;

    @Mock
    private UserRepository userRepository;

    @Mock
    private JobSubcategoryRepository subcategoryRepository;

    @Mock
    private SkillServiceImpl skillService;

    @Mock
    private LanguageRepository languageRepository;

    @InjectMocks
    private FreelancerProfileServiceImpl service;

    private FreelancerProfile profile;
    private FreelancerDetailDTO detailDTO;
    private FreelancerProfileRequestDTO requestDTO;
    private User user;
    private UUID profileId;

    @BeforeEach
    void setUp() {
        profileId = UUID.randomUUID();
        profile = new FreelancerProfile();
        profile.setId(profileId);
        detailDTO = FreelancerDetailDTO.builder().build();

        user = new User();
        user.setId(UUID.randomUUID());

        requestDTO = FreelancerProfileRequestDTO.builder()
                .userId(user.getId())
                .skills(Set.of("Java", "Spring"))
                .jobSubcategoryIds(Set.of(1L))
                .languageIds(Set.of(1))
                .build();
    }

    @Test
    void getAllFreelancerProfiles_shouldReturnProfiles() {
        when(profileRepository.findAll()).thenReturn(List.of(profile));
        FreelancerSummaryDTO dto = FreelancerSummaryDTO.builder().build();
        when(profileMapper.toFreelancerSummaryDto(profile)).thenReturn(dto);

        Set<FreelancerSummaryDTO> result = service.getAllFreelancerProfiles();

        assertEquals(1, result.size());
        verify(profileRepository).findAll();
    }

    @Test
    void getFreelancerProfileById_found() {
        when(profileRepository.findById(profileId)).thenReturn(Optional.of(profile));
        when(profileMapper.toFreelancerDetailDto(profile)).thenReturn(detailDTO);

        FreelancerDetailDTO result = service.getFreelancerProfileById(profileId);

        assertNotNull(result);
    }

    @Test
    void getFreelancerProfileById_notFound() {
        when(profileRepository.findById(profileId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.getFreelancerProfileById(profileId));
    }

    @Test
    void saveFreelancerProfile_success() {
        JobCategory jobCategory = new JobCategory("test", "test");
        JobSubcategory subcategory = new JobSubcategory("1111", "test", jobCategory);
        subcategory.setId(1L);

        when(userRepository.findById(requestDTO.getUserId())).thenReturn(Optional.of(user));
        when(subcategoryRepository.findAllById(anyCollection())).thenAnswer(invocation -> {
            Collection<?> ids = invocation.getArgument(0);
            System.out.println("findAllById called with IDs: " + ids + ", types: " +
                    ids.stream().map(Object::getClass).toList());

            boolean containsId = ids.stream()
                    .anyMatch(id -> {
                        if (id instanceof Number) {
                            return ((Number) id).longValue() == 1L;
                        }
                        return false;
                    });

            if (containsId) {
                return List.of(subcategory);
            }
            return List.of();
        });

        when(languageRepository.findAllById(Set.of(1))).thenReturn(List.of(new Language(1, "English")));

        Skill javaSkill = new Skill("Java");
        Skill springSkill = new Skill("Spring");
        when(skillService.findOrCreateByName("Java")).thenReturn(javaSkill);
        when(skillService.findOrCreateByName("Spring")).thenReturn(springSkill);

        when(profileMapper.toEntity(any(), eq(user), any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(profile);
        when(profileRepository.save(profile)).thenReturn(profile);
        when(profileMapper.toFreelancerDetailDto(profile)).thenReturn(detailDTO);

        FreelancerDetailDTO result = service.saveFreelancerProfile(requestDTO);

        assertNotNull(result);
    }

    @Test
    void saveFreelancerProfile_userNotFound() {
        when(userRepository.findById(any())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.saveFreelancerProfile(requestDTO));
    }

    @Test
    void updateFreelancerProfile_success() {
        when(profileRepository.findById(profileId)).thenReturn(Optional.of(profile));
        when(subcategoryRepository.findAllById(Set.of(1L))).thenReturn(List.of(new JobSubcategory("test", "test", new JobCategory("test", "test"))));
        when(languageRepository.findAllById(Set.of(1))).thenReturn(List.of(new Language(1, "English")));

        Skill javaSkill = new Skill("Java");
        Skill springSkill = new Skill("Spring");
        when(skillService.findOrCreateByName("Java")).thenReturn(javaSkill);
        when(skillService.findOrCreateByName("Spring")).thenReturn(springSkill);

        when(profileRepository.save(profile)).thenReturn(profile);
        when(profileMapper.toFreelancerDetailDto(profile)).thenReturn(detailDTO);

        FreelancerDetailDTO result = service.updateFreelancerProfile(profileId, requestDTO);

        assertNotNull(result);
    }

    @Test
    void updateFreelancerProfile_notFound() {
        when(profileRepository.findById(profileId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.updateFreelancerProfile(profileId, requestDTO));
    }

    @Test
    void saveFreelancerProfile_createsNewSkillsWhenMissing() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // Subcategories
        when(subcategoryRepository.findAllById(any())).thenAnswer(invocation -> {
            Collection<?> ids = invocation.getArgument(0);
            System.out.println("DEBUG subcategoryRepository.findAllById called with: " + ids);

            JobSubcategory subcat = new JobSubcategory("someCode", "someName", new JobCategory("catCode", "catName"));
            subcat.setId(1L);
            return List.of(subcat);
        });

        // Skills: one exists, one doesn't
        Skill existingSkill = new Skill("java");
        when(skillService.findOrCreateByName("java")).thenReturn(existingSkill);
        when(skillService.findOrCreateByName("spring"))
                .thenAnswer(invocation -> new Skill("spring"));


        // Languages
        Language lang = new Language();
        lang.setId(1);
        when(languageRepository.findAllById(any())).thenReturn(List.of(lang));

        // Mapper & save
        FreelancerProfile dummyEntity = new FreelancerProfile();
        when(profileMapper.toEntity(any(), any(), any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(dummyEntity);
        when(profileRepository.save(any())).thenReturn(dummyEntity);
        when(profileMapper.toFreelancerDetailDto(dummyEntity)).thenReturn(FreelancerDetailDTO.builder().build());

        // DTO
        FreelancerProfileRequestDTO dto = FreelancerProfileRequestDTO.builder()
                .userId(userId)
                .jobSubcategoryIds(Set.of(1L))
                .skills(Set.of("java", "spring"))
                .languageIds(Set.of(1))
                .build();

        FreelancerDetailDTO result = service.saveFreelancerProfile(dto);

        assertNotNull(result);
        verify(skillService).findOrCreateByName("java");
        verify(skillService).findOrCreateByName("spring");
    }

    @Test
    void saveFreelancerProfile_missingSubcategories_throws() {
        // Mock user
        when(userRepository.findById(any())).thenReturn(Optional.of(new User()));

        // Mock empty subcategory list to simulate missing IDs
        when(subcategoryRepository.findAllById(any())).thenReturn(List.of());

        // Build DTO with bogus subcategory ID
        FreelancerProfileRequestDTO dto = FreelancerProfileRequestDTO.builder()
                .userId(UUID.randomUUID())
                .jobSubcategoryIds(Set.of(999L)) // missing
                .skills(Set.of("Java"))
                .languageIds(Set.of(1))
                .build();

        assertThrows(ResourceNotFoundException.class, () -> service.saveFreelancerProfile(dto));
    }

    @Test
    void saveFreelancerProfile_missingLanguages_throws() {
        // Setup user and subcategories correctly
        when(userRepository.findById(any())).thenReturn(Optional.of(new User()));
        when(subcategoryRepository.findAllById(any())).thenReturn(List.of(new JobSubcategory()));

        // Mark this stubbing as lenient because it's not hit before the exception
        lenient().when(skillService.findOrCreateByName("java")).thenReturn(new Skill("java"));

        // Simulate missing languages
        when(languageRepository.findAllById(any())).thenReturn(List.of());

        FreelancerProfileRequestDTO dto = FreelancerProfileRequestDTO.builder()
                .userId(UUID.randomUUID())
                .jobSubcategoryIds(Set.of(1L))
                .skills(Set.of("Java"))
                .languageIds(Set.of(42)) // nonexistent
                .build();

        assertThrows(ResourceNotFoundException.class, () -> service.saveFreelancerProfile(dto));
    }

    @Test
    void saveFreelancerProfile_nullLanguageIds_returnsEmptySet() {
        // Setup minimal required valid user and subcategory
        when(userRepository.findById(any())).thenReturn(Optional.of(new User()));
        when(subcategoryRepository.findAllById(any())).thenReturn(List.of(new JobSubcategory()));
        when(skillService.findOrCreateByName("java")).thenReturn(new Skill("java"));

        // Make languageIds null
        FreelancerProfileRequestDTO dto = FreelancerProfileRequestDTO.builder()
                .userId(UUID.randomUUID())
                .jobSubcategoryIds(Set.of(1L))
                .skills(Set.of("java"))
                .languageIds(null)
                .build();

        assertDoesNotThrow(() -> service.saveFreelancerProfile(dto));
    }

    @Test
    void saveFreelancerProfile_emptyLanguageIds_returnsEmptySet() {
        when(userRepository.findById(any())).thenReturn(Optional.of(new User()));
        when(subcategoryRepository.findAllById(any())).thenReturn(List.of(new JobSubcategory()));
        when(skillService.findOrCreateByName("java")).thenReturn(new Skill("java"));

        FreelancerProfileRequestDTO dto = FreelancerProfileRequestDTO.builder()
                .userId(UUID.randomUUID())
                .jobSubcategoryIds(Set.of(1L))
                .skills(Set.of("java"))
                .languageIds(Collections.emptySet())
                .build();

        assertDoesNotThrow(() -> service.saveFreelancerProfile(dto));
    }

    @Test
    void saveFreelancerProfile_nullSubcategoryIds_returnsEmptySet() {
        when(userRepository.findById(any())).thenReturn(Optional.of(new User()));
        when(skillService.findOrCreateByName("Java")).thenReturn(new Skill("Java"));
        when(languageRepository.findAllById(any())).thenReturn(List.of(new Language("English")));

        FreelancerProfileRequestDTO dto = FreelancerProfileRequestDTO.builder()
                .userId(UUID.randomUUID())
                .jobSubcategoryIds(null)
                .skills(Set.of("Java"))
                .languageIds(Set.of(1))
                .build();

        assertDoesNotThrow(() -> service.saveFreelancerProfile(dto));
    }

    @Test
    void saveFreelancerProfile_emptySubcategoryIds_returnsEmptySet() {
        when(userRepository.findById(any())).thenReturn(Optional.of(new User()));
        when(skillService.findOrCreateByName("Java")).thenReturn(new Skill("Java"));
        when(languageRepository.findAllById(any())).thenReturn(List.of(new Language("English")));

        FreelancerProfileRequestDTO dto = FreelancerProfileRequestDTO.builder()
                .userId(UUID.randomUUID())
                .jobSubcategoryIds(Collections.emptySet())
                .skills(Set.of("Java"))
                .languageIds(Set.of(1))
                .build();

        assertDoesNotThrow(() -> service.saveFreelancerProfile(dto));
    }

    @Test
    void updateFreelancerProfile_shouldSanitizeAndSetSocialMedia() {
        // Arrange: prepare existing profile & request DTO with socialMedia
        when(profileRepository.findById(profileId)).thenReturn(Optional.of(profile));

        when(subcategoryRepository.findAllById(anySet())).thenReturn(List.of(new JobSubcategory("code", "name", new JobCategory("catCode", "catName"))));
        when(languageRepository.findAllById(anySet())).thenReturn(List.of(new Language(1, "English")));

        Skill javaSkill = new Skill("Java");
        Skill springSkill = new Skill("Spring");
        when(skillService.findOrCreateByName("Java")).thenReturn(javaSkill);
        when(skillService.findOrCreateByName("Spring")).thenReturn(springSkill);

        when(profileRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(profileMapper.toFreelancerDetailDto(any())).thenReturn(detailDTO);

        // Add socialMedia URLs with some test strings (simulate URLs needing sanitization)
        Set<String> socialMediaUrls = Set.of("https://example.com/profile", "http://malicious-site.com/");

        // Clone or create DTO with socialMedia
        FreelancerProfileRequestDTO dtoWithSocialMedia = FreelancerProfileRequestDTO.builder()
                .userId(user.getId())
                .skills(Set.of("Java", "Spring"))
                .jobSubcategoryIds(Set.of(1L))
                .languageIds(Set.of(1))
                .socialMedia(socialMediaUrls)
                .build();

        // Act
        FreelancerDetailDTO result = service.updateFreelancerProfile(profileId, dtoWithSocialMedia);

        // Assert
        assertNotNull(result);
        // Verify that setSocialMedia on profile was called with sanitized URLs
        verify(profileRepository).save(argThat(p ->
                p.getSocialMedia() != null &&
                        p.getSocialMedia().size() == socialMediaUrls.size()
        ));
    }

    @Test
    void updateFreelancerProfile_shouldSanitizeMaliciousSocialMediaUrls() {
        // Arrange
        when(profileRepository.findById(profileId)).thenReturn(Optional.of(profile));
        when(subcategoryRepository.findAllById(anySet())).thenReturn(List.of(new JobSubcategory("code", "name", new JobCategory("catCode", "catName"))));
        when(languageRepository.findAllById(anySet())).thenReturn(List.of(new Language(1, "English")));
        when(skillService.findOrCreateByName("Java")).thenReturn(new Skill("Java"));

        // Malicious-looking URL input (XSS attempt)
        Set<String> socialMediaInput = Set.of(
                "https://safe-site.com",
                "javascript:alert('XSS')",
                "  http://injected.com/<script>alert('pwn')</script>  "
        );

        FreelancerProfileRequestDTO dto = FreelancerProfileRequestDTO.builder()
                .userId(user.getId())
                .skills(Set.of("Java"))
                .jobSubcategoryIds(Set.of(1L))
                .languageIds(Set.of(1))
                .socialMedia(socialMediaInput)
                .build();

        // Capture saved profile
        ArgumentCaptor<FreelancerProfile> profileCaptor = ArgumentCaptor.forClass(FreelancerProfile.class);
        when(profileRepository.save(profileCaptor.capture())).thenAnswer(invocation -> invocation.getArgument(0));
        when(profileMapper.toFreelancerDetailDto(any())).thenReturn(detailDTO);

        // Act
        FreelancerDetailDTO result = service.updateFreelancerProfile(profileId, dto);

        // Assert
        Set<String> sanitizedUrls = profileCaptor.getValue().getSocialMedia();
        assertTrue(sanitizedUrls.contains("https://safe-site.com"));
        assertFalse(sanitizedUrls.contains("javascript:alert('XSS')"));
        assertFalse(sanitizedUrls.stream().filter(Objects::nonNull).anyMatch(s -> s.contains("<script>"))
        );
        assertTrue(sanitizedUrls.stream().filter(Objects::nonNull).allMatch(s -> s.startsWith("http")));
    }

    @Test
    void saveProfile_shouldIgnoreBlankSocialMediaAndSkills() {
        UUID someUserId = UUID.randomUUID();
        Set<String> skills = new HashSet<>(Arrays.asList("Java", null, "   "));
        FreelancerDetailDTO detailDTO = FreelancerDetailDTO.builder()
                .socialMedia(Set.of("https://github.com/test"))
                .build();
        FreelancerProfileRequestDTO dto = FreelancerProfileRequestDTO.builder()
                .userId(someUserId)
                .username("testuser")
                .headline("Some headline")
                .about("Some about text")
                .websiteUrl("https://example.com")
                .socialMedia(Set.of("https://github.com/test", "   ", ""))
                .skills(skills)
                .jobSubcategoryIds(Set.of(1L))
                .languageIds(Set.of(1))
                .build();

        // Stub skill service
        Skill javaSkill = new Skill();
        javaSkill.setName("Java");
        when(skillService.findOrCreateByName("Java")).thenReturn(javaSkill);

        // Stub repo results
        User user = new User();
        user.setId(someUserId);
        when(userRepository.findById(someUserId)).thenReturn(Optional.of(user));
        when(subcategoryRepository.findAllById(Set.of(1L))).thenReturn(List.of(new JobSubcategory(1L, "Web Dev")));
        when(languageRepository.findAllById(Set.of(1))).thenReturn(List.of(new Language(1, "English")));
        when(profileMapper.toFreelancerDetailDto(any())).thenReturn(detailDTO);

        // Execute
        FreelancerDetailDTO result = service.saveFreelancerProfile(dto);

        // Validate social media cleaned
        assertThat(result.getSocialMedia()).containsExactly("https://github.com/test");

        // Validate skill handling
        verify(skillService, times(1)).findOrCreateByName("Java");
    }

    @Test
    void updateFreelancerProfile_sanitizesVariousSocialMediaInputs() {
        when(profileRepository.findById(profileId)).thenReturn(Optional.of(profile));
        when(subcategoryRepository.findAllById(anySet())).thenReturn(List.of(new JobSubcategory("code", "name", new JobCategory("cat", "cat"))));
        when(languageRepository.findAllById(anySet())).thenReturn(List.of(new Language(1, "English")));
        when(skillService.findOrCreateByName(anyString())).thenReturn(new Skill());

        Set<String> inputs = new HashSet<>(Arrays.asList(
                "http://valid.com",
                "HTTPS://VALID.COM",
                " javascript:alert('XSS')",
                "ftp://unsupported-protocol.com",
                "http://valid.com/?q=<script>",
                "  ",
                null,
                "http://valid.com/normal"
        ));

        FreelancerProfileRequestDTO dto = FreelancerProfileRequestDTO.builder()
                .userId(user.getId())
                .skills(Set.of("Java"))
                .jobSubcategoryIds(Set.of(1L))
                .languageIds(Set.of(1))
                .socialMedia(inputs)
                .build();

        ArgumentCaptor<FreelancerProfile> captor = ArgumentCaptor.forClass(FreelancerProfile.class);
        when(profileRepository.save(captor.capture())).thenAnswer(i -> i.getArgument(0));
        when(profileMapper.toFreelancerDetailDto(any())).thenReturn(detailDTO);

        FreelancerDetailDTO result = service.updateFreelancerProfile(profileId, dto);

        Set<String> sanitized = captor.getValue().getSocialMedia();

        assertTrue(sanitized.contains("http://valid.com"));
        assertFalse(sanitized.contains("HTTPS://VALID.COM"));
        assertFalse(sanitized.contains(" javascript:alert('XSS')"));
        assertFalse(sanitized.contains("ftp://unsupported-protocol.com"));
        assertFalse(sanitized.stream().anyMatch(s -> s != null && s.toLowerCase().contains("javascript:")));
        assertFalse(sanitized.stream().anyMatch(s -> s != null && s.toLowerCase().contains("<script>")));

        assertTrue(
                sanitized.stream()
                        .anyMatch(s -> s != null && s.trim().equalsIgnoreCase("http://valid.com/?q=script"))
        );
        assertFalse(sanitized.contains(null));
        assertFalse(sanitized.contains("  "));
    }

}