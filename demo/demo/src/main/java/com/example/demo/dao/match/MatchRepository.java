package com.example.demo.dao.match;

import com.example.demo.entity.match.Match;
import com.example.demo.entity.match.MatchStatus;
import com.example.demo.entity.season.Season;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;


public interface MatchRepository extends JpaRepository<Match,Long> {

    Page<Match> findBySeason_Year(String year, Pageable pageable);



    @EntityGraph(attributePaths = {"homeTeam", "homeTeam.team", "homeTeam.team.stadium", "awayTeam", "awayTeam.team", "awayTeam.team.stadium", "season", "season.league", "stadium", "round"})
    Page<Match> findAll(Pageable pageable);


    @EntityGraph(attributePaths = {"homeTeam", "homeTeam.team", "homeTeam.team.stadium", "awayTeam", "awayTeam.team", "awayTeam.team.stadium", "season", "season.league", "stadium", "round"})
    @Query("""
    SELECT m FROM Match m
    WHERE 
        (:status IS NULL OR m.status = :status)
    AND
        (:search IS NULL OR :search = '' OR
            LOWER(m.homeTeam.team.name) LIKE LOWER(CONCAT('%', :search, '%')) OR
            LOWER(m.awayTeam.team.name) LIKE LOWER(CONCAT('%', :search, '%'))
        )
""")
    Page<Match> filterMatches(
            @Param("status") MatchStatus status,
            @Param("search") String search,
            Pageable pageable
    );



    // Lấy các trận đấu của một vòng đấu (Round)
    List<Match> findByRoundId(Long roundId);

    // Lấy các trận đấu sắp tới (chưa đá)
    @Query("SELECT m FROM Match m WHERE m.status = 'SCHEDULED' ORDER BY m.matchDate ASC")
    List<Match> findUpcomingMatches();

    Optional<Match> findBySeasonAndVpfMatchCode(Season season, Integer vpfMatchCode);

    Optional<Match> findBySportsDbEventId(String sportsDbEventId);

    List<Match> findBySeasonOrderByMatchDateAsc(Season season);

    @Query("""
        SELECT m
        FROM Match m
        JOIN FETCH m.season s
        JOIN FETCH m.round r
        JOIN FETCH m.homeTeam ht
        JOIN FETCH ht.team hTeam
        JOIN FETCH m.awayTeam at
        JOIN FETCH at.team aTeam
        LEFT JOIN FETCH m.stadium st
        WHERE s.year = :seasonYear
        ORDER BY r.roundNumber ASC, m.matchDate ASC
        """)
    List<Match> findVLeagueMatchesBySeasonYear(@Param("seasonYear") String seasonYear);




    @EntityGraph(attributePaths = {
            "season",
            "homeTeam",
            "homeTeam.team",
            "awayTeam",
            "awayTeam.team",
            "stadium"
    })
    List<Match> findBySeasonIdAndStatus(Long seasonId, MatchStatus status);

    @Query("""
    SELECT m
    FROM Match m
    JOIN FETCH m.season s
    JOIN FETCH m.homeTeam ht
    JOIN FETCH ht.team hTeam
    JOIN FETCH m.awayTeam at
    JOIN FETCH at.team aTeam
    WHERE m.id = :matchId
""")
    Optional<Match> findMatchWithSeasonTeams(@Param("matchId") Long matchId);


    @Query("""
    SELECT COUNT(m) > 0
    FROM Match m
    WHERE m.round.id = :roundId
      AND (:currentMatchId IS NULL OR m.id <> :currentMatchId)
      AND (
            m.homeTeam.id = :seasonTeamId
         OR m.awayTeam.id = :seasonTeamId
      )
""")
    boolean existsTeamInRound(
            @Param("roundId") Integer roundId,
            @Param("seasonTeamId") Long seasonTeamId,
            @Param("currentMatchId") Long currentMatchId
    );

    @Query("""
    SELECT COUNT(m)
    FROM Match m
    WHERE m.season.id = :seasonId
      AND (:currentMatchId IS NULL OR m.id <> :currentMatchId)
      AND (
            (m.homeTeam.id = :teamAId AND m.awayTeam.id = :teamBId)
         OR (m.homeTeam.id = :teamBId AND m.awayTeam.id = :teamAId)
      )
""")
    long countMatchesBetweenTwoSeasonTeams(
            @Param("seasonId") Long seasonId,
            @Param("teamAId") Long teamAId,
            @Param("teamBId") Long teamBId,
            @Param("currentMatchId") Long currentMatchId
    );

    @Query("""
    SELECT COUNT(m) > 0
    FROM Match m
    WHERE m.season.id = :seasonId
      AND (:currentMatchId IS NULL OR m.id <> :currentMatchId)
      AND m.homeTeam.id = :homeTeamId
      AND m.awayTeam.id = :awayTeamId
""")
    boolean existsSameHomeAwayPair(
            @Param("seasonId") Long seasonId,
            @Param("homeTeamId") Long homeTeamId,
            @Param("awayTeamId") Long awayTeamId,
            @Param("currentMatchId") Long currentMatchId
    );

    @Query("""
    SELECT COUNT(m)
    FROM Match m
    WHERE m.round.id = :roundId
      AND (:currentMatchId IS NULL OR m.id <> :currentMatchId)
""")
    long countByRoundIdExcludingCurrent(
            @Param("roundId") Integer roundId,
            @Param("currentMatchId") Long currentMatchId
    );

    @Query("""
    SELECT m
    FROM Match m
    WHERE m.season.id = :seasonId
      AND m.matchDate > :currentMatchDate
      AND m.status = com.example.demo.entity.match.MatchStatus.SCHEDULED
      AND (
            m.homeTeam.id = :seasonTeamId
         OR m.awayTeam.id = :seasonTeamId
      )
    ORDER BY m.matchDate ASC
""")
    List<Match> findNextMatchesOfSeasonTeamAfter(
            @Param("seasonId") Long seasonId,
            @Param("seasonTeamId") Long seasonTeamId,
            @Param("currentMatchDate") LocalDateTime currentMatchDate,
            Pageable pageable
    );


    @Query(
            value = """
SELECT m
FROM Match m
WHERE (:status IS NULL OR m.status = :status)
  AND (:seasonId IS NULL OR m.season.id = :seasonId)
  AND (:roundId IS NULL OR m.round.id = :roundId)
  AND (
        :teamId IS NULL
     OR m.homeTeam.team.id = :teamId
     OR m.awayTeam.team.id = :teamId
  )
  AND (
        :search IS NULL
     OR LOWER(m.homeTeam.team.name) LIKE CONCAT('%', :search, '%')
     OR LOWER(m.awayTeam.team.name) LIKE CONCAT('%', :search, '%')
     OR LOWER(m.stadium.name) LIKE CONCAT('%', :search, '%')
  )
ORDER BY
  CASE WHEN m.matchDate >= CURRENT_TIMESTAMP THEN 0 ELSE 1 END ASC,
  CASE WHEN m.matchDate >= CURRENT_TIMESTAMP THEN m.matchDate END ASC,
  CASE WHEN m.matchDate < CURRENT_TIMESTAMP THEN m.matchDate END DESC,
  m.id DESC
""",
            countQuery = """
SELECT COUNT(m)
FROM Match m
WHERE (:status IS NULL OR m.status = :status)
  AND (:seasonId IS NULL OR m.season.id = :seasonId)
  AND (:roundId IS NULL OR m.round.id = :roundId)
  AND (
        :teamId IS NULL
     OR m.homeTeam.team.id = :teamId
     OR m.awayTeam.team.id = :teamId
  )
  AND (
        :search IS NULL
     OR LOWER(m.homeTeam.team.name) LIKE CONCAT('%', :search, '%')
     OR LOWER(m.awayTeam.team.name) LIKE CONCAT('%', :search, '%')
     OR LOWER(m.stadium.name) LIKE CONCAT('%', :search, '%')
  )
"""
    )
    Page<Match> searchMatches(
            @Param("status") MatchStatus status,
            @Param("search") String search,
            @Param("seasonId") Long seasonId,
            @Param("roundId") Integer roundId,
            @Param("teamId") Long teamId,
            Pageable pageable
    );

    boolean existsBySeasonId(Long seasonId);

    @Query("""
    SELECT m.manOfTheMatch.id,
           m.manOfTheMatch.name,
           COUNT(m)
    FROM Match m
    WHERE m.season.id = :seasonId
      AND m.manOfTheMatch IS NOT NULL
    GROUP BY m.manOfTheMatch.id, m.manOfTheMatch.name
    ORDER BY COUNT(m) DESC
""")
    List<Object[]> countManOfTheMatchBySeason(@Param("seasonId") Long seasonId);


    @Query("""
    SELECT m
    FROM Match m
    WHERE m.season.id = :seasonId
      AND m.status = com.example.demo.entity.match.MatchStatus.FINISHED
      AND (
            (m.homeTeam.team.id = :teamAId AND m.awayTeam.team.id = :teamBId)
         OR (m.homeTeam.team.id = :teamBId AND m.awayTeam.team.id = :teamAId)
      )
""")
    List<Match> findHeadToHeadMatches(
            @Param("seasonId") Long seasonId,
            @Param("teamAId") Long teamAId,
            @Param("teamBId") Long teamBId
    );
}
