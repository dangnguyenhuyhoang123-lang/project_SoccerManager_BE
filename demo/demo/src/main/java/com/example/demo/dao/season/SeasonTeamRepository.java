package com.example.demo.dao.season;

import com.example.demo.entity.SeasonTeam;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;
import java.util.Optional;

@RepositoryRestResource(path = "team-season")
public interface SeasonTeamRepository extends JpaRepository<SeasonTeam, Long> {

    List<SeasonTeam> findBySeasonId(Long seasonId);

    Page<SeasonTeam> findBySeasonId(Long seasonId, Pageable pageable);

    Page<SeasonTeam> findByTeamId(Long teamId, Pageable pageable);

    Page<SeasonTeam> findBySeasonIdAndTeamId(Long seasonId, Long teamId, Pageable pageable);

    Optional<SeasonTeam> findOneBySeasonIdAndTeamId(Long seasonId, Long teamId);

    boolean existsBySeasonIdAndTeamId(Long seasonId, Long teamId);
}
