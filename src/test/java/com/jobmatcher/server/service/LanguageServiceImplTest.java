package com.jobmatcher.server.service;

import com.jobmatcher.server.domain.Language;
import com.jobmatcher.server.exception.ResourceNotFoundException;
import com.jobmatcher.server.mapper.LanguageMapper;
import com.jobmatcher.server.model.LanguageDTO;
import com.jobmatcher.server.repository.LanguageRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LanguageServiceImplTest {

    @Mock
    private LanguageRepository languageRepository;

    @Mock
    private LanguageMapper languageMapper;

    @InjectMocks
    private LanguageServiceImpl languageService;

    @Test
    void findLanguageById_found() {
        Language language = new Language("English");
        language.setId(1);
        LanguageDTO dto = LanguageDTO.builder().id(1).name("English").build();

        when(languageRepository.findById(1)).thenReturn(Optional.of(language));
        when(languageMapper.toDto(language)).thenReturn(dto);

        LanguageDTO result = languageService.findLanguageById(1);

        assertThat(result.getName()).isEqualTo("English");
        assertThat(result.getId()).isEqualTo(1);
    }

    @Test
    void findLanguageById_notFound_throws() {
        when(languageRepository.findById(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> languageService.findLanguageById(999))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Language not found");
    }

    @Test
    void findLanguageByName_found() {
        Language language = new Language("French");
        language.setId(2);
        LanguageDTO dto = LanguageDTO.builder().id(2).name("French").build();

        when(languageRepository.findByName("French")).thenReturn(Optional.of(language));
        when(languageMapper.toDto(language)).thenReturn(dto);

        LanguageDTO result = languageService.findLanguageByName("French");

        assertThat(result.getName()).isEqualTo("French");
        assertThat(result.getId()).isEqualTo(2);
    }

    @Test
    void findLanguageByName_notFound_throws() {
        when(languageRepository.findByName("NonExistent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> languageService.findLanguageByName("NonExistent"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Language not found");
    }

    @Test
    void getAllLanguages_returnsList() {
        Language lang1 = new Language("English");
        lang1.setId(1);
        Language lang2 = new Language("Spanish");
        lang2.setId(2);
        LanguageDTO dto1 = LanguageDTO.builder().id(1).name("English").build();
        LanguageDTO dto2 = LanguageDTO.builder().id(2).name("Spanish").build();

        when(languageRepository.findAll()).thenReturn(List.of(lang1, lang2));
        when(languageMapper.toDto(lang1)).thenReturn(dto1);
        when(languageMapper.toDto(lang2)).thenReturn(dto2);

        List<LanguageDTO> list = languageService.getAllLanguages();

        assertThat(list).hasSize(2);
        assertThat(list).extracting("name").containsExactlyInAnyOrder("English", "Spanish");
    }

    @Test
    void saveLanguage_success() {
        LanguageDTO dto = LanguageDTO.builder().name("German").build();
        Language entity = new Language("German");

        when(languageMapper.toEntity(dto)).thenReturn(entity);
        when(languageRepository.save(entity)).thenReturn(entity);
        when(languageMapper.toDto(entity)).thenReturn(LanguageDTO.builder().name("German").build());

        LanguageDTO saved = languageService.saveLanguage(dto);

        assertThat(saved.getName()).isEqualTo("German");
    }


}