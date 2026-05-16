package com.example.demo.dao.team;

import com.example.demo.entity.Team;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TeamRepository extends JpaRepository<Team, Long> {
    Optional<Team> findByNameIgnoreCase(String name);

    Optional<Team> findByNameIgnoreCaseAndCityIgnoreCase(String name, String city);

    Page<Team> findByNameContainingIgnoreCase(String name, Pageable pageable);

    Page<Team> findByCityContainingIgnoreCase(String city, Pageable pageable);

    Page<Team> findByNameContainingIgnoreCaseAndCityContainingIgnoreCase(String name, String city, Pageable pageable);
}
