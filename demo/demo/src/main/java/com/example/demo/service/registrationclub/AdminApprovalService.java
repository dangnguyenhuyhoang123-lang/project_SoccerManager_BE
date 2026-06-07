package com.example.demo.service.registrationclub;

import com.example.demo.dao.team.player.PlayerSeasonRepository;
import com.example.demo.dao.registerteam.RegistrationTeamRepository;
import com.example.demo.dao.season.SeasonTeamCoachRepository;
import com.example.demo.dao.season.SeasonTeamRepository;
import com.example.demo.dao.user.UserRepository;
import com.example.demo.dto.RealtimeEventDTO;
import com.example.demo.entity.*;
import com.example.demo.entity.player.Player;
import com.example.demo.entity.season.PlayerSeason;
import com.example.demo.entity.registerclub.*;
import com.example.demo.entity.season.Season;
import com.example.demo.entity.season.SeasonTeam;
import com.example.demo.entity.season.SeasonTeamCoach;
import com.example.demo.entity.team.Team;
import com.example.demo.entity.user.User;
import com.example.demo.service.realtime.NotificationService;
import com.example.demo.service.realtime.RealtimeEventService;
import com.example.demo.service.season.StandingService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AdminApprovalService {

    private final RegistrationTeamRepository registrationTeamRepository;
    private final SeasonTeamRepository seasonTeamRepository;
    private final PlayerSeasonRepository playerSeasonRepository;
    private final SeasonTeamCoachRepository seasonTeamCoachRepository;
    private final StandingService standingService;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final RealtimeEventService realtimeEventService;




    // ==================== QUERY METHODS ====================
    /**
     * Lấy đơn đăng ký theo id và đảm bảo đơn vẫn đang ở trạng thái chờ duyệt.
     * Chỉ các đơn PENDING mới được phép duyệt.
     */
    private RegistrationTeam getPendingRegistrationOrThrow(Long registrationId) {
        RegistrationTeam registration = registrationTeamRepository.findById(registrationId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn đăng ký"));

        if (registration.getStatus() != RegistrationStatus.PENDING) {
            throw new RuntimeException("Đơn này đã được xử lý");
        }

        return registration;
    }


    /**
     * Lấy bộ luật đang áp dụng cho mùa giải.
     * Chỉ cho phép xử lý hồ sơ nếu mùa giải đã có rule và rule đang ACTIVE.
     */
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


// ==================== COMMAND METHODS ====================

    @Transactional
    public void approveRegistration(Long registrationId) {
        // Lấy và kiểm tra đơn đăng ký đang ở trạng thái chờ duyệt
        RegistrationTeam registration = getPendingRegistrationOrThrow(registrationId);

        Team team = registration.getTeam();
        Season season = registration.getSeason();

        // Lấy bộ luật đang hoạt động của mùa giải
        SystemRule rule = getActiveSystemRuleOrThrow(season);

        // Kiểm tra các điều kiện trước khi duyệt đơn
        validateRegistrationCanBeApproved(registration, team, season, rule);

        // Tạo bản ghi CLB tham gia mùa giải
        SeasonTeam seasonTeam = createSeasonTeam(registration, team, season);

        // Tạo dữ liệu cầu thủ và HLV theo mùa giải
        createPlayerSeasons(registration, team, season, seasonTeam);
        createSeasonTeamCoaches(registration, team, season);

        // Khởi tạo bảng xếp hạng cho CLB vừa được duyệt
        initializeStandingForApprovedTeam(season, team);

        // Cập nhật trạng thái đơn và gửi thông báo/realtime
        RegistrationTeam savedRegistration = markRegistrationAsApproved(registration);
        notifyRegistrationApproved(savedRegistration);
    }

    @Transactional
    public void rejectRegistration(Long id, String reason) {
        RegistrationTeam reg = registrationTeamRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn đăng ký"));

        if (reg.getStatus() != RegistrationStatus.PENDING) {
            throw new RuntimeException("Chỉ có thể từ chối đơn đang chờ duyệt");
        }

        reg.setStatus(RegistrationStatus.REJECTED);
        reg.setRejectionReason(reason);

        RegistrationTeam savedRegistration = registrationTeamRepository.save(reg);
        notifyClubAboutRegistrationResult(savedRegistration, false, reason);
        sendRegistrationResultEvents(savedRegistration, false);
    }



// ==================== BUSINESS HELPERS ====================

    /**
     * Tạo entity PlayerSeason từ một cầu thủ trong hồ sơ đăng ký.
     */
    private PlayerSeason buildPlayerSeason(
            RegistrationPlayer registrationPlayer,
            Team team,
            Season season,
            SeasonTeam seasonTeam
    ) {
        PlayerSeason playerSeason = new PlayerSeason();

        playerSeason.setPlayer(registrationPlayer.getPlayer());
        playerSeason.setTeam(team);
        playerSeason.setSeason(season);
        playerSeason.setShirtNumber(registrationPlayer.getShirtNumber());
        playerSeason.setPrimaryPosition(registrationPlayer.getPosition());
        playerSeason.setTeamSeason(seasonTeam);
        playerSeason.setStatus("ACTIVE");

        return playerSeason;
    }


    /**
     * Tạo bản ghi CLB tham gia mùa giải sau khi hồ sơ được duyệt.
     * SeasonTeam là dữ liệu nền để quản lý cầu thủ, HLV, đội hình và bảng xếp hạng theo mùa.
     */
    private SeasonTeam createSeasonTeam(
            RegistrationTeam registration,
            Team team,
            Season season
    ) {
        SeasonTeam seasonTeam = new SeasonTeam();

        seasonTeam.setTeam(team);
        seasonTeam.setSeason(season);
        seasonTeam.setRegistrationTeam(registration);
        seasonTeam.setStatus("ACTIVE");

        return seasonTeamRepository.save(seasonTeam);
    }
    /**
     * Chuyển danh sách cầu thủ trong hồ sơ đăng ký thành PlayerSeason.
     * PlayerSeason đại diện cho cầu thủ của một CLB trong một mùa giải cụ thể.
     */
    private void createPlayerSeasons(
            RegistrationTeam registration,
            Team team,
            Season season,
            SeasonTeam seasonTeam
    ) {
        for (RegistrationPlayer registrationPlayer : registration.getPlayers()) {
            PlayerSeason playerSeason = buildPlayerSeason(
                    registrationPlayer,
                    team,
                    season,
                    seasonTeam
            );

            playerSeasonRepository.save(playerSeason);
        }
    }

    /**
     * Chuyển danh sách HLV trong hồ sơ đăng ký thành SeasonTeamCoach.
     * SeasonTeamCoach lưu vai trò của HLV trong CLB ở mùa giải cụ thể.
     */
    private void createSeasonTeamCoaches(
            RegistrationTeam registration,
            Team team,
            Season season
    ) {
        for (RegistrationCoach registrationCoach : registration.getCoaches()) {
            SeasonTeamCoach seasonTeamCoach = buildSeasonTeamCoach(
                    registrationCoach,
                    team,
                    season
            );

            seasonTeamCoachRepository.save(seasonTeamCoach);
        }
    }

    /**
     * Cập nhật trạng thái hồ sơ đăng ký sang APPROVED sau khi đã tạo xong dữ liệu mùa giải.
     */
    private RegistrationTeam markRegistrationAsApproved(RegistrationTeam registration) {
        registration.setStatus(RegistrationStatus.APPROVED);
        return registrationTeamRepository.save(registration);
    }

    /**
     * Tạo entity SeasonTeamCoach từ một HLV trong hồ sơ đăng ký.
     */
    private SeasonTeamCoach buildSeasonTeamCoach(
            RegistrationCoach registrationCoach,
            Team team,
            Season season
    ) {
        SeasonTeamCoach seasonTeamCoach = new SeasonTeamCoach();

        seasonTeamCoach.setCoach(registrationCoach.getCoach());
        seasonTeamCoach.setTeam(team);
        seasonTeamCoach.setSeason(season);
        seasonTeamCoach.setRole(registrationCoach.getTournamentRole());
        seasonTeamCoach.setAssignedDate(LocalDate.now());
        seasonTeamCoach.setStatus("ACTIVE");

        return seasonTeamCoach;
    }


    /**
     * Khởi tạo dòng bảng xếp hạng cho CLB vừa được duyệt tham gia mùa giải.
     */
    private void initializeStandingForApprovedTeam(Season season, Team team) {
        standingService.initializeStanding(season.getId(), team.getId());
    }


// ==================== VALIDATION HELPERS ====================
// Các hàm kiểm tra điều kiện, throw lỗi nếu không hợp lệ.

    /**
     * Kiểm tra toàn bộ điều kiện nghiệp vụ trước khi admin duyệt hồ sơ.
     *
     * Bao gồm:
     * - CLB chưa tham gia mùa giải.
     * - Mùa giải chưa vượt quá số đội tối đa.
     * - CLB đã thanh toán lệ phí.
     * - Danh sách cầu thủ và HLV hợp lệ theo rule.
     */
    private void validateRegistrationCanBeApproved(
            RegistrationTeam registration,
            Team team,
            Season season,
            SystemRule rule
    ) {
        validateTeamNotAlreadyInSeason(team, season);
        validateSeasonTeamLimit(season, rule);
        validateRegistrationFeePaid(registration);
        validateRegistrationPlayersByRule(registration, rule, season);
        validateNoDuplicateShirtNumbers(registration);
        validateCoaches(registration, rule);
    }

    /**
     * Đảm bảo CLB chưa được thêm vào mùa giải trước đó.
     * Tránh tạo trùng SeasonTeam cho cùng một CLB trong một mùa giải.
     */
    private void validateTeamNotAlreadyInSeason(Team team, Season season) {
        seasonTeamRepository.findBySeasonIdAndTeamId(season.getId(), team.getId())
                .filter(this::isInactiveSeasonTeam)
                .ifPresent(seasonTeam -> {
                    throw new RuntimeException("Đội bóng đã bị vô hiệu hóa trong mùa giải này, không thể duyệt đơn đăng ký.");
                });

        if (seasonTeamRepository.existsBySeasonIdAndTeamId(season.getId(), team.getId())) {
            throw new RuntimeException("CLB này đã tham gia mùa giải");
        }
    }


    /**
     * Kiểm tra mùa giải còn slot để nhận thêm CLB hay không.
     * Nếu rule có cấu hình maxTeams thì số đội hiện tại không được vượt quá giới hạn này.
     */
    private boolean isInactiveSeasonTeam(SeasonTeam seasonTeam) {
        return seasonTeam != null
                && seasonTeam.getStatus() != null
                && "INACTIVE".equalsIgnoreCase(seasonTeam.getStatus().trim());
    }

    private void validateSeasonTeamLimit(Season season, SystemRule rule) {
        if (rule.getMaxTeams() == null) {
            return;
        }

        long currentTeamCount = seasonTeamRepository.countBySeasonId(season.getId());

        if (currentTeamCount >= rule.getMaxTeams()) {
            throw new RuntimeException("Mùa giải đã đạt số đội tối đa: " + rule.getMaxTeams());
        }
    }


    /**
     * Chỉ cho phép duyệt hồ sơ khi CLB đã hoàn tất lệ phí tham gia giải.
     */
    private void validateRegistrationFeePaid(RegistrationTeam registration) {
        if (registration.getFeeStatus() != FeeStatus.PAID) {
            throw new RuntimeException("CLB chưa hoàn tất lệ phí tham gia giải");
        }
    }

    private void validateNoDuplicateShirtNumbers(RegistrationTeam reg) {
        if (reg.getPlayers() == null || reg.getPlayers().isEmpty()) {
            throw new RuntimeException("Đơn đăng ký không có cầu thủ");
        }

        Set<Integer> shirtNumbers = new HashSet<>();
        Set<Long> playerIds = new HashSet<>();

        for (RegistrationPlayer rp : reg.getPlayers()) {
            if (rp.getPlayer() == null || rp.getPlayer().getId() == null) {
                throw new RuntimeException("Đơn đăng ký có cầu thủ không hợp lệ");
            }

            if (!playerIds.add(rp.getPlayer().getId())) {
                throw new RuntimeException("Đơn đăng ký có cầu thủ bị trùng");
            }

            if (rp.getShirtNumber() == null) {
                throw new RuntimeException("Số áo cầu thủ không được để trống");
            }

            if (!shirtNumbers.add(rp.getShirtNumber())) {
                throw new RuntimeException("Trùng số áo trong đơn đăng ký: " + rp.getShirtNumber());
            }
        }
    }

    private void validateCoaches(RegistrationTeam reg, SystemRule rule) {
        if (reg.getCoaches() == null || reg.getCoaches().isEmpty()) {
            throw new RuntimeException("Đơn đăng ký chưa có ban huấn luyện");
        }

        int coachCount = reg.getCoaches().size();
        Integer minCoaches = rule != null && rule.getMinCoaches() != null ? rule.getMinCoaches() : 3;
        Integer maxCoaches = rule != null ? rule.getMaxCoaches() : null;

        if (coachCount < minCoaches) {
            throw new RuntimeException("Số lượng ban huấn luyện chưa đạt tối thiểu: " + minCoaches);
        }

        if (maxCoaches != null && coachCount > maxCoaches) {
            throw new RuntimeException("Số lượng ban huấn luyện vượt quá tối đa: " + maxCoaches);
        }

        Set<Long> coachIds = new HashSet<>();
        long headCoachCount = 0;

        for (RegistrationCoach rc : reg.getCoaches()) {
            if (rc.getCoach() == null || rc.getCoach().getId() == null) {
                throw new RuntimeException("Đơn đăng ký có HLV không hợp lệ");
            }

            if (!coachIds.add(rc.getCoach().getId())) {
                throw new RuntimeException("Đơn đăng ký có HLV bị trùng");
            }

            if (rc.getTournamentRole() == null || rc.getTournamentRole().isBlank()) {
                throw new RuntimeException("Vai trò HLV trong giải không được để trống");
            }

            String role = rc.getTournamentRole().trim().toLowerCase();
            if (role.contains("hlv trưởng") || role.contains("huấn luyện viên trưởng") || role.contains("head_coach")) {
                headCoachCount++;
            }
        }

        if (headCoachCount != 1) {
            throw new RuntimeException("Ban huấn luyện phải có đúng 01 Huấn luyện viên trưởng");
        }
    }
    private void validateRegistrationPlayersByRule(
            RegistrationTeam reg,
            SystemRule rule,
            Season season
    ) {
        List<RegistrationPlayer> players = reg.getPlayers();

        if (players == null || players.isEmpty()) {
            throw new RuntimeException("Đơn đăng ký không có cầu thủ");
        }

        int squadSize = players.size();

        if (rule.getMinPlayers() != null && squadSize < rule.getMinPlayers()) {
            throw new RuntimeException(
                    "Số cầu thủ đăng ký chưa đạt tối thiểu: " + rule.getMinPlayers()
            );
        }

        if (rule.getMaxPlayers() != null && squadSize > rule.getMaxPlayers()) {
            throw new RuntimeException(
                    "Số cầu thủ đăng ký vượt quá tối đa: " + rule.getMaxPlayers()
            );
        }

        long foreignCount = players.stream()
                .map(RegistrationPlayer::getPlayer)
                .filter(this::isForeignPlayer)
                .count();

        if (rule.getMaxForeignPlayers() != null
                && foreignCount > rule.getMaxForeignPlayers()) {
            throw new RuntimeException(
                    "Số ngoại binh vượt quá giới hạn: " + rule.getMaxForeignPlayers()
            );
        }

        LocalDate referenceDate = season.getStartDate() != null
                ? season.getStartDate()
                : LocalDate.now();

        for (RegistrationPlayer registrationPlayer : players) {
            Player player = registrationPlayer.getPlayer();

            if (player == null) {
                throw new RuntimeException("Đơn đăng ký có cầu thủ không hợp lệ");
            }

            if (player.getDateOfBirth() != null) {
                int age = Period.between(player.getDateOfBirth(), referenceDate).getYears();

                if (rule.getMinAge() != null && age < rule.getMinAge()) {
                    throw new RuntimeException(
                            "Cầu thủ " + player.getName() + " chưa đủ tuổi quy định"
                    );
                }

                if (rule.getMaxAge() != null && age > rule.getMaxAge()) {
                    throw new RuntimeException(
                            "Cầu thủ " + player.getName() + " vượt quá tuổi quy định"
                    );
                }
            }
        }
    }
    private boolean isForeignPlayer(Player player) {
        if (player == null || player.getNationality() == null) {
            return false;
        }

        String nationality = player.getNationality().trim().toLowerCase();

        return !nationality.equals("việt nam")
                && !nationality.equals("viet nam")
                && !nationality.equals("vietnam")
                && !nationality.equals("vn");
    }


// ==================== MAPPING HELPERS ====================
// Các hàm convert Entity -> DTO hoặc DTO -> Entity.










    // ==================== REALTIME / NOTIFICATION HELPERS ====================

    /**
     * Gửi thông báo và realtime sau khi hồ sơ đăng ký được duyệt.
     *
     * Người nhận:
     * - Quản lý CLB: nhận kết quả duyệt và reload dữ liệu mùa giải của đội.
     * - Admin: reload danh sách hồ sơ đăng ký nếu cần.
     */
    private void notifyRegistrationApproved(RegistrationTeam savedRegistration) {
        notifyClubAboutRegistrationResult(savedRegistration, true, null);
        sendRegistrationResultEvents(savedRegistration, true);
        sendTeamSeasonUpdatedEventToClubManager(savedRegistration);
    }
    private void notifyClubAboutRegistrationResult(
            RegistrationTeam registration,
            boolean approved,
            String reason
    ) {
        if (registration == null || registration.getTeam() == null) {
            return;
        }

        Team team = registration.getTeam();

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
            System.out.println("Không tìm thấy quản lý CLB cho teamId = " + team.getId());
            return;
        }

        User manager = managerOpt.get();

        String seasonName = registration.getSeason() != null
                ? registration.getSeason().getName()
                : "mùa giải";

        if (approved) {
            notificationService.notifyRegistrationApprovedToClub(
                    manager.getId(),
                    team.getName(),
                    seasonName,
                    registration.getId()
            );
        } else {
            notificationService.notifyRegistrationRejectedToClub(
                    manager.getId(),
                    team.getName(),
                    seasonName,
                    registration.getId(),
                    reason
            );
        }
    }

    private void sendRegistrationResultEvents(RegistrationTeam registration, boolean approved) {
        String type = approved ? "REGISTRATION_APPROVED" : "REGISTRATION_REJECTED";
        RealtimeEventDTO event = realtimeEvent(
                type,
                registration.getId(),
                "REGISTRATION_TEAM",
                "REFETCH_REGISTRATIONS"
        );

        findClubManagerByRegistration(registration)
                .map(User::getId)
                .ifPresent(userId -> realtimeEventService.sendToUser(userId, event));

        sendEventToAdmins(event);
    }

    private void sendTeamSeasonUpdatedEventToClubManager(RegistrationTeam registration) {
        if (registration == null || registration.getTeam() == null) {
            return;
        }

        RealtimeEventDTO event = realtimeEvent(
                "TEAM_SEASON_UPDATED",
                registration.getTeam().getId(),
                "TEAM_SEASON",
                "REFETCH_TEAM_SEASON"
        );

        findClubManagerByRegistration(registration)
                .map(User::getId)
                .ifPresent(userId -> realtimeEventService.sendToUser(userId, event));
    }

    private void sendEventToAdmins(RealtimeEventDTO event) {
        List<User> admins = userRepository.findUsersByRoleName("ROLE_ADMIN");

        for (User admin : admins) {
            realtimeEventService.sendToUser(admin.getId(), event);
        }
    }

    private Optional<User> findClubManagerByRegistration(RegistrationTeam registration) {
        if (registration == null || registration.getTeam() == null) {
            return Optional.empty();
        }

        Team team = registration.getTeam();

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

        return managerOpt;
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
