package com.example.demo.service.registrationclub;

import com.example.demo.dao.season.SeasonInvitationRepository;
import com.example.demo.dao.team.CoachRepository;
import com.example.demo.dao.team.player.PlayerRepository;
import com.example.demo.dao.registerteam.RegistrationCoachRepository;
import com.example.demo.dao.registerteam.RegistrationPlayerRepository;
import com.example.demo.dao.registerteam.RegistrationTeamRepository;
import com.example.demo.dao.season.SeasonRepository;
import com.example.demo.dao.season.SeasonTeamRepository;
import com.example.demo.dao.team.TeamRepository;
import com.example.demo.dao.user.UserRepository;
import com.example.demo.dto.RealtimeEventDTO;
import com.example.demo.dto.registrationclub.*;
import com.example.demo.entity.*;
import com.example.demo.entity.player.Player;
import com.example.demo.entity.registerclub.*;
import com.example.demo.entity.season.Season;
import com.example.demo.entity.season.SeasonTeam;
import com.example.demo.entity.team.Coach;
import com.example.demo.entity.team.Team;
import com.example.demo.entity.team.TeamKit;
import com.example.demo.entity.user.User;
import com.example.demo.service.realtime.RealtimeEventService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RegistrationService {

    private final RegistrationTeamRepository teamRegRepo;
    private final SeasonRepository seasonRepository;
    private final TeamRepository teamRepository;
    private final SeasonTeamRepository seasonTeamRepository;
    private final RegistrationTeamRepository registrationTeamRepository;
    private final RegistrationPlayerRepository registrationPlayerRepository;
    private final RegistrationCoachRepository registrationCoachRepository;
    private final PlayerRepository playerRepository;
    private  final CoachRepository coachRepository;
    private final UserRepository userRepository;
    private final RealtimeEventService realtimeEventService;
    private final SeasonInvitationRepository seasonInvitationRepository;




// ==================== QUERY METHODS ====================

    private SystemRule getRequiredActiveRule(Season season) {
        if (season == null) {
            throw new IllegalArgumentException("Không tìm thấy mùa giải");
        }

        SystemRule rule = season.getSystemRule();

        if (rule == null) {
            throw new IllegalArgumentException("Mùa giải chưa được cấu hình bộ luật");
        }

        if (!"ACTIVE".equalsIgnoreCase(rule.getStatus())) {
            throw new IllegalArgumentException("Bộ luật của mùa giải đang tạm ngưng");
        }

        return rule;
    }
    public List<RegistrationSummaryDTO> getRegistrations(RegistrationStatus status) {
        List<RegistrationTeam> registrations = status == null
                ? teamRegRepo.findAllByOrderByCreatedAtDesc()
                : teamRegRepo.findByStatusOrderByCreatedAtDesc(status);

        return registrations.stream()
                .map(this::toSummaryDto)
                .toList();
    }

    public RegistrationDetailDTO getRegistrationDetail(Long id) {
        RegistrationTeam registration = teamRegRepo.findOneById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn đăng ký"));

        List<RegistrationPlayer> registrationPlayers = registrationPlayerRepository.findByRegistrationTeamId(registration.getId());
        List<RegistrationCoach> registrationCoaches = registrationCoachRepository.findByRegistrationTeamId(registration.getId());
        return toDetailDto(registration, registrationPlayers, registrationCoaches);
    }

    /**
     * Lấy mùa giải theo id từ request.
     * Nếu mùa giải không tồn tại thì dừng luồng đăng ký.
     */
    private Season getSeasonOrThrow(Long seasonId) {
        return seasonRepository.findById(seasonId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy mùa giải"));
    }

    /**
     * Lấy CLB thực hiện đăng ký.
     * Nếu CLB không tồn tại thì không cho tạo đơn đăng ký.
     */
    private Team getTeamOrThrow(Long teamId) {
        return teamRepository.findById(teamId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy câu lạc bộ"));
    }

    /**
     * Lấy cầu thủ theo id.
     * Dùng khi tạo danh sách cầu thủ đăng ký cho hồ sơ.
     */
    private Player getPlayerOrThrow(Long playerId) {
        return playerRepository.findById(playerId)
                .orElseThrow(() -> new RuntimeException(
                        "Cầu thủ ID " + playerId + " không tồn tại"
                ));
    }

    /**
     * Lấy HLV theo id.
     * Nếu HLV không tồn tại thì không cho đưa vào hồ sơ đăng ký.
     */
    private Coach getCoachOrThrow(Long coachId) {
        return coachRepository.findById(coachId)
                .orElseThrow(() -> new RuntimeException(
                        "HLV ID " + coachId + " không tồn tại"
                ));
    }


// ==================== COMMAND METHODS ====================

    @Transactional
    public RegistrationSummaryDTO submitRegistration(FullRegistrationDTO dto) {
        // Kiểm tra cấu trúc dữ liệu đầu vào từ FE
        validateRequestShape(dto);

        // Lấy dữ liệu nền tảng gồm mùa giải, luật mùa giải và CLB đăng ký
        Season season = getSeasonOrThrow(dto.getSeasonID());
        SystemRule rule = getRequiredActiveRule(season);
        Team team = getTeamOrThrow(dto.getTeamInfo().getId());

        // Kiểm tra các ràng buộc nghiệp vụ trước khi tạo đơn đăng ký
        validateRegistrationBusiness(dto, team, season, rule);

        // Tạo đơn đăng ký tổng thể của CLB
        RegistrationTeam registration = buildRegistrationTeam(dto, team, season);

        // Gắn thông tin sân, cầu thủ, ban huấn luyện và lệ phí vào đơn đăng ký
        applyRegistrationStadium(registration, dto.getStadiumInfo());
        registration.setPlayers(buildRegistrationPlayers(registration, dto.getListPlayerInfo(), team));
        registration.setCoaches(buildRegistrationCoaches(registration, dto.getListCoachInfo()));
        applyRegistrationFee(registration);

        // Lưu đơn đăng ký và thông báo realtime cho admin
        RegistrationTeam savedRegistration = registrationTeamRepository.save(registration);
        sendRegistrationSubmittedEventToAdmins(savedRegistration);

        return toSummaryDto(savedRegistration);
    }

// ==================== BUSINESS HELPERS ====================

    @Transactional
    public RegistrationSummaryDTO markRegistrationPaid(Long id, String paymentProofUrl) {
        RegistrationTeam registration = registrationTeamRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn đăng ký id = " + id));

        if (registration.getStatus() != RegistrationStatus.PENDING) {
            throw new RuntimeException("Chỉ có thể cập nhật lệ phí cho đơn đang chờ duyệt");
        }

        registration.setFeeStatus(FeeStatus.PAID);
        registration.setPaymentProofUrl(paymentProofUrl);
        registration.setPaidAt(LocalDateTime.now());

        RegistrationTeam saved = registrationTeamRepository.save(registration);
        return toSummaryDto(saved);
    }

    private boolean isVietnam(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }

        String normalized = value.trim().toLowerCase();

        return normalized.equals("việt nam")
                || normalized.equals("viet nam")
                || normalized.equals("vietnam")
                || normalized.equals("vn");
    }
    private boolean isForeignPlayer(Player player) {
        String nationality = player.getNationality();

        if (nationality == null || nationality.isBlank()) {
            return false;
        }

        String normalized = nationality.trim().toLowerCase();

        return !normalized.equals("việt nam")
                && !normalized.equals("viet nam")
                && !normalized.equals("vietnam")
                && !normalized.equals("vn");
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }


    /**
     * Tạo đơn đăng ký tổng thể của CLB cho một mùa giải.
     * Entity này là cha của thông tin sân, cầu thủ và ban huấn luyện đăng ký.
     */
    private RegistrationTeam buildRegistrationTeam(
            FullRegistrationDTO dto,
            Team team,
            Season season
    ) {
        RegistrationTeam registration = new RegistrationTeam();

        registration.setTeam(team);
        registration.setSeason(season);
        registration.setStatus(RegistrationStatus.PENDING);

        registration.setNote(dto.getTeamInfo().getNote());
        registration.setHomeKitColor(dto.getTeamInfo().getHomeKitColor());
        registration.setAwayKitColor(dto.getTeamInfo().getAwayKitColor());
        registration.setHomeKitImageUrl(dto.getTeamInfo().getHomeKitImageUrl());
        registration.setAwayKitImageUrl(dto.getTeamInfo().getAwayKitImageUrl());

        return registration;
    }


    /**
     * Gắn thông tin sân vận động vào đơn đăng ký nếu CLB có gửi thông tin sân.
     * Đây là thông tin sân được dùng trong hồ sơ đăng ký mùa giải.
     */
    private void applyRegistrationStadium(
            RegistrationTeam registration,
            StadiumRegistrationDTO stadiumDto
    ) {
        if (stadiumDto == null) {
            return;
        }

        RegistrationStadium stadium = new RegistrationStadium();

        stadium.setName(stadiumDto.getName());
        stadium.setAddress(stadiumDto.getAddress());
        stadium.setCapacity(stadiumDto.getCapacity());
        stadium.setGrass(stadiumDto.getGrass());
        stadium.setCountry(stadiumDto.getCountry());
        stadium.setFifaStarRating(stadiumDto.getFifaStarRating());
        stadium.setCertificateUrl(stadiumDto.getCertificateUrl());

        registration.setStadium(stadium);
    }

    /**
     * Tạo danh sách cầu thủ đăng ký thi đấu từ dữ liệu FE gửi lên.
     * Mỗi cầu thủ phải tồn tại và thuộc biên chế của CLB đang đăng ký.
     */
    private List<RegistrationPlayer> buildRegistrationPlayers(
            RegistrationTeam registration,
            List<PlayerRegistrationDTO> playerDtos,
            Team team
    ) {
        List<RegistrationPlayer> registrationPlayers = new ArrayList<>();

        if (playerDtos == null) {
            return registrationPlayers;
        }

        for (PlayerRegistrationDTO playerDto : playerDtos) {
            Player player = getPlayerOrThrow(playerDto.getPlayerId());

            validatePlayerBelongsToTeam(player, team);

            RegistrationPlayer registrationPlayer = new RegistrationPlayer();
            registrationPlayer.setRegistrationTeam(registration);
            registrationPlayer.setPlayer(player);
            registrationPlayer.setShirtNumber(playerDto.getShirtNumber());
            registrationPlayer.setPosition(playerDto.getPosition());

            registrationPlayers.add(registrationPlayer);
        }

        return registrationPlayers;
    }


    /**
     * Tạo danh sách ban huấn luyện đăng ký thi đấu từ dữ liệu FE gửi lên.
     */
    private List<RegistrationCoach> buildRegistrationCoaches(
            RegistrationTeam registration,
            List<CoachRegistrationDTO> coachDtos
    ) {
        List<RegistrationCoach> registrationCoaches = new ArrayList<>();

        if (coachDtos == null) {
            return registrationCoaches;
        }

        for (CoachRegistrationDTO coachDto : coachDtos) {
            Coach coach = getCoachOrThrow(coachDto.getCoachId());

            RegistrationCoach registrationCoach = new RegistrationCoach();
            registrationCoach.setRegistrationTeam(registration);
            registrationCoach.setCoach(coach);
            registrationCoach.setTournamentRole(coachDto.getRole());

            registrationCoaches.add(registrationCoach);
        }

        return registrationCoaches;
    }





    private void applyRegistrationFee(RegistrationTeam registration) {
        registration.setFeeStatus(FeeStatus.UNPAID);
        registration.setFeeAmount(BigDecimal.valueOf(1_000_000_000L));
    }

// ==================== VALIDATION HELPERS ====================

    // Tổng hợp valide cho submit
    private void validateRegistrationBusiness(
            FullRegistrationDTO dto,
            Team team,
            Season season,
            SystemRule rule
    ) {
        validateBusinessRules(team, season, rule);
        validateCoachList(dto.getListCoachInfo(), rule);

        List<Player> players = getPlayersForValidation(dto.getListPlayerInfo());

        validatePlayersBelongToTeam(players, team);
        validatePlayerList(dto.getListPlayerInfo(), players, rule, season);
    }


    /**
     * Lấy danh sách cầu thủ từ database để phục vụ validate đăng ký.
     * Mỗi playerId trong DTO phải tồn tại trong hệ thống.
     */
    private List<Player> getPlayersForValidation(List<PlayerRegistrationDTO> playerDtos) {
        List<Player> players = new ArrayList<>();

        if (playerDtos == null) {
            return players;
        }

        for (PlayerRegistrationDTO playerDto : playerDtos) {
            Player player = playerRepository.findById(playerDto.getPlayerId())
                    .orElseThrow(() -> new RuntimeException(
                            "Cầu thủ ID " + playerDto.getPlayerId() + " không tồn tại"
                    ));

            players.add(player);
        }

        return players;
    }

    private void validatePlayersBelongToTeam(List<Player> players, Team team) {
        for (Player player : players) {
            if (player.getTeam() == null || !player.getTeam().getId().equals(team.getId())) {
                throw new IllegalArgumentException(
                        "Cầu thủ " + player.getName() + " không thuộc biên chế CLB " + team.getName()
                );
            }
        }
    }

    /**
     * Kiểm tra cầu thủ có thuộc biên chế CLB đang đăng ký hay không.
     */
    private void validatePlayerBelongsToTeam(Player player, Team team) {
        if (player.getTeam() == null || !player.getTeam().getId().equals(team.getId())) {
            throw new RuntimeException(
                    "Cầu thủ " + player.getName() + " không thuộc biên chế CLB của bạn"
            );
        }
    }


    private void validateRequestShape(FullRegistrationDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("Dữ liệu đăng ký không hợp lệ");
        }
        if (dto.getTeamInfo() == null || dto.getTeamInfo().getId() == null) {
            throw new IllegalArgumentException("Thiếu thông tin ID câu lạc bộ");
        }
        if (dto.getListPlayerInfo() == null || dto.getListPlayerInfo().isEmpty()) {
            throw new IllegalArgumentException("Danh sách cầu thủ không được để trống");
        }
        if (dto.getListCoachInfo() == null || dto.getListCoachInfo().isEmpty()) {
            throw new IllegalArgumentException("Danh sách ban huấn luyện không được để trống");
        }
        // Nếu có đăng ký sân nhà riêng, kiểm tra tên sân
        if (dto.getStadiumInfo() != null && (dto.getStadiumInfo().getName() == null || dto.getStadiumInfo().getName().trim().isEmpty())) {
            throw new IllegalArgumentException("Tên sân vận động đăng ký không được để trống");
        }
        validateStadiumInfo(dto.getStadiumInfo());
    }

//    Kiểm tra sân vận động


    private void validateStadiumInfo(StadiumRegistrationDTO stadium) {
        if (stadium == null) {
            throw new RuntimeException("Thông tin sân nhà không được để trống");
        }

        if (stadium.getCapacity() == null || stadium.getCapacity() < 10000) {
            throw new RuntimeException("Sân nhà phải có sức chứa tối thiểu 10.000 chỗ");
        }

        if (stadium.getFifaStarRating() == null || stadium.getFifaStarRating() < 2) {
            throw new RuntimeException("Sân nhà phải đạt tiêu chuẩn ít nhất 2 sao");
        }

        if (!isVietnam(stadium.getCountry())) {
            throw new RuntimeException("Sân nhà phải nằm tại Việt Nam");
        }
    }


    private void validateBusinessRules(Team team, Season season, SystemRule rule) {
        seasonTeamRepository.findBySeasonIdAndTeamId(season.getId(), team.getId())
                .filter(this::isInactiveSeasonTeam)
                .ifPresent(seasonTeam -> {
                    throw new RuntimeException("Đội bóng đã bị vô hiệu hóa trong mùa giải này, không thể nộp đơn đăng ký.");
                });

        if (seasonTeamRepository.existsBySeasonIdAndTeamId(season.getId(), team.getId())) {
            throw new IllegalArgumentException("Câu lạc bộ này đã tham gia mùa giải");
        }

        validateExistSeaSonInvitation(team,season);
        validateAcceptedSeasonInvitation(team, season);

        if (registrationTeamRepository.existsBySeasonIdAndTeamIdAndStatusIn(
                season.getId(),
                team.getId(),
                List.of(RegistrationStatus.PENDING, RegistrationStatus.APPROVED))) {
            throw new IllegalArgumentException("Câu lạc bộ này đã có đơn đăng ký trong mùa giải");
        }



        if (rule.getMaxTeams() != null) {
            long approvedTeamCount = seasonTeamRepository.countBySeasonId(season.getId());

            long pendingTeamCount = registrationTeamRepository.countBySeasonIdAndStatus(
                    season.getId(),
                    RegistrationStatus.PENDING
            );

            if (approvedTeamCount + pendingTeamCount >= rule.getMaxTeams()) {
                throw new IllegalArgumentException(
                        "Mùa giải đã đạt số đội tối đa:" + rule.getMaxTeams() +"Không thể đăng ký"
                );
            }
        }
    }
    /**
     * Kiểm tra CLB đã chấp nhận lời mời tham gia mùa giải hay chưa.
     * Chỉ khi lời mời ở trạng thái ACCEPTED thì CLB mới được nộp đơn đăng ký.
     */
    private boolean isInactiveSeasonTeam(SeasonTeam seasonTeam) {
        return seasonTeam != null
                && seasonTeam.getStatus() != null
                && "INACTIVE".equalsIgnoreCase(seasonTeam.getStatus().trim());
    }

    private void validateAcceptedSeasonInvitation(Team team, Season season) {
        boolean hasAcceptedInvitation =
                seasonInvitationRepository.existsBySeasonIdAndTeamIdAndStatusIn(
                        season.getId(),
                        team.getId(),
                        List.of(InvitationStatus.ACCEPTED)
                );

        if (!hasAcceptedInvitation) {
            throw new RuntimeException(
                    "Câu lạc bộ chưa chấp nhận lời mời tham gia mùa giải"
            );
        }
    }

    private void validateExistSeaSonInvitation(Team team , Season season)
    {
        boolean existSeasonInvitation = seasonInvitationRepository.existsBySeasonIdAndTeamId(season.getId(),team.getId());

        if(!existSeasonInvitation)
        {
            throw new RuntimeException(
              "Câu lạc bộ không được mời than gia mùa giải"
            );
        }
    }


    // Nhận vào danh sách DTO để check trùng, và danh sách Entity gốc (lấy từ DB) để check tuổi
    private void validatePlayerList(
            List<PlayerRegistrationDTO> dtoList,
            List<Player> dbPlayers,
            SystemRule rule,
            Season season
    ) {
        Set<Integer> shirtNumbers = new HashSet<>();
        Set<Long> playerIds = new HashSet<>();

        LocalDate referenceDate = season.getStartDate() != null
                ? season.getStartDate()
                : LocalDate.now();

        for (PlayerRegistrationDTO pDto : dtoList) {
            if (pDto.getPlayerId() == null) {
                throw new IllegalArgumentException("Cầu thủ không được để trống");
            }

            if (pDto.getShirtNumber() == null) {
                throw new IllegalArgumentException("Số áo cầu thủ không được để trống");
            }

            if (!shirtNumbers.add(pDto.getShirtNumber())) {
                throw new IllegalArgumentException(
                        "Có cầu thủ bị trùng số áo (" + pDto.getShirtNumber() + ") trong đơn đăng ký"
                );
            }

            if (!playerIds.add(pDto.getPlayerId())) {
                throw new IllegalArgumentException(
                        "Cầu thủ ID " + pDto.getPlayerId() + " bị chọn nhiều lần trong đơn"
                );
            }

            if (pDto.getPosition() == null || pDto.getPosition().isBlank()) {
                throw new IllegalArgumentException(
                        "Vị trí đăng ký của cầu thủ không được để trống"
                );
            }
        }

        int squadSize = dbPlayers.size();

        if (rule.getMinPlayers() != null && squadSize < rule.getMinPlayers()) {
            throw new IllegalArgumentException(
                    "Số lượng cầu thủ (" + squadSize + ") chưa đạt tối thiểu (" + rule.getMinPlayers() + ")"
            );
        }

        if (rule.getMaxPlayers() != null && squadSize > rule.getMaxPlayers()) {
            throw new IllegalArgumentException(
                    "Số lượng cầu thủ (" + squadSize + ") vượt quá tối đa (" + rule.getMaxPlayers() + ")"
            );
        }

        if (rule.getMaxForeignPlayers() != null) {
            long foreignCount = dbPlayers.stream()
                    .filter(this::isForeignPlayer)
                    .count();

            if (foreignCount > rule.getMaxForeignPlayers()) {
                throw new IllegalArgumentException(
                        "Số ngoại binh (" + foreignCount + ") vượt quá giới hạn theo luật ("
                                + rule.getMaxForeignPlayers() + ")"
                );
            }
        }

        for (Player player : dbPlayers) {
            if (player.getDateOfBirth() != null) {
                int age = Period.between(player.getDateOfBirth(), referenceDate).getYears();

                if (rule.getMinAge() != null && age < rule.getMinAge()) {
                    throw new IllegalArgumentException(
                            "Cầu thủ " + player.getName() + " (" + age + " tuổi) chưa đủ tuổi quy định"
                    );
                }

                if (rule.getMaxAge() != null && age > rule.getMaxAge()) {
                    throw new IllegalArgumentException(
                            "Cầu thủ " + player.getName() + " (" + age + " tuổi) vượt quá tuổi quy định"
                    );
                }
            }
        }
    }

    private void validateCoachList(List<CoachRegistrationDTO> coaches, SystemRule rule) {
        Set<Long> coachIds = new HashSet<>();

        int coachCount = coaches == null ? 0 : coaches.size();
        Integer minCoaches = rule != null && rule.getMinCoaches() != null ? rule.getMinCoaches() : 3;
        Integer maxCoaches = rule != null ? rule.getMaxCoaches() : null;

        if (coachCount < minCoaches) {
            throw new IllegalArgumentException(
                    "Số lượng ban huấn luyện (" + coachCount + ") chưa đạt tối thiểu theo luật (" + minCoaches + ")"
            );
        }

        if (maxCoaches != null && coachCount > maxCoaches) {
            throw new IllegalArgumentException(
                    "Số lượng ban huấn luyện (" + coachCount + ") vượt quá tối đa theo luật (" + maxCoaches + ")"
            );
        }

        long headCoachCount = 0;

        if (coaches == null) {
            return;
        }

        for (CoachRegistrationDTO coach : coaches) {
            if (coach.getCoachId() == null) {
                throw new IllegalArgumentException("ID Huấn luyện viên không được để trống");
            }
            if (!coachIds.add(coach.getCoachId())) {
                throw new IllegalArgumentException("Huấn luyện viên ID " + coach.getCoachId() + " bị chọn nhiều lần trong đơn");
            }

            String role = coach.getRole() == null ? "" : coach.getRole().trim().toLowerCase();
            if (role.contains("hlv trưởng") || role.contains("huấn luyện viên trưởng") || role.contains("head_coach")) {
                headCoachCount++;
            }
        }

        if (headCoachCount != 1) {
            throw new IllegalArgumentException("Ban huấn luyện phải có đúng 01 Huấn luyện viên trưởng");
        }
    }


// ==================== MAPPING HELPERS ====================



// ==================== REALTIME / NOTIFICATION HELPERS ====================




    private void sendRegistrationSubmittedEventToAdmins(RegistrationTeam savedRegistration) {
        List<User> admins = userRepository.findUsersByRoleName("ROLE_ADMIN");
        RealtimeEventDTO event = realtimeEvent(
                "REGISTRATION_SUBMITTED",
                savedRegistration.getId(),
                "REGISTRATION_TEAM",
                "REFETCH_REGISTRATIONS"
        );

        for (User admin : admins) {
            realtimeEventService.sendToUser(admin.getId(), event);
        }
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











    // ==========================================
    // CÁC HÀM MAPPING DTO (Đã viết lại theo Object)
    // ==========================================

    private RegistrationSummaryDTO toSummaryDto(RegistrationTeam reg) {
        return new RegistrationSummaryDTO(
                reg.getId(),
                reg.getSeason() != null ? reg.getSeason().getId() : null,
                reg.getSeason() != null ? reg.getSeason().getName() : null,
                reg.getTeam() != null ? reg.getTeam().getName() : null,
                reg.getTeam() != null ? reg.getTeam().getCity() : null,
                reg.getStatus(),
                Math.toIntExact(registrationPlayerRepository.countByRegistrationTeamId(reg.getId())),
                Math.toIntExact(registrationCoachRepository.countByRegistrationTeamId(reg.getId())),
                reg.getCreatedAt(),
                reg.getNote(),
                reg.getFeeAmount(),
                reg.getFeeStatus(),
                reg.getPaymentProofUrl(),
                reg.getPaidAt()
        );
    }

    private RegistrationDetailDTO toDetailDto(
            RegistrationTeam reg,
            List<RegistrationPlayer> registrationPlayers,
            List<RegistrationCoach> registrationCoaches
    ) {
        Team team = reg.getTeam();
        RegistrationStadium stadium = reg.getStadium();

        return new RegistrationDetailDTO(
                reg.getId(),
                reg.getSeason() != null ? reg.getSeason().getId() : null,
                reg.getSeason() != null ? reg.getSeason().getName() : null,

                team != null ? team.getName() : null,
                team != null ? team.getLogo() : null,
                team != null ? team.getEstablishedYear() : null,
                team != null ? team.getCity() : null,
                team != null ? team.getRegion() : null,
                team != null ? team.getOwner() : null,
                team != null ? team.getDescription() : null,

                stadium != null
                        ? stadium.getName()
                        : (team != null && team.getStadium() != null ? team.getStadium().getName() : null),

                stadium != null
                        ? stadium.getAddress()
                        : (team != null && team.getStadium() != null ? team.getStadium().getAddress() : null),

                stadium != null
                        ? stadium.getCapacity()
                        : (team != null && team.getStadium() != null ? team.getStadium().getCapacity() : null),

                stadium != null
                        ? stadium.getGrass()
                        : (team != null && team.getStadium() != null ? team.getStadium().getGrass() : null),

                stadium != null ? stadium.getCountry() : null,
                stadium != null ? stadium.getFifaStarRating() : null,
                stadium != null ? stadium.getCertificateUrl() : null,

                reg.getStatus(),
                reg.getNote(),
                reg.getCreatedAt(),

                registrationPlayers == null ? List.of() : registrationPlayers.stream()
                        .map(rp -> {
                            Player p = rp.getPlayer();

                            return new RegistrationPlayerViewDTO(
                                    p != null ? p.getName() : null,
                                    p != null ? p.getIDCode() : null,
                                    p != null ? p.getDateOfBirth() : null,
                                    rp.getPosition(),
                                    rp.getShirtNumber(),
                                    p != null ? p.getNationality() : null,
                                    p != null ? p.getHeight() : null,
                                    p != null ? p.getWeight() : null,
                                    false
                            );
                        }).toList(),

                registrationCoaches == null ? List.of() : registrationCoaches.stream()
                        .map(rc -> {
                            Coach c = rc.getCoach();

                            return new RegistrationCoachViewDTO(
                                    c != null ? c.getName() : null,
                                    c != null ? c.getNationality() : null,
                                    c != null ? c.getIDCode() : null,
                                    c != null ? c.getBirthDay() : null,
                                    rc.getTournamentRole(),
                                    c != null ? c.getDes() : null
                            );
                        }).toList(),

                reg.getHomeKitColor(),
                reg.getAwayKitColor(),
                reg.getHomeKitImageUrl(),
                reg.getAwayKitImageUrl(),

                reg.getFeeAmount(),
                reg.getFeeStatus(),
                reg.getPaymentProofUrl(),
                reg.getPaidAt()
        );
    }


}
