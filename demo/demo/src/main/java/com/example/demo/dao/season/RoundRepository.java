package com.example.demo.dao.season;

import com.example.demo.entity.season.Round;
import com.example.demo.entity.season.Season;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoundRepository extends JpaRepository<Round, Integer> {
    Page<Round> findBySeasonId(Long seasonId, Pageable pageable);

    Optional<Round> findBySeasonAndRoundNumber(Season season, Integer roundNumber);
    Optional<Round> findBySeasonIdAndRoundNumber(Long seasonId, Integer roundNumber);
}
