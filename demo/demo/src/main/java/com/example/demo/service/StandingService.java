package com.example.demo.service;

import com.example.demo.controller.StandingController;
import com.example.demo.dao.StandingRepository;
import com.example.demo.dao.match.MatchRepository;
import com.example.demo.dao.season.SeasonRepository;
import com.example.demo.dao.season.SeasonTeamRepository;
import com.example.demo.dao.team.TeamRepository;

import com.example.demo.entity.*;
import com.example.demo.entity.team.Team;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Service
@AllArgsConstructor
public class StandingService {

    private final StandingRepository standingRepository;
    private final SeasonTeamRepository seasonTeamRepository;
    private final SeasonRepository seasonRepository;
    private final TeamRepository teamRepository;
    private final MatchRepository matchRepository;





    public List<StandingController.StandingResponse> getStandings(Long seasonId) {
        List<Standing> standings;

        if (seasonId == null) {
            standings = standingRepository.findAll().stream()
                    .sorted(Comparator
                            .comparing((Standing standing) -> standing.getSeason() != null ? standing.getSeason().getId() : Long.MAX_VALUE)
                            .thenComparing(Standing::getRank, Comparator.nullsLast(Integer::compareTo)))
                    .toList();
        } else {
            Season season = seasonRepository.findById(seasonId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy mùa giải"));

            SystemRule rule = season.getSystemRule();

            List<Standing> currentStandings = standingRepository.findBySeasonId(seasonId);

            standings = rule != null
                    ? sortStandingsByRule(currentStandings, rule)
                    : currentStandings.stream()
                    .sorted(Comparator
                            .comparing(Standing::getPoints, Comparator.nullsLast(Comparator.reverseOrder()))
                            .thenComparing(Standing::getGoalDifference, Comparator.nullsLast(Comparator.reverseOrder()))
                            .thenComparing(Standing::getGoalsFor, Comparator.nullsLast(Comparator.reverseOrder())))
                    .toList();
        }

        return standings.stream()
                .map(this::toStandingResponse)
                .toList();
    }
    public StandingController.StandingResponse getStanding(Long id) {
        Standing standing = standingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Standing not found with id = " + id));
        return toStandingResponse(standing);
    }

    @Transactional
    public void initializeStanding(Long seasonId, Long teamId) {
        if (standingRepository.existsBySeasonIdAndTeamId(seasonId, teamId)) {
            return;
        }

        Season season = seasonRepository.findById(seasonId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy mùa giải"));
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đội bóng"));

        Standing standing = new Standing();
        standing.setSeason(season);
        standing.setTeam(team);
        standing.setPlayed(0);
        standing.setWin(0);
        standing.setDraw(0);
        standing.setLose(0);
        standing.setGoalsFor(0);
        standing.setGoalsAgainst(0);
        standing.setGoalDifference(0);
        standing.setPoints(0);
        standing.setCurrentRank(0);
        standing.setRecentForm("");

        standingRepository.save(standing);
    }

    private StandingController.StandingResponse toStandingResponse(Standing standing) {
        return new StandingController.StandingResponse(
                standing.getId(),
                standing.getSeason() != null ? standing.getSeason().getId() : null,
                standing.getSeason() != null ? standing.getSeason().getName() : null,
                standing.getTeam() != null ? standing.getTeam().getId() : null,
                standing.getTeam() != null ? standing.getTeam().getName() : null,
                standing.getPlayed(),
                standing.getWin(),
                standing.getDraw(),
                standing.getLose(),
                standing.getGoalsFor(),
                standing.getGoalsAgainst(),
                standing.getGoalDifference(),
                standing.getPoints(),
                standing.getRank(),
                standing.getCurrentRank(),
                standing.getRecentForm()
        );
    }

    @Transactional
    public List<StandingController.StandingResponse> recalculateBySeason(Long seasonId) {
        Season season = seasonRepository.findById(seasonId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy mùa giải"));
        SystemRule rule = season.getSystemRule();


        if (rule == null) {
            throw new RuntimeException("Mùa giải chưa được cấu hình bộ luật");
        }

        // 1. Đảm bảo mọi đội tham gia mùa giải đều có Standing
        List<SeasonTeam> seasonTeams = seasonTeamRepository.findBySeasonId(seasonId);

        for (SeasonTeam seasonTeam : seasonTeams) {
            Team team = seasonTeam.getTeam();

            Standing standing = standingRepository.findBySeasonIdAndTeamId(seasonId, team.getId())
                    .orElseGet(() -> {
                        Standing newStanding = new Standing();
                        newStanding.setSeason(season);
                        newStanding.setTeam(team);
                        return newStanding;
                    });

            resetStanding(standing);
            standingRepository.save(standing);
        }

        // 2. Lấy tất cả trận đã kết thúc
        List<Match> finishedMatches = matchRepository.findBySeasonIdAndStatus(seasonId, MatchStatus.FINISHED);

        // 3. Cộng dữ liệu từng trận vào standing
        for (Match match : finishedMatches) {
            if (match.getHomeScore() == null || match.getAwayScore() == null) {
                continue;
            }

            Team homeTeam = match.getHomeTeam().getTeam();
            Team awayTeam = match.getAwayTeam().getTeam();

            Standing homeStanding = standingRepository.findBySeasonIdAndTeamId(seasonId, homeTeam.getId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy standing đội nhà: " + homeTeam.getName()));

            Standing awayStanding = standingRepository.findBySeasonIdAndTeamId(seasonId, awayTeam.getId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy standing đội khách: " + awayTeam.getName()));

            applyMatchResult(homeStanding, awayStanding, match.getHomeScore(), match.getAwayScore(),rule);

            standingRepository.save(homeStanding);
            standingRepository.save(awayStanding);
        }

        // 4. Sắp xếp và cập nhật rank
//        List<Standing> sortedStandings = standingRepository
//                .findBySeasonIdOrderByPointsDescGoalDifferenceDescGoalsForDesc(seasonId);
//
//        int rank = 1;
//        for (Standing standing : sortedStandings) {
//            standing.setRank(rank);
//            standing.setCurrentRank(rank);
//            standingRepository.save(standing);
//            rank++;
//        }
//
//        return getStandings(seasonId);
        // 4. Sắp xếp theo rule và cập nhật rank
        List<Standing> currentStandings = standingRepository.findBySeasonId(seasonId);



// Nếu mùa giải đã kết thúc thì xếp hạng cuối mùa.
// Nếu mùa giải đang diễn ra thì cho phép đồng hạng khi bằng điểm + hiệu số.
        boolean finalRanking = isFinalRanking(season);

        List<Standing> sortedStandings = finalRanking
                ? sortFinalStandings(currentStandings, season)
                : sortInProgressStandings(currentStandings);

        assignRanks(sortedStandings, finalRanking);

        standingRepository.saveAll(sortedStandings);

        return sortedStandings.stream()
                .map(this::toStandingResponse)
                .toList();
    }

    private List<Standing> sortInProgressStandings(List<Standing> standings) {
        return standings.stream()
                .sorted(
                        Comparator
                                .comparing(Standing::getPoints, Comparator.nullsFirst(Integer::compareTo)).reversed()
                                .thenComparing(Standing::getGoalDifference, Comparator.nullsFirst(Integer::compareTo).reversed())
                )
                .toList();
    }
    private List<Standing> sortFinalStandings(List<Standing> standings, Season season) {
        return standings.stream()
                .sorted((a, b) -> compareFinalStandingByRule(a, b, season))
                .toList();
    }

    private int compareFinalStandingByRule(Standing a, Standing b, Season season) {
        String order = getRankingCriteriaOrder(season);
        return compareStandingByOrder(a, b, order, season);
    }

    private String getRankingCriteriaOrder(Season season) {
        if (season == null || season.getSystemRule() == null) {
            return "POINTS,GOAL_DIFFERENCE,HEAD_TO_HEAD,DRAW_LOT";
        }

        String order = season.getSystemRule().getRankingCriteriaOrder();

        if (order == null || order.isBlank()) {
            return "POINTS,GOAL_DIFFERENCE,HEAD_TO_HEAD,DRAW_LOT";
        }

        return order;
    }

    private int compareDrawLot(Standing a, Standing b) {
        return compareAsc(
                a.getDrawLotOrder() != null ? a.getDrawLotOrder() : Integer.MAX_VALUE,
                b.getDrawLotOrder() != null ? b.getDrawLotOrder() : Integer.MAX_VALUE
        );
    }

    private int compareDesc(Integer a, Integer b) {
        return Integer.compare(b != null ? b : 0, a != null ? a : 0);
    }

    private int compareAsc(Integer a, Integer b) {
        return Integer.compare(a != null ? a : Integer.MAX_VALUE, b != null ? b : Integer.MAX_VALUE);
    }


    private int compareHeadToHead(Standing a, Standing b, Season season) {
        if (a.getTeam() == null || b.getTeam() == null || season == null) {
            return 0;
        }

        Long teamAId = a.getTeam().getId();
        Long teamBId = b.getTeam().getId();

        List<Match> matches = matchRepository.findHeadToHeadMatches(
                season.getId(),
                teamAId,
                teamBId
        );

        if (matches.isEmpty()) {
            return 0;
        }

        int teamAGoals = 0;
        int teamBGoals = 0;

        for (Match match : matches) {
            int homeScore = match.getHomeScore() != null ? match.getHomeScore() : 0;
            int awayScore = match.getAwayScore() != null ? match.getAwayScore() : 0;

            Long homeTeamId = match.getHomeTeam().getTeam().getId();
            Long awayTeamId = match.getAwayTeam().getTeam().getId();

            if (homeTeamId.equals(teamAId)) {
                teamAGoals += homeScore;
                teamBGoals += awayScore;
            } else if (homeTeamId.equals(teamBId)) {
                teamBGoals += homeScore;
                teamAGoals += awayScore;
            }
        }

        return Integer.compare(teamBGoals, teamAGoals);
    }

    private boolean isFinalRanking(Season season) {
        if (season == null) {
            return false;
        }

//        if (season.get() != null) {
//            String status = season.getStatus().toString();
//            if ("FINISHED".equalsIgnoreCase(status)
//                    || "COMPLETED".equalsIgnoreCase(status)
//                    || "ENDED".equalsIgnoreCase(status)) {
//                return true;
//            }
//        }

        if (season.getEndDate() != null) {
            return LocalDate.now().isAfter(season.getEndDate());
        }

        return false;
    }

    private List<Standing> sortStandingsByRule(List<Standing> standings, SystemRule rule) {
        String order = rule != null ? rule.getRankingCriteriaOrder() : null;

        if (order == null || order.isBlank()) {
            order = "POINTS,GOAL_DIFFERENCE,HEAD_TO_HEAD,DRAW_LOT";
        }

        final String finalOrder = order;

        return standings.stream()
                .sorted((a, b) -> compareStandingByOrder(a, b, finalOrder, a.getSeason()))
                .toList();
    }

    private int compareStandingByOrder(
            Standing a,
            Standing b,
            String order,
            Season season
    ) {
        for (String rawCriterion : order.split(",")) {
            String criterion = rawCriterion.trim().toUpperCase();

            int result = switch (criterion) {
                case "POINTS" -> compareDesc(a.getPoints(), b.getPoints());

                case "GOAL_DIFFERENCE" -> compareDesc(
                        a.getGoalDifference(),
                        b.getGoalDifference()
                );

                case "GOALS_FOR" -> compareDesc(
                        a.getGoalsFor(),
                        b.getGoalsFor()
                );

                case "HEAD_TO_HEAD" -> compareHeadToHead(a, b, season);

                case "DRAW_LOT" -> compareDrawLot(a, b);

                default -> 0;
            };

            if (result != 0) {
                return result;
            }
        }

        return compareAsc(
                a.getTeam() != null ? a.getTeam().getId().intValue() : Integer.MAX_VALUE,
                b.getTeam() != null ? b.getTeam().getId().intValue() : Integer.MAX_VALUE
        );
    }
//    private void assignRanks(List<Standing> standings, boolean finalRanking) {
//        int displayedRank = 1;
//
//        for (int i = 0; i < standings.size(); i++) {
//            Standing cur = standings.get(i);
//
//            if (!finalRanking && i > 0) {
//                Standing prev = standings.get(i - 1);
//
//                if (Objects.equals(prev.getPoints(), cur.getPoints())
//                        && Objects.equals(prev.getGoalDifference(), cur.getGoalDifference())) {
//                    cur.setRank(prev.getRank());
//                    cur.setCurrentRank(prev.getCurrentRank());
//                    continue;
//                }
//            }
//
//            displayedRank = i + 1;
//            cur.setRank(displayedRank);
//            cur.setCurrentRank(displayedRank);
//        }
//    }

    private void assignRanks(List<Standing> standings, boolean finalRanking) {
        for (int i = 0; i < standings.size(); i++) {
            Standing current = standings.get(i);

            if (!finalRanking && i > 0) {
                Standing previous = standings.get(i - 1);

                boolean samePoints = Objects.equals(previous.getPoints(), current.getPoints());
                boolean sameGoalDiff = Objects.equals(previous.getGoalDifference(), current.getGoalDifference());

                if (samePoints && sameGoalDiff) {
                    current.setRank(previous.getRank());
                    current.setCurrentRank(previous.getCurrentRank());
                    continue;
                }
            }

            current.setRank(i + 1);
            current.setCurrentRank(i + 1);
        }
    }

    private void resetStanding(Standing standing) {
        standing.setPlayed(0);
        standing.setWin(0);
        standing.setDraw(0);
        standing.setLose(0);
        standing.setGoalsFor(0);
        standing.setGoalsAgainst(0);
        standing.setGoalDifference(0);
        standing.setPoints(0);
        standing.setRank(0);
        standing.setCurrentRank(0);
        standing.setRecentForm("");
    }

//    private void applyMatchResult(Standing homeStanding,
//                                  Standing awayStanding,
//                                  Integer homeScore,
//                                  Integer awayScore) {
//        homeStanding.setPlayed(homeStanding.getPlayed() + 1);
//        awayStanding.setPlayed(awayStanding.getPlayed() + 1);
//
//        homeStanding.setGoalsFor(homeStanding.getGoalsFor() + homeScore);
//        homeStanding.setGoalsAgainst(homeStanding.getGoalsAgainst() + awayScore);
//
//        awayStanding.setGoalsFor(awayStanding.getGoalsFor() + awayScore);
//        awayStanding.setGoalsAgainst(awayStanding.getGoalsAgainst() + homeScore);
//
//        if (homeScore > awayScore) {
//            homeStanding.setWin(homeStanding.getWin() + 1);
//            homeStanding.setPoints(homeStanding.getPoints() + 3);
//            homeStanding.setRecentForm(appendRecentForm(homeStanding.getRecentForm(), "W"));
//
//            awayStanding.setLose(awayStanding.getLose() + 1);
//            awayStanding.setRecentForm(appendRecentForm(awayStanding.getRecentForm(), "L"));
//        } else if (homeScore < awayScore) {
//            awayStanding.setWin(awayStanding.getWin() + 1);
//            awayStanding.setPoints(awayStanding.getPoints() + 3);
//            awayStanding.setRecentForm(appendRecentForm(awayStanding.getRecentForm(), "W"));
//
//            homeStanding.setLose(homeStanding.getLose() + 1);
//            homeStanding.setRecentForm(appendRecentForm(homeStanding.getRecentForm(), "L"));
//        } else {
//            homeStanding.setDraw(homeStanding.getDraw() + 1);
//            homeStanding.setPoints(homeStanding.getPoints() + 1);
//            homeStanding.setRecentForm(appendRecentForm(homeStanding.getRecentForm(), "D"));
//
//            awayStanding.setDraw(awayStanding.getDraw() + 1);
//            awayStanding.setPoints(awayStanding.getPoints() + 1);
//            awayStanding.setRecentForm(appendRecentForm(awayStanding.getRecentForm(), "D"));
//        }
//
//        homeStanding.updateGoalDifference();
//        awayStanding.updateGoalDifference();
//    }


    private void applyMatchResult(
            Standing homeStanding,
            Standing awayStanding,
            Integer homeScore,
            Integer awayScore,
            SystemRule rule
    ) {
        int winPoints = rule.getWinPoints() != null ? rule.getWinPoints() : 3;
        int drawPoints = rule.getDrawPoints() != null ? rule.getDrawPoints() : 1;
        int losePoints = rule.getLosePoints() != null ? rule.getLosePoints() : 0;

        homeStanding.setPlayed(homeStanding.getPlayed() + 1);
        awayStanding.setPlayed(awayStanding.getPlayed() + 1);

        homeStanding.setGoalsFor(homeStanding.getGoalsFor() + homeScore);
        homeStanding.setGoalsAgainst(homeStanding.getGoalsAgainst() + awayScore);

        awayStanding.setGoalsFor(awayStanding.getGoalsFor() + awayScore);
        awayStanding.setGoalsAgainst(awayStanding.getGoalsAgainst() + homeScore);

        if (homeScore > awayScore) {
            homeStanding.setWin(homeStanding.getWin() + 1);
            homeStanding.setPoints(homeStanding.getPoints() + winPoints);
            homeStanding.setRecentForm(appendRecentForm(homeStanding.getRecentForm(), "W"));

            awayStanding.setLose(awayStanding.getLose() + 1);
            awayStanding.setPoints(awayStanding.getPoints() + losePoints);
            awayStanding.setRecentForm(appendRecentForm(awayStanding.getRecentForm(), "L"));
        } else if (homeScore < awayScore) {
            awayStanding.setWin(awayStanding.getWin() + 1);
            awayStanding.setPoints(awayStanding.getPoints() + winPoints);
            awayStanding.setRecentForm(appendRecentForm(awayStanding.getRecentForm(), "W"));

            homeStanding.setLose(homeStanding.getLose() + 1);
            homeStanding.setPoints(homeStanding.getPoints() + losePoints);
            homeStanding.setRecentForm(appendRecentForm(homeStanding.getRecentForm(), "L"));
        } else {
            homeStanding.setDraw(homeStanding.getDraw() + 1);
            homeStanding.setPoints(homeStanding.getPoints() + drawPoints);
            homeStanding.setRecentForm(appendRecentForm(homeStanding.getRecentForm(), "D"));

            awayStanding.setDraw(awayStanding.getDraw() + 1);
            awayStanding.setPoints(awayStanding.getPoints() + drawPoints);
            awayStanding.setRecentForm(appendRecentForm(awayStanding.getRecentForm(), "D"));
        }

        homeStanding.updateGoalDifference();
        awayStanding.updateGoalDifference();
    }

    private String appendRecentForm(String currentForm, String result) {
        String form = currentForm == null ? "" : currentForm;
        form = form + result;

        if (form.length() > 5) {
            form = form.substring(form.length() - 5);
        }

        return form;
    }
}
