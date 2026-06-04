package com.example.demo.dao;

import com.example.demo.entity.Referee;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RefereeRepository extends JpaRepository<Referee, Long> {
    List<Referee> findByStatusIgnoreCase(String status);
}
