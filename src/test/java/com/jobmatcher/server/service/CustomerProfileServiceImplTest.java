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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerProfileServiceImplTest {

    @Mock
    private CustomerProfileRepository profileRepository;

    @Mock
    private CustomerProfileMapper profileMapper;

    @Mock
    private UserRepository userRepository;

    @Mock
    private LanguageRepository languageRepository;

    @InjectMocks
    private CustomerProfileServiceImpl service;

    @Test
    void getAllCustomerProfiles_shouldReturnMappedDTOs() {
        CustomerProfile profile = new CustomerProfile();
        CustomerSummaryDTO dto = CustomerSummaryDTO.builder().build();
        when(profileRepository.findAll()).thenReturn(List.of(profile));
        when(profileMapper.toCustomerSummaryDto(profile)).thenReturn(dto);

        Set<CustomerSummaryDTO> result = service.getAllCustomerProfiles();

        assertThat(result).containsExactly(dto);
    }

    @Test
    void getCustomerProfileById_found_shouldReturnDTO() {
        UUID id = UUID.randomUUID();
        CustomerProfile profile = new CustomerProfile();
        CustomerDetailDTO dto = CustomerDetailDTO.builder().build();
        when(profileRepository.findById(id)).thenReturn(Optional.of(profile));
        when(profileMapper.toCustomerDetailDto(profile)).thenReturn(dto);

        CustomerDetailDTO result = service.getCustomerProfileById(id);

        assertThat(result).isEqualTo(dto);
    }

    @Test
    void getCustomerProfileById_notFound_shouldThrow() {
        UUID id = UUID.randomUUID();
        when(profileRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getCustomerProfileById(id))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Profile not found");
    }

    @Test
    void saveCustomerProfile_shouldSaveAndReturnDTO() {
        UUID userId = UUID.randomUUID();
        Set<Integer> languageIds = Set.of(1, 2);
        Set<Language> languages = Set.of(new Language(1, "English"), new Language(2, "Spanish"));
        User user = new User();
        CustomerProfile profile = new CustomerProfile();
        CustomerProfile saved = new CustomerProfile();
        CustomerDetailDTO dto = CustomerDetailDTO.builder().build();

        CustomerProfileRequestDTO request = CustomerProfileRequestDTO.builder()
                .userId(userId)
                .languageIds(languageIds)
                .build();

        when(languageRepository.findAllById(languageIds)).thenReturn(List.copyOf(languages));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(profileMapper.toEntity(request, user, languages)).thenReturn(profile);
        when(profileRepository.save(profile)).thenReturn(saved);
        when(profileMapper.toCustomerDetailDto(saved)).thenReturn(dto);

        CustomerDetailDTO result = service.saveCustomerProfile(request);

        assertThat(result).isEqualTo(dto);
    }

    @Test
    void saveCustomerProfile_missingLanguages_shouldThrow() {
        CustomerProfileRequestDTO request = CustomerProfileRequestDTO.builder()
                .userId(UUID.randomUUID())
                .languageIds(Set.of(1, 2))
                .build();

        when(languageRepository.findAllById(any())).thenReturn(List.of(new Language(1, "English")));

        assertThatThrownBy(() -> service.saveCustomerProfile(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Languages not found");
    }

    @Test
    void saveCustomerProfile_missingUser_shouldThrow() {
        CustomerProfileRequestDTO request = CustomerProfileRequestDTO.builder()
                .userId(UUID.randomUUID())
                .languageIds(Set.of(1))
                .build();

        when(languageRepository.findAllById(any())).thenReturn(List.of(new Language(1, "English")));
        when(userRepository.findById(request.getUserId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.saveCustomerProfile(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    void updateCustomerProfile_shouldUpdateAndReturnDTO() {
        UUID profileId = UUID.randomUUID();
        Set<Integer> languageIds = Set.of(1);
        Set<Language> languages = Set.of(new Language(1, "English"));
        CustomerProfile profile = new CustomerProfile();
        CustomerProfile updated = new CustomerProfile();
        CustomerDetailDTO dto = CustomerDetailDTO.builder().build();

        CustomerProfileRequestDTO request = CustomerProfileRequestDTO.builder()
                .languageIds(languageIds)
                .build();

        when(languageRepository.findAllById(languageIds)).thenReturn(List.copyOf(languages));
        when(profileRepository.findById(profileId)).thenReturn(Optional.of(profile));
        when(profileRepository.save(profile)).thenReturn(updated);
        when(profileMapper.toCustomerDetailDto(updated)).thenReturn(dto);

        CustomerDetailDTO result = service.updateCustomerProfile(profileId, request);

        assertThat(result).isEqualTo(dto);
    }

    @Test
    void updateCustomerProfile_missingLanguages_shouldThrow() {
        CustomerProfileRequestDTO request = CustomerProfileRequestDTO.builder()
                .languageIds(Set.of(1, 2))
                .build();

        when(languageRepository.findAllById(any())).thenReturn(List.of(new Language(1, "English")));

        assertThatThrownBy(() -> service.updateCustomerProfile(UUID.randomUUID(), request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Languages not found");
    }

    @Test
    void updateCustomerProfile_missingProfile_shouldThrow() {
        CustomerProfileRequestDTO request = CustomerProfileRequestDTO.builder()
                .languageIds(Set.of(1))
                .build();
        UUID id = UUID.randomUUID();

        when(languageRepository.findAllById(any())).thenReturn(List.of(new Language(1, "English")));
        when(profileRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateCustomerProfile(id, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Profile not found");
    }
}