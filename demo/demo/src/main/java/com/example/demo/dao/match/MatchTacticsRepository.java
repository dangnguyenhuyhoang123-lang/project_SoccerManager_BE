package com.example.demo.dao.match;

import com.example.demo.entity.MatchTactics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MatchTacticsRepository extends JpaRepository<MatchTactics,Long> {
    // Xóa chiến thuật cũ của một đội trong một trận đấu cụ thể
    void deleteByMatchIdAndTeamId(Long matchId, Long teamId);

    // Lấy chiến thuật của một đội trong trận đấu
    Optional<MatchTactics> findByMatchIdAndTeamId(Long matchId, Long teamId);
}
