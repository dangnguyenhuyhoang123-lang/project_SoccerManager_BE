package com.example.demo.dao.player;

import com.example.demo.entity.Player;
import com.example.demo.entity.PlayerStats;
import com.example.demo.entity.Season;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlayerStatsRepository extends JpaRepository<PlayerStats,Long> {

    // Tìm vua phá lưới của mùa giải
    List<PlayerStats> findBySeasonIdOrderByGoalsDesc(Long seasonId);

    // Tìm "chuyên gia kiến tạo" của mùa giải
    List<PlayerStats> findBySeasonIdOrderByAssistsDesc(Long seasonId);

    // Tìm thông số của 1 cầu thủ trong 1 mùa nhất định để cập nhật real-time
    Optional<PlayerStats> findBySeasonIdAndPlayerId(Long seasonId, Long playerId);

    Optional<PlayerStats> findByPlayerAndSeason(Player player, Season season);

    List<PlayerStats> findBySeasonId(Long seasonId);

    boolean existsByPlayerIdAndSeasonId(Long playerId, Long seasonId);


}

