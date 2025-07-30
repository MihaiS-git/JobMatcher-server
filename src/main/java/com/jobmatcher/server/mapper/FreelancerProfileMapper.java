package com.jobmatcher.server.mapper;

import com.jobmatcher.server.domain.*;
import com.jobmatcher.server.model.*;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class FreelancerProfileMapper {

    private final JobSubcategoryMapper jobSubcategoryMapper;
    private final SkillMapper skillMapper;
    private final LanguageMapper languageMapper;

    public FreelancerProfileMapper(
            JobSubcategoryMapper jobSubcategoryMapper,
            SkillMapper skillMapper,
            LanguageMapper languageMapper
    ) {
        this.jobSubcategoryMapper = jobSubcategoryMapper;
        this.skillMapper = skillMapper;
        this.languageMapper = languageMapper;
    }

    public FreelancerSummaryDTO toFreelancerSummaryDto(FreelancerProfile entity){
        if(entity == null) return null;

        return mapSummaryFields(entity).build();
    }

    public FreelancerDetailDTO toFreelancerDetailDto(FreelancerProfile entity){
        if(entity == null) return null;

        FreelancerSummaryDTO summary = mapSummaryFields(entity).build();

        return FreelancerDetailDTO.builder()
                .userId(summary.getUserId())
                .profileId(summary.getProfileId())
                .username(summary.getUsername())
                .experienceLevel(summary.getExperienceLevel())
                .headline(summary.getHeadline())
                .jobSubcategories(summary.getJobSubcategories())
                .hourlyRate(summary.getHourlyRate())
                .availableForHire(summary.getAvailableForHire())
                .pictureUrl(summary.getPictureUrl())
                .skills(summary.getSkills())
                .languages(summary.getLanguages())
                .rating(summary.getRating())
                .about(entity.getAbout())
                .socialMedia(entity.getSocialMedia())
                .websiteUrl(entity.getWebsiteUrl())
                .build();
    }


    public FreelancerProfile toEntity(
            FreelancerProfileRequestDTO dto,
            User user, Set<Skill> skills,
            Set<JobSubcategory> jobSubcategories,
            Set<Language> languages
    ){
        FreelancerProfile freelancerProfile = new FreelancerProfile();
        freelancerProfile.setUser(user);
        freelancerProfile.setUsername(dto.getUsername());
        freelancerProfile.setExperienceLevel(dto.getExperienceLevel());
        freelancerProfile.setHeadline(dto.getHeadline());
        freelancerProfile.setJobSubcategories(jobSubcategories);
        freelancerProfile.setHourlyRate(dto.getHourlyRate());
        freelancerProfile.setAvailableForHire(dto.getAvailableForHire());
        freelancerProfile.setSkills(skills);
        freelancerProfile.setLanguages(languages);
        freelancerProfile.setAbout(dto.getAbout());
        freelancerProfile.setSocialMedia(dto.getSocialMedia());
        freelancerProfile.setWebsiteUrl(dto.getWebsiteUrl());

        return freelancerProfile;
    }


    private Set<LanguageDTO> getLanguageDTOS(FreelancerProfile entity) {
        return Optional.ofNullable(entity.getLanguages()).orElse(Set.of())
                .stream()
                .map(languageMapper::toDto)
                .collect(Collectors.toSet());
    }

    private Set<SkillDTO> getSkillDTOS(FreelancerProfile entity) {
        return Optional.ofNullable(entity.getSkills()).orElse(Set.of())
                .stream()
                .map(skillMapper::toDto)
                .collect(Collectors.toSet());
    }

    private Set<JobSubcategoryDTO> getJobSubcategoryDTOS(FreelancerProfile entity) {
        return entity.getJobSubcategories().stream()
                .map(jobSubcategoryMapper::toDto)
                .collect(Collectors.toSet());
    }

    private FreelancerSummaryDTO.FreelancerSummaryDTOBuilder mapSummaryFields(FreelancerProfile entity) {
        return FreelancerSummaryDTO.builder()
                .userId(entity.getUser().getId())
                .profileId(entity.getId())
                .username(entity.getUsername())
                .experienceLevel(entity.getExperienceLevel())
                .headline(entity.getHeadline())
                .jobSubcategories(getJobSubcategoryDTOS(entity))
                .hourlyRate(entity.getHourlyRate())
                .availableForHire(entity.getAvailableForHire())
                .pictureUrl(entity.getUser().getPictureUrl())
                .skills(getSkillDTOS(entity))
                .languages(getLanguageDTOS(entity))
                .rating(entity.getRating());
    }
}
