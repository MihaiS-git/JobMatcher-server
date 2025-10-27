package com.jobmatcher.server.controller;

import com.jobmatcher.server.model.analytics.customer.MonthlySpendingDTO;
import com.jobmatcher.server.model.analytics.customer.ProjectStatsDTO;
import com.jobmatcher.server.model.analytics.customer.TopFreelancerDTO;
import com.jobmatcher.server.repository.CustomerAnalyticsRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

import static com.jobmatcher.server.model.ApiConstants.API_VERSION;

@Slf4j
@RestController
@RequestMapping(API_VERSION + "/customers/{customerId}/analytics")
public class CustomerAnalyticsController {

    private final CustomerAnalyticsRepository customerAnalyticsRepository;

    public CustomerAnalyticsController(CustomerAnalyticsRepository customerAnalyticsRepository) {
        this.customerAnalyticsRepository = customerAnalyticsRepository;
    }

    @GetMapping("/monthly-spending")
    public List<MonthlySpendingDTO> getMonthlySpending(@PathVariable String customerId) {
        log.info("Fetching monthly spending for customerId: {}", customerId);
        List<MonthlySpendingDTO> spending = customerAnalyticsRepository.findMonthlySpending(UUID.fromString(customerId));
        log.info("Monthly spending data: {}", spending);
        return spending;
    }

    @GetMapping("/project-stats")
    public ProjectStatsDTO getProjectStats(@PathVariable String customerId) {
        log.info("Fetching project stats for customerId: {}", customerId);
        ProjectStatsDTO stats = customerAnalyticsRepository.findProjectStats(UUID.fromString(customerId));
        log.info("Project stats data: {}", stats);
        return stats;
    }

    @GetMapping("/top-freelancers")
    public List<TopFreelancerDTO> getTopFreelancers(@PathVariable String customerId) {
        log.info("Fetching top freelancers for customerId: {}", customerId);
        List<TopFreelancerDTO> topFreelancers = customerAnalyticsRepository.findTopFreelancers(UUID.fromString(customerId));
        log.info("Top freelancers data: {}", topFreelancers);
        return topFreelancers;
    }
}
