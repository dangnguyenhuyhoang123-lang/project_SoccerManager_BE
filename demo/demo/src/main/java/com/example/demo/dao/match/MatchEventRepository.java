package com.example.demo.dao.match;

import com.example.demo.entity.EventType;
import com.example.demo.entity.MatchEvent;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;



@Repository
public interface MatchEventRepository extends JpaRepository<MatchEvent,Long> {
    // Lấy tất cả sự kiện của 1 trận đấu sắp xếp theo thời gian để làm Timeline
    List<MatchEvent> findByMatchIdOrderByMinuteAsc(Long matchId);

    // Đếm số lượng sự kiện theo loại (Dùng nếu bạn muốn kiểm tra chéo với MatchStats)
    long countByMatchIdAndTeamIdAndEventType(Long matchId, Long teamId, EventType eventType);



    @EntityGraph(attributePaths = {
            "match",
            "team",
            "player",
            "playerIn",
            "assistPlayer"
    })
    List<MatchEvent> findByMatchIdOrderByMinuteAscExtraMinuteAscEventOrderAscIdAsc(Long matchId);

    void deleteByMatchId(Long matchId);

    @EntityGraph(attributePaths = {
            "match",
            "match.season",
            "player",
            "assistPlayer"
    })
    List<MatchEvent> findByMatchSeasonId(Long seasonId);

    @EntityGraph(attributePaths = {
            "match",
            "match.season",
            "player",
            "assistPlayer"
    })
    List<MatchEvent> findByMatchSeasonIdOrderByMatchIdAscMinuteAscIdAsc(Long seasonId);

    List<MatchEvent> findByMatchId(Long matchId);
}
