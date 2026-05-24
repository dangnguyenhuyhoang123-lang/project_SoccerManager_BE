package com.example.demo.dao;

import com.example.demo.entity.Achievement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface AchievementRepository extends JpaRepository<Achievement,Long> {
}
