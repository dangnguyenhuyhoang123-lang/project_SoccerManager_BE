package com.example.demo.dao;

import com.example.demo.entity.Coach;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.Optional;

@RepositoryRestResource(path = "coach")
public interface CoachRepository extends JpaRepository<Coach,Long> {
    boolean existsByIDCode(String IDCode);

    Optional<Coach> findByIDCode(String IDCode);

    Page<Coach> findByNameContainingIgnoreCase(String name, Pageable pageable);

    Page<Coach> findByStatus(String status, Pageable pageable);

    Page<Coach> findByNameContainingIgnoreCaseAndStatus(String name, String status, Pageable pageable);



    Page<Coach> findByTeamId(Long teamId, Pageable pageable);
}
