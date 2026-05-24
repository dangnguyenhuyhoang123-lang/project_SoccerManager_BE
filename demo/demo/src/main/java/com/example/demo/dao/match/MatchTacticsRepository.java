package com.example.demo.dao.match;

import com.example.demo.entity.MatchTactics;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MatchTacticsRepository extends JpaRepository<MatchTactics,Long> {
    // Xóa chiến thuật cũ của một đội trong một trận đấu cụ thể
    void deleteByMatch_IdAndTeam_Id(Long matchId, Long teamId);

    // Lấy chiến thuật của một đội trong trận đấu
    @EntityGraph(attributePaths = {"match", "team"})
    Optional<MatchTactics> findByMatch_IdAndTeam_Id(Long matchId, Long teamId);

    @EntityGraph(attributePaths = {"match", "team"})
    List<MatchTactics> findByMatch_Id(Long matchId);

    @EntityGraph(attributePaths = {"match", "team"})
    Optional<MatchTactics> findOneById(Long id);
}
