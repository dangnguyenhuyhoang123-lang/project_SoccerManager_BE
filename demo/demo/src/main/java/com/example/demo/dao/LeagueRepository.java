package com.example.demo.dao;

import com.example.demo.entity.League;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.Optional;


public interface LeagueRepository extends JpaRepository<League,Long> {
    Optional<League> findByNameIgnoreCase(String name);

    Optional<League> findBySportsDbLeagueId(String sportsDbLeagueId);

    Optional<League> findByVpfLeagueSlug(String vpfLeagueSlug);
}
