package com.jobmatcher.server.service;

import com.jobmatcher.server.domain.*;
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
@Transactional
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
                new ResourceNotFoundException("Profile not found."));
        return profileMapper.toFreelancerDetailDto(profile);
    }

    @Override
    public FreelancerDetailDTO saveFreelancerProfile(FreelancerProfileRequestDTO dto) {
        User user = userRepository.findById(dto.getUserId()).orElseThrow(() ->
                new ResourceNotFoundException("User not found."));

        Set<JobSubcategory> subcategories = fetchJobSubcategories(dto.getJobSubcategoryIds());
        Set<Language> languages = fetchLanguages(dto.getLanguageIds());
        Set<Skill> skills = resolveSkillsFromNames(dto.getSkills());

        String username = SanitizationUtil.sanitizeText(dto.getUsername());
        String headline = SanitizationUtil.sanitizeText(dto.getHeadline());
        String about = SanitizationUtil.sanitizeText(dto.getAbout());
        String websiteUrl = SanitizationUtil.sanitizeUrl(dto.getWebsiteUrl());
        Set<String> sanitizedSocialMedia = Optional.ofNullable(dto.getSocialMedia())
                .orElse(Collections.emptySet())
                .stream()
                .map(SanitizationUtil::sanitizeUrl)
                .filter(Objects::nonNull)
                .filter(s -> !s.isBlank())
                .collect(Collectors.toSet());


        FreelancerProfile profile = profileMapper.toEntity(
                dto, user, username, headline, about, websiteUrl,
                skills, subcategories, languages, sanitizedSocialMedia);
        FreelancerProfile savedProfile = profileRepository.save(profile);
        return profileMapper.toFreelancerDetailDto(savedProfile);
    }

    @Override
    public FreelancerDetailDTO updateFreelancerProfile(UUID id, FreelancerProfileRequestDTO dto) {
        FreelancerProfile existentProfile = profileRepository.findById(id).orElseThrow(() ->
                new ResourceNotFoundException("Profile not found."));

        Set<JobSubcategory> subcategories = fetchJobSubcategories(dto.getJobSubcategoryIds());
        Set<Language> languages = fetchLanguages(dto.getLanguageIds());
        Set<Skill> skills = resolveSkillsFromNames(dto.getSkills());
        Set<String> sanitizedSocialMedia = Optional.ofNullable(dto.getSocialMedia())
                .orElse(Collections.emptySet())
                .stream()
                .map(SanitizationUtil::sanitizeUrl) // your existing logic
                .filter(Objects::nonNull)
                .filter(s -> !s.isBlank())
                .collect(Collectors.toSet());

        existentProfile.setUsername(SanitizationUtil.sanitizeText(dto.getUsername()));
        existentProfile.setExperienceLevel(dto.getExperienceLevel());
        existentProfile.setHeadline(SanitizationUtil.sanitizeText(dto.getHeadline()));
        existentProfile.setJobSubcategories(subcategories);
        existentProfile.setHourlyRate(dto.getHourlyRate());
        existentProfile.setAvailableForHire(dto.getAvailableForHire());
        existentProfile.setSkills(skills);
        existentProfile.setLanguages(languages);
        existentProfile.setAbout(SanitizationUtil.sanitizeText(dto.getAbout()));
        existentProfile.setSocialMedia(sanitizedSocialMedia);
        existentProfile.setWebsiteUrl(SanitizationUtil.sanitizeUrl(dto.getWebsiteUrl()));

        FreelancerProfile savedProfile = profileRepository.save(existentProfile);
        return profileMapper.toFreelancerDetailDto(savedProfile);
    }

    private Set<Skill> resolveSkillsFromNames(Set<String> skillNames) {
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