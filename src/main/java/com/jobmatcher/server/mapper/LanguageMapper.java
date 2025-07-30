package com.jobmatcher.server.mapper;

import com.jobmatcher.server.domain.Language;
import com.jobmatcher.server.model.LanguageDTO;
import org.springframework.stereotype.Component;

@Component
public class LanguageMapper {
    public LanguageDTO toDto(Language language){
        if(language == null) return null;

        return LanguageDTO.builder()
                .id(language.getId())
                .name(language.getName())
                .build();
    }

    public Language toEntity(LanguageDTO dto){
        if(dto == null) return null;

        Language language = new Language();
        if (dto.getId() != null) {
            language.setId(dto.getId());
        }
        language.setName(dto.getName());

        return language;
    }
}
