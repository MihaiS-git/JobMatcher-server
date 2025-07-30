package com.jobmatcher.server.mapper;

import com.jobmatcher.server.domain.Skill;
import com.jobmatcher.server.model.SkillDTO;

public class SkillMapper {
    public SkillDTO toDto(Skill skill){
        if(skill == null) return null;

        return SkillDTO.builder()
                .name(skill.getName())
                .build();
    }
}
