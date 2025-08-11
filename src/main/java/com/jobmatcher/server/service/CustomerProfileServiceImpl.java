package com.jobmatcher.server.service;

import com.jobmatcher.server.domain.CustomerProfile;
import com.jobmatcher.server.domain.Language;
import com.jobmatcher.server.domain.User;
import com.jobmatcher.server.exception.ResourceNotFoundException;
import com.jobmatcher.server.mapper.CustomerProfileMapper;
import com.jobmatcher.server.model.CustomerDetailDTO;
import com.jobmatcher.server.model.CustomerProfileRequestDTO;
import com.jobmatcher.server.model.CustomerSummaryDTO;
import com.jobmatcher.server.repository.CustomerProfileRepository;
import com.jobmatcher.server.repository.LanguageRepository;
import com.jobmatcher.server.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Transactional
@Slf4j
@Service
public class CustomerProfileServiceImpl implements ICustomerProfileService{

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
                .map(profileMapper::toCustomerSummaryDto)
                .collect(Collectors.toSet());
    }

    @Transactional(readOnly = true)
    @Override
    public CustomerDetailDTO getCustomerProfileById(UUID id) {
        CustomerProfile profile = profileRepository.findById(id).orElseThrow(() ->
                new ResourceNotFoundException("Profile not found."));
        return profileMapper.toCustomerDetailDto(profile);
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
        Set<Integer> languageIds = dto.getLanguageIds();
        Set<Language> languages = new HashSet<>(languageRepository.findAllById(languageIds));
        if (languages.size() != languageIds.size()) {
            Set<Integer> foundIds = languages.stream()
                    .map(Language::getId)
                    .collect(Collectors.toSet());
            Set<Integer> missingIds = new HashSet<>(languageIds);
            missingIds.remove(foundIds);
            throw new ResourceNotFoundException("Languages not found for IDs: " + missingIds);
        }

        User user = userRepository.findById(dto.getUserId()).orElseThrow(() -> new ResourceNotFoundException("User not found."));
        CustomerProfile profile = profileMapper.toEntity(dto, user, languages);
        CustomerProfile savedProfile = profileRepository.save(profile);
        return profileMapper.toCustomerDetailDto(savedProfile);
    }

    @Override
    public CustomerDetailDTO updateCustomerProfile(UUID id, CustomerProfileRequestDTO dto) {
        Set<Integer> languageIds = dto.getLanguageIds();
        Set<Language> languages = new HashSet<>(languageRepository.findAllById(languageIds));
        if(languages.size() != languageIds.size()) {
            Set<Integer> foundIds = languages.stream()
                    .map(Language::getId)
                    .collect(Collectors.toSet());
            Set<Integer> missingIds = new HashSet<>(languageIds);
            missingIds.remove(foundIds);

            throw new ResourceNotFoundException(String.format("Languages not found for IDs: %s", missingIds));
        }

        CustomerProfile existentProfile = profileRepository.findById(id).orElseThrow(() ->
                new ResourceNotFoundException("Profile not found."));
        existentProfile.setUsername(dto.getUsername());
        existentProfile.setCompany(dto.getCompany());
        existentProfile.setAbout(dto.getAbout());
        existentProfile.setLanguages(languages);
        existentProfile.setWebsiteUrl(dto.getWebsiteUrl());
        existentProfile.setSocialMedia(dto.getSocialMedia());

        CustomerProfile savedProfile = profileRepository.save(existentProfile);
        return profileMapper.toCustomerDetailDto(savedProfile);
    }
}
