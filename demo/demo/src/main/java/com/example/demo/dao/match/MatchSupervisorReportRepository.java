package com.example.demo.dao.match;

import com.example.demo.entity.MatchSupervisorReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MatchSupervisorReportRepository extends JpaRepository<MatchSupervisorReport, Long> {
    Optional<MatchSupervisorReport> findByMatchId(Long matchId);
}
