package com.jobmatcher.server.service;

import com.jobmatcher.server.domain.*;
import com.jobmatcher.server.exception.ResourceNotFoundException;
import com.jobmatcher.server.mapper.FreelancerProfileMapper;
import com.jobmatcher.server.model.FreelancerDetailDTO;
import com.jobmatcher.server.model.FreelancerProfileRequestDTO;
import com.jobmatcher.server.model.FreelancerSummaryDTO;
import com.jobmatcher.server.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Transactional
@Service
public class FreelancerProfileServiceImpl implements IFreelancerProfileService {

    private final FreelancerProfileRepository profileRepository;
    private final FreelancerProfileMapper profileMapper;
    private final UserRepository userRepository;
    private final JobSubcategoryRepository subcategoryRepository;
    private final SkillRepository skillRepository;
    private final LanguageRepository languageRepository;

    public FreelancerProfileServiceImpl(
            FreelancerProfileRepository profileRepository,
            FreelancerProfileMapper profileMapper,
            UserRepository userRepository,
            JobSubcategoryRepository subcategoryRepository,
            SkillRepository skillRepository,
            LanguageRepository languageRepository
    ) {
        this.profileRepository = profileRepository;
        this.profileMapper = profileMapper;
        this.userRepository = userRepository;
        this.subcategoryRepository = subcategoryRepository;
        this.skillRepository = skillRepository;
        this.languageRepository = languageRepository;
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


        FreelancerProfile profile = profileMapper.toEntity(dto, user, skills, subcategories, languages);
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

        existentProfile.setUsername(dto.getUsername());
        existentProfile.setExperienceLevel(dto.getExperienceLevel());
        existentProfile.setHeadline(dto.getHeadline());
        existentProfile.setJobSubcategories(subcategories);
        existentProfile.setHourlyRate(dto.getHourlyRate());
        existentProfile.setAvailableForHire(dto.getAvailableForHire());
        existentProfile.setSkills(skills);
        existentProfile.setLanguages(languages);
        existentProfile.setAbout(dto.getAbout());
        existentProfile.setSocialMedia(dto.getSocialMedia());
        existentProfile.setWebsiteUrl(dto.getWebsiteUrl());

        FreelancerProfile savedProfile = profileRepository.save(existentProfile);
        return profileMapper.toFreelancerDetailDto(savedProfile);
    }

    private Set<Skill> resolveSkillsFromNames(Set<String> skillNames) {
        return skillNames.stream()
                .map(name -> {
                    String normalized = name.trim().toLowerCase();
                    return skillRepository.findByNameIgnoreCase(normalized)
                            .orElseGet(() -> skillRepository.save(new Skill(normalized)));
                })
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
