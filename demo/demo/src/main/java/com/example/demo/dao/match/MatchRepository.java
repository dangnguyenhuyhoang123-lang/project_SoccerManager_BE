package com.example.demo.dao.match;

import com.example.demo.entity.Match;
import com.example.demo.entity.MatchStatus;
import com.example.demo.entity.Season;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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


//    @Query("SELECT m FROM Match m " +
//            "JOIN FETCH m.homeTeam " +
//            "JOIN FETCH m.awayTeam " +
//            "JOIN FETCH m.stadium " +
//            "WHERE m.league.id = :leagueId " +
//            "AND (:seasonId IS NULL OR m.season.id = :seasonId) " +
//            "AND (:roundId IS NULL OR m.round.id = :roundId) " +
//            "ORDER BY m.matchDate ASC")
//    List<Match> findMatchesCustom(@Param("leagueId") Long leagueId,
//                                  @Param("seasonId") Long seasonId,
//                                  @Param("roundId") Long roundId);


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
}
