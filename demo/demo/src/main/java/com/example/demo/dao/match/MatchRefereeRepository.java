package com.example.demo.dao.match;

import com.example.demo.entity.MatchReferee;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface MatchRefereeRepository extends JpaRepository<MatchReferee, Long> {

    @EntityGraph(attributePaths = {"match", "referee"})
    List<MatchReferee> findByMatchId(Long matchId);

    boolean existsByMatchIdAndRefereeId(Long matchId, Long refereeId);

    boolean existsByRefereeId(Long refereeId);

    boolean existsByMatchIdAndRoleIgnoreCase(Long matchId, String role);

    @Query("""
        SELECT COUNT(mr) > 0
        FROM MatchReferee mr
        WHERE mr.referee.id = :refereeId
          AND mr.match.matchDate = :matchDate
          AND (:currentAssignmentId IS NULL OR mr.id <> :currentAssignmentId)
    """)
    boolean existsRefereeAssignmentAtSameTime(
            @Param("refereeId") Long refereeId,
            @Param("matchDate") LocalDateTime matchDate,
            @Param("currentAssignmentId") Long currentAssignmentId
    );
}
