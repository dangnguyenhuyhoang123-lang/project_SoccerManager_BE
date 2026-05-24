package com.example.demo.dao.match;

import com.example.demo.entity.MatchLineup;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MatchLineupRepository extends JpaRepository<MatchLineup,Long> {
    // Spring sẽ tự hiểu: MatchLineup -> MatchTactics -> Match -> Id
    Page<MatchLineup> findByMatchTactics_Match_Id(Long matchId, Pageable pageable);

    Page<MatchLineup> findByMatchTactics_Id(Long tacticsId, Pageable pageable);

    @EntityGraph(attributePaths = {"player", "matchTactics", "matchTactics.team", "matchTactics.match"})
    List<MatchLineup> findByMatchTactics_Match_IdOrderByMatchTactics_IdAscLineupOrderAsc(Long matchId);

    @EntityGraph(attributePaths = {"player", "matchTactics", "matchTactics.team", "matchTactics.match"})
    List<MatchLineup> findByMatchTactics_Match_IdAndMatchTactics_Team_IdOrderByLineupOrderAsc(Long matchId, Long teamId);

    @EntityGraph(attributePaths = {"player", "matchTactics", "matchTactics.team", "matchTactics.match"})
    List<MatchLineup> findByMatchTactics_IdOrderByLineupOrderAsc(Long tacticsId);
}
