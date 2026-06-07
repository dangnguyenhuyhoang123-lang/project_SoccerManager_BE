package com.example.demo.service.season;

import com.example.demo.controller.LeagueController;
import com.example.demo.dao.LeagueRepository;
import com.example.demo.dao.season.SeasonRepository;
import com.example.demo.dto.RealtimeEventDTO;
import com.example.demo.entity.League;
import com.example.demo.entity.season.Season;
import com.example.demo.service.realtime.RealtimeEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LeagueService {

    private final LeagueRepository leagueRepository;
    private final SeasonRepository seasonRepository;
    private final RealtimeEventService realtimeEventService;



    public Page<LeagueController.LeagueResponse> getLeagues(int page, int size, String search) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        if (search == null || search.isBlank()) {
            return leagueRepository.findAll(pageable).map(this::toLeagueResponse);
        }

        List<LeagueController.LeagueResponse> filtered = leagueRepository.findAll().stream()
                .filter(league -> matchesLeagueSearch(league, search))
                .map(this::toLeagueResponse)
                .toList();
        return paginate(pageable, filtered);
    }

    public LeagueController.LeagueResponse getLeague(Long id) {
        return toLeagueResponse(findLeagueEntity(id));
    }

    public List<LeagueController.SeasonResponse> getLeagueSeasons(Long leagueId) {
        if (!leagueRepository.existsById(leagueId)) {
            throw new ResourceNotFoundException("League not found with id = " + leagueId);
        }
        return seasonRepository.findByLeagueId(leagueId).stream()
                .map(this::toSeasonResponse)
                .toList();
    }

    public LeagueController.LeagueResponse create(LeagueController.LeagueRequest request) {
        League league = new League();
        applyLeagueRequest(league, request);

        League saved = leagueRepository.save(league);

        RealtimeEventDTO event = realtimeEvent(
                "LEAGUE_CREATED",
                saved.getId(),
                "LEAGUE",
                "REFETCH_LEAGUES"
        );

        realtimeEventService.sendToPublicLeagues(event);
        realtimeEventService.sendToAdmins(event);

        return toLeagueResponse(saved);
    }


    public LeagueController.LeagueResponse update(Long id, LeagueController.LeagueRequest request) {
        League league = findLeagueEntity(id);
        applyLeagueRequest(league, request);

        League saved = leagueRepository.save(league);

        RealtimeEventDTO event = realtimeEvent(
                "LEAGUE_UPDATED",
                saved.getId(),
                "LEAGUE",
                "REFETCH_LEAGUES"
        );

        realtimeEventService.sendToPublicLeagues(event);
        realtimeEventService.sendToAdmins(event);

        return toLeagueResponse(saved);
    }


    public void delete(Long id) {
        if (!leagueRepository.existsById(id)) {
            throw new ResourceNotFoundException("League not found with id = " + id);
        }

        leagueRepository.deleteById(id);

        RealtimeEventDTO event = realtimeEvent(
                "LEAGUE_DELETED",
                id,
                "LEAGUE",
                "REFETCH_LEAGUES"
        );

        realtimeEventService.sendToPublicLeagues(event);
        realtimeEventService.sendToAdmins(event);
    }


    private League findLeagueEntity(Long id) {
        return leagueRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("League not found with id = " + id));
    }

    private void applyLeagueRequest(League league, LeagueController.LeagueRequest request) {
        league.setName(request.name());
        league.setCountry(request.country());
        league.setScale(request.scale());
        league.setStatus(request.status());
        league.setLogo(request.logo());
    }

    private boolean matchesLeagueSearch(League league, String search) {
        String keyword = search.trim().toLowerCase();
        return containsIgnoreCase(league.getName(), keyword)
                || containsIgnoreCase(league.getCountry(), keyword)
                || containsIgnoreCase(league.getScale(), keyword);
    }

    private boolean containsIgnoreCase(String value, String keyword) {
        return value != null && value.toLowerCase().contains(keyword);
    }

    private LeagueController.LeagueResponse toLeagueResponse(League league) {
        return new LeagueController.LeagueResponse(
                league.getId(),
                league.getName(),
                league.getCountry(),
                league.getScale(),
                league.getStatus(),
                league.getLogo()
        );
    }

    private LeagueController.SeasonResponse toSeasonResponse(Season season) {
        return new LeagueController.SeasonResponse(
                season.getId(),
                season.getYear(),
                season.getName(),
                season.getStartDate(),
                season.getEndDate(),
                season.getLeague() != null ? season.getLeague().getId() : null,
                season.getLeague() != null ? season.getLeague().getName() : null,
                season.getSystemRule() != null ? season.getSystemRule().getId() : null
        );
    }

    private <T> Page<T> paginate(Pageable pageable, List<T> items) {
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), items.size());
        List<T> content = start >= items.size() ? List.of() : items.subList(start, end);
        return new PageImpl<>(content, pageable, items.size());
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
                java.time.LocalDateTime.now()
        );
    }

}
