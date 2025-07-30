package com.jobmatcher.server.repository;

import com.jobmatcher.server.domain.Language;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LanguageRepository extends JpaRepository<Language, Integer> {
    Optional<Language> findByName(String name);
}
