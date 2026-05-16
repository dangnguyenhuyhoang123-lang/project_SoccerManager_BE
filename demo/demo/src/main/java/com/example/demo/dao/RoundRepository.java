package com.example.demo.dao;

import com.example.demo.entity.Round;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoundRepository extends JpaRepository<Round, Integer> {
    Page<Round> findBySeasonId(Long seasonId, Pageable pageable);
}
