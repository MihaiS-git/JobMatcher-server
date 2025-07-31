package com.jobmatcher.server.mapper;

import com.jobmatcher.server.domain.Skill;
import com.jobmatcher.server.model.SkillDTO;
import org.springframework.stereotype.Component;

@Component
public class SkillMapper {
    public SkillDTO toDto(Skill skill){
        if(skill == null) return null;

        return SkillDTO.builder()
                .name(skill.getName())
                .build();
    }
}
