package com.jobmatcher.server.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobmatcher.server.controller.config.AbstractIntegrationTest;
import com.jobmatcher.server.model.LanguageDTO;
import com.jobmatcher.server.repository.LanguageRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@AutoConfigureMockMvc
public class LanguageControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LanguageRepository languageRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser(username = "test", roles = {"STAFF"})
    void shouldReturnLanguageById() throws Exception {
        var language = languageRepository.findAll().getFirst();

        mockMvc.perform(get("/api/v0/languages/{id}", language.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(language.getName()));
    }

    @Test
    @WithMockUser(username = "test", roles = {"STAFF"})
    void shouldReturn404ForNonExistingLanguageById() throws Exception {
        mockMvc.perform(get("/api/v0/languages/{id}", 9999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Language not found."));
    }

    @Test
    @WithMockUser(username = "test", roles = {"STAFF"})
    void shouldReturnLanguageByName() throws Exception {
        var language = languageRepository.findAll().getFirst();

        mockMvc.perform(get("/api/v0/languages/name/{name}", language.getName()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(language.getName()));
    }

    @Test
    @WithMockUser(username = "test", roles = {"STAFF"})
    void shouldReturn404ForNonExistingLanguageByName() throws Exception {
        mockMvc.perform(get("/api/v0/languages/name/{name}", "NonExistingLanguage"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Language not found."));
    }

    @Test
    @WithMockUser(username = "test", roles = {"STAFF"})
    void shouldReturnLanguages() throws Exception {
        var languages = languageRepository.findAll();

        mockMvc.perform(get("/api/v0/languages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(languages.size()))
                .andExpect(jsonPath("$[0].name").value(languages.get(0).getName()))
                .andExpect(jsonPath("$[1].name").value(languages.get(1).getName()));
    }

    @Test
    @WithMockUser(username = "test", roles = {"STAFF"})
    void shouldReturnCreatedLanguage() throws Exception {
        LanguageDTO newLanguage = LanguageDTO.builder()
                .name("Klingon")
                .build();

        mockMvc.perform(post("/api/v0/languages")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(newLanguage)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/v0/languages/")))
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("Klingon"));
    }

    @Test
    @WithMockUser(username = "test", roles = {"STAFF"})
    void shouldReturnBadRequestWhenNameIsMissing() throws Exception {
        LanguageDTO invalidLanguage = LanguageDTO.builder().name(null).build();

        mockMvc.perform(post("/api/v0/languages")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(invalidLanguage)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.validationErrors.name").value("must not be blank"));
    }
}
