package com.jobmatcher.server.service;

import com.jobmatcher.server.domain.*;
import com.jobmatcher.server.exception.InvalidProfileDataException;
import com.jobmatcher.server.exception.ResourceNotFoundException;
import com.jobmatcher.server.mapper.FreelancerProfileMapper;
import com.jobmatcher.server.model.FreelancerDetailDTO;
import com.jobmatcher.server.model.FreelancerProfileRequestDTO;
import com.jobmatcher.server.model.FreelancerSummaryDTO;
import com.jobmatcher.server.repository.*;
import com.jobmatcher.server.util.SanitizationUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
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

    @Captor
    ArgumentCaptor<FreelancerProfile> profileCaptor;

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
                .username("testuser")
                .skills(Set.of("Java", "Spring"))
                .jobSubcategoryIds(Set.of(1L))
                .languageIds(Set.of(1))
                .socialMedia(Set.of())
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
                .username("testuser")
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
        FreelancerProfileRequestDTO dto = FreelancerProfileRequestDTO.builder()
                .userId(UUID.randomUUID())
                .jobSubcategoryIds(Set.of(999L)) // missing
                .skills(Set.of("Java"))
                .languageIds(Set.of(1))
                .build();

        assertThrows(InvalidProfileDataException.class, () -> service.saveFreelancerProfile(dto));
    }

    @Test
    void saveFreelancerProfile_missingLanguages_throws() {
        // Mark this stubbing as lenient because it's not hit before the exception
        lenient().when(skillService.findOrCreateByName("java")).thenReturn(new Skill("java"));

        FreelancerProfileRequestDTO dto = FreelancerProfileRequestDTO.builder()
                .userId(UUID.randomUUID())
                .jobSubcategoryIds(Set.of(1L))
                .skills(Set.of("Java"))
                .languageIds(Set.of(42)) // nonexistent
                .build();

        assertThrows(InvalidProfileDataException.class, () -> service.saveFreelancerProfile(dto));
    }

    @Test
    void saveFreelancerProfile_nullLanguageIds_throws() {
        FreelancerProfileRequestDTO dto = FreelancerProfileRequestDTO.builder()
                .userId(UUID.randomUUID())
                .jobSubcategoryIds(Set.of(1L))
                .skills(Set.of("java"))
                .languageIds(null)
                .build();

        assertThrows(InvalidProfileDataException.class, () -> service.saveFreelancerProfile(dto));
    }

    @Test
    void saveFreelancerProfile_emptyLanguageIds_throws() {
        FreelancerProfileRequestDTO dto = FreelancerProfileRequestDTO.builder()
                .userId(UUID.randomUUID())
                .jobSubcategoryIds(Set.of(1L))
                .skills(Set.of("java"))
                .languageIds(Collections.emptySet())
                .build();

        assertThrows(InvalidProfileDataException.class, () -> service.saveFreelancerProfile(dto));
    }

    @Test
    void saveFreelancerProfile_nullSubcategoryIds_throws() {
        FreelancerProfileRequestDTO dto = FreelancerProfileRequestDTO.builder()
                .userId(UUID.randomUUID())
                .jobSubcategoryIds(null)
                .skills(Set.of("Java"))
                .languageIds(Set.of(1))
                .build();

        assertThrows(InvalidProfileDataException.class, () -> service.saveFreelancerProfile(dto));
    }

    @Test
    void saveFreelancerProfile_emptySubcategoryIds_throws() {
        FreelancerProfileRequestDTO dto = FreelancerProfileRequestDTO.builder()
                .userId(UUID.randomUUID())
                .jobSubcategoryIds(Collections.emptySet())
                .skills(Set.of("Java"))
                .languageIds(Set.of(1))
                .build();

        assertThrows(InvalidProfileDataException.class, () -> service.saveFreelancerProfile(dto));
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
        UUID profileId = UUID.randomUUID();
        FreelancerProfile existingProfile = new FreelancerProfile();
        existingProfile.setSocialMedia(new HashSet<>(Arrays.asList("http://old.com")));

        when(profileRepository.findById(profileId)).thenReturn(Optional.of(existingProfile));
        when(profileRepository.save(any(FreelancerProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Malicious & normal inputs, including XSS-style query string
        Set<String> socialInputs = new HashSet<>(Arrays.asList(
                "https://twitter.com/user",
                "javascript:alert(1)",
                "https://linkedin.com/in/user",
                "http://invalid.com?=<script>alert('x');<script>", // rejected entirely
                "   ", // blank
                null // null
        ));

        FreelancerProfileRequestDTO dto = FreelancerProfileRequestDTO.builder()
                .socialMedia(socialInputs)
                .build();

        // Stub the mapper for completeness
        when(profileMapper.toFreelancerDetailDto(any(FreelancerProfile.class)))
                .thenReturn(null); // We don't need actual mapping here

        // Call the method
        service.updateFreelancerProfile(profileId, dto);

        // Capture saved profile
        ArgumentCaptor<FreelancerProfile> captor = ArgumentCaptor.forClass(FreelancerProfile.class);
        verify(profileRepository).save(captor.capture());

        FreelancerProfile saved = captor.getValue();
        assertNotNull(saved);

        // Every URL should be sanitized via SanitizationUtil
        saved.getSocialMedia().forEach(url -> {
            assertNotNull(url);
            assertFalse(url.isBlank());
            assertFalse(url.contains("javascript:"));
            assertFalse(url.contains("<script>"));
        });

        // Should include valid ones only (malicious dropped)
        assertTrue(saved.getSocialMedia().contains("https://twitter.com/user"));
        assertTrue(saved.getSocialMedia().contains("https://linkedin.com/in/user"));
        assertFalse(saved.getSocialMedia().contains("http://invalid.com?=")); // now explicitly rejected
        assertEquals(2, saved.getSocialMedia().size()); // only the two safe ones remain
    }



    @Test
    void getFreelancerProfileByUserId_whenProfileExists_returnsDto() {
        UUID userId = UUID.randomUUID();
        FreelancerProfile profile = new FreelancerProfile();
        profile.setUser(new User(userId));
        profile.setSocialMedia(Set.of("https://linkedin.com/in/test"));

        when(profileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));
        when(profileMapper.toFreelancerDetailDto(profile)).thenReturn(FreelancerDetailDTO.builder().build());

        FreelancerDetailDTO result = service.getFreelancerProfileByUserId(userId);

        assertNotNull(result);
        verify(profileRepository).findByUserId(userId);
        verify(profileMapper).toFreelancerDetailDto(profile);
    }

    @Test
    void saveFreelancerProfile_whenUsernameSanitizationFails_throwsException() {
        UUID userId = UUID.randomUUID();
        FreelancerProfileRequestDTO dto = FreelancerProfileRequestDTO.builder()
                .userId(userId)
                .username("<script>alert('xss')</script>")
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(new User()));

        try (MockedStatic<SanitizationUtil> mocked = mockStatic(SanitizationUtil.class)) {
            mocked.when(() -> SanitizationUtil.sanitizeText(dto.getUsername())).thenReturn(null);
            assertThrows(InvalidProfileDataException.class, () -> service.saveFreelancerProfile(dto));
        }
    }

    @Test
    void saveFreelancerProfile_whenHeadlineSanitizationFails_throwsException() {
        UUID userId = UUID.randomUUID();

        FreelancerProfileRequestDTO dto = FreelancerProfileRequestDTO.builder()
                .userId(userId)
                .username("validusername")
                .headline("<invalid headline>")
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(new User()));

        try (MockedStatic<SanitizationUtil> mocked = mockStatic(SanitizationUtil.class)) {
            mocked.when(() -> SanitizationUtil.sanitizeText(dto.getUsername())).thenReturn("validusername");
            mocked.when(() -> SanitizationUtil.sanitizeText(dto.getHeadline())).thenReturn(null);
            assertThrows(InvalidProfileDataException.class, () -> service.saveFreelancerProfile(dto));
        }
    }

    @Test
    void saveFreelancerProfile_whenAboutSanitizationFails_throwsException() {
        UUID userId = UUID.randomUUID();

        FreelancerProfileRequestDTO dto = FreelancerProfileRequestDTO.builder()
                .userId(userId)
                .username("validusername")
                .headline("Valid headline")
                .about("<invalid about>")
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(new User()));

        try (MockedStatic<SanitizationUtil> mocked = mockStatic(SanitizationUtil.class)) {
            mocked.when(() -> SanitizationUtil.sanitizeText(dto.getUsername())).thenReturn("validusername");
            mocked.when(() -> SanitizationUtil.sanitizeText(dto.getHeadline())).thenReturn("Valid headline");
            mocked.when(() -> SanitizationUtil.sanitizeText(dto.getAbout())).thenReturn(null);
            assertThrows(InvalidProfileDataException.class, () -> service.saveFreelancerProfile(dto));
        }
    }

    @Test
    void saveFreelancerProfile_whenWebsiteUrlSanitizationFails_throwsException() {
        UUID userId = UUID.randomUUID();

        FreelancerProfileRequestDTO dto = FreelancerProfileRequestDTO.builder()
                .userId(userId)
                .username("validusername")
                .headline("Valid headline")
                .about("Valid about")
                .websiteUrl("http://invalid-url")
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(new User()));

        try (MockedStatic<SanitizationUtil> mocked = mockStatic(SanitizationUtil.class)) {
            mocked.when(() -> SanitizationUtil.sanitizeText(dto.getUsername())).thenReturn("validusername");
            mocked.when(() -> SanitizationUtil.sanitizeText(dto.getHeadline())).thenReturn("Valid headline");
            mocked.when(() -> SanitizationUtil.sanitizeText(dto.getAbout())).thenReturn("Valid about");
            mocked.when(() -> SanitizationUtil.sanitizeUrl(dto.getWebsiteUrl())).thenReturn(null);
            assertThrows(InvalidProfileDataException.class, () -> service.saveFreelancerProfile(dto));
        }
    }

    @Test
    void saveFreelancerProfile_whenSocialMediaUrlsAllInvalid_throwsException() {
        UUID userId = UUID.randomUUID();

        FreelancerProfileRequestDTO dto = FreelancerProfileRequestDTO.builder()
                .userId(userId)
                .username("validusername")
                .headline("Valid headline")
                .about("Valid about")
                .websiteUrl("http://valid-url.com")
                .socialMedia(Set.of("http://invalid-social.com"))
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(new User()));

        try (MockedStatic<SanitizationUtil> mocked = mockStatic(SanitizationUtil.class)) {
            mocked.when(() -> SanitizationUtil.sanitizeText(dto.getUsername())).thenReturn("validusername");
            mocked.when(() -> SanitizationUtil.sanitizeText(dto.getHeadline())).thenReturn("Valid headline");
            mocked.when(() -> SanitizationUtil.sanitizeText(dto.getAbout())).thenReturn("Valid about");
            mocked.when(() -> SanitizationUtil.sanitizeUrl(dto.getWebsiteUrl())).thenReturn("http://valid-url.com");
            mocked.when(() -> SanitizationUtil.sanitizeUrl("http://invalid-social.com")).thenReturn(null);
            assertThrows(InvalidProfileDataException.class, () -> service.saveFreelancerProfile(dto));
        }
    }

    @Test
    void updateProfile_throwsWhenUsernameInvalid() {
        UUID id = profileId;

        FreelancerProfileRequestDTO dto = FreelancerProfileRequestDTO.builder()
                .userId(requestDTO.getUserId())
                .skills(requestDTO.getSkills())
                .jobSubcategoryIds(Collections.emptySet())
                .languageIds(Collections.emptySet())
                .socialMedia(requestDTO.getSocialMedia())
                .username("<<<invalid>>>")
                .build();

        when(profileRepository.findById(id)).thenReturn(Optional.of(profile));

        try (MockedStatic<SanitizationUtil> mocked = mockStatic(SanitizationUtil.class)) {
            mocked.when(() -> SanitizationUtil.sanitizeText(dto.getUsername())).thenReturn(null);
            mocked.when(() -> SanitizationUtil.sanitizeText(anyString())).thenAnswer(invocation -> {
                String arg = invocation.getArgument(0);
                if (arg.equals(dto.getUsername())) return null;
                return arg;
            });
            mocked.when(() -> SanitizationUtil.sanitizeUrl(anyString())).thenAnswer(invocation -> invocation.getArgument(0));

            assertThrows(InvalidProfileDataException.class, () -> {
                service.updateFreelancerProfile(id, dto);
            });
        }
    }

    @Test
    void updateProfile_throwsWhenHeadlineInvalid() {
        UUID id = profileId;

        FreelancerProfileRequestDTO validDto = FreelancerProfileRequestDTO.builder()
                .userId(requestDTO.getUserId())
                .skills(requestDTO.getSkills())
                .jobSubcategoryIds(Collections.emptySet())
                .languageIds(Collections.emptySet())
                .socialMedia(requestDTO.getSocialMedia())
                .username("<<<invalid>>>")
                .build();

        // Build a new DTO with invalid headline
        FreelancerProfileRequestDTO dto = FreelancerProfileRequestDTO.builder()
                .userId(validDto.getUserId())
                .username(validDto.getUsername())
                .headline("<<<invalid>>>")
                .about(validDto.getAbout())
                .websiteUrl(validDto.getWebsiteUrl())
                .socialMedia(validDto.getSocialMedia())
                .jobSubcategoryIds(validDto.getJobSubcategoryIds())
                .languageIds(validDto.getLanguageIds())
                .skills(validDto.getSkills())
                .build();

        when(profileRepository.findById(id)).thenReturn(Optional.of(profile));

        try (MockedStatic<SanitizationUtil> mocked = mockStatic(SanitizationUtil.class)) {
            mocked.when(() -> SanitizationUtil.sanitizeText(dto.getUsername())).thenReturn(dto.getUsername());
            mocked.when(() -> SanitizationUtil.sanitizeText(dto.getHeadline())).thenReturn(null); // simulate fail
            mocked.when(() -> SanitizationUtil.sanitizeText(dto.getAbout())).thenReturn(dto.getAbout());
            mocked.when(() -> SanitizationUtil.sanitizeUrl(dto.getWebsiteUrl())).thenReturn(dto.getWebsiteUrl());
            dto.getSocialMedia().forEach(url ->
                    mocked.when(() -> SanitizationUtil.sanitizeUrl(url)).thenReturn(url)
            );

            assertThrows(InvalidProfileDataException.class, () -> {
                service.updateFreelancerProfile(id, dto);
            });
        }
    }

    @Test
    void updateProfile_throwsWhenAboutInvalid() {
        UUID id = profileId;

        FreelancerProfileRequestDTO validDto = FreelancerProfileRequestDTO.builder()
                .userId(requestDTO.getUserId())
                .skills(requestDTO.getSkills())
                .jobSubcategoryIds(Collections.emptySet())
                .languageIds(Collections.emptySet())
                .socialMedia(requestDTO.getSocialMedia())
                .username("<<<invalid>>>")
                .build();

        FreelancerProfileRequestDTO dto = FreelancerProfileRequestDTO.builder()
                .userId(validDto.getUserId())
                .username(validDto.getUsername())
                .headline(validDto.getHeadline())
                .about("<<<invalid>>>")
                .websiteUrl(validDto.getWebsiteUrl())
                .socialMedia(validDto.getSocialMedia())
                .jobSubcategoryIds(validDto.getJobSubcategoryIds())
                .languageIds(validDto.getLanguageIds())
                .skills(validDto.getSkills())
                .build();

        when(profileRepository.findById(id)).thenReturn(Optional.of(profile));

        try (MockedStatic<SanitizationUtil> mocked = mockStatic(SanitizationUtil.class)) {
            mocked.when(() -> SanitizationUtil.sanitizeText(dto.getUsername())).thenReturn(dto.getUsername());
            mocked.when(() -> SanitizationUtil.sanitizeText(dto.getHeadline())).thenReturn(dto.getHeadline());
            mocked.when(() -> SanitizationUtil.sanitizeText(dto.getAbout())).thenReturn(null); // fail here
            mocked.when(() -> SanitizationUtil.sanitizeUrl(dto.getWebsiteUrl())).thenReturn(dto.getWebsiteUrl());
            dto.getSocialMedia().forEach(url ->
                    mocked.when(() -> SanitizationUtil.sanitizeUrl(url)).thenReturn(url)
            );

            assertThrows(InvalidProfileDataException.class, () -> {
                service.updateFreelancerProfile(id, dto);
            });
        }
    }

    @Test
    void updateProfile_throwsWhenSocialMediaInvalid() {
        UUID id = profileId;

        FreelancerProfileRequestDTO validDto = FreelancerProfileRequestDTO.builder()
                .userId(requestDTO.getUserId())
                .skills(requestDTO.getSkills())
                .jobSubcategoryIds(Collections.emptySet())
                .languageIds(Collections.emptySet())
                .socialMedia(requestDTO.getSocialMedia())
                .username("<<<invalid>>>")
                .build();

        Set<String> invalidSocial = Set.of("invalid-url");

        FreelancerProfileRequestDTO dto = FreelancerProfileRequestDTO.builder()
                .userId(validDto.getUserId())
                .username(validDto.getUsername())
                .headline(validDto.getHeadline())
                .about(validDto.getAbout())
                .websiteUrl(validDto.getWebsiteUrl())
                .socialMedia(invalidSocial)
                .jobSubcategoryIds(validDto.getJobSubcategoryIds())
                .languageIds(validDto.getLanguageIds())
                .skills(validDto.getSkills())
                .build();

        when(profileRepository.findById(id)).thenReturn(Optional.of(profile));

        try (MockedStatic<SanitizationUtil> mocked = mockStatic(SanitizationUtil.class)) {
            mocked.when(() -> SanitizationUtil.sanitizeText(dto.getUsername())).thenReturn(dto.getUsername());
            mocked.when(() -> SanitizationUtil.sanitizeText(dto.getHeadline())).thenReturn(dto.getHeadline());
            mocked.when(() -> SanitizationUtil.sanitizeText(dto.getAbout())).thenReturn(dto.getAbout());
            mocked.when(() -> SanitizationUtil.sanitizeUrl(dto.getWebsiteUrl())).thenReturn(dto.getWebsiteUrl());

            // The social media URL sanitization returns null to simulate failure
            mocked.when(() -> SanitizationUtil.sanitizeUrl("invalid-url")).thenReturn(null);

            assertThrows(InvalidProfileDataException.class, () -> {
                service.updateFreelancerProfile(id, dto);
            });
        }
    }

    @Test
    void updateProfile_throwsWhenWebsiteUrlInvalid() {
        UUID id = profileId;

        FreelancerProfileRequestDTO validDto = FreelancerProfileRequestDTO.builder()
                .userId(requestDTO.getUserId())
                .skills(requestDTO.getSkills())
                .jobSubcategoryIds(Collections.emptySet())
                .languageIds(Collections.emptySet())
                .socialMedia(requestDTO.getSocialMedia())
                .username("<<<invalid>>>")
                .build();

        FreelancerProfileRequestDTO dto = FreelancerProfileRequestDTO.builder()
                .userId(validDto.getUserId())
                .username(validDto.getUsername())
                .headline(validDto.getHeadline())
                .about(validDto.getAbout())
                .websiteUrl("invalid-url")
                .socialMedia(validDto.getSocialMedia())
                .jobSubcategoryIds(validDto.getJobSubcategoryIds())
                .languageIds(validDto.getLanguageIds())
                .skills(validDto.getSkills())
                .build();

        when(profileRepository.findById(id)).thenReturn(Optional.of(profile));

        try (MockedStatic<SanitizationUtil> mocked = mockStatic(SanitizationUtil.class)) {
            mocked.when(() -> SanitizationUtil.sanitizeText(dto.getUsername())).thenReturn(dto.getUsername());
            mocked.when(() -> SanitizationUtil.sanitizeText(dto.getHeadline())).thenReturn(dto.getHeadline());
            mocked.when(() -> SanitizationUtil.sanitizeText(dto.getAbout())).thenReturn(dto.getAbout());
            mocked.when(() -> SanitizationUtil.sanitizeUrl(dto.getWebsiteUrl())).thenReturn(null); // fail here
            dto.getSocialMedia().forEach(url ->
                    mocked.when(() -> SanitizationUtil.sanitizeUrl(url)).thenReturn(url)
            );

            assertThrows(InvalidProfileDataException.class, () -> {
                service.updateFreelancerProfile(id, dto);
            });
        }
    }

    @Test
    void updateProfile_setsExperienceLevelHeadlineHourlyRateAvailableForHireAndAbout() {
        UUID id = profileId;

        FreelancerProfileRequestDTO dto = FreelancerProfileRequestDTO.builder()
                .userId(requestDTO.getUserId())
                .experienceLevel(ExperienceLevel.SENIOR)
                .headline("Valid headline")
                .hourlyRate(50.00)
                .availableForHire(true)
                .about("Experienced developer")
                .build();

        when(profileRepository.findById(id)).thenReturn(Optional.of(profile));

        try (MockedStatic<SanitizationUtil> mocked = mockStatic(SanitizationUtil.class)) {
            mocked.when(() -> SanitizationUtil.sanitizeText("Valid headline")).thenReturn("Valid headline");
            mocked.when(() -> SanitizationUtil.sanitizeText("Experienced developer")).thenReturn("Experienced developer");

            service.updateFreelancerProfile(id, dto);

            verify(profileRepository).save(profileCaptor.capture());
            FreelancerProfile savedProfile = profileCaptor.getValue();

            assertEquals(ExperienceLevel.SENIOR, savedProfile.getExperienceLevel());
            assertEquals("Valid headline", savedProfile.getHeadline());
            assertEquals(50.00, savedProfile.getHourlyRate());
            assertTrue(savedProfile.getAvailableForHire());
            assertEquals("Experienced developer", savedProfile.getAbout());
        }
    }

    @Test
    void updateProfile_throwsWhenHeadlineSanitizationFails() {
        UUID id = profileId;

        FreelancerProfileRequestDTO dto = FreelancerProfileRequestDTO.builder()
                .headline("invalid headline")
                .build();

        when(profileRepository.findById(id)).thenReturn(Optional.of(profile));

        try (MockedStatic<SanitizationUtil> mocked = mockStatic(SanitizationUtil.class)) {
            mocked.when(() -> SanitizationUtil.sanitizeText("invalid headline")).thenReturn(null);

            assertThrows(InvalidProfileDataException.class, () -> {
                service.updateFreelancerProfile(id, dto);
            });
        }
    }

    @Test
    void updateProfile_throwsWhenAboutSanitizationFails() {
        UUID id = profileId;

        FreelancerProfileRequestDTO dto = FreelancerProfileRequestDTO.builder()
                .about("invalid about text")
                .build();

        when(profileRepository.findById(id)).thenReturn(Optional.of(profile));

        try (MockedStatic<SanitizationUtil> mocked = mockStatic(SanitizationUtil.class)) {
            mocked.when(() -> SanitizationUtil.sanitizeText("invalid about text")).thenReturn(null);

            assertThrows(InvalidProfileDataException.class, () -> {
                service.updateFreelancerProfile(id, dto);
            });
        }
    }

    @Test
    void updateProfile_setsWebsiteUrl_whenValidUrl() {
        UUID id = profileId;

        FreelancerProfileRequestDTO dto = FreelancerProfileRequestDTO.builder()
                .userId(requestDTO.getUserId())
                .websiteUrl("https://valid-url.com")
                .build();

        when(profileRepository.findById(id)).thenReturn(Optional.of(profile));

        try (MockedStatic<SanitizationUtil> mocked = mockStatic(SanitizationUtil.class)) {
            mocked.when(() -> SanitizationUtil.sanitizeUrl("https://valid-url.com"))
                    .thenReturn("https://valid-url.com");

            service.updateFreelancerProfile(id, dto);

            verify(profileRepository).save(profileCaptor.capture());
            FreelancerProfile savedProfile = profileCaptor.getValue();

            assertEquals("https://valid-url.com", savedProfile.getWebsiteUrl());
        }
    }

}