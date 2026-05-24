package com.example.demo.dao.season;

import com.example.demo.entity.SeasonTeamCoach;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SeasonTeamCoachRepository extends JpaRepository<SeasonTeamCoach,Long> {
    Page<SeasonTeamCoach> findBySeasonId(Long seasonId, Pageable pageable);

    Page<SeasonTeamCoach> findByTeamId(Long teamId, Pageable pageable);

    Page<SeasonTeamCoach> findByCoachId(Long coachId, Pageable pageable);

    Page<SeasonTeamCoach> findBySeasonIdAndTeamId(Long seasonId, Long teamId, Pageable pageable);
}
