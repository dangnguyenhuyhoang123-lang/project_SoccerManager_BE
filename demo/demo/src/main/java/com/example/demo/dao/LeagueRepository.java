package com.example.demo.dao;

import com.example.demo.entity.League;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;


public interface LeagueRepository extends JpaRepository<League,Long> {
}
