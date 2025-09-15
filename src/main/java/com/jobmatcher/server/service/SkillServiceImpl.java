package com.jobmatcher.server.service;

import com.jobmatcher.server.domain.Skill;
import com.jobmatcher.server.mapper.SkillMapper;
import com.jobmatcher.server.model.SkillDTO;
import com.jobmatcher.server.repository.SkillRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Pattern;

@Slf4j
@Service
public class SkillServiceImpl implements ISkillService {

    private final SkillRepository skillRepository;
    private final SkillMapper skillMapper;

    public SkillServiceImpl(SkillRepository skillRepository, SkillMapper skillMapper) {
        this.skillRepository = skillRepository;
        this.skillMapper = skillMapper;
    }

    @Override
    public Skill findOrCreateByName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Skill name is empty or invalid.");
        }

        String sanitized = name.trim();
        return skillRepository.findByNameIgnoreCase(sanitized)
                .orElseGet(() -> skillRepository.save(new Skill(sanitized)));
    }

    @Override
    public List<SkillDTO> getAllSkills() {
        return skillRepository.findAll().stream().map(skillMapper::toDto).toList();
    }

    public String sanitizeSkillName(String input) {
        if (input == null) return "";
        String trimmed = input.trim();
        String cleaned = trimmed.replaceAll("[^a-zA-Z0-9+#\\s]", "");
        if (cleaned.length() > 50) {
            cleaned = cleaned.substring(0, 50);
        }
        return cleaned;
    }
}
