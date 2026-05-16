package com.example.demo.dao.team;

import com.example.demo.entity.TeamStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.stereotype.Repository;

@Repository
public interface TeamStatsRepository extends JpaRepository<TeamStats,Long> {
}
