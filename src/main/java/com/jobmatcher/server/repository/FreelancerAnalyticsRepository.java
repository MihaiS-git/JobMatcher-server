package com.jobmatcher.server.repository;

import com.jobmatcher.server.domain.FreelancerProfile;
import com.jobmatcher.server.model.analytics.freelancer.JobCompletionDTO;
import com.jobmatcher.server.model.analytics.freelancer.MonthlyEarningsDTO;
import com.jobmatcher.server.model.analytics.freelancer.SkillEarningsDTO;
import com.jobmatcher.server.model.analytics.freelancer.TopClientDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FreelancerAnalyticsRepository extends JpaRepository<FreelancerProfile, UUID> {

    // Monthly earnings
    @Query(value = """
        SELECT 
            CAST(EXTRACT(YEAR FROM p.paid_at) AS INTEGER) AS year,
            CAST(EXTRACT(MONTH FROM p.paid_at) AS INTEGER) AS month,
            SUM(p.amount) AS total
        FROM payments p
        JOIN contracts c ON c.payment_id = p.id
        WHERE c.freelancer_id = :freelancerId
        GROUP BY year, month
        ORDER BY year, month
    """, nativeQuery = true)
    List<MonthlyEarningsDTO> findMonthlyEarnings(@Param("freelancerId") UUID freelancerId);

    // Job completion rate
    @Query(value = """
        SELECT SUM(CASE WHEN pr.status = 'ACCEPTED' THEN 1 ELSE 0 END) AS completed,
               COUNT(*) AS total,
               SUM(CASE WHEN pr.status = 'ACCEPTED' THEN 1 ELSE 0 END)::decimal / COUNT(*) * 100 AS rate
        FROM proposals pr
        WHERE pr.freelancer_id = :freelancerId
    """, nativeQuery = true)
    JobCompletionDTO findJobCompletionRate(@Param("freelancerId") UUID freelancerId);

    // Top clients
    @Query(value = """
        SELECT cust.username AS clientName,
               SUM(pay.amount) AS totalSpent
        FROM contracts c
        JOIN payments pay ON pay.id = c.payment_id
        JOIN customer_profile cp ON cp.id = c.customer_id
        JOIN public_profile cust ON cust.id = cp.id
        WHERE c.freelancer_id = :freelancerId
        GROUP BY cust.username
        ORDER BY totalSpent DESC
        LIMIT 5
    """, nativeQuery = true)
    List<TopClientDTO> findTopClients(@Param("freelancerId") UUID freelancerId);

    // Earnings per skill (fixed join table name)
    @Query(value = """
        SELECT s.name AS skillName,
               SUM(p.amount) AS earnings
        FROM payments p
        JOIN contracts c ON c.payment_id = p.id
        JOIN public_profile_skills fps ON fps.profile_id = c.freelancer_id
        JOIN skills s ON s.id = fps.skill_id
        WHERE c.freelancer_id = :freelancerId
        GROUP BY s.name
        ORDER BY earnings DESC
    """, nativeQuery = true)
    List<SkillEarningsDTO> findSkillEarnings(@Param("freelancerId") UUID freelancerId);
}
