package com.jobmatcher.server.service;

import com.jobmatcher.server.domain.CustomerProfile;
import com.jobmatcher.server.domain.Language;
import com.jobmatcher.server.domain.User;
import com.jobmatcher.server.exception.InvalidProfileDataException;
import com.jobmatcher.server.exception.ResourceNotFoundException;
import com.jobmatcher.server.mapper.CustomerProfileMapper;
import com.jobmatcher.server.model.*;
import com.jobmatcher.server.repository.CustomerProfileRepository;
import com.jobmatcher.server.repository.LanguageRepository;
import com.jobmatcher.server.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

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

    @Captor
    ArgumentCaptor<CustomerProfile> profileCaptor;

    private CustomerProfile profile;
    private CustomerDetailDTO detailDTO;
    private CustomerProfileRequestDTO requestDTO;
    private User user;
    private UUID profileId;

    @BeforeEach
    void setUp() {
        profileId = UUID.randomUUID();

        // Entity
        profile = new CustomerProfile();
        profile.setId(profileId);

        // DTOs
        detailDTO = CustomerDetailDTO.builder()
                .username("testuser")
                .build();

        user = new User();
        user.setId(UUID.randomUUID());

        requestDTO = CustomerProfileRequestDTO.builder()
                .userId(user.getId())
                .username("testuser")
                .languageIds(Set.of(1))
                .socialMedia(Set.of())
                .build();
    }

    @Test
    void getAllCustomerProfiles_shouldReturnProfiles() {
        when(profileRepository.findAll()).thenReturn(List.of(profile));
        CustomerSummaryDTO dto = CustomerSummaryDTO.builder().build();
        when(profileMapper.toCustomerSummaryDto(profile, true)).thenReturn(dto);

        Set<CustomerSummaryDTO> result = service.getAllCustomerProfiles();

        assertEquals(1, result.size());
        verify(profileRepository).findAll();
    }

    @Test
    void getCustomerProfileById_existingProfile_shouldReturnDetailDTO() {
        when(profileRepository.findById(profileId)).thenReturn(Optional.of(profile));
        when(profileMapper.toCustomerDetailDto(profile)).thenReturn(detailDTO);

        CustomerDetailDTO result = service.getCustomerProfileById(profileId);

        assertEquals(detailDTO, result);
        verify(profileRepository).findById(profileId);
    }

    @Test
    void getCustomerProfileById_nonExistingProfile_shouldThrow() {
        when(profileRepository.findById(profileId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.getCustomerProfileById(profileId));
    }

    @Test
    void getCustomerProfileByUserId_existingUser_shouldReturnDetailDTO() {
        profile.setUser(user);
        when(profileRepository.findByUserId(user.getId())).thenReturn(Optional.of(profile));
        when(profileMapper.toCustomerDetailDto(profile)).thenReturn(detailDTO);

        CustomerDetailDTO result = service.getCustomerProfileByUserId(user.getId());

        assertEquals(detailDTO, result);
        verify(profileRepository).findByUserId(user.getId());
    }

    @Test
    void getCustomerProfileByUserId_nonExistingUser_shouldReturnNull() {
        when(profileRepository.findByUserId(user.getId())).thenReturn(Optional.empty());

        CustomerDetailDTO result = service.getCustomerProfileByUserId(user.getId());

        assertNull(result);
    }

    @Test
    void saveCustomerProfile_validRequest_shouldSaveAndReturnDTO() {
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        Language lang = new Language();
        lang.setId(1);
        when(languageRepository.findAllById(Set.of(1))).thenReturn(List.of(lang));

        when(profileMapper.toEntity(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(profile);
        when(profileRepository.save(profile)).thenReturn(profile);
        when(profileMapper.toCustomerDetailDto(profile)).thenReturn(detailDTO);

        CustomerDetailDTO result = service.saveCustomerProfile(requestDTO);

        assertEquals(detailDTO, result);
        verify(profileRepository).save(profile);
    }

    @Test
    void saveCustomerProfile_invalidUsername_shouldThrow() {
        requestDTO = CustomerProfileRequestDTO.builder()
                .userId(user.getId())
                .username("")
                .build();

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        assertThrows(InvalidProfileDataException.class, () -> service.saveCustomerProfile(requestDTO));
    }

    @Test
    void updateCustomerProfile_existingProfile_shouldUpdateFields() {
        when(profileRepository.findById(profileId)).thenReturn(Optional.of(profile));
        requestDTO = CustomerProfileRequestDTO.builder()
                .username("newname")
                .company("New Company")
                .about("New About")
                .websiteUrl("https://example.com")
                .socialMedia(Set.of("https://twitter.com/test"))
                .languageIds(Set.of(1))
                .build();

        Language lang = new Language();
        lang.setId(1);
        when(languageRepository.findAllById(Set.of(1))).thenReturn(List.of(lang));

        when(profileRepository.save(any())).thenReturn(profile);
        when(profileMapper.toCustomerDetailDto(profile)).thenReturn(detailDTO);

        CustomerDetailDTO result = service.updateCustomerProfile(profileId, requestDTO);

        assertEquals(detailDTO, result);
        verify(profileRepository).save(profileCaptor.capture());
        CustomerProfile captured = profileCaptor.getValue();
        assertEquals("newname", captured.getUsername());
        assertEquals("New Company", captured.getCompany());
    }

    @Test
    void updateCustomerProfile_invalidUsername_shouldSanitize() {
        profile.setUsername("old");
        requestDTO = CustomerProfileRequestDTO.builder()
                .userId(user.getId())
                .username("bad<name>")
                .build();

        when(profileRepository.findById(profileId)).thenReturn(Optional.of(profile));
        when(profileRepository.save(any(CustomerProfile.class))).thenAnswer(inv -> inv.getArgument(0));
        when(profileMapper.toCustomerDetailDto(any(CustomerProfile.class))).thenReturn(CustomerDetailDTO.builder().build());

        CustomerDetailDTO result = service.updateCustomerProfile(profileId, requestDTO);

        assertEquals("bad", profile.getUsername());
        verify(profileRepository).save(profileCaptor.capture());
        assertEquals("bad", profileCaptor.getValue().getUsername());
    }
}
