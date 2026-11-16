package com.disasterrecovery.repository;

import com.disasterrecovery.entities.GeneratedSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GeneratedSummaryRepository extends JpaRepository<GeneratedSummary, Integer> {

    Optional<GeneratedSummary> findByReportId(Integer reportId);

    List<GeneratedSummary> findByIsPublished(Boolean isPublished);

    List<GeneratedSummary> findByStatus(String status);
}
---------------------------------------------------------------------------
