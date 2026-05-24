package com.example.demo.dao.player;

import com.example.demo.entity.PlayerSeason;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlayerSeasonRepository extends JpaRepository<PlayerSeason,Long> {
    Page<PlayerSeason> findBySeasonId(Long seasonId, Pageable pageable);

    Page<PlayerSeason> findByTeamId(Long teamId, Pageable pageable);

    Page<PlayerSeason> findByPlayerId(Long playerId, Pageable pageable);

    Page<PlayerSeason> findBySeasonIdAndTeamId(Long seasonId, Long teamId, Pageable pageable);

    List<PlayerSeason> findByTeamIdOrderByShirtNumberAsc(Long teamId);

    List<PlayerSeason> findByTeamSeasonIdOrderByShirtNumberAsc(Long teamSeasonId);
}
