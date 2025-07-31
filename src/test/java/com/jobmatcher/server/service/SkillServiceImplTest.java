package com.jobmatcher.server.service;

import com.jobmatcher.server.domain.Skill;
import com.jobmatcher.server.mapper.SkillMapper;
import com.jobmatcher.server.model.SkillDTO;
import com.jobmatcher.server.repository.SkillRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SkillServiceImplTest {

    @Mock
    private SkillRepository skillRepository;

    @Mock
    private SkillMapper skillMapper;

    @InjectMocks
    private SkillServiceImpl skillService;

    @Test
    void findOrCreateByName_existingSkill() {
        Skill skill = new Skill("ReactJS");

        when(skillRepository.findByNameIgnoreCase("ReactJS")).thenReturn(Optional.of(skill));

        Skill result = skillService.findOrCreateByName("ReactJS");

        assertThat(result.getName()).isEqualTo("ReactJS");
    }

    @Test
    void findOrCreateByName_newSkill() {
        String input = "  ReactJS  ";
        Skill newSkill = new Skill("ReactJS");

        when(skillRepository.findByNameIgnoreCase("ReactJS")).thenReturn(Optional.empty());
        when(skillRepository.save(any(Skill.class))).thenReturn(newSkill);

        Skill result = skillService.findOrCreateByName(input);

        assertThat(result.getName()).isEqualTo("ReactJS");
    }

    @Test
    void findOrCreateByName_emptyAfterSanitize_throws() {
        assertThatThrownBy(() -> skillService.findOrCreateByName("!@#$%^&*()"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("invalid characters");
    }

    @Test
    void findOrCreateByName_null_throws() {
        assertThatThrownBy(() -> skillService.findOrCreateByName(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("empty or invalid");
    }

    @Test
    void findOrCreateByName_blank_throws() {
        assertThatThrownBy(() -> skillService.findOrCreateByName("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("empty or invalid");
    }

    @Test
    void sanitizeSkillName_removesInvalidChars_andTrims() {
        String input = "  ReactJS!!!@$%^&*()_=-{}[]|\\:;\"'<>,.?/~`  ";
        String cleaned = skillService.sanitizeSkillName(input);
        assertThat(cleaned).isEqualTo("ReactJS");
    }

    @Test
    void sanitizeSkillName_truncatesLongInput() {
        String longInput = "VeryLongSkillNameThatExceedsTheFiftyCharacterLimit1234567890";
        String cleaned = skillService.sanitizeSkillName(longInput);
        assertThat(cleaned.length()).isLessThanOrEqualTo(50);
    }

    @Test
    void getAllSkills_returnsList() {
        Skill skill1 = new Skill("Java");
        Skill skill2 = new Skill("Python");
        SkillDTO dto1 = SkillDTO.builder().name("Java").build();
        SkillDTO dto2 = SkillDTO.builder().name("Python").build();

        when(skillRepository.findAll()).thenReturn(List.of(skill1, skill2));
        when(skillMapper.toDto(skill1)).thenReturn(dto1);
        when(skillMapper.toDto(skill2)).thenReturn(dto2);

        List<SkillDTO> skills = skillService.getAllSkills();

        assertThat(skills).hasSize(2);
        assertThat(skills).extracting("name").containsExactlyInAnyOrder("Java", "Python");
    }
}