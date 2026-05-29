package com.example.demo.dao.match;

import com.example.demo.entity.MatchTactics;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MatchTacticsRepository extends JpaRepository<MatchTactics,Long> {
    // Xóa chiến thuật cũ của một đội trong một trận đấu cụ thể
    void deleteByMatch_IdAndTeam_Id(Long matchId, Long teamId);

    // Lấy chiến thuật của một đội trong trận đấu
    @EntityGraph(attributePaths = {"match", "team"})
    Optional<MatchTactics> findByMatch_IdAndTeam_Id(Long matchId, Long teamId);

    @EntityGraph(attributePaths = {"match", "team"})
    List<MatchTactics> findByMatch_Id(Long matchId);

    @EntityGraph(attributePaths = {"match", "team"})
    Optional<MatchTactics> findOneById(Long id);


    @EntityGraph(attributePaths = {"match", "team", "lineups", "lineups.player"})
    List<MatchTactics> findByMatchId(Long matchId);

    Optional<MatchTactics> findByMatchIdAndTeamId(Long matchId, Long teamId);

    void deleteByMatchId(Long matchId);

    @EntityGraph(attributePaths = {"match", "team", "lineups", "lineups.player"})
    Optional<MatchTactics> findWithLineupsById(Long id);

    @Query("""
    select distinct mt
    from MatchTactics mt
    left join fetch mt.team
    left join fetch mt.lineups l
    left join fetch l.player
    where mt.match.id = :matchId
""")
    List<MatchTactics> findByMatchIdWithLineups(@Param("matchId") Long matchId);
}
