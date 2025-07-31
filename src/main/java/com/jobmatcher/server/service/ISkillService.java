package com.jobmatcher.server.service;

import com.jobmatcher.server.domain.Skill;
import com.jobmatcher.server.model.SkillDTO;

import java.util.List;

public interface ISkillService {
    Skill findOrCreateByName(String name);
    List<SkillDTO> getAllSkills();
}
