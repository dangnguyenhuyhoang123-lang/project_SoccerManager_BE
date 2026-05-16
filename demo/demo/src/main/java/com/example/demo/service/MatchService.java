package com.example.demo.service;

import com.example.demo.dao.match.MatchLineupRepository;
import com.example.demo.dao.match.MatchEventRepository;
import com.example.demo.dao.match.MatchRepository;
import com.example.demo.dao.match.MatchStatsRepository;
import com.example.demo.dto.*;
import com.example.demo.entity.*;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class MatchService {

    @Autowired
    private MatchRepository matchRepository;

    @Autowired
    private MatchStatsRepository matchStatsRepository;

    @Autowired
    private MatchEventRepository matchEventRepository;

    @Autowired
    private MatchLineupRepository matchLineupRepository;

//    public List<MatchDTO> getAllMatches() {
//        return matchRepository.findAll()
//                .stream()
//                .map(this::toDTO)
//                .toList();
//    }
//
//    public Page<MatchDTO> getMatches(Pageable pageable) {
//        return matchRepository.findAll(pageable)
//                .map(this::toDTO);
//    }
//
//    public MatchDTO getMatchById(Long id) {
//        Match match = matchRepository.findById(id)
//                .orElseThrow(() -> new RuntimeException("Match not found"));
//
//        return toDTO(match);
//    }
//
//    public Page<MatchDTO> getMatchesByLeague_Name(String leagueName,Pageable pageable)
//    {
//        return matchRepository.findByLeague_Name(leagueName,pageable)
//                .map(
//                        match ->
//                            new MatchDTO(
//                                    match.getId(),
//                                    match.getStatus().name(),
//                                    match.getHomeScore(),
//                                    match.getAwayScore(),
//                                    match.getMatchDate(),
//
//                                    new TeamDTO(
//                                            match.getHomeTeam().getId(),
//                                            match.getHomeTeam().getName(),
//                                            match.getHomeTeam().getLogo(),
//                                            match.getHomeTeam().getStadium()
//
//
//                                    ),
//
//                                    new TeamDTO(
//                                            match.getAwayTeam().getId(),
//                                            match.getAwayTeam().getName(),
//                                            match.getAwayTeam().getLogo(),
//                                            match.getAwayTeam().getStadium()
//
//                                    ),
//
//                                    new LeagueDTO(
//                                            match.getLeague().getId(),
//                                            match.getLeague().getName()
//                                    ),
//
//                                    new SeasonDTO(
//                                            match.getSeason().getId(),
//                                            match.getSeason().getYear()
//                                    )
//                            )
//                );
//    }
//
//
//    public Page<MatchDTO> getMatchesBySeason_Year(String season,Pageable pageable)
//    {
//        return matchRepository.findBySeason_Year(season,pageable)
//                .map(
//                        match ->
//                                new MatchDTO(
//                                        match.getId(),
//                                        match.getStatus().name(),
//                                        match.getHomeScore(),
//                                        match.getAwayScore(),
//                                        match.getMatchDate(),
//
//                                        new TeamDTO(
//                                                match.getHomeTeam().getId(),
//                                                match.getHomeTeam().getName(),
//                                                match.getHomeTeam().getLogo(),
//                                                match.getHomeTeam().getStadium()
//
//
//                                        ),
//
//                                        new TeamDTO(
//                                                match.getAwayTeam().getId(),
//                                                match.getAwayTeam().getName(),
//                                                match.getAwayTeam().getLogo(),
//                                                match.getAwayTeam().getStadium()
//
//                                        ),
//
//                                        new LeagueDTO(
//                                                match.getLeague().getId(),
//                                                match.getLeague().getName()
//                                        ),
//
//                                        new SeasonDTO(
//                                                match.getSeason().getId(),
//                                                match.getSeason().getYear()
//                                        )
//                                )
//                );
//    }
//
//    public Page<MatchDTO> getMatchesLeague_NameAndSeason_Year(String leagueName,String season,Pageable pageable)
//    {
//        return matchRepository.findByLeague_NameAndSeason_Year(leagueName,season,pageable)
//                .map(
//                        match ->
//                                new MatchDTO(
//                                        match.getId(),
//                                        match.getStatus().name(),
//                                        match.getHomeScore(),
//                                        match.getAwayScore(),
//                                        match.getMatchDate(),
//
//                                        new TeamDTO(
//                                                match.getHomeTeam().getId(),
//                                                match.getHomeTeam().getName(),
//                                                match.getHomeTeam().getLogo(),
//                                                match.getHomeTeam().getStadium()
//
//
//                                        ),
//
//                                        new TeamDTO(
//                                                match.getAwayTeam().getId(),
//                                                match.getAwayTeam().getName(),
//                                                match.getAwayTeam().getLogo(),
//                                                match.getAwayTeam().getStadium()
//
//                                        ),
//
//                                        new LeagueDTO(
//                                                match.getLeague().getId(),
//                                                match.getLeague().getName()
//                                        ),
//
//                                        new SeasonDTO(
//                                                match.getSeason().getId(),
//                                                match.getSeason().getYear()
//                                        )
//                                )
//                );
//    }
//
    private MatchDTO toDTO(Match match) {
        return new MatchDTO(
                match.getId(),
                match.getStatus().name(),
                match.getHomeScore(),
                match.getAwayScore(),
                match.getMatchDate(),

                new TeamDTO(
                        match.getHomeTeam().getId(),
                        match.getHomeTeam().getName(),
                        match.getHomeTeam().getLogo(),
                        match.getHomeTeam().getStadium().getName()


                ),

                new TeamDTO(
                        match.getAwayTeam().getId(),
                        match.getAwayTeam().getName(),
                        match.getAwayTeam().getLogo(),
                        match.getAwayTeam().getStadium().getName()

                ),

                new SeasonDTO(
                        match.getSeason().getId(),
                        match.getSeason().getYear()
                )

        );
    }
//
//    private void mapHome(MatchStatsDTO dto, MatchStats s) {
//        dto.shotsHome = s.getShots();
//        dto.shotsOnTargetHome = s.getShotsOnTarget();
//        dto.possessionHome = s.getPossession();
//        dto.cornersHome = s.getCorners();
//        dto.foulsHome = s.getFouls();
//        dto.offsidesHome = s.getOffsides();
//        dto.yellowCardsHome = s.getYellowCards();
//        dto.redCardsHome = s.getRedCards();
//    }
//
//    private void mapAway(MatchStatsDTO dto, MatchStats s) {
//        dto.shotsAway = s.getShots();
//        dto.shotsOnTargetAway = s.getShotsOnTarget();
//        dto.possessionAway = s.getPossession();
//        dto.cornersAway = s.getCorners();
//        dto.foulsAway = s.getFouls();
//        dto.offsidesAway = s.getOffsides();
//        dto.yellowCardsAway = s.getYellowCards();
//        dto.redCardsAway = s.getRedCards();
//    }
//
//    public MatchStatsDTO getMatchStats(Long matchId) {
//
//        List<MatchStats> statsList = matchStatsRepository.findFullStats(matchId);
//
//        if (statsList.size() != 2) {
//            throw new RuntimeException("Match stats must have 2 teams");
//        }
//
//        MatchStatsDTO dto = new MatchStatsDTO();
//
//        MatchStats s1 = statsList.get(0);
//        MatchStats s2 = statsList.get(1);
//
//        Long homeTeamId = s1.getMatch().getHomeTeam().getId();
//
//        MatchStats homeStats;
//        MatchStats awayStats;
//
//        if (s1.getTeam().getId().equals(homeTeamId)) {
//            homeStats = s1;
//            awayStats = s2;
//        } else {
//            homeStats = s2;
//            awayStats = s1;
//        }
//
//        mapHome(dto, homeStats);
//        mapAway(dto, awayStats);
//
//        return dto;
//    }
//    private String buildDescription(MatchEvent event) {
//        String description = "";
//
//        switch (event.getEventType()) {
//            case GOAL -> {
//                String playerName = event.getPlayer() != null
//                        ? event.getPlayer().getName()
//                        : "Unknown";
//
//                description = playerName  + event.getMinute() + "'";
//            }
//
//            case SUBSTITUTION -> {
//                String inName = event.getPlayerIn() != null
//                        ? event.getPlayerIn().getName()
//                        : "Unknown";
//
//                String outName = event.getPlayerOut() != null
//                        ? event.getPlayerOut().getName()
//                        : "Unknown";
//
//                description = inName + " ⬅ " + outName + " " + event.getMinute() + "'";
//            }
//
//            case YELLOW_CARD -> {
//                String playerName = event.getPlayer() != null
//                        ? event.getPlayer().getName()
//                        : "Unknown";
//
//                description = "🟨 " + playerName + " " + event.getMinute() + "'";
//            }
//
//            case RED_CARD -> {
//                String playerName = event.getPlayer() != null
//                        ? event.getPlayer().getName()
//                        : "Unknown";
//
//                description = playerName + " " + event.getMinute() + "'";
//            }
//
//            default -> {
//                description = "Event at " + event.getMinute() + "'";
//            }
//        }
//
//
//        return description;
//    }
//    public List<MatchEventDTO> getEventsByMatch(Long matchId) {
//        return matchEventRepository
//                .findByMatchIdOrderByMinuteAsc(matchId)
//                .stream()
//                .map(event -> {
//                    MatchEventDTO dto = new MatchEventDTO();
//
//                    dto.setId(event.getId());
//                    dto.setMinute(event.getMinute());
//                    dto.setEventType(event.getEventType());
//
//                    if (event.getPlayer() != null) {
//                        dto.setPlayerId(event.getPlayer().getId());
//                        dto.setPlayerName(event.getPlayer().getName());
//                    }
//                    if (event.getPlayerIn() != null) {
//                        dto.setPlayerInId(event.getPlayerIn().getId());
//                        dto.setPlayerInName(event.getPlayerIn().getName());
//                    }
//                    if (event.getPlayerOut() != null) {
//                        dto.setPlayerOutId(event.getPlayerOut().getId());
//                        dto.setPlayerOutName(event.getPlayerOut().getName());
//                    }
//                    if (event.getTeam() != null) {
//                        dto.setTeamName(event.getTeam().getName());
//                    }
//
//                    // ✅ chỉ dùng hàm này
//                    dto.setDescription(buildDescription(event));
//
//                    return dto;
//                })
//                .toList();
//    }
//
//    public Page<LineUpDTO> getLineup(Long matchId, Pageable pageable) {
//        return lineupRepository.findByMatchId(matchId, pageable)
//                .map(l -> new LineUpDTO(
//                        l.getId(),
//                        l.getPlayer().getName(),
//                        l.getPlayer().getNumber(),
//                        l.getPosition(),
//                        l.getTeam().getId(),
//                        l.getTeam().getName(),
//                        l.getPlayer().getAvatar(),
//                        l.getIsStarting(),
//                        l.getLineupOrder(),
//                        l.getRole()
//                ));
//    }

    public Match getMatchById(Long id) {
        Match match = matchRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Match not found"));

        return match;
    }

    public Page<MatchDTO> getAllMatches(int page, int size, String status, String search) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("matchDate").descending());

        boolean noStatus = (status == null || status.equals("Tất cả trạng thái"));
        boolean noSearch = (search == null || search.isEmpty());

        // Không filter
        if (noStatus && noSearch) {
            return matchRepository.findAll(pageable)
                    .map(this::toDTO);
        }

        // Normalize input
        String finalStatus = noStatus ? null : status;
        String finalSearch = noSearch ? "" : search;

        // Có filter
        return matchRepository.filterMatches(finalStatus, finalSearch, pageable)
                .map(this::toDTO);
    }
    public Match save(Match match) {
        // 1. Chỉ tự động gán sân vận động nếu trận đấu chưa có sân (tránh ghi đè nếu admin muốn đổi sân trung lập)
        if (match.getStadium() == null && match.getHomeTeam() != null) {
            match.setStadium(match.getHomeTeam().getStadium());
        }

        // 2. Thiết lập trạng thái mặc định nếu là trận đấu mới tạo
        if (match.getStatus() == null) {
            match.setStatus(MatchStatus.SCHEDULED); // Hoặc PENDING tùy Enum của bạn
        }

        // 3. Khởi tạo tỷ số bằng 0 thay vì để null (tránh lỗi khi hiển thị)
        if (match.getHomeScore() == null) match.setHomeScore(-1);
        if (match.getAwayScore() == null) match.setAwayScore(-1);

        return matchRepository.save(match);
    }


    @Transactional
    public Match update(Long id, Match match) {
        Match existing = getMatchById(id);

        existing.setStatus(match.getStatus());
        existing.setHomeScore(match.getHomeScore());
        existing.setAwayScore(match.getAwayScore());
        existing.setMatchDate(match.getMatchDate());
        existing.setStadium(match.getStadium());

        existing.setRound(match.getRound());
        existing.setSeason(match.getSeason());

        existing.setHomeTeam(match.getHomeTeam());
        existing.setAwayTeam(match.getAwayTeam());


        return matchRepository.save(existing);
    }

    @Transactional
    public void delete(Long id)
    {
        if (!matchRepository.existsById(id)) {
            throw new ResourceNotFoundException("Match not found with id = " + id);
        }
        matchRepository.deleteById(id);
    }
}