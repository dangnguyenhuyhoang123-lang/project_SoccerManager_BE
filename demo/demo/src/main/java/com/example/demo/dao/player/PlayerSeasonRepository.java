package com.example.demo.dao.player;

import com.example.demo.entity.Player;
import com.example.demo.entity.PlayerSeason;
import com.example.demo.entity.Season;
import com.example.demo.entity.SeasonTeam;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlayerSeasonRepository extends JpaRepository<PlayerSeason,Long> {
    Page<PlayerSeason> findBySeasonId(Long seasonId, Pageable pageable);

    Page<PlayerSeason> findByTeamId(Long teamId, Pageable pageable);

    Page<PlayerSeason> findByPlayerId(Long playerId, Pageable pageable);

    Page<PlayerSeason> findBySeasonIdAndTeamId(Long seasonId, Long teamId, Pageable pageable);

    List<PlayerSeason> findByTeamIdOrderByShirtNumberAsc(Long teamId);

    List<PlayerSeason> findByTeamSeasonIdOrderByShirtNumberAsc(Long teamSeasonId);

    @EntityGraph(attributePaths = {"player", "season"})
    List<PlayerSeason> findBySeasonId(Long seasonId);

//    @Query("""
//        SELECT COUNT(ps) > 0
//        FROM PlayerSeason ps
//        WHERE ps.player.id = :playerId
//          AND ps.team.id = :teamId
//          AND ps.season.id = :seasonId
//          AND
//        """)
//    boolean existsByPlayerTeamSeason(
//            @Param("playerId") Long playerId,
//            @Param("teamId") Long teamId,
//            @Param("seasonId") Long seasonId
//    );

    @Query("""
    SELECT COUNT(ps) > 0
    FROM PlayerSeason ps
    WHERE ps.player.id = :playerId
      AND ps.teamSeason.team.id = :teamId
      AND ps.teamSeason.season.id = :seasonId
""")
    boolean existsByPlayerTeamSeason(
            @Param("playerId") Long playerId,
            @Param("teamId") Long teamId,
            @Param("seasonId") Long seasonId
    );

    Optional<PlayerSeason> findByPlayerAndSeason(Player player, Season season);

    Optional<PlayerSeason> findByTeamSeasonAndShirtNumber(
            SeasonTeam teamSeason,
            Integer shirtNumber
    );
}
