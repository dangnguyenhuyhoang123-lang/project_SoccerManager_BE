package com.example.demo.dao.match;

import com.example.demo.entity.MatchReferee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MatchRefereeRepository extends JpaRepository<MatchReferee,Long> {
}
