package com.example.demo.dao.season;

import com.example.demo.entity.League;
import com.example.demo.entity.Season;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SeasonRepository extends JpaRepository<Season,Long> {
    List<Season> findByLeagueId(Long leagueId);


    Optional<Season> findByYearAndLeague(String year, League league);

    Optional<Season> findByYear(String year);

    boolean existsBySystemRuleId(Long systemRuleId);
    List<Season> findBySystemRuleId(Long systemRuleId);

}
