package com.example.demo.dao;

import com.example.demo.entity.Referee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(path = "referee")
public interface RefereeRepository extends JpaRepository<Referee,Long> {
}
