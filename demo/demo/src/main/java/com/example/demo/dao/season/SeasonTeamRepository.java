package com.example.demo.dao.season;

import com.example.demo.entity.SeasonTeam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;

@RepositoryRestResource(path = "team-season")
public interface SeasonTeamRepository extends JpaRepository<SeasonTeam, Long> {

    List<SeasonTeam> findBySeasonId(Long seasonId);

    boolean existsBySeasonIdAndTeamId(Long seasonId, Long teamId);
}
