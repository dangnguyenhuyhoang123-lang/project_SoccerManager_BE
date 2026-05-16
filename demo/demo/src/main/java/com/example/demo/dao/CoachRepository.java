package com.example.demo.dao;

import com.example.demo.entity.Coach;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.Optional;

@RepositoryRestResource(path = "coach")
public interface CoachRepository extends JpaRepository<Coach,Long> {
    boolean existsByIDCode(String IDCode);

    Optional<Coach> findByIDCode(String IDCode);
}
