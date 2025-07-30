package com.jobmatcher.server.service;

import com.jobmatcher.server.model.SkillDTO;

import java.util.List;

public interface ISkillService {
    SkillDTO findOrCreateByName(String name);
    List<SkillDTO> getAllSkills();
}
