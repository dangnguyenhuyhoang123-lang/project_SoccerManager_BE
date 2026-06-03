package com.example.demo.dao.player;

import com.example.demo.dto.PlayerSearchResponse;
import com.example.demo.entity.Player;
import com.example.demo.entity.PlayerSeason;
import com.example.demo.entity.SeasonTeam;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PlayerRepository extends JpaRepository<Player, Long> {
    Page<Player> findByPosition(String position, Pageable pageable);

    Page<Player> findByStatus(String status, Pageable pageable);

    Page<Player> findByPositionAndStatus(String position, String status, Pageable pageable);

    Optional<Player> findByIDCode(String idCode);

    Page<Player> findByTeamId(Long teamId, Pageable pageable );

    Optional<Player> findBySourceUrl(String sourceUrl);

    Optional<Player> findByNormalizedName(String normalizedName);


    Optional<Player> findByVpfPlayerSlug(String vpfPlayerSlug);

    Optional<Player> findByNormalizedNameAndDateOfBirth(String normalizedName, LocalDate dateOfBirth);


//    @Query("""
//    SELECT new com.example.demo.dto.PlayerSearchResponse(
//        p.id,
//        p.name,
//        t.id,
//        t.name,
//        s.id,
//        CASE
//            WHEN LOWER(p.nationality) IN ('việt nam', 'viet nam', 'vietnam', 'vn') THEN 'DOMESTIC'
//            ELSE 'FOREIGN'
//        END,
//        p.nationality,
//        COALESCE(ps.goals, 0)
//    )
//    FROM PlayerSeason psn
//    JOIN psn.player p
//    JOIN psn.team t
//    JOIN psn.season s
//    LEFT JOIN PlayerStats ps ON ps.player.id = p.id AND ps.season.id = s.id
//    WHERE (:seasonId IS NULL OR s.id = :seasonId)
//      AND (:teamId IS NULL OR t.id = :teamId)
//      AND (:keyword IS NULL OR LOWER(p.name) LIKE CONCAT('%', :keyword, '%'))
//""")
//    List<PlayerSearchResponse> searchPlayers(
//            @Param("seasonId") Long seasonId,
//            @Param("teamId") Long teamId,
//            @Param("keyword") String keyword
//    );


    @Query("""
    SELECT new com.example.demo.dto.PlayerSearchResponse(
        p.id,
        p.name,
        t.id,
        t.name,
        :seasonId,
        CASE
            WHEN LOWER(p.nationality) IN ('việt nam', 'viet nam', 'vietnam', 'vn')
                THEN 'DOMESTIC'
            ELSE 'FOREIGN'
        END,
        p.nationality,
        COALESCE(SUM(ps.goals), 0L)
    )
    FROM Player p
    LEFT JOIN p.team t
    LEFT JOIN PlayerStats ps
        ON ps.player.id = p.id
       AND (:seasonId IS NULL OR ps.season.id = :seasonId)
    WHERE (:teamId IS NULL OR t.id = :teamId)
      AND (:keyword IS NULL OR LOWER(p.name) LIKE CONCAT('%', :keyword, '%'))
    GROUP BY p.id, p.name, t.id, t.name, p.nationality
""")
    List<PlayerSearchResponse> searchPlayersFromPlayer(
            @Param("seasonId") Long seasonId,
            @Param("teamId") Long teamId,
            @Param("keyword") String keyword
    );

}
