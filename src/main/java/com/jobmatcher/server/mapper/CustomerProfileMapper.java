package com.jobmatcher.server.mapper;

import com.jobmatcher.server.domain.CustomerProfile;
import com.jobmatcher.server.domain.Language;
import com.jobmatcher.server.domain.User;
import com.jobmatcher.server.model.CustomerDetailDTO;
import com.jobmatcher.server.model.CustomerProfileRequestDTO;
import com.jobmatcher.server.model.CustomerSummaryDTO;
import com.jobmatcher.server.model.LanguageDTO;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

@Component
public class CustomerProfileMapper {

    private final LanguageMapper languageMapper;

    public CustomerProfileMapper(LanguageMapper languageMapper) {
        this.languageMapper = languageMapper;
    }

    public CustomerSummaryDTO toCustomerSummaryDto(CustomerProfile entity) {
        if (entity == null) return null;

        Set<LanguageDTO> languages = mapLanguagesToDtos(entity);

        return CustomerSummaryDTO.builder()
                .profileId(entity.getId())
                .userId(entity.getUser().getId())
                .username(entity.getUsername())
                .company(entity.getCompany())
                .languages(languages)
                .rating(entity.getRating())
                .pictureUrl(entity.getUser().getPictureUrl())
                .build();
    }

    public CustomerDetailDTO toCustomerDetailDto(CustomerProfile entity) {
        if (entity == null) return null;

        Set<LanguageDTO> languages = mapLanguagesToDtos(entity);

        return CustomerDetailDTO.builder()
                .profileId(entity.getId())
                .userId(entity.getUser().getId())
                .username(entity.getUsername())
                .company(entity.getCompany())
                .languages(languages)
                .rating(entity.getRating())
                .pictureUrl(entity.getUser().getPictureUrl())
                .websiteUrl(entity.getWebsiteUrl())
                .socialMedia(entity.getSocialMedia())
                .about(entity.getAbout())
                .build();
    }

    public CustomerProfile toEntity(CustomerProfileRequestDTO dto, User user, Set<Language> languages) {
        CustomerProfile profile = new CustomerProfile();
        profile.setUser(user);
        profile.setUsername(dto.getUsername());
        profile.setCompany(dto.getCompany());
        profile.setAbout(dto.getAbout());
        profile.setLanguages(languages);
        profile.setWebsiteUrl(dto.getWebsiteUrl());
        profile.setSocialMedia(dto.getSocialMedia());

        return profile;
    }


    private Set<LanguageDTO> mapLanguagesToDtos(CustomerProfile entity) {
        return (entity.getLanguages() == null)
                ? Set.of()
                : entity.getLanguages().stream()
                .map(languageMapper::toDto)
                .collect(Collectors.toSet());
    }
}
