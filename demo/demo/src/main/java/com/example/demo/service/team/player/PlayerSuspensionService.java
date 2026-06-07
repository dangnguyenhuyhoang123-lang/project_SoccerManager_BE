package com.example.demo.service.team.player;

import com.example.demo.dao.match.MatchEventRepository;
import com.example.demo.dao.match.MatchRepository;
import com.example.demo.dao.team.player.PlayerSuspensionRepository;
import com.example.demo.dto.player.PlayerSuspensionResponse;
import com.example.demo.entity.match.EventType;
import com.example.demo.entity.match.Match;
import com.example.demo.entity.match.MatchEvent;
import com.example.demo.entity.match.MatchStatus;
import com.example.demo.entity.player.Player;
import com.example.demo.entity.player.PlayerSuspension;
import com.example.demo.entity.season.SeasonTeam;
import com.example.demo.entity.team.Team;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PlayerSuspensionService {

    private static final String REASON_RED_CARD = "RED_CARD";
    private static final String REASON_TWO_YELLOWS = "TWO_YELLOWS";

    private final MatchRepository matchRepository;
    private final MatchEventRepository matchEventRepository;
    private final PlayerSuspensionRepository playerSuspensionRepository;

    /**
     * Sinh án treo giò sau khi một trận kết thúc.
     *
     * Luật hiện tại:
     * - 1 thẻ đỏ trong trận => treo giò trận kế tiếp.
     * - Mỗi 2 thẻ vàng trong mùa => treo giò trận kế tiếp.
     *
     * Nên gọi hàm này khi Match chuyển sang FINISHED.
     */
    @Transactional
    public void generateSuspensionsAfterMatch(Long matchId) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy trận đấu id = " + matchId));

        if (match.getStatus() != MatchStatus.FINISHED) {
            return;
        }

        if (match.getSeason() == null) {
            throw new RuntimeException("Trận đấu chưa gắn mùa giải");
        }

        if (match.getMatchDate() == null) {
            throw new RuntimeException("Trận đấu chưa có ngày thi đấu");
        }

        List<MatchEvent> events = matchEventRepository.findByMatchId(matchId);

        Map<Long, MatchEvent> redCardEventsByPlayer = new LinkedHashMap<>();
        Map<Long, MatchEvent> yellowCardEventsByPlayer = new LinkedHashMap<>();

        for (MatchEvent event : events) {
            if (event.getPlayer() == null || event.getEventType() == null) {
                continue;
            }

            Long playerId = event.getPlayer().getId();

            if (event.getEventType() == EventType.RED_CARD) {
                redCardEventsByPlayer.putIfAbsent(playerId, event);
            }

            if (event.getEventType() == EventType.YELLOW_CARD) {
                yellowCardEventsByPlayer.putIfAbsent(playerId, event);
            }
        }

        // 1. Xử lý thẻ đỏ: 1 thẻ đỏ => treo trận kế tiếp.
        for (MatchEvent redCardEvent : redCardEventsByPlayer.values()) {
            createSuspensionForNextMatchIfPossible(
                    match,
                    redCardEvent.getPlayer(),
                    redCardEvent.getTeam(),
                    REASON_RED_CARD
            );
        }

        // 2. Xử lý 2 thẻ vàng trong mùa.
        for (MatchEvent yellowCardEvent : yellowCardEventsByPlayer.values()) {
            Player player = yellowCardEvent.getPlayer();

            // Nếu cầu thủ đã bị thẻ đỏ và đã tạo án cho trận kế tiếp rồi thì không tạo thêm án 2 thẻ vàng cho cùng trận đó.
            if (redCardEventsByPlayer.containsKey(player.getId())) {
                continue;
            }

            long totalYellowCardsUntilThisMatch =
                    matchEventRepository.countPlayerEventsInSeasonUntil(
                            match.getSeason().getId(),
                            player.getId(),
                            EventType.YELLOW_CARD,
                            match.getMatchDate()
                    );

            long alreadyCreatedTwoYellowSuspensions =
                    playerSuspensionRepository.countByPlayerIdAndSeasonIdAndReason(
                            player.getId(),
                            match.getSeason().getId(),
                            REASON_TWO_YELLOWS
                    );

            long expectedTwoYellowSuspensions = totalYellowCardsUntilThisMatch / 2;

            if (expectedTwoYellowSuspensions > alreadyCreatedTwoYellowSuspensions) {
                createSuspensionForNextMatchIfPossible(
                        match,
                        player,
                        yellowCardEvent.getTeam(),
                        REASON_TWO_YELLOWS
                );
            }
        }
    }

    private void createSuspensionForNextMatchIfPossible(
            Match sourceMatch,
            Player player,
            Team team,
            String reason
    ) {
        if (player == null || team == null) {
            return;
        }

        if (playerSuspensionRepository.existsByPlayerIdAndSourceMatchIdAndReason(
                player.getId(),
                sourceMatch.getId(),
                reason
        )) {
            return;
        }

        SeasonTeam seasonTeam = resolveSeasonTeamInMatch(sourceMatch, team)
                .orElse(null);

        if (seasonTeam == null) {
            return;
        }

        Match nextMatch = findNextMatchOfSeasonTeam(sourceMatch, seasonTeam)
                .orElse(null);

        // Nếu đã hết mùa, không còn trận kế tiếp thì không tạo án treo giò.
        if (nextMatch == null) {
            return;
        }

        // Tránh tạo nhiều án active cho cùng một cầu thủ trong cùng trận bị treo.
        boolean alreadySuspendedInNextMatch =
                playerSuspensionRepository.existsByPlayerIdAndSuspendedMatchIdAndServedFalse(
                        player.getId(),
                        nextMatch.getId()
                );

        if (alreadySuspendedInNextMatch) {
            return;
        }

        PlayerSuspension suspension = new PlayerSuspension();
        suspension.setPlayer(player);
        suspension.setSeason(sourceMatch.getSeason());
        suspension.setSourceMatch(sourceMatch);
        suspension.setSuspendedMatch(nextMatch);
        suspension.setReason(reason);
        suspension.setServed(false);

        playerSuspensionRepository.save(suspension);
    }

    private Optional<SeasonTeam> resolveSeasonTeamInMatch(Match match, Team team) {
        if (match == null || team == null || team.getId() == null) {
            return Optional.empty();
        }

        if (match.getHomeTeam() != null
                && match.getHomeTeam().getTeam() != null
                && team.getId().equals(match.getHomeTeam().getTeam().getId())) {
            return Optional.of(match.getHomeTeam());
        }

        if (match.getAwayTeam() != null
                && match.getAwayTeam().getTeam() != null
                && team.getId().equals(match.getAwayTeam().getTeam().getId())) {
            return Optional.of(match.getAwayTeam());
        }

        return Optional.empty();
    }

    private Optional<Match> findNextMatchOfSeasonTeam(Match sourceMatch, SeasonTeam seasonTeam) {
        List<Match> nextMatches = matchRepository.findNextMatchesOfSeasonTeamAfter(
                sourceMatch.getSeason().getId(),
                seasonTeam.getId(),
                sourceMatch.getMatchDate(),
                PageRequest.of(0, 1)
        );

        if (nextMatches.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(nextMatches.get(0));
    }

    @Transactional
    public void markSuspensionsServedAfterMatch(Long matchId) {
        List<PlayerSuspension> suspensions = playerSuspensionRepository
                .findBySuspendedMatchIdAndServedFalse(matchId);

        for (PlayerSuspension suspension : suspensions) {
            suspension.setServed(true);
        }

        playerSuspensionRepository.saveAll(suspensions);
    }

    public List<PlayerSuspensionResponse> getBySeason(Long seasonId) {
        if (seasonId == null || seasonId <= 0) {
            throw new RuntimeException("seasonId không hợp lệ");
        }

        return playerSuspensionRepository.findBySeasonId(seasonId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<PlayerSuspensionResponse> getActiveByMatch(Long matchId) {
        if (matchId == null || matchId <= 0) {
            throw new RuntimeException("matchId không hợp lệ");
        }

        return playerSuspensionRepository.findBySuspendedMatchIdAndServedFalse(matchId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private PlayerSuspensionResponse toResponse(PlayerSuspension suspension) {
        return new PlayerSuspensionResponse(
                suspension.getId(),
                suspension.getPlayer() != null ? suspension.getPlayer().getId() : null,
                suspension.getPlayer() != null ? suspension.getPlayer().getName() : null,
                suspension.getSeason() != null ? suspension.getSeason().getId() : null,
                suspension.getSourceMatch() != null ? suspension.getSourceMatch().getId() : null,
                suspension.getSuspendedMatch() != null ? suspension.getSuspendedMatch().getId() : null,
                suspension.getReason(),
                suspension.getServed()
        );
    }
}
