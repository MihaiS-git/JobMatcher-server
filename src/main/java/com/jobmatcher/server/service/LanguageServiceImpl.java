package com.jobmatcher.server.service;

import com.jobmatcher.server.domain.Language;
import com.jobmatcher.server.exception.ResourceNotFoundException;
import com.jobmatcher.server.mapper.LanguageMapper;
import com.jobmatcher.server.model.LanguageDTO;
import com.jobmatcher.server.repository.LanguageRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LanguageServiceImpl implements ILanguageService{

    private final LanguageRepository languageRepository;
    private final LanguageMapper languageMapper;

    public LanguageServiceImpl(LanguageRepository languageRepository, LanguageMapper languageMapper) {
        this.languageRepository = languageRepository;
        this.languageMapper = languageMapper;
    }

    @Override
    public LanguageDTO findLanguageById(Integer id) {
        Language language = languageRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Language not found."));
        return languageMapper.toDto(language);
    }

    @Override
    public LanguageDTO findLanguageByName(String name) {
        Language language = languageRepository.findByName(name).orElseThrow(() -> new ResourceNotFoundException("Language not found."));
        return languageMapper.toDto(language);
    }

    @Override
    public List<LanguageDTO> getAllLanguages() {
        List<LanguageDTO> languages = languageRepository.findAll().stream().map(languageMapper::toDto).toList();
        if(languages.isEmpty()){
            throw new ResourceNotFoundException("No languages found.");
        }
        return languages;
    }

    @Override
    public LanguageDTO saveLanguage(LanguageDTO dto) {
        Language savedLanguage = languageRepository.save(languageMapper.toEntity(dto));
        return languageMapper.toDto(savedLanguage);
    }
}
