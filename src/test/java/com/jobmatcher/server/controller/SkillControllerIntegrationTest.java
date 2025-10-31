package com.jobmatcher.server.controller;

import com.jobmatcher.server.controller.config.AbstractIntegrationTest;
import com.jobmatcher.server.domain.Skill;
import com.jobmatcher.server.repository.SkillRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@AutoConfigureMockMvc
public class SkillControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SkillRepository skillRepository;

    @Test
    @WithMockUser(username = "test", roles = {"STAFF"})
    void shouldReturnSkillsList() throws Exception {
        List<Skill> skills = skillRepository.findAll();

        mockMvc.perform(get("/api/v0/skills"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(skills.size()))
                .andExpect(jsonPath("$[0].name").value(skills.getFirst().getName()));
    }
}
