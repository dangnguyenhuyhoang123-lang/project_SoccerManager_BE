package com.example.demo.service.crawl;


import com.example.demo.dao.*;
import com.example.demo.dao.match.MatchRepository;
import com.example.demo.dao.player.PlayerRepository;
import com.example.demo.dao.player.PlayerSeasonRepository;
import com.example.demo.dao.player.PlayerStatsRepository;
import com.example.demo.dao.season.SeasonRepository;
import com.example.demo.dao.season.SeasonTeamCoachRepository;
import com.example.demo.dao.season.SeasonTeamRepository;
import com.example.demo.dao.team.TeamRepository;
import com.example.demo.dto.crawl.*;
import com.example.demo.entity.*;
import com.example.demo.service.StandingService;
import com.example.demo.service.TeamStatsService;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.LocalDate;
import java.util.*;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Service
public class VpfVLeagueSyncService {

    private static final String SOURCE_NAME = "VPF";

    private static final String VPF_VLEAGUE_MATCH_URL =
            "https://vpf.vn/season/v-league-2026/?action=calendar&pagejs=1";
    private static final String VPF_VLEAGUE_TEAMS_URL =
            "https://vpf.vn/cac-doi-bong-v-league/";

    private static final String LEAGUE_NAME = "V.League 1";
    private static final String SEASON_YEAR = "2025-2026";
    private static final String SEASON_NAME = "LPBank V.League 1-2025/26";

    private final LeagueRepository leagueRepository;
    private final SeasonRepository seasonRepository;
    private final RoundRepository roundRepository;
    private final TeamRepository teamRepository;
    private final SeasonTeamRepository seasonTeamRepository;
    private final StadiumRepository stadiumRepository;
    private final MatchRepository matchRepository;
    private final StandingService standingService;
    private final TeamStatsService teamStatsService;

    private final PlayerRepository playerRepository;
    private final PlayerSeasonRepository playerSeasonRepository;
    private final PlayerStatsRepository playerStatsRepository;
    private final CoachRepository coachRepository;
    private final SeasonTeamCoachRepository seasonTeamCoachRepository;


    public VpfVLeagueSyncService(LeagueRepository leagueRepository, SeasonRepository seasonRepository, RoundRepository roundRepository, TeamRepository teamRepository, SeasonTeamRepository seasonTeamRepository, StadiumRepository stadiumRepository, MatchRepository matchRepository, StandingService standingService, TeamStatsService teamStatsService, PlayerRepository playerRepository, PlayerSeasonRepository playerSeasonRepository, PlayerStatsRepository playerStatsRepository, CoachRepository coachRepository, SeasonTeamCoachRepository seasonTeamCoachRepository) {
        this.leagueRepository = leagueRepository;
        this.seasonRepository = seasonRepository;
        this.roundRepository = roundRepository;
        this.teamRepository = teamRepository;
        this.seasonTeamRepository = seasonTeamRepository;
        this.stadiumRepository = stadiumRepository;
        this.matchRepository = matchRepository;
        this.standingService = standingService;
        this.teamStatsService = teamStatsService;
        this.playerRepository = playerRepository;
        this.playerSeasonRepository = playerSeasonRepository;
        this.playerStatsRepository = playerStatsRepository;
        this.coachRepository = coachRepository;
        this.seasonTeamCoachRepository = seasonTeamCoachRepository;
    }

    //    private Document loadVpfCalendarDocument() {
//        try {
//            return Jsoup.connect(VPF_VLEAGUE_MATCH_URL)
//                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0 Safari/537.36")
//                    .referrer("https://vpf.vn/")
//                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
//                    .header("Accept-Language", "vi-VN,vi;q=0.9,en-US;q=0.8,en;q=0.7")
//                    .header("Connection", "close")
//                    .ignoreHttpErrors(true)
//                    .followRedirects(true)
//                    .timeout(60000)
//                    .get();
//        } catch (Exception e) {
//            throw new RuntimeException("Không thể tải dữ liệu lịch V.League từ VPF.", e);
//        }
//    }
        private Document loadVpfCalendarDocument(String calendarUrl) {
            try {
        return Jsoup.connect(calendarUrl)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .referrer("https://vpf.vn/")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Accept-Language", "vi-VN,vi;q=0.9,en-US;q=0.8,en;q=0.7")
                .header("Connection", "close")
                .ignoreHttpErrors(true)
                .followRedirects(true)
                .timeout(60000)
                .get();
             } catch (Exception e) {
        throw new RuntimeException("Không thể tải dữ liệu lịch từ VPF: " + calendarUrl, e);
             }
        }

//    @Transactional
//    public void syncVLeagueCalendar() {
//        try {
//            Document doc = loadVpfCalendarDocument();
//
//            League league = findOrCreateLeague();
//            Season season = findOrCreateSeason(league);
//
//            List<VpfMatchCrawlDto> crawledMatches = parseCalendar(doc);
//
//            for (VpfMatchCrawlDto dto : crawledMatches) {
//                syncOneMatch(season, dto);
//            }
//
//            standingService.recalculateBySeason(season.getId());
//            teamStatsService.recalculateBySeason((season.getId()));
//
//        } catch (Exception e) {
//            e.printStackTrace();
//            throw new RuntimeException("Không thể đồng bộ lịch V.League từ VPF.");
//        }
//    }

    @Transactional
    public void syncVLeagueCalendar() {
        syncVLeagueCalendar(defaultVLeague1_2025_2026());
    }

    @Transactional
    public void syncVLeagueCalendar(VpfSeasonSyncRequest request) {
        try {
            Document doc = loadVpfCalendarDocument(request.getCalendarUrl());

            League league = findOrCreateLeague(request);
            Season season = findOrCreateSeason(league, request);

            List<VpfMatchCrawlDto> crawledMatches = parseCalendar(doc, request);

            for (VpfMatchCrawlDto dto : crawledMatches) {
                syncOneMatch(season, dto);
            }

            standingService.recalculateBySeason(season.getId());
            teamStatsService.recalculateBySeason(season.getId());

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Không thể đồng bộ lịch VPF: " + e.getMessage(), e);
        }
    }
//    @Transactional
//    public void syncVLeagueTeams() {
//        try {
//            Document doc = Jsoup.connect(VPF_VLEAGUE_TEAMS_URL)
//                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
//                    .referrer("https://vpf.vn/")
//                    .header("Accept-Language", "vi-VN,vi;q=0.9,en-US;q=0.8")
//                    .timeout(60000)
//                    .get();
//
//            League league = findOrCreateLeague();
//            Season season = findOrCreateSeason(league);
//
//            List<VpfTeamCrawlDto> teams = parseTeams(doc);
//
//            for (VpfTeamCrawlDto dto : teams) {
//                syncOneTeam(season, dto);
//            }
//
//        } catch (Exception e) {
//            e.printStackTrace();
//            throw new RuntimeException("Không thể đồng bộ danh sách đội V.League từ VPF.");
//        }
//    }
    @Transactional
    public void syncVLeagueTeams() {
        syncVLeagueTeams(defaultVLeague1_2025_2026());
}
    @Transactional
    public void syncVLeagueTeams(VpfSeasonSyncRequest request) {
        try {
            Document doc = loadVpfTeamsDocument(request.getTeamsUrl());

            League league = findOrCreateLeague(request);
            Season season = findOrCreateSeason(league, request);

            List<VpfTeamCrawlDto> teams = parseTeams(doc, request);

            for (VpfTeamCrawlDto dto : teams) {
                syncOneTeam(season, dto);
            }

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Không thể đồng bộ danh sách đội VPF: " + e.getMessage(), e);
        }
    }
//    @Transactional
//    public void syncVLeagueTeamDetails() {
//        try {
//            League league = findOrCreateLeague();
//            Season season = findOrCreateSeason(league);
//
//            Document doc = Jsoup.connect(VPF_VLEAGUE_TEAMS_URL)
//                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
//                    .referrer("https://vpf.vn/")
//                    .header("Accept-Language", "vi-VN,vi;q=0.9,en-US;q=0.8")
//                    .timeout(60000)
//                    .get();
//
//            List<VpfTeamCrawlDto> teams = parseTeams(doc);
//
//            for (VpfTeamCrawlDto teamDto : teams) {
//                Team team = syncOneTeamAndReturn(season, teamDto);
//
//                VpfTeamDetailCrawlDto detailDto = previewTeamDetail(teamDto.getSourceUrl());
//
//                syncOneTeamDetail(season, team, detailDto);
//            }
//
//        } catch (Exception e) {
//            e.printStackTrace();
//            throw new RuntimeException("Không thể đồng bộ chi tiết đội bóng V.League từ VPF.");
//        }
//    }
        @Transactional
        public void syncVLeagueTeamDetails() {
            syncVLeagueTeamDetails(defaultVLeague1_2025_2026());
        }

    @Transactional
    public void syncVLeagueTeamDetails(VpfSeasonSyncRequest request) {
        try {
            League league = findOrCreateLeague(request);
            Season season = findOrCreateSeason(league, request);

            Document doc = loadVpfTeamsDocument(request.getTeamsUrl());

            List<VpfTeamCrawlDto> teams = parseTeams(doc, request);

            for (VpfTeamCrawlDto teamDto : teams) {
                try {
                    System.out.println("Sync team detail: " + teamDto.getTeamName() + " - " + teamDto.getSourceUrl());

                    Team team = syncOneTeamAndReturn(season, teamDto);

                    VpfTeamDetailCrawlDto detailDto = previewTeamDetail(teamDto.getSourceUrl());

                    syncOneTeamDetail(season, team, detailDto);

                } catch (Exception ex) {
                    ex.printStackTrace();
                    throw new RuntimeException(
                            "Lỗi khi sync team detail: "
                                    + teamDto.getTeamName()
                                    + " - "
                                    + teamDto.getSourceUrl()
                                    + " | "
                                    + ex.getMessage(),
                            ex
                    );
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Không thể đồng bộ chi tiết đội bóng VPF: " + e.getMessage(), e);
        }
    }

    private void syncCoachForTeam(Season season, Team team, VpfTeamDetailCrawlDto detailDto) {
        if (detailDto == null) {
            return;
        }

        String coachName = detailDto.getCoachName();

        System.out.println("Parsed coach for team " + team.getName() + ": " + coachName);

        if (coachName == null || coachName.isBlank()) {
            return;
        }

        Coach coach = findOrCreateCoach(coachName, team);

        // Cập nhật team hiện tại cho coach
        coach.setTeam(team);
        coach.setStatus("ACTIVE");
        coach.setSourceName(SOURCE_NAME);

        Coach savedCoach = coachRepository.save(coach);

        SeasonTeamCoach seasonTeamCoach = seasonTeamCoachRepository
                .findBySeasonAndTeamAndCoach(season, team, savedCoach)
                .orElseGet(() -> {
                    SeasonTeamCoach newAssignment = new SeasonTeamCoach();
                    newAssignment.setSeason(season);
                    newAssignment.setTeam(team);
                    newAssignment.setCoach(savedCoach);
                    return newAssignment;
                });

        seasonTeamCoach.setRole("HLV Trưởng");

        if (seasonTeamCoach.getAssignedDate() == null) {
            seasonTeamCoach.setAssignedDate(
                    season.getStartDate() != null ? season.getStartDate() : LocalDate.now()
            );
        }

        seasonTeamCoach.setStatus("ACTIVE");

        seasonTeamCoachRepository.save(seasonTeamCoach);

        System.out.println("Saved coach assignment: "
                + savedCoach.getName()
                + " - "
                + team.getName()
                + " - "
                + season.getName());
    }

    private Coach findOrCreateCoach(String coachName, Team team) {
        String normalizedName = normalizeName(coachName);

        if (!normalizedName.isBlank()) {
            Optional<Coach> byNormalizedName = coachRepository.findByNormalizedName(normalizedName);

            if (byNormalizedName.isPresent()) {
                return byNormalizedName.get();
            }
        }

        Optional<Coach> byName = coachRepository.findByNameIgnoreCase(coachName);

        if (byName.isPresent()) {
            return byName.get();
        }

        Coach coach = new Coach();
        coach.setName(coachName);
        coach.setNormalizedName(normalizedName);
        coach.setStatus("ACTIVE");
        coach.setSourceName(SOURCE_NAME);

        // Dòng quan trọng để không lỗi team_id null
        coach.setTeam(team);

        return coachRepository.save(coach);
    }

    private VpfSeasonSyncRequest defaultVLeague1_2025_2026() {
        VpfSeasonSyncRequest request = new VpfSeasonSyncRequest();

        request.setLeagueName("V.League 1");
        request.setLeagueSlug("v-league-2026");

        request.setSeasonName("LPBank V.League 1-2025/26");
        request.setSeasonYear("2025-2026");

        request.setCalendarUrl("https://vpf.vn/season/v-league-2026/?action=calendar&pagejs=1");
        request.setTeamsUrl("https://vpf.vn/cac-doi-bong-v-league/");
        request.setTeamSid("121994");

        request.setStartDate(LocalDate.of(2025, 8, 1));
        request.setEndDate(LocalDate.of(2026, 6, 30));

        request.setCountry("Vietnam");
        request.setScale("NATIONAL");

        return request;
    }



    private void syncOneMatch(Season season, VpfMatchCrawlDto dto) {
        Round round = findOrCreateRound(season, dto);
        Stadium stadium = findOrCreateStadium(dto.getStadiumName());

        Team homeTeam = findOrCreateTeam(dto.getHomeTeamName());
        Team awayTeam = findOrCreateTeam(dto.getAwayTeamName());

        SeasonTeam homeSeasonTeam = findOrCreateSeasonTeam(season, homeTeam);
        SeasonTeam awaySeasonTeam = findOrCreateSeasonTeam(season, awayTeam);

        Match match = matchRepository
                .findBySeasonAndVpfMatchCode(season, dto.getVpfMatchCode())
                .orElse(new Match());

        match.setSeason(season);
        match.setRound(round);
        match.setStadium(stadium);
        match.setHomeTeam(homeSeasonTeam);
        match.setAwayTeam(awaySeasonTeam);
        match.setMatchDate(dto.getMatchDate());
        match.setVpfMatchCode(dto.getVpfMatchCode());
        match.setHomeScore(dto.getHomeScore());
        match.setAwayScore(dto.getAwayScore());
        match.setStatus(dto.getStatus());
        match.setBroadcast(dto.getBroadcast());
        match.setAttendance(dto.getAttendance());
        match.setSourceName(SOURCE_NAME);
        match.setSourceUrl(dto.getSourceUrl());

        matchRepository.save(match);
    }

//    private League findOrCreateLeague() {
//        return leagueRepository.findByNameIgnoreCase(LEAGUE_NAME)
//                .orElseGet(() -> {
//                    League league = new League();
//                    league.setName(LEAGUE_NAME);
//                    league.setCountry("Vietnam");
//                    league.setScale("NATIONAL");
//                    league.setStatus("ACTIVE");
//                    league.setVpfLeagueSlug("v-league-2026");
//                    league.setSourceName(SOURCE_NAME);
//                    return leagueRepository.save(league);
//                });
//    }

    private League findOrCreateLeague(VpfSeasonSyncRequest request) {
        return leagueRepository.findByNameIgnoreCase(request.getLeagueName())
                .orElseGet(() -> {
                    League league = new League();
                    league.setName(request.getLeagueName());
                    league.setCountry(request.getCountry());
                    league.setScale(request.getScale());
                    league.setStatus("ACTIVE");
                    league.setVpfLeagueSlug(request.getLeagueSlug());
                    league.setSourceName(SOURCE_NAME);
                    return leagueRepository.save(league);
                });
    }

//    private Season findOrCreateSeason(League league) {
//        return seasonRepository.findByYearAndLeague(SEASON_YEAR, league)
//                .orElseGet(() -> {
//                    Season season = new Season();
//                    season.setYear(SEASON_YEAR);
//                    season.setName(SEASON_NAME);
//                    season.setLeague(league);
//                    season.setStartDate(LocalDate.of(2025, 8, 1));
//                    season.setEndDate(LocalDate.of(2026, 6, 30));
//                    season.setVpfSeasonUrl(VPF_VLEAGUE_MATCH_URL);
//                    season.setSourceName(SOURCE_NAME);
//                    return seasonRepository.save(season);
//                });
//    }

    private Season findOrCreateSeason(League league, VpfSeasonSyncRequest request) {
        return seasonRepository.findByYearAndLeague(request.getSeasonYear(), league)
                .orElseGet(() -> {
                    Season season = new Season();
                    season.setYear(request.getSeasonYear());
                    season.setName(request.getSeasonName());
                    season.setLeague(league);
                    season.setStartDate(request.getStartDate());
                    season.setEndDate(request.getEndDate());
                    season.setVpfSeasonUrl(request.getCalendarUrl());
                    season.setSourceName(SOURCE_NAME);
                    return seasonRepository.save(season);
                });
    }

    private Round findOrCreateRound(Season season, VpfMatchCrawlDto dto) {
        Integer roundNumber = dto.getRoundNumber();

        if (roundNumber == null) {
            throw new RuntimeException("Không parse được vòng đấu từ VPF: " + dto.getRoundName());
        }

        return roundRepository.findBySeasonAndRoundNumber(season, roundNumber)
                .orElseGet(() -> {
                    Round round = new Round();
                    round.setSeason(season);
                    round.setRoundNumber(roundNumber);
                    round.setName(dto.getRoundName());
                    round.setStatus("ACTIVE");
                    round.setNotifyTeams(false);
                    round.setMaxMatches(7);
                    return roundRepository.save(round);
                });
    }

    private Stadium findOrCreateStadium(String stadiumName) {
        String normalized = normalizeName(stadiumName);

        return stadiumRepository.findByNormalizedName(normalized)
                .orElseGet(() -> {
                    Stadium stadium = new Stadium();
                    stadium.setName(stadiumName);
                    stadium.setNormalizedName(normalized);
                    stadium.setSourceName(SOURCE_NAME);

                    // Nếu GrassType của bạn có NATURAL thì giữ dòng này.
                    // Nếu enum khác tên, sửa lại cho đúng.
                    stadium.setGrass(GrassType.Standard);

                    return stadiumRepository.save(stadium);
                });
    }

    private Team findOrCreateTeam(String teamName) {
        String normalized = normalizeName(teamName);

        return teamRepository.findByNormalizedName(normalized)
                .orElseGet(() -> {
                    Team team = new Team();
                    team.setName(teamName);
                    team.setNormalizedName(normalized);
                    team.setSourceName(SOURCE_NAME);
                    team.setStatus("ACTIVE");
                    return teamRepository.save(team);
                });
    }

    private SeasonTeam findOrCreateSeasonTeam(Season season, Team team) {
        return seasonTeamRepository.findBySeasonAndTeam(season, team)
                .orElseGet(() -> {
                    SeasonTeam seasonTeam = new SeasonTeam();
                    seasonTeam.setSeason(season);
                    seasonTeam.setTeam(team);
                    seasonTeam.setStatus("ACTIVE");
                    return seasonTeamRepository.save(seasonTeam);
                });
    }

    private String normalizeName(String input) {
        if (input == null) {
            return "";
        }

        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replace("đ", "d")
                .replaceAll("[^a-z0-9]+", " ")
                .trim()
                .replaceAll("\\s+", " ");

        return normalized;
    }

//    private List<VpfMatchCrawlDto> parseCalendar(Document doc) {
//        List<VpfMatchCrawlDto> result = new ArrayList<>();
//
//        Elements elements = doc.select(".jsrow-matchday-name, .js-matchday-wrapper");
//
//        String currentRoundName = null;
//        Integer currentRoundNumber = null;
//
//        for (Element element : elements) {
//            if (element.hasClass("jsrow-matchday-name")) {
//                currentRoundName = element.text().trim();
//                currentRoundNumber = parseRoundNumber(currentRoundName);
//                continue;
//            }
//
//            if (element.hasClass("js-matchday-wrapper")) {
//                LocalDate matchDate = parseVietnameseDate(
//                        element.select(".js-matchday-date").text().trim()
//                );
//
//                Elements matchRows = element.select(".js-matchday-matches > .jstable-row");
//
//                for (Element row : matchRows) {
//                    String timeText = row.select(".jsMatchDivTime .jsDivLineEmbl").text().trim();
//                    String matchCodeText = row.select(".js-ma-tran .jsDivLineEmbl").text().trim();
//                    String stadiumName = row.select(".jsMatchDivVenue").text().trim();
//
//                    String homeTeamName = row.select(".jsMatchDivHome .js_div_particName a").text().trim();
//                    String awayTeamName = row.select(".jsMatchDivAway .js_div_particName a").text().trim();
//
//                    String scoreText = row.select(".jsMatchDivScore a").text().trim();
//
//                    String broadcast = row.select(".jsChannelDiv").text().trim();
//                    String audienceText = row.select(".js-audience .jsDivLineEmbl").text().trim();
//
//                    String matchDetailUrl = row.select(".jsMatchDivScore a").attr("href");
//
//                    if (matchCodeText.isBlank()
//                            || homeTeamName.isBlank()
//                            || awayTeamName.isBlank()) {
//                        continue;
//                    }
//
//                    VpfMatchCrawlDto dto = new VpfMatchCrawlDto();
//
//                    dto.setLeagueName(LEAGUE_NAME);
//                    dto.setSeasonYear(SEASON_YEAR);
//                    dto.setSeasonName(SEASON_NAME);
//
//                    dto.setRoundNumber(currentRoundNumber);
//                    dto.setRoundName(currentRoundName);
//
//                    dto.setVpfMatchCode(parseInteger(matchCodeText));
//                    dto.setMatchDate(buildMatchDateTime(matchDate, timeText));
//
//                    dto.setStadiumName(stadiumName);
//                    dto.setHomeTeamName(homeTeamName);
//                    dto.setAwayTeamName(awayTeamName);
//
//                    applyScore(dto, scoreText);
//
//                    dto.setBroadcast(broadcast);
//                    dto.setAttendance(parseAudience(audienceText));
//
//                    dto.setSourceUrl(matchDetailUrl != null && !matchDetailUrl.isBlank()
//                            ? matchDetailUrl
//                            : VPF_VLEAGUE_MATCH_URL);
//
//                    result.add(dto);
//                }
//            }
//        }
//
//        return result;
//    }

    private List<VpfMatchCrawlDto> parseCalendar(Document doc, VpfSeasonSyncRequest request) {
        List<VpfMatchCrawlDto> result = new ArrayList<>();

        Elements elements = doc.select(".jsrow-matchday-name, .js-matchday-wrapper");

        String currentRoundName = null;
        Integer currentRoundNumber = null;

        for (Element element : elements) {
            if (element.hasClass("jsrow-matchday-name")) {
                currentRoundName = element.text().trim();
                currentRoundNumber = parseRoundNumber(currentRoundName);
                continue;
            }

            if (element.hasClass("js-matchday-wrapper")) {
                LocalDate matchDate = parseVietnameseDate(
                        element.select(".js-matchday-date").text().trim()
                );

                Elements matchRows = element.select(".js-matchday-matches > .jstable-row");

                for (Element row : matchRows) {
                    String timeText = row.select(".jsMatchDivTime .jsDivLineEmbl").text().trim();
                    String matchCodeText = row.select(".js-ma-tran .jsDivLineEmbl").text().trim();
                    String stadiumName = row.select(".jsMatchDivVenue").text().trim();

                    String homeTeamName = row.select(".jsMatchDivHome .js_div_particName a").text().trim();
                    String awayTeamName = row.select(".jsMatchDivAway .js_div_particName a").text().trim();

                    String scoreText = row.select(".jsMatchDivScore a").text().trim();

                    String broadcast = row.select(".jsChannelDiv").text().trim();
                    String audienceText = row.select(".js-audience .jsDivLineEmbl").text().trim();

                    String matchDetailUrl = row.select(".jsMatchDivScore a").attr("href");

                    if (matchCodeText.isBlank()
                            || homeTeamName.isBlank()
                            || awayTeamName.isBlank()) {
                        continue;
                    }

                    VpfMatchCrawlDto dto = new VpfMatchCrawlDto();

                    dto.setLeagueName(request.getLeagueName());
                    dto.setSeasonYear(request.getSeasonYear());
                    dto.setSeasonName(request.getSeasonName());

                    dto.setRoundNumber(currentRoundNumber);
                    dto.setRoundName(currentRoundName);

                    dto.setVpfMatchCode(parseInteger(matchCodeText));
                    dto.setMatchDate(buildMatchDateTime(matchDate, timeText));

                    dto.setStadiumName(stadiumName);
                    dto.setHomeTeamName(homeTeamName);
                    dto.setAwayTeamName(awayTeamName);

                    applyScore(dto, scoreText);

                    dto.setBroadcast(broadcast);
                    dto.setAttendance(parseAudience(audienceText));

                    dto.setSourceUrl(matchDetailUrl != null && !matchDetailUrl.isBlank()
                            ? matchDetailUrl
                            : request.getCalendarUrl());

                    result.add(dto);
                }
            }
        }

        return result;
    }

    private Integer parseRoundNumber(String roundName) {
        if (roundName == null || roundName.isBlank()) {
            return null;
        }

        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
                "(?i)vòng\\s+(\\d+)"
        );

        java.util.regex.Matcher matcher = pattern.matcher(roundName.trim());

        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }

        return null;
    }


    private LocalDate parseVietnameseDate(String dateText) {
        if (dateText == null || dateText.isBlank()) {
            return null;
        }

        String normalized = dateText
                .replace(",", "")
                .replace("Tháng", "")
                .replaceAll("\\s+", " ")
                .trim();

        // Ví dụ: "05 Tháng 04, 2026" -> "05 04 2026"
        String[] parts = normalized.split(" ");

        if (parts.length < 3) {
            throw new RuntimeException("Không parse được ngày từ VPF: " + dateText);
        }

        int day = Integer.parseInt(parts[0]);
        int month = Integer.parseInt(parts[1]);
        int year = Integer.parseInt(parts[2]);

        return LocalDate.of(year, month, day);
    }

    private LocalDateTime buildMatchDateTime(LocalDate date, String timeText) {
        if (date == null) {
            return null;
        }

        if (timeText == null || timeText.isBlank()) {
            return date.atStartOfDay();
        }

        LocalTime time = LocalTime.parse(timeText.trim());
        return LocalDateTime.of(date, time);
    }



//    private Integer parseAudience(String audienceText) {
//        if (audienceText == null || audienceText.isBlank()) {
//            return null;
//        }
//
//        // Ví dụ: "KG: 5000 người" -> 5000
//        String numberOnly = audienceText.replaceAll("[^0-9]", "");
//
//        if (numberOnly.isBlank()) {
//            return null;
//        }
//
//        return Integer.parseInt(numberOnly);
//    }
        private Integer parseAudience(String audienceText) {
            return parseInteger(audienceText);
}

    private void applyScore(VpfMatchCrawlDto dto, String scoreText) {
        if (scoreText == null || scoreText.isBlank()) {
            dto.setHomeScore(null);
            dto.setAwayScore(null);
            dto.setStatus(MatchStatus.SCHEDULED);
            return;
        }

        String normalized = scoreText.trim();

        if (normalized.equalsIgnoreCase("v")
                || normalized.equalsIgnoreCase("vs")
                || !normalized.contains("-")) {
            dto.setHomeScore(null);
            dto.setAwayScore(null);
            dto.setStatus(MatchStatus.SCHEDULED);
            return;
        }

        String[] parts = normalized.split("-");

        if (parts.length != 2) {
            dto.setHomeScore(null);
            dto.setAwayScore(null);
            dto.setStatus(MatchStatus.SCHEDULED);
            return;
        }

        dto.setHomeScore(parseInteger(parts[0]));
        dto.setAwayScore(parseInteger(parts[1]));
        dto.setStatus(MatchStatus.FINISHED);
    }




//    public List<VpfMatchCrawlDto> previewVLeagueCalendar() {
//        try {
//            Document doc = loadVpfCalendarDocument();
//
//            System.out.println("VPF title: " + doc.title());
//            System.out.println("Round count: " + doc.select(".jsrow-matchday-name").size());
//            System.out.println("Matchday count: " + doc.select(".js-matchday-wrapper").size());
//            System.out.println("Match row count: " + doc.select(".jstable-row").size());
//            return parseCalendar(doc);
//
//        } catch (Exception e) {
//            e.printStackTrace();
//            throw new RuntimeException("Không thể preview lịch V.League từ VPF.");
//        }
//    }

    public List<VpfMatchCrawlDto> previewVLeagueCalendar() {
        return previewVLeagueCalendar(defaultVLeague1_2025_2026());
    }

    public List<VpfMatchCrawlDto> previewVLeagueCalendar(VpfSeasonSyncRequest request) {
        try {
            Document doc = loadVpfCalendarDocument(request.getCalendarUrl());

            System.out.println("VPF title: " + doc.title());
            System.out.println("Round count: " + doc.select(".jsrow-matchday-name").size());
            System.out.println("Matchday count: " + doc.select(".js-matchday-wrapper").size());
            System.out.println("Match row count: " + doc.select(".jstable-row").size());

            return parseCalendar(doc, request);

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Không thể preview lịch VPF: " + e.getMessage(), e);
        }
    }

//    ===================CRAWL TEAMS=================

//    public List<VpfTeamCrawlDto> previewVLeagueTeams() {
//        try {
//            Document doc = Jsoup.connect(VPF_VLEAGUE_TEAMS_URL)
//                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
//                    .referrer("https://vpf.vn/")
//                    .header("Accept-Language", "vi-VN,vi;q=0.9,en-US;q=0.8")
//                    .timeout(60000)
//                    .get();
//
//            return parseTeams(doc);
//
//        } catch (Exception e) {
//            throw new RuntimeException("Không thể preview danh sách đội bóng V.League từ VPF.", e);
//        }
//    }

    public List<VpfTeamCrawlDto> previewVLeagueTeams() {
        return previewVLeagueTeams(defaultVLeague1_2025_2026());
    }
    public List<VpfTeamCrawlDto> previewVLeagueTeams(VpfSeasonSyncRequest request) {
        try {
            Document doc = loadVpfTeamsDocument(request.getTeamsUrl());
            return parseTeams(doc, request);
        } catch (Exception e) {
            throw new RuntimeException("Không thể preview danh sách đội VPF: " + e.getMessage(), e);
        }
    }
//    private List<VpfTeamCrawlDto> parseTeams(Document doc) {
//        List<VpfTeamCrawlDto> result = new ArrayList<>();
//
//        Elements teamLinks = doc.select("a[href*='/club/'], a[href*='/team/'], a[href*='/doi-bong/']");
//
//        for (Element link : teamLinks) {
//            String teamName = link.text().trim();
//            String sourceUrl = link.absUrl("href");
//
//            Element card = link.parent();
//            String logoUrl = "";
//
//            if (card != null) {
//                Element img = card.selectFirst("img");
//                if (img == null && card.parent() != null) {
//                    img = card.parent().selectFirst("img");
//                }
//                if (img != null) {
//                    logoUrl = img.absUrl("src");
//                }
//            }
//
//            if (teamName.isBlank() || logoUrl.isBlank()) {
//                continue;
//            }
//
//            VpfTeamCrawlDto dto = new VpfTeamCrawlDto();
//            dto.setSeasonYear(SEASON_YEAR);
//            dto.setSeasonName(SEASON_NAME);
//            dto.setTeamName(teamName);
//            dto.setNormalizedName(normalizeName(teamName));
//            dto.setLogoUrl(logoUrl);
//            dto.setSourceUrl(sourceUrl);
//            dto.setSourceName(SOURCE_NAME);
//
//            result.add(dto);
//        }
//
//        return cleanVpfTeams(result);
//    }

    private List<VpfTeamCrawlDto> parseTeams(Document doc, VpfSeasonSyncRequest request) {
        List<VpfTeamCrawlDto> result = new ArrayList<>();

        Elements teamLinks = doc.select("a[href*='/club/'], a[href*='/team/'], a[href*='/doi-bong/']");

        for (Element link : teamLinks) {
            String teamName = link.text().trim();
            String sourceUrl = link.absUrl("href");

            Element card = link.parent();
            String logoUrl = "";

            if (card != null) {
                Element img = card.selectFirst("img");
                if (img == null && card.parent() != null) {
                    img = card.parent().selectFirst("img");
                }
                if (img != null) {
                    logoUrl = img.absUrl("src");
                }
            }

            if (teamName.isBlank() || sourceUrl.isBlank()) {
                continue;
            }

            VpfTeamCrawlDto dto = new VpfTeamCrawlDto();
            dto.setSeasonYear(request.getSeasonYear());
            dto.setSeasonName(request.getSeasonName());
            dto.setTeamName(teamName);
            dto.setNormalizedName(normalizeName(teamName));
            dto.setLogoUrl(logoUrl);
            dto.setSourceUrl(sourceUrl);
            dto.setSourceName(SOURCE_NAME);

            result.add(dto);
        }

        return cleanVpfTeams(result, request.getTeamSid());
    }
//    private List<VpfTeamCrawlDto> cleanVpfTeams(List<VpfTeamCrawlDto> rawTeams) {
//        Map<String, VpfTeamCrawlDto> uniqueTeams = new LinkedHashMap<>();
//
//        for (VpfTeamCrawlDto dto : rawTeams) {
//            if (dto.getSourceUrl() == null || dto.getSourceUrl().isBlank()) {
//                continue;
//            }
//
//            // Chỉ lấy đúng mùa V.League 1 2025/26
//            if (!dto.getSourceUrl().contains("sid=121994")) {
//                continue;
//            }
//
//            String key = removeQuery(dto.getSourceUrl());
//
//            VpfTeamCrawlDto existing = uniqueTeams.get(key);
//
//            if (existing == null || shouldReplaceTeam(existing, dto)) {
//                uniqueTeams.put(key, dto);
//            }
//        }
//
//        return new ArrayList<>(uniqueTeams.values());
//    }

    private List<VpfTeamCrawlDto> cleanVpfTeams(List<VpfTeamCrawlDto> rawTeams, String teamSid) {
        Map<String, VpfTeamCrawlDto> uniqueTeams = new LinkedHashMap<>();

        for (VpfTeamCrawlDto dto : rawTeams) {
            if (dto.getSourceUrl() == null || dto.getSourceUrl().isBlank()) {
                continue;
            }

            String sourceUrl = dto.getSourceUrl();

            if (teamSid != null && !teamSid.isBlank()) {
                if (!sourceUrl.contains("sid=")) {
                    sourceUrl = removeQuery(sourceUrl) + "?sid=" + teamSid;
                    dto.setSourceUrl(sourceUrl);
                }

                if (!sourceUrl.contains("sid=" + teamSid)) {
                    continue;
                }
            }

            String key = removeQuery(dto.getSourceUrl());

            VpfTeamCrawlDto existing = uniqueTeams.get(key);

            if (existing == null || shouldReplaceTeam(existing, dto)) {
                uniqueTeams.put(key, dto);
            }
        }

        return new ArrayList<>(uniqueTeams.values());
    }

    private String removeQuery(String url) {
        int index = url.indexOf("?");
        if (index == -1) {
            return url;
        }
        return url.substring(0, index);
    }

    private boolean shouldReplaceTeam(VpfTeamCrawlDto oldDto, VpfTeamCrawlDto newDto) {
        String oldName = oldDto.getTeamName() == null ? "" : oldDto.getTeamName().trim();
        String newName = newDto.getTeamName() == null ? "" : newDto.getTeamName().trim();

        boolean oldLooksLikeCode = isTeamCode(oldName);
        boolean newLooksLikeCode = isTeamCode(newName);

        if (oldLooksLikeCode && !newLooksLikeCode) {
            return true;
        }

        if (!oldLooksLikeCode && newLooksLikeCode) {
            return false;
        }

        // Nếu cả hai cùng loại, ưu tiên tên dài hơn
        return newName.length() > oldName.length();
    }

    private boolean isTeamCode(String name) {
        if (name == null || name.isBlank()) {
            return true;
        }

        // Ví dụ: CAHN, TCVT, TXND, HNFC, HPFC
        return name.matches("^[A-ZĐ]{2,8}$");
    }

//    =============== TEAM DETAILS===========


    private void applyTeamBasicInfo(Team team, VpfTeamCrawlDto dto) {
        if (dto.getTeamName() != null && !dto.getTeamName().isBlank()) {
            team.setName(dto.getTeamName());
            team.setNormalizedName(dto.getNormalizedName());
        }

        if (dto.getLogoUrl() != null && !dto.getLogoUrl().isBlank()) {
            team.setLogo(dto.getLogoUrl());
        }

        if (dto.getSourceUrl() != null && !dto.getSourceUrl().isBlank()) {
            team.setSourceUrl(dto.getSourceUrl());
        }

        team.setSourceName(SOURCE_NAME);
        team.setStatus("ACTIVE");
    }

    private void syncOneTeam(Season season, VpfTeamCrawlDto dto) {
        Team team = findExistingTeamForSync(dto);

        applyTeamBasicInfo(team, dto);

        Team savedTeam = teamRepository.save(team);

        seasonTeamRepository.findBySeasonAndTeam(season, savedTeam)
                .orElseGet(() -> {
                    SeasonTeam seasonTeam = new SeasonTeam();
                    seasonTeam.setSeason(season);
                    seasonTeam.setTeam(savedTeam);
                    seasonTeam.setStatus("ACTIVE");
                    return seasonTeamRepository.save(seasonTeam);
                });
    }
    private Team syncOneTeamAndReturn(Season season, VpfTeamCrawlDto dto) {
        Team team = findExistingTeamForSync(dto);

        applyTeamBasicInfo(team, dto);

        Team savedTeam = teamRepository.save(team);

        seasonTeamRepository.findBySeasonAndTeam(season, savedTeam)
                .orElseGet(() -> {
                    SeasonTeam seasonTeam = new SeasonTeam();
                    seasonTeam.setSeason(season);
                    seasonTeam.setTeam(savedTeam);
                    seasonTeam.setStatus("ACTIVE");
                    return seasonTeamRepository.save(seasonTeam);
                });

        return savedTeam;
    }
    private void syncOneTeamDetail(Season season, Team team, VpfTeamDetailCrawlDto detailDto) {
        if (detailDto == null) {
            return;
        }

        if (detailDto.getTeamName() != null && !detailDto.getTeamName().isBlank()) {
            team.setName(detailDto.getTeamName());
            team.setNormalizedName(detailDto.getNormalizedName());
        }

        if (detailDto.getLogoUrl() != null && !detailDto.getLogoUrl().isBlank()) {
            team.setLogo(detailDto.getLogoUrl());
        }

        if (detailDto.getDescription() != null && !detailDto.getDescription().isBlank()) {
            team.setDescription(detailDto.getDescription());
        }

        if (detailDto.getHomeStadiumName() != null && !detailDto.getHomeStadiumName().isBlank()) {
            Stadium stadium = findOrCreateStadium(detailDto.getHomeStadiumName());

            stadium.setCapacity(detailDto.getStadiumCapacity());
            stadium.setAddress(detailDto.getStadiumAddress());
            stadium.setImageUrl(detailDto.getStadiumImageUrl());

            Stadium savedStadium = stadiumRepository.save(stadium);

            team.setStadium(savedStadium);
        }


        Team savedTeam = teamRepository.save(team);
        System.out.println("Team detail parsed: "
                + savedTeam.getName()
                + ", stadium="
                + detailDto.getHomeStadiumName()
                + ", players="
                + (detailDto.getPlayers() == null ? 0 : detailDto.getPlayers().size()));

        syncCoachForTeam(season, savedTeam, detailDto);
        syncPlayersForTeam(season, savedTeam, detailDto.getPlayers());

    }

    private void syncPlayersForTeam(Season season, Team team, List<VpfPlayerCrawlDto> players) {
        if (players == null || players.isEmpty()) {
            return;
        }

        for (VpfPlayerCrawlDto dto : players) {
            if (dto.getFullName() == null || dto.getFullName().isBlank()) {
                continue;
            }

            // Lọc staff/HLV/quan chức, chỉ lưu cầu thủ thật
            if (!isRealPlayer(dto)) {
                continue;
            }

            Player player = findOrCreatePlayer(dto);

            applyPlayerInfo(player, dto, team);

            Player savedPlayer = playerRepository.save(player);

            syncPlayerSeason(season, team, savedPlayer, dto);

            syncInitialPlayerStats(season, savedPlayer, dto);
        }
    }

//    private void syncInitialPlayerStats(Season season, Player player, VpfPlayerCrawlDto dto) {
//        PlayerStats stats = playerStatsRepository
//                .findByPlayerAndSeason(player, season)
//                .orElseGet(() -> {
//                    PlayerStats newStats = new PlayerStats();
//                    newStats.setPlayer(player);
//                    newStats.setSeason(season);
//                    return newStats;
//                });
//
//        stats.setGoals(dto.getGoals() == null ? 0 : dto.getGoals());
//        stats.setAssists(0);
//        stats.setAppearances(0);
//        stats.setMinutesPlayed(0);
//        stats.setYellowCards(dto.getYellowCards() == null ? 0 : dto.getYellowCards());
//        stats.setRedCards(dto.getRedCards() == null ? 0 : dto.getRedCards());
//
//        playerStatsRepository.save(stats);
//    }
private void syncInitialPlayerStats(Season season, Player player, VpfPlayerCrawlDto dto) {
    Optional<PlayerStats> existingStats = playerStatsRepository.findByPlayerAndSeason(player, season);

    PlayerStats stats = existingStats.orElseGet(() -> {
        PlayerStats newStats = new PlayerStats();
        newStats.setPlayer(player);
        newStats.setSeason(season);
        return newStats;
    });

    boolean isNew = existingStats.isEmpty();

    boolean statsLooksEmpty =
            safe(stats.getGoals()) == 0
                    && safe(stats.getAssists()) == 0
                    && safe(stats.getAppearances()) == 0
                    && safe(stats.getMinutesPlayed()) == 0
                    && safe(stats.getYellowCards()) == 0
                    && safe(stats.getRedCards()) == 0;

    boolean dtoHasStats =
            safe(dto.getGoals()) > 0
                    || safe(dto.getYellowCards()) > 0
                    || safe(dto.getRedCards()) > 0;

    if (!isNew && !(statsLooksEmpty && dtoHasStats)) {
        return;
    }

    stats.setGoals(safe(dto.getGoals()));
    stats.setAssists(0);
    stats.setAppearances(0);
    stats.setMinutesPlayed(0);
    stats.setYellowCards(safe(dto.getYellowCards()));
    stats.setRedCards(safe(dto.getRedCards()));

    playerStatsRepository.save(stats);
}

    private int safe(Integer value) {
        return value == null ? 0 : value;
    }
    private boolean isRealPlayer(VpfPlayerCrawlDto dto) {
        String position = dto.getPosition();

        if (position == null || position.isBlank()) {
            return false;
        }

        String normalizedPosition = normalizeName(position);

        // Các dòng staff thường có chữ này
        if (normalizedPosition.contains("huan luyen")
                || normalizedPosition.contains("tro ly")
                || normalizedPosition.contains("quan chuc")
                || normalizedPosition.contains("giam doc")
                || normalizedPosition.contains("can bo")
                || normalizedPosition.contains("hlv")) {
            return false;
        }

        // Nếu không có số áo thì nhiều khả năng là staff
        if (dto.getShirtNumber() == null) {
            return false;
        }

        return true;
    }

//    private Player findOrCreatePlayer(VpfPlayerCrawlDto dto) {
//        if (dto.getSourceUrl() != null && !dto.getSourceUrl().isBlank()) {
//            Optional<Player> bySourceUrl = playerRepository.findBySourceUrl(dto.getSourceUrl());
//
//            if (bySourceUrl.isPresent()) {
//                return bySourceUrl.get();
//            }
//        }
//
//        if (dto.getNormalizedName() != null && !dto.getNormalizedName().isBlank()) {
//            Optional<Player> byNormalizedName = playerRepository.findByNormalizedName(dto.getNormalizedName());
//
//            if (byNormalizedName.isPresent()) {
//                return byNormalizedName.get();
//            }
//        }
//
//        return new Player();
//    }

    private Player findOrCreatePlayer(VpfPlayerCrawlDto dto) {
        String playerSlug = extractPlayerSlug(dto.getSourceUrl());

        if (playerSlug != null && !playerSlug.isBlank()) {
            Optional<Player> bySlug = playerRepository.findByVpfPlayerSlug(playerSlug);

            if (bySlug.isPresent()) {
                return bySlug.get();
            }
        }

        if (dto.getNormalizedName() != null
                && !dto.getNormalizedName().isBlank()
                && dto.getDateOfBirth() != null) {

            Optional<Player> byNameAndDob =
                    playerRepository.findByNormalizedNameAndDateOfBirth(
                            dto.getNormalizedName(),
                            dto.getDateOfBirth()
                    );

            if (byNameAndDob.isPresent()) {
                return byNameAndDob.get();
            }
        }

        return new Player();
    }
    private void applyPlayerInfo(Player player, VpfPlayerCrawlDto dto, Team team) {
        player.setName(dto.getFullName());
        player.setNormalizedName(dto.getNormalizedName());
        player.setTeam(team);

        player.setVpfPlayerSlug(extractPlayerSlug(dto.getSourceUrl()));

        if (dto.getImageUrl() != null && !dto.getImageUrl().isBlank()) {
            player.setAvatar(dto.getImageUrl());
        }

        player.setSourceUrl(dto.getSourceUrl());
        player.setSourceName(SOURCE_NAME);

        player.setPosition(dto.getPosition());
        player.setShirtNumber(dto.getShirtNumber());

        if (dto.getDateOfBirth() != null) {
            player.setDateOfBirth(dto.getDateOfBirth());
        }

        if (dto.getHeightCm() != null) {
            player.setHeight(dto.getHeightCm());
        }

        if (dto.getWeightKg() != null) {
            player.setWeight(dto.getWeightKg());
        }

        player.setStatus("ACTIVE");
    }

//    private void syncPlayerSeason(Season season, Team team, Player player, VpfPlayerCrawlDto dto) {
//        SeasonTeam seasonTeam = findOrCreateSeasonTeam(season, team);
//
//        PlayerSeason playerSeason = playerSeasonRepository
//                .findByPlayerAndSeason(player, season)
//                .orElseGet(() -> {
//                    PlayerSeason newPlayerSeason = new PlayerSeason();
//                    newPlayerSeason.setPlayer(player);
//                    newPlayerSeason.setSeason(season);
//                    return newPlayerSeason;
//                });
//
//        playerSeason.setTeam(team);
//        playerSeason.setTeamSeason(seasonTeam);
//
//        playerSeason.setShirtNumber(dto.getShirtNumber());
//        playerSeason.setPrimaryPosition(dto.getPosition());
//        playerSeason.setStatus("ACTIVE");
//
//        playerSeasonRepository.save(playerSeason);
//    }

    private void syncPlayerSeason(Season season, Team team, Player player, VpfPlayerCrawlDto dto) {
        SeasonTeam seasonTeam = findOrCreateSeasonTeam(season, team);

        Optional<PlayerSeason> byPlayerAndSeason =
                playerSeasonRepository.findByPlayerAndSeason(player, season);

        PlayerSeason playerSeason;

        if (byPlayerAndSeason.isPresent()) {
            playerSeason = byPlayerAndSeason.get();
        } else {
            Optional<PlayerSeason> byShirtNumber = Optional.empty();

            if (dto.getShirtNumber() != null) {
                byShirtNumber = playerSeasonRepository.findByTeamSeasonAndShirtNumber(
                        seasonTeam,
                        dto.getShirtNumber()
                );
            }

            if (byShirtNumber.isPresent()) {
                PlayerSeason existing = byShirtNumber.get();

                System.out.println("Duplicate shirt number detected: team="
                        + team.getName()
                        + ", season="
                        + season.getName()
                        + ", shirt="
                        + dto.getShirtNumber()
                        + ", existingPlayer="
                        + existing.getPlayer().getName()
                        + ", newPlayer="
                        + player.getName());

                // Trong giai đoạn crawl, nên bỏ qua để không crash toàn bộ sync
                return;
            }

            playerSeason = new PlayerSeason();
            playerSeason.setPlayer(player);
            playerSeason.setSeason(season);
        }

        playerSeason.setTeam(team);
        playerSeason.setTeamSeason(seasonTeam);
        playerSeason.setShirtNumber(dto.getShirtNumber());
        playerSeason.setPrimaryPosition(dto.getPosition());
        playerSeason.setStatus("ACTIVE");

        playerSeasonRepository.save(playerSeason);
    }

    public VpfTeamDetailCrawlDto previewTeamDetail(String teamUrl) {
        try {
            Document doc = Jsoup.connect(teamUrl)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .referrer("https://vpf.vn/")
                    .header("Accept-Language", "vi-VN,vi;q=0.9,en-US;q=0.8")
                    .timeout(60000)
                    .get();

            return parseTeamDetail(doc, teamUrl);

        } catch (Exception e) {
            throw new RuntimeException("Không thể preview chi tiết đội bóng từ VPF.", e);
        }
    }

    private VpfTeamDetailCrawlDto parseTeamDetail(Document doc, String teamUrl) {
        VpfTeamDetailCrawlDto dto = new VpfTeamDetailCrawlDto();

        String teamName = doc.selectFirst(".jo-team-header .entry-title") != null
                ? doc.selectFirst(".jo-team-header .entry-title").text().trim()
                : "";

        String logoUrl = "";
        Element logoImg = doc.selectFirst(".jo-team-logo .td-post-featured-image img");
        if (logoImg != null) {
            logoUrl = logoImg.absUrl("src");
        }

        dto.setTeamName(teamName);
        dto.setNormalizedName(normalizeName(teamName));
        dto.setLogoUrl(logoUrl);
        dto.setSourceUrl(teamUrl);

        parseTeamHeaderInfo(doc, dto);
        parseVenueInfo(doc, dto);
        parseDescription(doc, dto);
        dto.setPlayers(parsePlayers(doc));

        return dto;
    }

    private void parseTeamHeaderInfo(Document doc, VpfTeamDetailCrawlDto dto) {
        Elements infoBlocks = doc.select(".jo-team-header-info-text");

        for (Element block : infoBlocks) {
            String title = block.select(".jo-title").text().trim();
            String value = block.select("strong").text().trim();

            if (title.equalsIgnoreCase("Sân Nhà")) {
                dto.setHomeStadiumName(parseStadiumName(value));
                dto.setStadiumCapacity(parseCapacity(value));
            }

            if (title.equalsIgnoreCase("Huấn luyện viên")) {
                dto.setCoachName(value);
            }
        }
    }

    private String parseStadiumName(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }

        int index = text.indexOf("(");
        if (index >= 0) {
            return text.substring(0, index).trim();
        }

        return text.trim();
    }

    private Integer parseCapacity(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }

        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("Sức chứa:\\s*([\\d\\.]+)");
        java.util.regex.Matcher matcher = pattern.matcher(text);

        if (matcher.find()) {
            return parseInteger(matcher.group(1));
        }

        return null;
    }

    private void parseVenueInfo(Document doc, VpfTeamDetailCrawlDto dto) {
        Element venueTab = doc.selectFirst("#stab_venue");
        if (venueTab == null) {
            return;
        }

        String venueName = venueTab.select(".venue-info-title").text().trim();
        if (!venueName.isBlank()) {
            dto.setHomeStadiumName(venueName);
        }

        for (Element li : venueTab.select(".venue-info-list li")) {
            String text = li.text().trim();

            if (text.startsWith("Địa chỉ:")) {
                dto.setStadiumAddress(text.replace("Địa chỉ:", "").trim());
            }

            if (text.startsWith("Sức chứa:")) {
                dto.setStadiumCapacity(parseInteger(text));
            }
        }

        Element venueLink = venueTab.selectFirst(".venue-gallery a");
        if (venueLink != null) {
            dto.setStadiumImageUrl(venueLink.absUrl("href"));
        } else {
            Element venueImg = venueTab.selectFirst(".venue-gallery img");
            if (venueImg != null) {
                dto.setStadiumImageUrl(venueImg.absUrl("src"));
            }
        }
    }

    private void parseDescription(Document doc, VpfTeamDetailCrawlDto dto) {
        Element mainTab = doc.selectFirst("#stab_main");
        if (mainTab == null) {
            return;
        }

        String description = mainTab.text().trim();
        dto.setDescription(description);
    }
//    private List<VpfPlayerCrawlDto> parsePlayers(Document doc) {
//        List<VpfPlayerCrawlDto> players = new ArrayList<>();
//
//        Elements rows = doc.select("#jstable_plz tbody tr");
//
//        for (Element row : rows) {
//            Elements cols = row.select("td");
//
//            if (cols.size() < 11) {
//                continue;
//            }
//
//            Element nameLink = cols.get(0).selectFirst(".js_div_particName a");
//            if (nameLink == null) {
//                continue;
//            }
//
//            String fullName = nameLink.text().trim();
//            String sourceUrl = nameLink.absUrl("href");
//
//            String imageUrl = "";
//            Element img = cols.get(0).selectFirst("img");
//            if (img != null) {
//                imageUrl = img.absUrl("src");
//            }
//
//            VpfPlayerCrawlDto dto = new VpfPlayerCrawlDto();
//            dto.setFullName(fullName);
//            dto.setNormalizedName(normalizeName(fullName));
//            dto.setSourceUrl(sourceUrl);
//            dto.setImageUrl(imageUrl);
//
//            dto.setPosition(cols.get(1).text().trim());
//            dto.setShirtNumber(parseInteger(cols.get(2).text()));
//            dto.setHeightCm(parseInteger(cols.get(3).text()));
//            dto.setWeightKg(parseInteger(cols.get(4).text()));
//            dto.setDateOfBirth(parseVpfDate(cols.get(5).text()));
//
//            dto.setGoals(parseIntegerDefaultZero(cols.get(6).text()));
//            dto.setPenalties(parseIntegerDefaultZero(cols.get(7).text()));
//            dto.setOwnGoals(parseIntegerDefaultZero(cols.get(8).text()));
//            dto.setYellowCards(parseIntegerDefaultZero(cols.get(9).text()));
//            dto.setRedCards(parseIntegerDefaultZero(cols.get(10).text()));
//
//            players.add(dto);
//        }
//
//        return players;
//    }
private List<VpfPlayerCrawlDto> parsePlayers(Document doc) {
    List<VpfPlayerCrawlDto> players = new ArrayList<>();

    Elements rows = doc.select("#jstable_plz tbody tr");

    System.out.println("parsePlayers rows = " + rows.size());

    for (Element row : rows) {
        Elements cols = row.select("td");

        // Hạng Nhất có 10 cột, V.League 1 có thể có 11 cột
        if (cols.size() < 10) {
            continue;
        }

        Element nameLink = cols.get(0).selectFirst(".js_div_particName a");
        if (nameLink == null) {
            nameLink = cols.get(0).selectFirst("a[href*='/player/']");
        }

        if (nameLink == null) {
            continue;
        }

        String fullName = nameLink.text().trim();
        String sourceUrl = nameLink.absUrl("href");

        if (fullName.isBlank()) {
            continue;
        }

        String imageUrl = "";
        Element img = cols.get(0).selectFirst("img");
        if (img != null) {
            imageUrl = img.absUrl("src");
        }

        VpfPlayerCrawlDto dto = new VpfPlayerCrawlDto();

        dto.setFullName(fullName);
        dto.setNormalizedName(normalizeName(fullName));
        dto.setSourceUrl(sourceUrl);
        dto.setImageUrl(imageUrl);

        dto.setPosition(getColumnText(cols, 1));
        dto.setShirtNumber(parseInteger(getColumnText(cols, 2)));
        dto.setHeightCm(parseInteger(getColumnText(cols, 3)));
        dto.setWeightKg(parseInteger(getColumnText(cols, 4)));
        dto.setDateOfBirth(parseVpfDate(getColumnText(cols, 5)));

        dto.setGoals(parseIntegerDefaultZero(getColumnText(cols, 6)));

        if (cols.size() >= 11) {
            // Dạng cũ: Có cột penalties
            dto.setPenalties(parseIntegerDefaultZero(getColumnText(cols, 7)));
            dto.setOwnGoals(parseIntegerDefaultZero(getColumnText(cols, 8)));
            dto.setYellowCards(parseIntegerDefaultZero(getColumnText(cols, 9)));
            dto.setRedCards(parseIntegerDefaultZero(getColumnText(cols, 10)));
        } else {
            // Dạng Hạng Nhất: Không có cột penalties
            dto.setPenalties(0);
            dto.setOwnGoals(parseIntegerDefaultZero(getColumnText(cols, 7)));
            dto.setYellowCards(parseIntegerDefaultZero(getColumnText(cols, 8)));
            dto.setRedCards(parseIntegerDefaultZero(getColumnText(cols, 9)));
        }

        players.add(dto);
    }

    System.out.println("parsePlayers result = " + players.size());

    return players;
}

    private String getColumnText(Elements cols, int index) {
        if (cols == null || index < 0 || index >= cols.size()) {
            return "";
        }

        return cols.get(index).text().trim();
    }
    private LocalDate parseVpfDate(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }

        try {
            String normalized = text.trim().replace("/", "-");
            String[] parts = normalized.split("-");

            if (parts.length != 3) {
                return null;
            }

            int day = Integer.parseInt(parts[0]);
            int month = Integer.parseInt(parts[1]);
            int year = Integer.parseInt(parts[2]);

            return LocalDate.of(year, month, day);
        } catch (Exception e) {
            return null;
        }
    }

    private Integer parseIntegerDefaultZero(String text) {
        Integer value = parseInteger(text);
        return value == null ? 0 : value;
    }

    private Integer parseInteger(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }

        String numberOnly = text.replace(".", "").replaceAll("[^0-9]", "");

        if (numberOnly.isBlank()) {
            return null;
        }

        return Integer.parseInt(numberOnly);
    }


    private Team findExistingTeamForSync(VpfTeamCrawlDto dto) {
        if (dto.getNormalizedName() != null && !dto.getNormalizedName().isBlank()) {
            Optional<Team> byNormalizedName =
                    teamRepository.findByNormalizedName(dto.getNormalizedName());

            if (byNormalizedName.isPresent()) {
                return byNormalizedName.get();
            }
        }

        if (dto.getTeamName() != null && !dto.getTeamName().isBlank()) {
            Optional<Team> byName =
                    teamRepository.findByNameIgnoreCase(dto.getTeamName());

            if (byName.isPresent()) {
                return byName.get();
            }
        }

        return new Team();
    }



//    =================//

    @Transactional
    public void syncFullSeason(VpfSeasonSyncRequest request) {
        syncVLeagueTeams(request);
        syncVLeagueTeamDetails(request);
        syncVLeagueCalendar(request);
    }


    private Document loadVpfTeamsDocument(String teamsUrl) {
        try {
            return Jsoup.connect(teamsUrl)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .referrer("https://vpf.vn/")
                    .header("Accept-Language", "vi-VN,vi;q=0.9,en-US;q=0.8")
                    .timeout(60000)
                    .get();
        } catch (Exception e) {
            throw new RuntimeException("Không thể tải danh sách đội từ VPF: " + teamsUrl, e);
        }
    }


    private String extractPlayerSlug(String sourceUrl) {
        if (sourceUrl == null || sourceUrl.isBlank()) {
            return null;
        }

        String noQuery = removeQuery(sourceUrl);

        String marker = "/player/";
        int index = noQuery.indexOf(marker);

        if (index < 0) {
            return null;
        }

        String slug = noQuery.substring(index + marker.length());

        if (slug.endsWith("/")) {
            slug = slug.substring(0, slug.length() - 1);
        }

        return slug.isBlank() ? null : slug;
    }

}
