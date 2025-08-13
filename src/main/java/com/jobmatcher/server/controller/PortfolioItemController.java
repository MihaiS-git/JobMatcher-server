package com.jobmatcher.server.controller;

import com.jobmatcher.server.model.PortfolioItemDetailDTO;
import com.jobmatcher.server.model.PortfolioItemRequestDTO;
import com.jobmatcher.server.model.PortfolioItemSummaryDTO;
import com.jobmatcher.server.service.IPortfolioItemService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Set;
import java.util.UUID;

import static com.jobmatcher.server.model.ApiConstants.API_VERSION;

@RestController
@RequestMapping(API_VERSION + "/portfolio-items")
public class PortfolioItemController {

    private final IPortfolioItemService portfolioItemService;

    public PortfolioItemController(IPortfolioItemService portfolioItemService) {
        this.portfolioItemService = portfolioItemService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<PortfolioItemDetailDTO> getPortfolioItemById(@PathVariable UUID id) {
        PortfolioItemDetailDTO item = portfolioItemService.getPortfolioItemById(id);
        return ResponseEntity.ok(item);
    }

    @GetMapping("/freelancer/{freelancerProfileId}")
    public ResponseEntity<Set<PortfolioItemSummaryDTO>> getPortfolioItemsByFreelancerProfileId(@PathVariable UUID freelancerProfileId) {
        Set<PortfolioItemSummaryDTO> items = portfolioItemService.getPortfolioItemsByFreelancerProfileId(freelancerProfileId);
        return ResponseEntity.ok(items);
    }

    @PostMapping
    public ResponseEntity<PortfolioItemDetailDTO> savePortfolioItem(@RequestBody @Valid PortfolioItemRequestDTO portfolioItem) {
        PortfolioItemDetailDTO savedItem = portfolioItemService.savePortfolioItem(portfolioItem);
        URI location = URI.create(API_VERSION + "/portfolio-items/" + savedItem.getId());
        return ResponseEntity.created(location).body(savedItem);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<PortfolioItemDetailDTO> updatePortfolioItem(@PathVariable UUID id,
                                                                      @RequestBody @Valid PortfolioItemRequestDTO portfolioItem) {
        PortfolioItemDetailDTO updatedItem = portfolioItemService.updatePortfolioItem(id, portfolioItem);
        return ResponseEntity.ok(updatedItem);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePortfolioItem(@PathVariable UUID id) {
        portfolioItemService.deletePortfolioItem(id);
        return ResponseEntity.noContent().build();
    }

}
