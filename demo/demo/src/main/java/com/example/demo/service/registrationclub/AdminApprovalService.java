package com.example.demo.service.registrationclub;

import com.example.demo.dao.player.PlayerSeasonRepository;
import com.example.demo.dao.registerteam.RegistrationTeamRepository;
import com.example.demo.dao.season.SeasonTeamCoachRepository;
import com.example.demo.dao.season.SeasonTeamRepository;
import com.example.demo.dao.user.UserRepository;
import com.example.demo.dto.RealtimeEventDTO;
import com.example.demo.entity.*;
import com.example.demo.entity.registerclub.*;
import com.example.demo.entity.user.User;
import com.example.demo.service.NotificationService;
import com.example.demo.service.RealtimeEventService;
import com.example.demo.service.StandingService;
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

    @Transactional
    public void approveRegistration(Long registrationId) {
        RegistrationTeam reg = registrationTeamRepository.findById(registrationId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn đăng ký"));

        if (reg.getStatus() != RegistrationStatus.PENDING) {
            throw new RuntimeException("Đơn này đã được xử lý");
        }

        Team team = reg.getTeam();
        Season season = reg.getSeason();

        SystemRule rule = season.getSystemRule();

        if (rule == null) {
            throw new RuntimeException("Mùa giải chưa được cấu hình bộ luật");
        }

        if (!"ACTIVE".equalsIgnoreCase(rule.getStatus())) {
            throw new RuntimeException("Bộ luật của mùa giải đang tạm ngưng");
        }

        if (seasonTeamRepository.existsBySeasonIdAndTeamId(season.getId(), team.getId())) {
            throw new RuntimeException("CLB này đã tham gia mùa giải");
        }

        if (rule.getMaxTeams() != null) {
            long currentTeamCount = seasonTeamRepository.countBySeasonId(season.getId());

            if (currentTeamCount >= rule.getMaxTeams()) {
                throw new RuntimeException("Mùa giải đã đạt số đội tối đa: " + rule.getMaxTeams());
            }
        }
        validateRegistrationPlayersByRule(reg, rule, season);
        validateNoDuplicateShirtNumbers(reg);
        validateCoaches(reg);
        // 1. Đăng ký Đội vào mùa giải
        SeasonTeam seasonTeam = new SeasonTeam();
        seasonTeam.setTeam(team);
        seasonTeam.setSeason(season);
        seasonTeam.setStatus("ACTIVE");
        seasonTeamRepository.save(seasonTeam);

        // 2. Đăng ký Cầu thủ vào mùa giải (PlayerSeason)
        for (RegistrationPlayer regPlayer : reg.getPlayers()) {
            PlayerSeason ps = new PlayerSeason();
            ps.setPlayer(regPlayer.getPlayer()); // Lấy player gốc
            ps.setTeam(team);
            ps.setSeason(season);
            ps.setShirtNumber(regPlayer.getShirtNumber());
            ps.setPrimaryPosition(regPlayer.getPosition());
            ps.setTeamSeason(seasonTeam);
            playerSeasonRepository.save(ps);
        }

        // 3. Đăng ký HLV vào mùa giải (SeasonTeamCoach)
        for (RegistrationCoach regCoach : reg.getCoaches()) {
            SeasonTeamCoach stc = new SeasonTeamCoach();
            stc.setCoach(regCoach.getCoach()); // Lấy coach gốc
            stc.setTeam(team);
            stc.setSeason(season);
            stc.setRole(regCoach.getTournamentRole());
            stc.setAssignedDate(LocalDate.now());
            stc.setStatus("ACTIVE");
            seasonTeamCoachRepository.save(stc);
        }

        // 4. Khởi tạo bảng xếp hạng
        standingService.initializeStanding(season.getId(), team.getId());

        reg.setStatus(RegistrationStatus.APPROVED);

        RegistrationTeam savedRegistration = registrationTeamRepository.save(reg);
        notifyClubAboutRegistrationResult(savedRegistration, true, null);
        sendRegistrationResultEvents(savedRegistration, true);
        sendTeamSeasonUpdatedEventToClubManager(savedRegistration);
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

    private void validateCoaches(RegistrationTeam reg) {
        if (reg.getCoaches() == null || reg.getCoaches().isEmpty()) {
            throw new RuntimeException("Đơn đăng ký chưa có ban huấn luyện");
        }

        Set<Long> coachIds = new HashSet<>();

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

        if (rule.getMinRegistrationPlayers() != null
                && squadSize < rule.getMinRegistrationPlayers()) {
            throw new RuntimeException(
                    "Số cầu thủ đăng ký chưa đạt tối thiểu: " + rule.getMinRegistrationPlayers()
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
