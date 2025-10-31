package com.jobmatcher.server.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobmatcher.server.controller.config.AbstractIntegrationTest;
import com.jobmatcher.server.domain.*;
import com.jobmatcher.server.model.AuthenticationRequest;
import com.jobmatcher.server.repository.FreelancerProfileRepository;
import com.jobmatcher.server.repository.JobCategoryRepository;
import com.jobmatcher.server.repository.PortfolioItemRepository;
import com.jobmatcher.server.model.PortfolioItemRequestDTO;
import com.jobmatcher.server.repository.UserRepository;
import com.jobmatcher.server.service.CloudinaryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static com.jobmatcher.server.model.ApiConstants.API_VERSION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@AutoConfigureMockMvc
public class PortfolioItemControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PortfolioItemRepository portfolioItemRepository;

    @Autowired
    private FreelancerProfileRepository freelancerProfileRepository;

    @Autowired
    private JobCategoryRepository jobCategoryRepository;

    @Autowired
    private UserRepository userRepository;

    private UUID portfolioItemId;
    private UUID freelancerProfileId;
    private Long categoryId;
    private User user;

    private String jwtToken;
    private String refreshToken;
    List<Long> subcategoryIds;

    private PortfolioItemController portfolioItemController;

    @TestConfiguration
    static class PortfolioItemTestConfig {
        @Autowired
        private PortfolioItemRepository portfolioItemRepository;

        @Bean
        public CloudinaryService cloudinaryService() {
            return new CloudinaryService(null, null, null, null, portfolioItemRepository) {
                @Override
                public void uploadMultipleImages(UUID portfolioItemId, UUID userId, List<MultipartFile> files) {
                    // do nothing for testing
                }
            };
        }
    }

    @BeforeEach
    void setUp() throws Exception {
        // Authenticate seeded user and get JWT
        String seededEmail = "user1@jobmatcher.com";
        String seededPassword = "Password!23";

        AuthenticationRequest loginRequest = new AuthenticationRequest();
        loginRequest.setEmail(seededEmail);
        loginRequest.setPassword(seededPassword);

        String responseBody = mockMvc.perform(post(API_VERSION + "/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        jwtToken = objectMapper.readTree(responseBody).get("token").asText();
        refreshToken = objectMapper.readTree(responseBody).get("refreshToken").asText();
        user = userRepository.findByEmail(seededEmail).orElseThrow();

        // Use seeded FreelancerProfile and JobCategory
        FreelancerProfile freelancerProfile = freelancerProfileRepository.findByUserId(user.getId()).orElseThrow();
        freelancerProfileId = freelancerProfile.getId();

        // Use first JobCategory and its subcategories
        JobCategory category = jobCategoryRepository.findAll().get(0);
        categoryId = category.getId();
        Set<JobSubcategory> subcategories = category.getSubcategories();
        subcategoryIds = subcategories.stream().map(JobSubcategory::getId).toList();

        // Clear PortfolioItems
        portfolioItemRepository.deleteAll();

        // Seed one portfolio item
        PortfolioItem item = new PortfolioItem();
        item.setTitle("Existing Item");
        item.setFreelancerProfile(freelancerProfile);
        item.setCategory(category);
        item.setClientName("Client A");
        item.setSubcategories(new HashSet<>(subcategories));
        item = portfolioItemRepository.save(item);
        portfolioItemId = item.getId();

    }

    @Test
    void shouldGetPortfolioItemById() throws Exception {
        mockMvc.perform(get(API_VERSION + "/portfolio-items/{id}", portfolioItemId)
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(portfolioItemId.toString()))
                .andExpect(jsonPath("$.title").value("Existing Item"));
    }

    @Test
    void shouldCreatePortfolioItem() throws Exception {
        PortfolioItemRequestDTO request = PortfolioItemRequestDTO.builder()
                .title("New Item")
                .description("New description")
                .categoryId(categoryId)
                .subcategoryIds(new HashSet<>(subcategoryIds))
                .demoUrl("https://example.com/demo")
                .sourceUrl("http://example.com/source")
                .clientName("Client B")
                .freelancerProfileId(freelancerProfileId)
                .build();

        mockMvc.perform(post(API_VERSION + "/portfolio-items")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("New Item"))
                .andExpect(jsonPath("$.category.id").value(categoryId.toString()));
    }

    @Test
    void shouldUpdatePortfolioItem() throws Exception {
        PortfolioItemRequestDTO request = PortfolioItemRequestDTO.builder()
                .title("Updated Item")
                .description("Updated description")
                .categoryId(categoryId)
                .subcategoryIds(new HashSet<>(subcategoryIds))
                .demoUrl("https://example.com/demo")
                .sourceUrl("http://example.com/source")
                .clientName("Client B")
                .freelancerProfileId(freelancerProfileId)
                .build();

        mockMvc.perform(patch(API_VERSION + "/portfolio-items/{id}", portfolioItemId)
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated Item"));
    }

    @Test
    void shouldDeletePortfolioItem() throws Exception {
        mockMvc.perform(delete(API_VERSION + "/portfolio-items/{id}", portfolioItemId)
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldUploadPortfolioItemImages() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "files", "image.png", "image/png", "dummy data".getBytes()
        );

        mockMvc.perform(multipart(API_VERSION + "/portfolio-items/images/upload/{id}", portfolioItemId)
                        .file(file)
                        .param("userId", freelancerProfileId.toString())
                        .with(request -> {
                            request.setMethod("PATCH");
                            return request;
                        }) // force PATCH
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void shouldDeletePortfolioItemImage() throws Exception {
        mockMvc.perform(patch(API_VERSION + "/portfolio-items/images/remove/{portfolioItemId}", portfolioItemId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("\"https://example.com/image.png\"")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void shouldGetPortfolioItemsByFreelancerProfileId() throws Exception {
        UUID freelancerId = freelancerProfileId;

        mockMvc.perform(get(API_VERSION + "/portfolio-items/freelancer/{freelancerProfileId}", freelancerId)
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1)) // we seeded 1 portfolio item
                .andExpect(jsonPath("$[0].id").value(portfolioItemId.toString()))
                .andExpect(jsonPath("$[0].title").value("Existing Item"))
                .andExpect(jsonPath("$[0].category.id").value(categoryId.toString()));
    }

}
