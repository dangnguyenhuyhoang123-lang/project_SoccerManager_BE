package com.example.demo.dao.team;

import com.example.demo.entity.Season;
import com.example.demo.entity.Team;
import com.example.demo.entity.TeamStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TeamStatsRepository extends JpaRepository<TeamStats,Long> {

    Optional<TeamStats> findBySeasonAndTeam(Season season, Team team);

    List<TeamStats> findBySeasonId(Long seasonId);

    boolean existsBySeasonIdAndTeamId(Long seasonId, Long teamId);
}
