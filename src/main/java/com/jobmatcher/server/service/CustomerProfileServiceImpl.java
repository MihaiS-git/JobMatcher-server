package com.jobmatcher.server.service;

import com.jobmatcher.server.domain.*;
import com.jobmatcher.server.exception.InvalidProfileDataException;
import com.jobmatcher.server.exception.ResourceNotFoundException;
import com.jobmatcher.server.mapper.CustomerProfileMapper;
import com.jobmatcher.server.model.*;
import com.jobmatcher.server.repository.CustomerProfileRepository;
import com.jobmatcher.server.repository.LanguageRepository;
import com.jobmatcher.server.repository.UserRepository;
import com.jobmatcher.server.util.SanitizationUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Transactional
@Slf4j
@Service
public class CustomerProfileServiceImpl implements ICustomerProfileService {

    private final CustomerProfileRepository profileRepository;
    private final CustomerProfileMapper profileMapper;
    private final UserRepository userRepository;
    private final LanguageRepository languageRepository;


    public CustomerProfileServiceImpl(CustomerProfileRepository profileRepository, CustomerProfileMapper profileMapper, UserRepository userRepository, LanguageRepository languageRepository) {
        this.profileRepository = profileRepository;
        this.profileMapper = profileMapper;
        this.userRepository = userRepository;
        this.languageRepository = languageRepository;
    }

    @Transactional(readOnly = true)
    @Override
    public Set<CustomerSummaryDTO> getAllCustomerProfiles() {
        return profileRepository.findAll().stream()
                .map(profile -> profileMapper.toCustomerSummaryDto(profile, true))
                .collect(Collectors.toSet());
    }

    @Transactional(readOnly = true)
    @Override
    public CustomerDetailDTO getCustomerProfileById(UUID id) {
        return profileRepository.findById(id)
                .map(profile -> {
                    profile.getSocialMedia().size(); // force lazy load
                    return profileMapper.toCustomerDetailDto(profile);
                })
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found."));
    }

    @Override
    public CustomerDetailDTO getCustomerProfileByUserId(UUID userId) {
        return profileRepository.findByUserId(userId)
                .map(profile -> {
                    profile.getSocialMedia().size(); // force lazy load
                    return profileMapper.toCustomerDetailDto(profile);
                })
                .orElse(null);
    }

    @Override
    public CustomerDetailDTO saveCustomerProfile(CustomerProfileRequestDTO dto) {
        if (dto.getUsername() == null) {
            throw new InvalidProfileDataException("Username is required");
        }

        User user = userRepository.findById(dto.getUserId()).orElseThrow(() ->
                new ResourceNotFoundException("User not found with ID: " + dto.getUserId()));

        Set<Language> languages = fetchLanguages(dto.getLanguageIds());

        String username = SanitizationUtil.sanitizeText(dto.getUsername());
        if (username == null && dto.getUsername() != null) {
            throw new InvalidProfileDataException("Invalid username provided");
        }
        String company = null;
        if (dto.getCompany() != null && !dto.getCompany().isBlank()) {
            company = dto.getCompany().isBlank()
                    ? null
                    : SanitizationUtil.sanitizeText(dto.getCompany());
            if (dto.getCompany() != null && company == null) {
                throw new InvalidProfileDataException("Invalid company provided");
            }
        }

        String about = null;
        if (dto.getAbout() != null && !dto.getAbout().isBlank()) {
            about = dto.getAbout().isBlank()
                    ? null
                    : SanitizationUtil.sanitizeText(dto.getAbout());
            if (dto.getAbout() != null && about == null) {
                throw new InvalidProfileDataException("Invalid about text provided");
            }
        }
        String websiteUrl = null;
        if (dto.getWebsiteUrl() != null) {
            websiteUrl = dto.getWebsiteUrl().isBlank()
                    ? null
                    : SanitizationUtil.sanitizeUrl(dto.getWebsiteUrl());

            if (!dto.getWebsiteUrl().isEmpty() && websiteUrl == null) {
                throw new InvalidProfileDataException("Invalid website URL provided");
            }
        }
        Set<String> socialMedia = null;
        if (dto.getSocialMedia() != null) {
            socialMedia = dto.getSocialMedia().isEmpty()
                    ? Collections.emptySet()
                    : dto.getSocialMedia().stream()
                    .map(SanitizationUtil::sanitizeUrl)
                    .filter(s -> s != null && !s.isBlank())
                    .collect(Collectors.toSet());

            if (!dto.getSocialMedia().isEmpty() && socialMedia.isEmpty()) {
                throw new InvalidProfileDataException("Invalid social media URL provided");
            }
        }

        CustomerProfile profile = profileMapper.toEntity(
                dto, user, username, company, about, websiteUrl,
                languages, socialMedia);
        CustomerProfile savedProfile = profileRepository.save(profile);
        return profileMapper.toCustomerDetailDto(savedProfile);
    }

    @Override
    public CustomerDetailDTO updateCustomerProfile(UUID id, CustomerProfileRequestDTO dto) {
        CustomerProfile existentProfile = profileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found."));

        if (dto.getUsername() != null) {
            String username = SanitizationUtil.sanitizeText(dto.getUsername());
            if (dto.getUsername() != null && username == null && !dto.getUsername().isBlank()) {
                throw new InvalidProfileDataException("Invalid username provided");
            }
            existentProfile.setUsername(username);
        }

        if (dto.getCompany() != null) {
            String company = dto.getCompany().isBlank() ? null : SanitizationUtil.sanitizeText(dto.getCompany());
            if (!dto.getCompany().isEmpty() && company == null) {
                throw new InvalidProfileDataException("Invalid headline text provided");
            }
            existentProfile.setCompany(company);
        }

        Set<Language> languages = (dto.getLanguageIds() == null || dto.getLanguageIds().isEmpty())
                ? Collections.emptySet()
                : fetchLanguages(dto.getLanguageIds());
        existentProfile.setLanguages(languages);

        if (dto.getAbout() != null) {
            String about = dto.getAbout().isBlank() ? null : SanitizationUtil.sanitizeText(dto.getAbout());
            if (!dto.getAbout().isEmpty() && about == null) {
                throw new InvalidProfileDataException("Invalid about text provided");
            }
            existentProfile.setAbout(about);
        }

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

        if (dto.getWebsiteUrl() != null) {
            String trimmed = dto.getWebsiteUrl().trim();
            String websiteUrl = trimmed.isEmpty() ? null : SanitizationUtil.sanitizeUrl(trimmed);
            if (!trimmed.isEmpty() && websiteUrl == null) {
                throw new InvalidProfileDataException("Invalid website URL provided");
            }
            existentProfile.setWebsiteUrl(websiteUrl);
        }

        CustomerProfile savedProfile = profileRepository.save(existentProfile);
        return profileMapper.toCustomerDetailDto(savedProfile);
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
