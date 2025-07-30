package com.jobmatcher.server.repository;

import com.jobmatcher.server.domain.CustomerProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CustomerProfileRepository extends JpaRepository<CustomerProfile, UUID> {
}
