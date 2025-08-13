package com.jobmatcher.server.service;

import com.jobmatcher.server.model.PortfolioItemDetailDTO;
import com.jobmatcher.server.model.PortfolioItemRequestDTO;
import com.jobmatcher.server.model.PortfolioItemSummaryDTO;

import java.util.Set;
import java.util.UUID;

public interface IPortfolioItemService {
    PortfolioItemDetailDTO getPortfolioItemById(UUID id);
    Set<PortfolioItemSummaryDTO> getPortfolioItemsByFreelancerProfileId(UUID freelancerProfileId);
    PortfolioItemDetailDTO savePortfolioItem(PortfolioItemRequestDTO portfolioItem);
    PortfolioItemDetailDTO updatePortfolioItem(UUID id, PortfolioItemRequestDTO portfolioItem);
    void deletePortfolioItem(UUID id);
}
