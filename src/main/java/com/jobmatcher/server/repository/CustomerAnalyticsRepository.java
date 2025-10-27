package com.jobmatcher.server.repository;

import com.jobmatcher.server.domain.CustomerProfile;
import com.jobmatcher.server.model.analytics.customer.MonthlySpendingDTO;
import com.jobmatcher.server.model.analytics.customer.ProjectStatsDTO;
import com.jobmatcher.server.model.analytics.customer.TopFreelancerDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CustomerAnalyticsRepository extends JpaRepository<CustomerProfile, UUID> {

    // Monthly spending
    @Query(value = """
        SELECT 
            CAST(EXTRACT(YEAR FROM p.paid_at) AS INTEGER) AS year,
            CAST(EXTRACT(MONTH FROM p.paid_at) AS INTEGER) AS month,
            SUM(p.amount) AS total
        FROM payments p
        JOIN contracts c ON c.payment_id = p.id
        WHERE c.customer_id = :customerId
        GROUP BY year, month
        ORDER BY year, month
    """, nativeQuery = true)
    List<MonthlySpendingDTO> findMonthlySpending(@Param("customerId") UUID customerId);

    // Project status summary
    @Query(value = """
        SELECT SUM(CASE WHEN pr.status = 'ACCEPTED' THEN 1 ELSE 0 END) AS completed,
               SUM(CASE WHEN pr.status != 'ACCEPTED' THEN 1 ELSE 0 END) AS active
        FROM proposals pr
        JOIN projects pj ON pj.id = pr.project_id
        WHERE pj.customer_id = :customerId
    """, nativeQuery = true)
    ProjectStatsDTO findProjectStats(@Param("customerId") UUID customerId);

    // Top freelancers
    @Query(value = """
        SELECT p.username AS freelancerName,
               SUM(pay.amount) AS totalEarned
        FROM contracts c
        JOIN payments pay ON pay.id = c.payment_id
        JOIN freelancer_profile f ON f.id = c.freelancer_id
        JOIN public_profile p ON p.id = f.id
        WHERE c.customer_id = :customerId
        GROUP BY p.username
        ORDER BY totalEarned DESC
        LIMIT 5
    """, nativeQuery = true)
    List<TopFreelancerDTO> findTopFreelancers(@Param("customerId") UUID customerId);
}
