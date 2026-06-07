package com.example.demo.service.season;

import com.example.demo.controller.season.RoundController;
import com.example.demo.dao.season.RoundRepository;
import com.example.demo.dao.season.SeasonRepository;
import com.example.demo.dto.RealtimeEventDTO;
import com.example.demo.entity.season.Round;
import com.example.demo.entity.season.Season;
import com.example.demo.service.realtime.RealtimeEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class RoundService {

    private final RoundRepository roundRepository;
    private final SeasonRepository seasonRepository;
    private final RealtimeEventService realtimeEventService;


    public Page<RoundController.RoundResponse> getRounds(int page, int size, Long seasonId) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        Page<Round> rounds = seasonId == null
                ? roundRepository.findAll(pageable)
                : roundRepository.findBySeasonId(seasonId, pageable);
        return rounds.map(this::toRoundResponse);
    }

    public RoundController.RoundResponse getRound(Integer id) {
        return toRoundResponse(findRoundEntity(id));
    }

    public RoundController.RoundResponse create(RoundController.RoundRequest request) {
        Round round = new Round();
        applyRoundRequest(round, request);

        Round saved = roundRepository.save(round);

        RealtimeEventDTO event = realtimeEvent(
                "ROUND_CREATED",
                saved.getId().longValue(),
                "ROUND",
                "REFETCH_ROUNDS"
        );

        realtimeEventService.sendToPublicLeagues(event);
        realtimeEventService.sendToAdmins(event);

        return toRoundResponse(saved);
    }


    public RoundController.RoundResponse update(Integer id, RoundController.RoundRequest request) {
        Round round = findRoundEntity(id);
        applyRoundRequest(round, request);

        Round saved = roundRepository.save(round);

        RealtimeEventDTO event = realtimeEvent(
                "ROUND_UPDATED",
                saved.getId().longValue(),
                "ROUND",
                "REFETCH_ROUNDS"
        );

        realtimeEventService.sendToPublicLeagues(event);
        realtimeEventService.sendToAdmins(event);

        return toRoundResponse(saved);
    }


    public void delete(Integer id) {
        if (!roundRepository.existsById(id)) {
            throw new ResourceNotFoundException("Round not found with id = " + id);
        }

        roundRepository.deleteById(id);

        RealtimeEventDTO event = realtimeEvent(
                "ROUND_DELETED",
                id.longValue(),
                "ROUND",
                "REFETCH_ROUNDS"
        );

        realtimeEventService.sendToPublicLeagues(event);
        realtimeEventService.sendToAdmins(event);
    }


    private Round findRoundEntity(Integer id) {
        return roundRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Round not found with id = " + id));
    }

    private void applyRoundRequest(Round round, RoundController.RoundRequest request) {
        Season season = seasonRepository.findById(request.seasonId())
                .orElseThrow(() -> new ResourceNotFoundException("Season not found with id = " + request.seasonId()));

        round.setRoundNumber(request.roundNumber());
        round.setName(request.name());
        round.setStartDate(request.startDate());
        round.setEndDate(request.endDate());
        round.setMaxMatches(request.maxMatches());
        round.setStatus(request.status());
        round.setNotifyTeams(request.notifyTeams());
        round.setSeason(season);
    }

    private RoundController.RoundResponse toRoundResponse(Round round) {
        return new RoundController.RoundResponse(
                round.getId(),
                round.getRoundNumber(),
                round.getName(),
                round.getStartDate(),
                round.getEndDate(),
                round.getMaxMatches(),
                round.getStatus(),
                round.getNotifyTeams(),
                round.getSeason() != null ? round.getSeason().getId() : null,
                round.getSeason() != null ? round.getSeason().getName() : null
        );
    }

    private RealtimeEventDTO realtimeEvent(
            String type,
            Long referenceId,
            String referenceType,
            String action
    ) {
        return new RealtimeEventDTO(
                type,
                referenceId,
                referenceType,
                action,
                null,
                LocalDateTime.now()
        );
    }

}
