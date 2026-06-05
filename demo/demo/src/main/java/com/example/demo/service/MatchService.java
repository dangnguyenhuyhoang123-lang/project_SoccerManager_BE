package com.example.demo.service;

import com.example.demo.dao.RoundRepository;
import com.example.demo.dao.StadiumRepository;
import com.example.demo.dao.match.MatchEventRepository;
import com.example.demo.dao.match.MatchLineupRepository;
import com.example.demo.dao.match.MatchRepository;
import com.example.demo.dao.match.MatchStatsRepository;
import com.example.demo.dao.player.PlayerRepository;
import com.example.demo.dao.player.PlayerSeasonRepository;
import com.example.demo.dao.season.SeasonRepository;
import com.example.demo.dao.season.SeasonTeamRepository;
import com.example.demo.dao.team.TeamRepository;
import com.example.demo.dao.user.UserRepository;
import com.example.demo.dto.*;
import com.example.demo.dto.aipredict.MatchPredictResponse;
import com.example.demo.entity.*;
import com.example.demo.entity.team.Team;
import com.example.demo.entity.user.User;
import com.example.demo.service.ai.MatchPredictionService;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@AllArgsConstructor
public class MatchService {


    private final MatchRepository matchRepository;


    private final MatchStatsRepository matchStatsRepository;


    private final MatchEventRepository matchEventRepository;


    private final MatchLineupRepository matchLineupRepository;


    private final TeamRepository teamRepository;


    private final SeasonRepository seasonRepository;


    private final StadiumRepository stadiumRepository;


    private final RoundRepository roundRepository;


    private final SeasonTeamRepository seasonTeamRepository;

    private final StandingService standingService;

    private final NotificationService notificationService;

    private final RealtimeEventService realtimeEventService;

    private final UserRepository userRepository;

    private final MatchPredictionService matchPredictionService;

    private  final PlayerSuspensionService playerSuspensionService;

    private final PlayerRepository playerRepository;
    private final PlayerSeasonRepository playerSeasonRepository;


//    public MatchDTO getMatchById(Long id) {
//        Match match = matchRepository.findById(id)
//                .orElseThrow(() -> new RuntimeException("Match not found"));
//
//        return toDTO(match);
//    }

    public MatchDTO getMatchById(Long id) {
        Match match = matchRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy trận đấu id = " + id
                ));

        return toDTO(match);
    }

    public Page<MatchDTO> getAllMatches(
            Pageable pageable,
            String status,
            String search,
            Long seasonId,
            Integer roundId,
            Long teamId
    ) {
        MatchStatus matchStatus = null;

        if (status != null && !status.isBlank()) {
            matchStatus = MatchStatus.valueOf(status);
        }

        return matchRepository.searchMatches(
                        matchStatus,
                        normalize(search),
                        seasonId,
                        roundId,
                        teamId,
                        pageable
                )
                .map(this::toDTO);
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim().toLowerCase();
    }

    @Transactional
    public MatchDTO save(MatchUpsertDTO request) {
        Match match = new Match();
        applyRequest(match, request);

        if (match.getStadium() == null && match.getHomeTeam() != null) {
            match.setStadium(match.getHomeTeam().getTeam().getStadium());
        }

        if (match.getStatus() == null) {
            match.setStatus(MatchStatus.SCHEDULED);
        }

        if (match.getStatus() == MatchStatus.SCHEDULED) {
            match.setHomeScore(null);
            match.setAwayScore(null);
        }

        Match savedMatch = matchRepository.save(match);



        if (savedMatch.getStatus() == MatchStatus.SCHEDULED) {
            sendMatchCreatedNotifications(savedMatch);
        }
        if (savedMatch.getStatus() == MatchStatus.FINISHED) {
            Long seasonId = savedMatch.getSeason().getId();

            standingService.recalculateBySeason(seasonId);

            RealtimeEventDTO standingEvent = realtimeEvent(
                    "STANDING_UPDATED",
                    seasonId,
                    "STANDING",
                    "REFETCH_STANDINGS"
            );

            realtimeEventService.sendToPublicStandings(seasonId, standingEvent);
        }

        RealtimeEventDTO matchCreatedEvent = realtimeEvent(
                "MATCH_CREATED",
                savedMatch.getId(),
                "MATCH",
                "REFETCH_MATCHES"
        );

        sendMatchEventToRelatedClubManagers(savedMatch, matchCreatedEvent);

        realtimeEventService.sendToPublicMatches(matchCreatedEvent);
        realtimeEventService.sendToPublicMatch(savedMatch.getId(), matchCreatedEvent);

        return toDTO(savedMatch);
    }

    private void sendMatchCreatedNotifications(Match match) {
        if (match == null) {
            return;
        }

        SeasonTeam homeSeasonTeam = match.getHomeTeam();
        SeasonTeam awaySeasonTeam = match.getAwayTeam();



        String matchName = buildMatchName(match);

        notifyClubManager(homeSeasonTeam, match, matchName);
        notifyClubManager(awaySeasonTeam, match, matchName);
    }

    private String buildMatchName(Match match) {
        String homeName = match.getHomeTeam() != null
                && match.getHomeTeam().getTeam() != null
                ? match.getHomeTeam().getTeam().getName()
                : "Đội chủ nhà";

        String awayName = match.getAwayTeam() != null
                && match.getAwayTeam().getTeam() != null
                ? match.getAwayTeam().getTeam().getName()
                : "Đội khách";

        return homeName + " vs " + awayName;
    }

    private void notifyClubManager(SeasonTeam seasonTeam, Match match, String matchName) {
        if (seasonTeam == null || seasonTeam.getTeam() == null) {
            return;
        }

        Team team = seasonTeam.getTeam();

    /*
      Chỗ này bạn cần chỉnh theo model thật của bạn.
      Nếu Team có manager/user:
          Long managerUserId = team.getManager().getId();

      Nếu User có teamId:
          cần query UserRepository tìm user theo teamId + role CLUB_MANAGER.

      Tạm thời mình để dạng TODO để bạn map đúng entity hiện tại.
    */



        Optional<User> managerOpt =
                userRepository.findClubManagerByTeamIdAndRoleName(
                        team.getId(),
                        "ROLE_CLUB_MANAGER"
                );

        if (managerOpt.isEmpty()) {
            managerOpt = userRepository.findClubManagerByTeamIdAndRoleName(
                    team.getId(),
                    "CLUB_MANAGER"
            );
        }

        if (managerOpt.isEmpty()) {
            managerOpt = userRepository.findFirstByTeamId(team.getId());
        }

        Long managerUserId = managerOpt
                .map(User::getId)
                .orElse(null);

        if (managerUserId == null) {
            return;
        }

        notificationService.sendToUser(
                managerUserId,
                "Trận đấu mới được tạo",
                "CLB của bạn có trận đấu mới: " + matchName + ". Vui lòng cập nhật đội hình thi đấu.",
                "MATCH_CREATED",
                match.getId(),
                "MATCH"
        );
    }

    @Transactional
    public MatchDTO update(Long id, MatchUpsertDTO request) {
        Match existing = matchRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Match not found"));

        Long oldSeasonId = existing.getSeason() != null ? existing.getSeason().getId() : null;

        applyRequest(existing, request);
        if (existing.getStadium() == null && existing.getHomeTeam() != null) {
            existing.setStadium(existing.getHomeTeam().getTeam().getStadium());
        }

        Match savedMatch = matchRepository.save(existing);

        Long newSeasonId = savedMatch.getSeason() != null ? savedMatch.getSeason().getId() : null;

        if (oldSeasonId != null) {
            standingService.recalculateBySeason(oldSeasonId);
        }

        if (newSeasonId != null && !newSeasonId.equals(oldSeasonId)) {
            standingService.recalculateBySeason(newSeasonId);
        }

        sendMatchEventToRelatedClubManagers(
                savedMatch,
                realtimeEvent("MATCH_UPDATED", savedMatch.getId(), "MATCH", "REFETCH_MATCHES")
        );
        RealtimeEventDTO publicEvent = realtimeEvent(
                "MATCH_UPDATED",
                savedMatch.getId(),
                "MATCH",
                "REFETCH_MATCHES"
        );

        realtimeEventService.sendToPublicMatches(publicEvent);
        realtimeEventService.sendToPublicMatch(savedMatch.getId(), publicEvent);

        return toDTO(savedMatch);
    }

    @Transactional
    public void delete(Long id) {
        Match match = matchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Match not found with id = " + id));

        Long seasonId = match.getSeason() != null ? match.getSeason().getId() : null;
        Set<Long> clubManagerUserIds = findRelatedClubManagerUserIds(match);

        matchRepository.delete(match);

        if (seasonId != null) {
            standingService.recalculateBySeason(seasonId);
        }

        realtimeEventService.sendToUsers(
                clubManagerUserIds,
                realtimeEvent("MATCH_DELETED", id, "MATCH", "REFETCH_MATCHES")
        );

        RealtimeEventDTO publicEvent = realtimeEvent(
                "MATCH_DELETED",
                id,
                "MATCH",
                "REFETCH_MATCHES"
        );

        realtimeEventService.sendToPublicMatches(publicEvent);
    }

    private void applyRequest(Match match, MatchUpsertDTO request) {

        validateMatchRequest(request);


        SeasonTeam homeTeam = seasonTeamRepository.findById(request.getHomeTeamId())
                .orElseThrow(() -> new RuntimeException("Home team not found with id = " + request.getHomeTeamId()));
        SeasonTeam awayTeam = seasonTeamRepository.findById(request.getAwayTeamId())
                .orElseThrow(() -> new RuntimeException("Away team not found with id = " + request.getAwayTeamId()));
        Season season = seasonRepository.findById(request.getSeasonId())
                .orElseThrow(() -> new RuntimeException("Season not found with id = " + request.getSeasonId()));
        Round round = roundRepository.findById(request.getRoundId())
                .orElseThrow(() -> new RuntimeException("Round not found with id = " + request.getRoundId()));


        validateMatchScheduleConstraints(
                match,
                season,
                round,
                homeTeam,
                awayTeam,
                request.getMatchDate()
        );

        if (season.getSystemRule() == null) {
            throw new RuntimeException("Mùa giải chưa được cấu hình bộ luật, không thể tạo trận đấu");
        }
        if (!homeTeam.getSeason().getId().equals(season.getId())) {
            throw new RuntimeException("Đội chủ nhà không thuộc mùa giải đã chọn");
        }

        if (!awayTeam.getSeason().getId().equals(season.getId())) {
            throw new RuntimeException("Đội khách không thuộc mùa giải đã chọn");
        }
        if (round.getSeason() == null || !round.getSeason().getId().equals(season.getId())) {
            throw new RuntimeException("Vòng đấu không thuộc mùa giải đã chọn");
        }


        if (homeTeam.getId().equals(awayTeam.getId())) {
            throw new RuntimeException("Đội chủ nhà và đội khách không được trùng nhau");
        }
        if (request.getMatchDate() != null) {
            LocalDate matchDate = request.getMatchDate().toLocalDate();

            if (season.getStartDate() != null && matchDate.isBefore(season.getStartDate())) {
                throw new RuntimeException("Ngày thi đấu không được trước ngày bắt đầu mùa giải");
            }

            if (season.getEndDate() != null && matchDate.isAfter(season.getEndDate())) {
                throw new RuntimeException("Ngày thi đấu không được sau ngày kết thúc mùa giải");
            }
        }

        match.setStatus(request.getStatus());
        match.setHomeScore(request.getHomeScore());
        match.setAwayScore(request.getAwayScore());
        match.setMatchDate(request.getMatchDate());
        match.setSeason(season);
        match.setRound(round);
        match.setHomeTeam(homeTeam);
        match.setAwayTeam(awayTeam);

        if (request.getStadiumId() != null) {
            Stadium stadium = stadiumRepository.findById(request.getStadiumId())
                    .orElseThrow(() -> new RuntimeException("Stadium not found with id = " + request.getStadiumId()));
            match.setStadium(stadium);
        } else {
            match.setStadium(null);
        }
    }

    private void validateMatchRequest(MatchUpsertDTO request) {
        if (request == null) {
            throw new RuntimeException("Dữ liệu trận đấu không được để trống");
        }

        if (request.getHomeTeamId() == null) {
            throw new RuntimeException("Đội chủ nhà không được để trống");
        }

        if (request.getAwayTeamId() == null) {
            throw new RuntimeException("Đội khách không được để trống");
        }

        if (request.getSeasonId() == null) {
            throw new RuntimeException("Mùa giải không được để trống");
        }

        if (request.getRoundId() == null) {
            throw new RuntimeException("Vòng đấu không được để trống");
        }

        if (request.getMatchDate() == null) {
            throw new RuntimeException("Ngày thi đấu không được để trống");
        }
    }

    @Transactional
    public MatchDTO updateStatus(Long id, MatchStatusUpdateDTO request) {
        Match match = matchRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Match not found with id = " + id));

        if (request.getStatus() == null) {
            throw new RuntimeException("Trạng thái trận đấu không được để trống");
        }

        match.setStatus(request.getStatus());

        Match savedMatch = matchRepository.save(match);

        if (savedMatch.getStatus() == MatchStatus.FINISHED
                && savedMatch.getSeason() != null) {

            // 1. Nếu trận này là trận mà cầu thủ bị treo giò, đánh dấu án đã chấp hành xong.
            playerSuspensionService.markSuspensionsServedAfterMatch(savedMatch.getId());

            // 2. Dựa vào thẻ phạt trong trận này, tạo án treo giò cho trận kế tiếp.
            playerSuspensionService.generateSuspensionsAfterMatch(savedMatch.getId());

            // 3. Tính lại bảng xếp hạng.
            standingService.recalculateBySeason(savedMatch.getSeason().getId());
        }

        sendMatchEventToRelatedClubManagers(
                savedMatch,
                realtimeEvent("MATCH_STATUS_UPDATED", savedMatch.getId(), "MATCH", "REFETCH_MATCHES")
        );

        RealtimeEventDTO publicEvent = realtimeEvent(
                "MATCH_STATUS_UPDATED",
                savedMatch.getId(),
                "MATCH",
                "REFETCH_MATCHES"
        );

        realtimeEventService.sendToPublicMatches(publicEvent);
        realtimeEventService.sendToPublicMatch(savedMatch.getId(), publicEvent);

        if (savedMatch.getStatus() == MatchStatus.FINISHED
                && savedMatch.getSeason() != null) {
            Long seasonId = savedMatch.getSeason().getId();

            RealtimeEventDTO standingEvent = realtimeEvent(
                    "STANDING_UPDATED",
                    seasonId,
                    "STANDING",
                    "REFETCH_STANDINGS"
            );

            sendMatchEventToRelatedClubManagers(savedMatch, standingEvent);
            realtimeEventService.sendToPublicStandings(seasonId, standingEvent);
        }

        return toDTO(savedMatch);
    }

    private Long findClubManagerUserIdBySeasonTeam(SeasonTeam seasonTeam) {
        return findClubManagerBySeasonTeam(seasonTeam)
                .map(User::getId)
                .orElse(null);
    }

    private Optional<User> findClubManagerBySeasonTeam(SeasonTeam seasonTeam) {
        if (seasonTeam == null || seasonTeam.getTeam() == null) {
            return Optional.empty();
        }

        Team team = seasonTeam.getTeam();

        Optional<User> managerOpt =
                userRepository.findClubManagerByTeamIdAndRoleName(
                        team.getId(),
                        "ROLE_CLUB_MANAGER"
                );

        if (managerOpt.isEmpty()) {
            managerOpt = userRepository.findClubManagerByTeamIdAndRoleName(
                    team.getId(),
                    "CLUB_MANAGER"
            );
        }

        if (managerOpt.isEmpty()) {
            managerOpt = userRepository.findFirstByTeamId(team.getId());
        }

        return managerOpt;
    }

    private Set<Long> findRelatedClubManagerUserIds(Match match) {
        Set<Long> userIds = new LinkedHashSet<>();

        if (match == null) {
            return userIds;
        }

        Long homeManagerId = findClubManagerUserIdBySeasonTeam(match.getHomeTeam());
        Long awayManagerId = findClubManagerUserIdBySeasonTeam(match.getAwayTeam());

        if (homeManagerId != null) {
            userIds.add(homeManagerId);
        }

        if (awayManagerId != null) {
            userIds.add(awayManagerId);
        }

        return userIds;
    }

    private void sendMatchEventToRelatedClubManagers(Match match, RealtimeEventDTO event) {
        realtimeEventService.sendToUsers(findRelatedClubManagerUserIds(match), event);
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

    public MatchTeamSeasonDTO getTeamSeasonByMatchAndTeam(Long matchId, Long teamId) {
        Match match = matchRepository.findMatchWithSeasonTeams(matchId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy trận đấu"));

        Long seasonId = match.getSeason().getId();

        SeasonTeam homeSeasonTeam = match.getHomeTeam();
        SeasonTeam awaySeasonTeam = match.getAwayTeam();

        if (homeSeasonTeam.getTeam().getId().equals(teamId)) {
            return new MatchTeamSeasonDTO(
                    match.getId(),
                    teamId,
                    seasonId,
                    homeSeasonTeam.getId()
            );
        }

        if (awaySeasonTeam.getTeam().getId().equals(teamId)) {
            return new MatchTeamSeasonDTO(
                    match.getId(),
                    teamId,
                    seasonId,
                    awaySeasonTeam.getId()
            );
        }

        throw new RuntimeException("Đội bóng không thuộc trận đấu này");
    }


    private MatchDTO toDTO(Match match) {
        return new MatchDTO(
                match.getId(),
                match.getStatus() != null ? match.getStatus().name() : null,
                match.getHomeScore(),
                match.getAwayScore(),
                match.getMatchDate(),
                toStadiumDto(match.getStadium()),
                toRoundDto(match.getRound()),
                match.getHomeTeam() != null ? toTeamDto(match.getHomeTeam().getTeam()) : null,
                match.getAwayTeam() != null ? toTeamDto(match.getAwayTeam().getTeam()) : null,
                toSeasonDto(match.getSeason()),
                match.getPredictedHomeScore(),
                match.getPredictedAwayScore(),
                match.getManOfTheMatch() != null ? match.getManOfTheMatch().getId() : null,
                match.getManOfTheMatch() != null ? match.getManOfTheMatch().getName() : null
        );
    }

    @Transactional
    public MatchDTO updateManOfTheMatch(Long matchId, Long playerId) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy trận đấu id = " + matchId));

        if (match.getSeason() == null) {
            throw new RuntimeException("Trận đấu chưa gắn mùa giải");
        }

        if (match.getHomeTeam() == null || match.getAwayTeam() == null) {
            throw new RuntimeException("Trận đấu thiếu thông tin đội bóng");
        }

        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy cầu thủ id = " + playerId));

        Long seasonId = match.getSeason().getId();
        Long homeTeamId = match.getHomeTeam().getTeam().getId();
        Long awayTeamId = match.getAwayTeam().getTeam().getId();

        boolean belongsToHome = playerSeasonRepository.existsByPlayerTeamSeason(
                playerId,
                homeTeamId,
                seasonId
        );

        boolean belongsToAway = playerSeasonRepository.existsByPlayerTeamSeason(
                playerId,
                awayTeamId,
                seasonId
        );

        if (!belongsToHome && !belongsToAway) {
            throw new RuntimeException("Cầu thủ không thuộc hai đội thi đấu trong mùa giải này");
        }

        match.setManOfTheMatch(player);

        Match savedMatch = matchRepository.save(match);

        sendMatchEventToRelatedClubManagers(
                savedMatch,
                realtimeEvent("MATCH_UPDATED", savedMatch.getId(), "MATCH", "REFETCH_MATCHES")
        );

        return toDTO(savedMatch);
    }

    @Transactional
    public List<MatchDTO> generateDoubleRoundRobinSchedule(
            Long seasonId,
            ScheduleGenerateRequest request
    ) {
        Season season = seasonRepository.findById(seasonId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy mùa giải id = " + seasonId));

        List<SeasonTeam> teams = seasonTeamRepository.findBySeasonIdAndStatus(seasonId, "ACTIVE");

        if (teams.size() % 2 != 0) {
            throw new RuntimeException("Số đội phải là số chẵn để sinh lịch vòng tròn");
        }

        int n = teams.size();
        int roundsPerLeg = n - 1;
        int matchesPerRound = n / 2;

        LocalDateTime startDate = request.getFirstMatchDate();
        if (startDate == null) {
            throw new RuntimeException("Ngày bắt đầu lịch thi đấu không được để trống");
        }

        // nếu đã có match trong mùa thì chặn để tránh tạo trùng
        if (matchRepository.existsBySeasonId(seasonId)) {
            throw new RuntimeException("Mùa giải đã có lịch thi đấu, không thể sinh tự động");
        }

        List<SeasonTeam> rotation = new ArrayList<>(teams);
        List<Match> createdMatches = new ArrayList<>();

        for (int roundIndex = 0; roundIndex < roundsPerLeg; roundIndex++) {
            Round round = ensureRound(season, roundIndex + 1, matchesPerRound);

            for (int i = 0; i < matchesPerRound; i++) {
                SeasonTeam teamA = rotation.get(i);
                SeasonTeam teamB = rotation.get(n - 1 - i);

                boolean swapHome = roundIndex % 2 == 1;

                SeasonTeam home = swapHome ? teamB : teamA;
                SeasonTeam away = swapHome ? teamA : teamB;

                Match match = buildGeneratedMatch(
                        season,
                        round,
                        home,
                        away,
                        startDate.plusDays((long) roundIndex * request.getDaysBetweenRounds())
                );

                createdMatches.add(matchRepository.save(match));
            }

            rotateTeams(rotation);
        }

        // lượt về: đảo sân nhà/sân khách
        for (int roundIndex = 0; roundIndex < roundsPerLeg; roundIndex++) {
            Round round = ensureRound(season, roundsPerLeg + roundIndex + 1, matchesPerRound);

            List<Match> firstLegRoundMatches = createdMatches.subList(
                    roundIndex * matchesPerRound,
                    roundIndex * matchesPerRound + matchesPerRound
            );

            for (Match firstLeg : firstLegRoundMatches) {
                Match secondLeg = buildGeneratedMatch(
                        season,
                        round,
                        firstLeg.getAwayTeam(),
                        firstLeg.getHomeTeam(),
                        startDate.plusDays((long) (roundsPerLeg + roundIndex) * request.getDaysBetweenRounds())
                );

                createdMatches.add(matchRepository.save(secondLeg));
            }
        }

        return createdMatches.stream().map(this::toDTO).toList();
    }
    private void rotateTeams(List<SeasonTeam> teams) {
        if (teams.size() <= 2) return;

        SeasonTeam fixed = teams.get(0);
        SeasonTeam last = teams.remove(teams.size() - 1);
        teams.add(1, last);
        teams.set(0, fixed);
    }

    private Match buildGeneratedMatch(
            Season season,
            Round round,
            SeasonTeam home,
            SeasonTeam away,
            LocalDateTime matchDate
    ) {
        Match match = new Match();
        match.setSeason(season);
        match.setRound(round);
        match.setHomeTeam(home);
        match.setAwayTeam(away);
        match.setMatchDate(matchDate);
        match.setStatus(MatchStatus.SCHEDULED);

        if (home.getTeam() != null && home.getTeam().getStadium() != null) {
            match.setStadium(home.getTeam().getStadium());
        }

        return match;
    }
    private Round ensureRound(Season season, int roundNumber, int maxMatches) {
        return roundRepository.findBySeasonIdAndRoundNumber(season.getId(), roundNumber)
                .orElseGet(() -> {
                    Round round = new Round();
                    round.setSeason(season);
                    round.setRoundNumber(roundNumber);
                    round.setName("Vòng " + roundNumber);
                    round.setMaxMatches(maxMatches);
                    return roundRepository.save(round);
                });
    }


    public List<ManOfTheMatchStatsResponse> getManOfTheMatchStats(Long seasonId) {
        if (seasonId == null || seasonId <= 0) {
            throw new RuntimeException("seasonId không hợp lệ");
        }

        if (!seasonRepository.existsById(seasonId)) {
            throw new RuntimeException("Không tìm thấy mùa giải id = " + seasonId);
        }

        return matchRepository.countManOfTheMatchBySeason(seasonId)
                .stream()
                .map(row -> new ManOfTheMatchStatsResponse(
                        ((Number) row[0]).longValue(),
                        (String) row[1],
                        seasonId,
                        ((Number) row[2]).longValue()
                ))
                .toList();
    }

    private TeamDTO toTeamDto(Team team) {
        if (team == null) {
            return null;
        }

        return new TeamDTO(
                team.getId(),
                team.getName(),
                team.getLogo(),
                team.getCity(),
                team.getStatus(),
                team.getStadium() != null ? Long.valueOf(team.getStadium().getId()) : null,
                team.getStadium() != null ? team.getStadium().getName() : null
        );
    }

    private SeasonDTO toSeasonDto(Season season) {
        if (season == null) {
            return null;
        }

        LeagueDTO leagueDto = null;
        if (season.getLeague() != null) {
            leagueDto = new LeagueDTO(
                    season.getLeague().getId(),
                    season.getLeague().getName(),
                    season.getLeague().getCountry(),
                    season.getLeague().getScale(),
                    season.getLeague().getStatus()
            );
        }

        return new SeasonDTO(
                season.getId(),
                season.getYear(),
                season.getName(),
                season.getStartDate(),
                season.getEndDate(),
                leagueDto
        );
    }

    private StadiumDTO toStadiumDto(Stadium stadium) {
        if (stadium == null) {
            return null;
        }

        return new StadiumDTO(
                Long.valueOf(stadium.getId()),
                stadium.getName(),
                stadium.getAddress(),
                stadium.getCapacity(),
                stadium.getGrass()
        );
    }

    private RoundDTO toRoundDto(Round round) {
        if (round == null) {
            return null;
        }

        return new RoundDTO(
                round.getId(),
                round.getRoundNumber(),
                round.getName(),
                round.getStartDate(),
                round.getEndDate(),
                round.getStatus(),
                round.getSeason() != null ? round.getSeason().getId() : null
        );
    }

//    ============Predict===========

    @Transactional
    public MatchDTO predictMatchScore(Long matchId) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy trận đấu"));

        // Chỉ cho dự đoán trận chưa diễn ra
//        if (match.getStatus() != MatchStatus.SCHEDULED) {
//            throw new RuntimeException("Chỉ có thể dự đoán trận đấu chưa diễn ra");
//        }

        // Nếu đã có tỉ số thật thì không cho dự đoán nữa
//        if (match.getHomeScore() != null || match.getAwayScore() != null) {
//            throw new RuntimeException("Trận đấu đã có tỉ số thật, không thể dự đoán");
//        }

        SeasonTeam homeSeasonTeam = match.getHomeTeam();
        SeasonTeam awaySeasonTeam = match.getAwayTeam();

        if (homeSeasonTeam == null || awaySeasonTeam == null) {
            throw new RuntimeException("Trận đấu thiếu thông tin đội bóng");
        }

        Team homeTeam = homeSeasonTeam.getTeam();
        Team awayTeam = awaySeasonTeam.getTeam();

        if (homeTeam == null || awayTeam == null) {
            throw new RuntimeException("SeasonTeam chưa liên kết với Team");
        }

        String homeTeamName = homeTeam.getName();
        String awayTeamName = awayTeam.getName();

        if (homeTeamName == null || homeTeamName.isBlank()) {
            throw new RuntimeException("Tên đội chủ nhà không hợp lệ");
        }

        if (awayTeamName == null || awayTeamName.isBlank()) {
            throw new RuntimeException("Tên đội khách không hợp lệ");
        }

        MatchPredictResponse prediction = matchPredictionService.predict(
                homeTeamName,
                awayTeamName
        );

        if (prediction == null) {
            throw new RuntimeException("Không nhận được kết quả dự đoán từ AI service");
        }

        if (prediction.getHomeScore() == null || prediction.getAwayScore() == null) {
            throw new RuntimeException("AI service trả về thiếu tỉ số dự đoán");
        }

        Integer predictedHomeScore = Math.max(0, prediction.getHomeScore());
        Integer predictedAwayScore = Math.max(0, prediction.getAwayScore());

        match.setPredictedHomeScore(predictedHomeScore);
        match.setPredictedAwayScore(predictedAwayScore);

        Match savedMatch = matchRepository.save(match);

        return toDTO(savedMatch);
    }


    private void validateMatchScheduleConstraints(
            Match currentMatch,
            Season season,
            Round round,
            SeasonTeam homeTeam,
            SeasonTeam awayTeam,
            LocalDateTime matchDate
    ) {
        Long currentMatchId = currentMatch.getId();

        // 1. Một đội không đá 2 trận trong cùng vòng
        boolean homeBusyInRound = matchRepository.existsTeamInRound(
                round.getId(),
                homeTeam.getId(),
                currentMatchId
        );

        boolean awayBusyInRound = matchRepository.existsTeamInRound(
                round.getId(),
                awayTeam.getId(),
                currentMatchId
        );

        if (homeBusyInRound) {
            throw new RuntimeException("Đội chủ nhà đã có trận trong vòng này");
        }

        if (awayBusyInRound) {
            throw new RuntimeException("Đội khách đã có trận trong vòng này");
        }

        // 2. Một cặp đội không được gặp nhau quá 2 lần
        long pairCount = matchRepository.countMatchesBetweenTwoSeasonTeams(
                season.getId(),
                homeTeam.getId(),
                awayTeam.getId(),
                currentMatchId
        );

        if (pairCount >= 2) {
            throw new RuntimeException("Hai đội này đã gặp nhau đủ 2 lượt trong mùa giải");
        }

        // 3. Không trùng chính xác cùng cặp sân nhà/sân khách
        boolean sameDirectionExists = matchRepository.existsSameHomeAwayPair(
                season.getId(),
                homeTeam.getId(),
                awayTeam.getId(),
                currentMatchId
        );

        if (sameDirectionExists) {
            throw new RuntimeException("Lượt đấu này đã tồn tại. Lượt về phải đảo sân nhà/sân khách");
        }

        // 4. Một round không vượt quá số trận tối đa
        Integer maxMatches = round.getMaxMatches() != null ? round.getMaxMatches() : 5;

        long matchCountInRound = matchRepository.countByRoundIdExcludingCurrent(
                round.getId(),
                currentMatchId
        );

        if (matchCountInRound >= maxMatches) {
            throw new RuntimeException("Vòng đấu đã đạt số trận tối đa: " + maxMatches);
        }
    }
}
