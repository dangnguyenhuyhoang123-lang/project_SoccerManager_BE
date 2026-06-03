package com.example.demo.dao.player;

import com.example.demo.entity.player.PlayerSuspension;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlayerSuspensionRepository extends JpaRepository<PlayerSuspension, Long> {

    boolean existsByPlayerIdAndSuspendedMatchIdAndServedFalse(Long playerId, Long matchId);

    boolean existsByPlayerIdAndSourceMatchIdAndReason(Long playerId, Long sourceMatchId, String reason);

    boolean existsByPlayerIdAndSuspendedMatchIdAndReasonAndServedFalse(
            Long playerId,
            Long suspendedMatchId,
            String reason
    );

    long countByPlayerIdAndSeasonIdAndReason(Long playerId, Long seasonId, String reason);

    List<PlayerSuspension> findBySeasonId(Long seasonId);

    List<PlayerSuspension> findBySuspendedMatchIdAndServedFalse(Long suspendedMatchId);
}
