package com.example.demo.service;

import com.example.demo.dao.match.MatchEventRepository;
import com.example.demo.dao.player.PlayerSeasonRepository;
import com.example.demo.dao.player.PlayerStatsRepository;
import com.example.demo.dao.season.SeasonRepository;
import com.example.demo.entity.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PlayerStatsService {

    private final PlayerStatsRepository playerStatsRepository;
    private final PlayerSeasonRepository playerSeasonRepository;
    private final SeasonRepository seasonRepository;
    private final MatchEventRepository matchEventRepository;

    @Transactional
    public List<PlayerStatsResponse> recalculateBySeason(Long seasonId) {
        Season season = seasonRepository.findById(seasonId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy mùa giải id = " + seasonId));

        // 1. Khởi tạo/reset PlayerStats cho các cầu thủ thuộc mùa
        List<PlayerSeason> playerSeasons = playerSeasonRepository.findBySeasonId(seasonId);

        for (PlayerSeason playerSeason : playerSeasons) {
            Player player = playerSeason.getPlayer();

            PlayerStats stats = playerStatsRepository.findByPlayerAndSeason(player, season)
                    .orElseGet(() -> {
                        PlayerStats newStats = new PlayerStats();
                        newStats.setPlayer(player);
                        newStats.setSeason(season);
                        return newStats;
                    });

            resetStats(stats);
            playerStatsRepository.save(stats);
        }

        // 2. Lấy toàn bộ sự kiện của mùa
        List<MatchEvent> events = matchEventRepository.findByMatchSeasonId(seasonId);

        // 3. Tính goals, assists, cards
        for (MatchEvent event : events) {
            applyEventToPlayerStats(event, season);
        }

        return getBySeason(seasonId);
    }

    public List<PlayerStatsResponse> getBySeason(Long seasonId) {
        return playerStatsRepository.findBySeasonId(seasonId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private void resetStats(PlayerStats stats) {
        stats.setGoals(0);
        stats.setAssists(0);
        stats.setAppearances(0);
        stats.setMinutesPlayed(0);
        stats.setYellowCards(0);
        stats.setRedCards(0);
    }

    private void applyEventToPlayerStats(MatchEvent event, Season season) {
        EventType eventType = event.getEventType();

        if (eventType == null) {
            return;
        }

        if (eventType == EventType.GOAL && event.getPlayer() != null) {
            PlayerStats scorerStats = getOrCreateStats(event.getPlayer(), season);
            scorerStats.setGoals(scorerStats.getGoals() + 1);
            playerStatsRepository.save(scorerStats);
        }

        if (eventType == EventType.GOAL && event.getAssistPlayer() != null) {
            PlayerStats assistStats = getOrCreateStats(event.getAssistPlayer(), season);
            assistStats.setAssists(assistStats.getAssists() + 1);
            playerStatsRepository.save(assistStats);
        }

        if (eventType == EventType.YELLOW_CARD && event.getPlayer() != null) {
            PlayerStats cardStats = getOrCreateStats(event.getPlayer(), season);
            cardStats.setYellowCards(cardStats.getYellowCards() + 1);
            playerStatsRepository.save(cardStats);
        }

        if (eventType == EventType.RED_CARD && event.getPlayer() != null) {
            PlayerStats cardStats = getOrCreateStats(event.getPlayer(), season);
            cardStats.setRedCards(cardStats.getRedCards() + 1);
            playerStatsRepository.save(cardStats);
        }
    }

    private PlayerStats getOrCreateStats(Player player, Season season) {
        return playerStatsRepository.findByPlayerAndSeason(player, season)
                .orElseGet(() -> {
                    PlayerStats stats = new PlayerStats();
                    stats.setPlayer(player);
                    stats.setSeason(season);
                    resetStats(stats);
                    return playerStatsRepository.save(stats);
                });
    }

    @Transactional
    public void applyEvent(MatchEvent event, int sign) {
        if (event == null || event.getMatch() == null || event.getMatch().getSeason() == null) {
            return;
        }

        Season season = event.getMatch().getSeason();

        if (event.getEventType() == EventType.GOAL && event.getPlayer() != null) {
            PlayerStats stats = getOrCreateStats(event.getPlayer(), season);
            stats.setGoals(safe(stats.getGoals()) + sign);
            playerStatsRepository.save(stats);
        }

        if (event.getEventType() == EventType.GOAL && event.getAssistPlayer() != null) {
            PlayerStats stats = getOrCreateStats(event.getAssistPlayer(), season);
            stats.setAssists(safe(stats.getAssists()) + sign);
            playerStatsRepository.save(stats);
        }

        if (event.getEventType() == EventType.YELLOW_CARD && event.getPlayer() != null) {
            PlayerStats stats = getOrCreateStats(event.getPlayer(), season);
            stats.setYellowCards(safe(stats.getYellowCards()) + sign);
            playerStatsRepository.save(stats);
        }

        if (event.getEventType() == EventType.RED_CARD && event.getPlayer() != null) {
            PlayerStats stats = getOrCreateStats(event.getPlayer(), season);
            stats.setRedCards(safe(stats.getRedCards()) + sign);
            playerStatsRepository.save(stats);
        }
    }

    private int safe(Integer value) {
        return value == null ? 0 : value;
    }

    private PlayerStatsResponse toResponse(PlayerStats stats) {
        Player player = stats.getPlayer();

        return new PlayerStatsResponse(
                stats.getId(),
                stats.getSeason() != null ? stats.getSeason().getId() : null,
                stats.getSeason() != null ? stats.getSeason().getName() : null,
                player != null ? player.getId() : null,
                player != null ? player.getName() : null,
                stats.getGoals(),
                stats.getAssists(),
                stats.getAppearances(),
                stats.getMinutesPlayed(),
                stats.getYellowCards(),
                stats.getRedCards()
        );
    }

    public record PlayerStatsResponse(
            Long id,
            Long seasonId,
            String seasonName,
            Long playerId,
            String playerName,
            Integer goals,
            Integer assists,
            Integer appearances,
            Integer minutesPlayed,
            Integer yellowCards,
            Integer redCards
    ) {
    }
}
