package com.jobmatcher.server.controller;

import com.jobmatcher.server.domain.Skill;
import com.jobmatcher.server.model.analytics.freelancer.JobCompletionDTO;
import com.jobmatcher.server.model.analytics.freelancer.MonthlyEarningsDTO;
import com.jobmatcher.server.model.analytics.freelancer.SkillEarningsDTO;
import com.jobmatcher.server.model.analytics.freelancer.TopClientDTO;
import com.jobmatcher.server.repository.FreelancerAnalyticsRepository;
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
@RequestMapping(API_VERSION + "/freelancers/{freelancerId}/analytics")
public class FreelancerAnalyticsController {

    private final FreelancerAnalyticsRepository freelancerAnalyticsRepository;

    public FreelancerAnalyticsController(FreelancerAnalyticsRepository freelancerAnalyticsRepository) {
        this.freelancerAnalyticsRepository = freelancerAnalyticsRepository;
    }

    @GetMapping("/monthly-earnings")
    public List<MonthlyEarningsDTO> getMonthlyEarnings(@PathVariable String freelancerId) {
        log.info("Fetching monthly earnings for freelancerId: {}", freelancerId);
        List<MonthlyEarningsDTO> earnings = freelancerAnalyticsRepository.findMonthlyEarnings(UUID.fromString(freelancerId));
        log.info("Monthly earnings data: {}", earnings);
        return earnings;
    }

    @GetMapping("/job-completion")
    public JobCompletionDTO getJobCompletionRate(@PathVariable String freelancerId) {
        log.info("Fetching job completion rate for freelancerId: {}", freelancerId);
        JobCompletionDTO completionRate = freelancerAnalyticsRepository.findJobCompletionRate(UUID.fromString(freelancerId));
        log.info("Job completion data: {}", completionRate);
        return completionRate;
    }

    @GetMapping("/top-clients")
    public List<TopClientDTO> getTopClients(@PathVariable String freelancerId) {
        log.info("Fetching top clients for freelancerId: {}", freelancerId);
        List<TopClientDTO> topClients = freelancerAnalyticsRepository.findTopClients(UUID.fromString(freelancerId));
        log.info("Top clients data: {}", topClients);
        return topClients;
    }

    @GetMapping("/skill-earnings")
    public List<SkillEarningsDTO> getSkillEarnings(@PathVariable String freelancerId) {
        log.info("Fetching skill earnings for freelancerId: {}", freelancerId);
        List<SkillEarningsDTO> skillEarnings = freelancerAnalyticsRepository.findSkillEarnings(UUID.fromString(freelancerId));
        log.info("Skill earnings data: {}", skillEarnings);
        return skillEarnings;
    }
}
