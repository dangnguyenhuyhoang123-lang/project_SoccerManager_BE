package com.example.demo.dao.match;

import com.example.demo.entity.MatchStats;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MatchStatsRepository extends JpaRepository<MatchStats,Long> {
    @Query("""
    SELECT ms FROM MatchStats ms
    JOIN FETCH ms.team
    JOIN FETCH ms.match m
    JOIN FETCH m.homeTeam
    JOIN FETCH m.awayTeam
    WHERE m.id = :matchId
""")
    List<MatchStats> findFullStats(@Param("matchId") Long matchId);



    @EntityGraph(attributePaths = {"match", "team"})
    List<MatchStats> findByMatchId(Long matchId);

    Optional<MatchStats> findByMatchIdAndTeamId(Long matchId, Long teamId);

    void deleteByMatchId(Long matchId);
}
