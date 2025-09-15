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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Transactional(rollbackFor = Exception.class)
@Service
public class FreelancerProfileServiceImpl implements IFreelancerProfileService {

    private static final Pattern SKILL_NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9 +#.-]{1,50}$");

    private final FreelancerProfileRepository profileRepository;
    private final FreelancerProfileMapper profileMapper;
    private final UserRepository userRepository;
    private final JobSubcategoryRepository subcategoryRepository;
    private final LanguageRepository languageRepository;
    private final ISkillService skillService;

    public FreelancerProfileServiceImpl(
            FreelancerProfileRepository profileRepository,
            FreelancerProfileMapper profileMapper,
            UserRepository userRepository,
            JobSubcategoryRepository subcategoryRepository,
            LanguageRepository languageRepository,
            ISkillService skillService
    ) {
        this.profileRepository = profileRepository;
        this.profileMapper = profileMapper;
        this.userRepository = userRepository;
        this.subcategoryRepository = subcategoryRepository;
        this.languageRepository = languageRepository;
        this.skillService = skillService;
    }

    @Transactional(readOnly = true)
    @Override
    public Set<FreelancerSummaryDTO> getAllFreelancerProfiles() {
        return profileRepository.findAll().stream()
                .map(profileMapper::toFreelancerSummaryDto)
                .collect(Collectors.toSet());
    }

    @Transactional(readOnly = true)
    @Override
    public FreelancerDetailDTO getFreelancerProfileById(UUID id) {
        FreelancerProfile profile = profileRepository.findById(id).orElseThrow(() ->
                new ResourceNotFoundException("Profile not found for ID: " + id));
        return profileMapper.toFreelancerDetailDto(profile);
    }

    @Override
    public FreelancerDetailDTO getFreelancerProfileByUserId(UUID userId) {
        return profileRepository.findByUserId(userId)
                .map(profile -> {
                    profile.getSocialMedia().size(); // force lazy load
                    return profileMapper.toFreelancerDetailDto(profile);
                })
                .orElse(null);
    }

    @Override
    public FreelancerDetailDTO saveFreelancerProfile(FreelancerProfileRequestDTO dto) {
        if (dto.getUsername() == null) {
            throw new InvalidProfileDataException("Username is required");
        }

        User user = userRepository.findById(dto.getUserId()).orElseThrow(() ->
                new ResourceNotFoundException("User not found with ID: " + dto.getUserId()));

        Set<JobSubcategory> subcategories = fetchJobSubcategories(dto.getJobSubcategoryIds());
        Set<Language> languages = fetchLanguages(dto.getLanguageIds());
        Set<Skill> skills = resolveSkillsFromNames(dto.getSkills());

        String username = SanitizationUtil.sanitizeText(dto.getUsername());
        if (username == null && dto.getUsername() != null) {
            throw new InvalidProfileDataException("Invalid username provided");
        }
        String headline = null;
        if(dto.getHeadline() != null && !dto.getHeadline().isBlank()) {
            headline = dto.getHeadline().isBlank()
                    ? null
                    : SanitizationUtil.sanitizeText(dto.getHeadline());
            if (dto.getHeadline() != null && headline == null) {
                throw new InvalidProfileDataException("Invalid headline provided");
            }
        }
        Double hourlyRate;
        if(dto.getHourlyRate() != null) {
            hourlyRate = dto.getHourlyRate();
            if(hourlyRate <= 0) {
                throw new InvalidProfileDataException("Hourly rate must be positive");
            }
        }
        String about = null;
        if(dto.getAbout() != null && !dto.getAbout().isBlank()) {
            about = dto.getAbout().isBlank()
                    ? null
                    : SanitizationUtil.sanitizeText(dto.getAbout());
            if (dto.getAbout() != null && about == null) {
                throw new InvalidProfileDataException("Invalid about text provided");
            }
        }
        String websiteUrl = null;
        if(dto.getWebsiteUrl() != null) {
            websiteUrl = dto.getWebsiteUrl().isBlank()
                    ? null
                    : SanitizationUtil.sanitizeUrl(dto.getWebsiteUrl());

            if(!dto.getWebsiteUrl().isEmpty() && websiteUrl == null) {
                throw new InvalidProfileDataException("Invalid website URL provided");
            }
        }
        Set<String> socialMedia = null;
        if(dto.getSocialMedia() != null) {
            socialMedia = dto.getSocialMedia().isEmpty()
                    ? Collections.emptySet()
                    : dto.getSocialMedia().stream()
                    .map(SanitizationUtil::sanitizeUrl)
                    .filter(s -> s != null && !s.isBlank())
                    .collect(Collectors.toSet());

            if(!dto.getSocialMedia().isEmpty() && socialMedia.isEmpty()) {
                throw new InvalidProfileDataException("Invalid social media URL provided");
            }
        }

        FreelancerProfile profile = profileMapper.toEntity(
                dto, user, username, headline, about, websiteUrl,
                skills, subcategories, languages, socialMedia);
        FreelancerProfile savedProfile = profileRepository.save(profile);
        return profileMapper.toFreelancerDetailDto(savedProfile);

    }

    @Override
    public FreelancerDetailDTO updateFreelancerProfile(UUID id, FreelancerProfileRequestDTO dto) {
        FreelancerProfile existentProfile = profileRepository.findById(id).orElseThrow(() ->
                new ResourceNotFoundException("Profile not found with ID: " + id));

        // Username: sanitize and allow null to clear, no update if dto.getUsername() is null
        if (dto.getUsername() != null) {
            String username = SanitizationUtil.sanitizeText(dto.getUsername());
            if (dto.getUsername() != null && username == null && !dto.getUsername().isBlank()) {
                throw new InvalidProfileDataException("Invalid username provided");
            }
            existentProfile.setUsername(username);
        }

        // Experience level: update if not null
        if (dto.getExperienceLevel() != null) {
            existentProfile.setExperienceLevel(dto.getExperienceLevel());
        }

        // Headline: null means no update, empty means clear (set to null)
        if (dto.getHeadline() != null) {
            String headline = dto.getHeadline().isBlank() ? null : SanitizationUtil.sanitizeText(dto.getHeadline());
            if (!dto.getHeadline().isEmpty() && headline == null) {
                throw new InvalidProfileDataException("Invalid headline text provided");
            }
            existentProfile.setHeadline(headline);
        }

        // Job subcategories: always update, treat empty as clear
        Set<JobSubcategory> subcategories = (dto.getJobSubcategoryIds() == null || dto.getJobSubcategoryIds().isEmpty())
                ? Collections.emptySet()
                : fetchJobSubcategories(dto.getJobSubcategoryIds());
        existentProfile.setJobSubcategories(subcategories);

        // Hourly rate: update if not null
        if (dto.getHourlyRate() != null) {
            existentProfile.setHourlyRate(dto.getHourlyRate());
        }

        // Available for hire: update if not null
        if (dto.getAvailableForHire() != null) {
            existentProfile.setAvailableForHire(dto.getAvailableForHire());
        }

        // Skills: always update, empty means clear
        Set<Skill> skills = (dto.getSkills() == null || dto.getSkills().isEmpty())
                ? Collections.emptySet()
                : resolveSkillsFromNames(dto.getSkills());
        existentProfile.setSkills(skills);

        // Languages: always update, empty means clear
        Set<Language> languages = (dto.getLanguageIds() == null || dto.getLanguageIds().isEmpty())
                ? Collections.emptySet()
                : fetchLanguages(dto.getLanguageIds());
        existentProfile.setLanguages(languages);

        // About: null means no update, empty means clear
        if (dto.getAbout() != null) {
            String about = dto.getAbout().isBlank() ? null : SanitizationUtil.sanitizeText(dto.getAbout());
            if (!dto.getAbout().isEmpty() && about == null) {
                throw new InvalidProfileDataException("Invalid about text provided");
            }
            existentProfile.setAbout(about);
        }

        // Social media URLs: always update, empty means clear
        Set<String> sanitizedSocialMedia = (dto.getSocialMedia() == null)
                ? Collections.emptySet()
                : dto.getSocialMedia().stream()
                .map(SanitizationUtil::sanitizeUrl)
                .filter(s -> s != null && !s.isBlank())
                .collect(Collectors.toSet());

        if (dto.getSocialMedia() != null && !dto.getSocialMedia().isEmpty() && sanitizedSocialMedia.isEmpty()) {
            throw new InvalidProfileDataException("Invalid social media URL provided");
        }
        existentProfile.setSocialMedia(sanitizedSocialMedia);

        // Website URL: null means no update, empty means clear
        if (dto.getWebsiteUrl() != null) {
            String websiteUrl = dto.getWebsiteUrl().isBlank() ? null : SanitizationUtil.sanitizeUrl(dto.getWebsiteUrl());
            if (!dto.getWebsiteUrl().isEmpty() && websiteUrl == null) {
                throw new InvalidProfileDataException("Invalid website URL provided");
            }
            existentProfile.setWebsiteUrl(websiteUrl);
        }

        FreelancerProfile savedProfile = profileRepository.save(existentProfile);
        return profileMapper.toFreelancerDetailDto(savedProfile);
    }

    private Set<Skill> resolveSkillsFromNames(Set<String> skillNames) {
        if (skillNames == null || skillNames.isEmpty()) return Set.of();
        return skillNames.stream()
                .map(SanitizationUtil::sanitizeText)
                .filter(name -> name != null && !name.isBlank())
                .filter(name -> SKILL_NAME_PATTERN.matcher(name).matches())
                .map(skillService::findOrCreateByName)
                .collect(Collectors.toSet());
    }

    private Set<JobSubcategory> fetchJobSubcategories(Set<Long> ids) {
        if (ids == null || ids.isEmpty()) return Collections.emptySet();
        List<JobSubcategory> found = subcategoryRepository.findAllById(ids);
        if (found.size() != ids.size()) {
            Set<Long> foundIds = found.stream().map(JobSubcategory::getId).collect(Collectors.toSet());
            Set<Long> missing = new HashSet<>(ids);
            missing.removeAll(foundIds);
            throw new ResourceNotFoundException("Job subcategories not found for IDs: " + missing);
        }
        return new HashSet<>(found);
    }

    private Set<Language> fetchLanguages(Set<Integer> ids) {
        if (ids == null || ids.isEmpty()) return Collections.emptySet();
        List<Language> found = languageRepository.findAllById(ids);
        if (found.size() != ids.size()) {
            Set<Integer> foundIds = found.stream().map(Language::getId).collect(Collectors.toSet());
            Set<Integer> missing = new HashSet<>(ids);
            missing.removeAll(foundIds);
            throw new ResourceNotFoundException("Languages not found for IDs: " + missing);
        }
        return new HashSet<>(found);
    }
}