package com.example.demo.service.season;


import com.example.demo.dao.season.StandingRepository;
import com.example.demo.dao.match.MatchRepository;
import com.example.demo.dao.season.SeasonRepository;
import com.example.demo.dao.season.SeasonTeamRepository;
import com.example.demo.dao.team.TeamRepository;

import com.example.demo.dto.StandingResponse;
import com.example.demo.entity.*;
import com.example.demo.entity.match.Match;
import com.example.demo.entity.match.MatchStatus;
import com.example.demo.entity.season.Season;
import com.example.demo.entity.season.SeasonTeam;
import com.example.demo.entity.season.Standing;
import com.example.demo.entity.team.Team;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.stereotype.Service;


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



    // ==================== QUERY METHODS ====================

    public List<StandingResponse> getStandings(Long seasonId) {
        List<Standing> standings;

        if (seasonId == null) {
            standings = standingRepository.findAll().stream()
                    .sorted(Comparator
                            .comparing((Standing standing) -> standing.getSeason() != null ? standing.getSeason().getId() : Long.MAX_VALUE)
                            .thenComparing(Standing::getRank, Comparator.nullsLast(Integer::compareTo)))
                    .toList();
        } else {
            seasonRepository.findById(seasonId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy mùa giải"));

            List<Standing> currentStandings = standingRepository.findBySeasonId(seasonId);

            standings = sortInProgressStandings(currentStandings);
        }

        return standings.stream()
                .map(this::toStandingResponse)
                .toList();
    }

    public StandingResponse getStanding(Long id) {
        Standing standing = standingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Standing not found with id = " + id));
        return toStandingResponse(standing);
    }




    /**
     * Lấy mùa giải cần tính lại bảng xếp hạng.
     */
    private Season getSeasonOrThrow(Long seasonId) {
        return seasonRepository.findById(seasonId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy mùa giải"));
    }

    private SystemRule getActiveSystemRuleOrThrow(Season season) {
        SystemRule rule = season.getSystemRule();

        if (rule == null) {
            throw new RuntimeException("Mùa giải chưa được cấu hình bộ luật");
        }

        if (!"ACTIVE".equalsIgnoreCase(rule.getStatus())) {
            throw new RuntimeException("Bộ luật của mùa giải đang tạm ngưng");
        }

        return rule;
    }

    /**
     * Lấy Standing của một đội trong mùa giải.
     * Nếu không tìm thấy thì báo lỗi vì Standing lẽ ra đã được khởi tạo trước đó.
     */
    private Standing getStandingOrThrow(Long seasonId, Team team, String sideLabel) {
        return standingRepository.findBySeasonIdAndTeamId(seasonId, team.getId())
                .orElseThrow(() -> new RuntimeException(
                        "Không tìm thấy standing " + sideLabel + ": " + team.getName()
                ));
    }

    /**
     * Đảm bảo mỗi CLB tham gia mùa giải đều có một dòng Standing.
     * Sau đó reset toàn bộ chỉ số về 0 để chuẩn bị tính lại từ đầu.
     */


    /**
     * Lấy Standing hiện có của một đội trong mùa giải.
     * Nếu chưa có thì tạo mới để đảm bảo đội đó xuất hiện trên bảng xếp hạng.
     */
    private Standing getOrCreateStanding(Season season, Team team) {
        return standingRepository.findBySeasonIdAndTeamId(season.getId(), team.getId())
                .orElseGet(() -> {
                    Standing newStanding = new Standing();
                    newStanding.setSeason(season);
                    newStanding.setTeam(team);
                    return newStanding;
                });
    }


// ==================== COMMAND METHODS ====================

    @Transactional
    public List<StandingResponse> recalculateBySeason(Long seasonId) {
        // Lấy mùa giải và bộ luật tính điểm/xếp hạng
        Season season = getSeasonOrThrow(seasonId);
        SystemRule rule = getActiveSystemRuleOrThrow(season);

        // Đảm bảo mọi CLB tham gia mùa giải đều có dòng Standing và reset số liệu về 0
        initializeAndResetStandings(season);

        // Cộng kết quả các trận đã kết thúc vào bảng xếp hạng
        applyFinishedMatchesToStandings(seasonId, rule);

        // Sắp xếp bảng xếp hạng và cập nhật thứ hạng
        List<Standing> sortedStandings = sortAndAssignRanks(season);

        // Lưu bảng xếp hạng sau khi tính toán
        standingRepository.saveAll(sortedStandings);

        return toStandingResponses(sortedStandings);
    }

    private void initializeAndResetStandings(Season season) {
        List<SeasonTeam> seasonTeams = seasonTeamRepository.findBySeasonId(season.getId());

        for (SeasonTeam seasonTeam : seasonTeams) {
            Standing standing = getOrCreateStanding(season, seasonTeam.getTeam());

            resetStanding(standing);
            standingRepository.save(standing);
        }
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


// ==================== BUSINESS HELPERS ====================

    private String appendRecentForm(String currentForm, String result) {
        String form = currentForm == null ? "" : currentForm;
        form = form + result;

        if (form.length() > 5) {
            form = form.substring(form.length() - 5);
        }

        return form;
    }

    /**
     * Duyệt toàn bộ trận đã kết thúc của mùa giải và cộng kết quả vào bảng xếp hạng.
     * Các trận chưa có đủ tỉ số sẽ bị bỏ qua để tránh tính sai dữ liệu.
     */
    private void applyFinishedMatchesToStandings(Long seasonId, SystemRule rule) {
        List<Match> finishedMatches = matchRepository.findBySeasonIdAndStatus(
                seasonId,
                MatchStatus.FINISHED
        );

        for (Match match : finishedMatches) {
            if (hasMissingScore(match)) {
                continue;
            }

            applyFinishedMatchToStandings(seasonId, match, rule);
        }
    }




    /**
     * Cộng kết quả của một trận đã kết thúc vào Standing của đội nhà và đội khách.
     */
    private void applyFinishedMatchToStandings(
            Long seasonId,
            Match match,
            SystemRule rule
    ) {
        Team homeTeam = match.getHomeTeam().getTeam();
        Team awayTeam = match.getAwayTeam().getTeam();

        Standing homeStanding = getStandingOrThrow(
                seasonId,
                homeTeam,
                "đội nhà"
        );

        Standing awayStanding = getStandingOrThrow(
                seasonId,
                awayTeam,
                "đội khách"
        );

        applyMatchResult(
                homeStanding,
                awayStanding,
                match.getHomeScore(),
                match.getAwayScore(),
                rule
        );

        standingRepository.save(homeStanding);
        standingRepository.save(awayStanding);
    }

    /**
     * Sắp xếp bảng xếp hạng theo 3 tiêu chí:
     * điểm, hiệu số bàn thắng, số bàn thắng ghi được.
     * Nếu bằng cả 3 tiêu chí thì cho đồng hạng.
     */
    private List<Standing> sortAndAssignRanks(Season season) {
        List<Standing> currentStandings = standingRepository.findBySeasonId(season.getId());

        List<Standing> sortedStandings = sortInProgressStandings(currentStandings);

        assignRanks(sortedStandings);

        return sortedStandings;
    }


    private List<Standing> sortInProgressStandings(List<Standing> standings) {
        return standings.stream()
                .sorted(
                        Comparator
                                // Ưu tiên 1: đội có điểm cao hơn xếp trên
                                .comparing(
                                        Standing::getPoints,
                                        Comparator.nullsFirst(Integer::compareTo)
                                ).reversed()

                                // Ưu tiên 2: nếu bằng điểm, đội có hiệu số tốt hơn xếp trên
                                .thenComparing(
                                        Standing::getGoalDifference,
                                        Comparator.nullsFirst(Integer::compareTo).reversed()
                                )

                                // Ưu tiên 3: nếu bằng điểm và hiệu số, đội ghi nhiều bàn hơn xếp trên
                                .thenComparing(
                                        Standing::getGoalsFor,
                                        Comparator.nullsFirst(Integer::compareTo).reversed()
                                )
                )
                .toList();
    }









// ==================== VALIDATION HELPERS ====================
    /**
     * Kiểm tra trận đã kết thúc nhưng chưa có đủ tỉ số.
     * Những trận như vậy không được dùng để tính bảng xếp hạng.
     */
    private boolean hasMissingScore(Match match) {
        return match.getHomeScore() == null || match.getAwayScore() == null;
    }


    private void assignRanks(List<Standing> standings) {
        for (int i = 0; i < standings.size(); i++) {
            Standing current = standings.get(i);

            if (i > 0) {
                Standing previous = standings.get(i - 1);

                boolean samePoints = Objects.equals(previous.getPoints(), current.getPoints());
                boolean sameGoalDiff = Objects.equals(previous.getGoalDifference(), current.getGoalDifference());
                boolean sameGoalsFor = Objects.equals(previous.getGoalsFor(), current.getGoalsFor());

                if (samePoints && sameGoalDiff && sameGoalsFor) {
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



// ==================== MAPPING HELPERS ====================
    /**
     * Chuyển danh sách Standing entity sang DTO trả về cho FE.
     */
    private List<StandingResponse> toStandingResponses(List<Standing> standings) {
        return standings.stream()
                .map(this::toStandingResponse)
                .toList();
    }

    private StandingResponse toStandingResponse(Standing standing) {
        return new StandingResponse(
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
// ==================== REALTIME / NOTIFICATION HELPERS ====================















}
