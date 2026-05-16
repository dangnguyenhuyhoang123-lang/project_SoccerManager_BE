package com.example.demo.dao.season;

import com.example.demo.entity.SeasonTeamCoach;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SeasonTeamCoachRepo extends JpaRepository<SeasonTeamCoach,Long> {
}
