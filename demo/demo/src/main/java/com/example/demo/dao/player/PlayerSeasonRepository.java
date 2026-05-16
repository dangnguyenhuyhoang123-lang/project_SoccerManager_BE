package com.example.demo.dao.player;

import com.example.demo.entity.PlayerSeason;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.stereotype.Repository;

@Repository
public interface PlayerSeasonRepository extends JpaRepository<PlayerSeason,Long> {
}
