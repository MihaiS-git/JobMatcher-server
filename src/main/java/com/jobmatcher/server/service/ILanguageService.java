package com.jobmatcher.server.service;

import com.jobmatcher.server.model.LanguageDTO;

import java.util.List;

public interface ILanguageService {
    LanguageDTO findLanguageById(Integer id);
    LanguageDTO findLanguageByName(String name);
    List<LanguageDTO> getAllLanguages();
    LanguageDTO saveLanguage(LanguageDTO dto);
}
