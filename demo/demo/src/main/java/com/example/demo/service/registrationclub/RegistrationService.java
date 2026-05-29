package com.example.demo.service.registrationclub;

import com.example.demo.dao.CoachRepository;
import com.example.demo.dao.player.PlayerRepository;
import com.example.demo.dao.registerteam.RegistrationCoachRepository;
import com.example.demo.dao.registerteam.RegistrationPlayerRepository;
import com.example.demo.dao.registerteam.RegistrationTeamRepository;
import com.example.demo.dao.season.SeasonRepository;
import com.example.demo.dao.season.SeasonTeamRepository;
import com.example.demo.dao.team.TeamRepository;
import com.example.demo.dto.registrationclub.CoachRegistrationDTO;
import com.example.demo.dto.registrationclub.FullRegistrationDTO;
import com.example.demo.dto.registrationclub.PlayerRegistrationDTO;
import com.example.demo.dto.registrationclub.RegistrationCoachViewDTO;
import com.example.demo.dto.registrationclub.RegistrationDetailDTO;
import com.example.demo.dto.registrationclub.RegistrationPlayerViewDTO;
import com.example.demo.dto.registrationclub.RegistrationSummaryDTO;
import com.example.demo.entity.*;
import com.example.demo.entity.registerclub.RegistrationCoach;
import com.example.demo.entity.registerclub.RegistrationPlayer;
import com.example.demo.entity.registerclub.RegistrationStadium;
import com.example.demo.entity.registerclub.RegistrationStatus;
import com.example.demo.entity.registerclub.RegistrationTeam;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
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

    @Transactional
    public RegistrationSummaryDTO submitRegistration(FullRegistrationDTO dto) {
        // 2. Lấy Season và Team
        Season season = seasonRepository.findById(dto.getSeasonID())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy mùa giải"));

        SystemRule rule = season.getSystemRule();

        Team team = teamRepository.findById(dto.getTeamInfo().getId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy câu lạc bộ"));

        if (rule == null) {
            throw new RuntimeException("Mùa giải chưa được cấu hình bộ luật");
        }

        if (!"ACTIVE".equalsIgnoreCase(rule.getStatus())) {
            throw new RuntimeException("Bộ luật của mùa giải đang tạm ngưng");
        }
        // 1. Kiểm tra hình dáng DTO
        validateRequestShape(dto);





        // 3. Kiểm tra luật của giải (có bị trùng đơn không)
        validateBusinessRules(team, season,rule);

        // 4. KIỂM TRA HLV VÀ CẦU THỦ
        validateCoachList(dto.getListCoachInfo());

        // Tạo list Entity để hứng dữ liệu từ DB
        List<Player> dbPlayersForValidation = new ArrayList<>();
        for (PlayerRegistrationDTO pDto : dto.getListPlayerInfo()) {
            Player p = playerRepository.findById(pDto.getPlayerId()).orElseThrow();
            dbPlayersForValidation.add(p);
        }

        validatePlayersBelongToTeam(dbPlayersForValidation, team);
        // Gọi hàm kiểm tra tuổi và số lượng
        validatePlayerList(dto.getListPlayerInfo(), dbPlayersForValidation, season.getSystemRule(), season);



        // 3. Khởi tạo Đơn đăng ký tổng thể
        RegistrationTeam registration = new RegistrationTeam();
        registration.setTeam(team);
        registration.setSeason(season);
        registration.setStatus(RegistrationStatus.PENDING);

        if (dto.getTeamInfo().getNote() != null) {
            registration.setNote(dto.getTeamInfo().getNote());
        }

        // 4. Xử lý thông tin Sân vận động
        if (dto.getStadiumInfo() != null) {
            RegistrationStadium stadium = new RegistrationStadium();
            stadium.setName(dto.getStadiumInfo().getName());
            stadium.setAddress(dto.getStadiumInfo().getAddress());
            stadium.setCapacity(dto.getStadiumInfo().getCapacity());
            stadium.setGrass(dto.getStadiumInfo().getGrass());
            registration.setStadium(stadium);
        }

        // 5. Xử lý danh sách Cầu thủ
        List<RegistrationPlayer> regPlayers = new ArrayList<>();
        if (dto.getListPlayerInfo() != null) {
            for (PlayerRegistrationDTO pDto : dto.getListPlayerInfo()) {

                // Tìm cầu thủ gốc từ DB dựa vào playerId
                Player player = playerRepository.findById(pDto.getPlayerId())
                        .orElseThrow(() -> new RuntimeException("Cầu thủ ID " + pDto.getPlayerId() + " không tồn tại"));

                // Validate: Cầu thủ này phải thuộc biên chế của CLB đang đăng ký
                if (!player.getTeam().getId().equals(team.getId())) {
                    throw new RuntimeException("Cầu thủ " + player.getName() + " không thuộc biên chế CLB của bạn");
                }

                RegistrationPlayer rp = new RegistrationPlayer();
                rp.setRegistrationTeam(registration);
                rp.setPlayer(player);
                rp.setShirtNumber(pDto.getShirtNumber());
                rp.setPosition(pDto.getPosition()); // Tùy bạn đặt tên field trong DTO là position hay tournamentPosition

                regPlayers.add(rp);
            }
        }
        registration.setPlayers(regPlayers);

        // 6. Xử lý danh sách Ban huấn luyện
        List<RegistrationCoach> regCoaches = new ArrayList<>();
        if (dto.getListCoachInfo() != null) {
            for (CoachRegistrationDTO cDto : dto.getListCoachInfo()) {

                // Tìm HLV gốc từ DB
                Coach coach = coachRepository.findById(cDto.getCoachId())
                        .orElseThrow(() -> new RuntimeException("HLV ID " + cDto.getCoachId() + " không tồn tại"));

                RegistrationCoach rc = new RegistrationCoach();
                rc.setRegistrationTeam(registration);
                rc.setCoach(coach);
                rc.setTournamentRole(cDto.getRole()); // Tùy bạn đặt tên field trong DTO

                regCoaches.add(rc);
            }
        }
        registration.setCoaches(regCoaches);

        // 7. Lưu toàn bộ đơn đăng ký vào DB
        RegistrationTeam savedRegistration = registrationTeamRepository.save(registration);
        return toSummaryDto(savedRegistration);
    }

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

    private void validatePlayersBelongToTeam(List<Player> players, Team team) {
        for (Player player : players) {
            if (player.getTeam() == null || !player.getTeam().getId().equals(team.getId())) {
                throw new IllegalArgumentException(
                        "Cầu thủ " + player.getName() + " không thuộc biên chế CLB " + team.getName()
                );
            }
        }
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
    }

    // Chú ý: Truyền thẳng Object Team vào thay vì đi tìm bằng tên
//    private void validateBusinessRules(Team team, Season season) {
//        // 1. Check xem Team này đã có đơn nào đang chờ duyệt hoặc đã duyệt chưa (Bạn cần viết thêm hàm này trong Repo)
//        if (registrationTeamRepository.existsBySeasonIdAndTeamIdAndStatusIn(
//                season.getId(),
//                team.getId(),
//                List.of(RegistrationStatus.PENDING, RegistrationStatus.APPROVED))) {
//            throw new IllegalArgumentException("Câu lạc bộ này đã có đơn đăng ký trong mùa giải");
//        }
//
//        // 2. Check xem Team đã chính thức nằm trong mùa giải chưa
//        if (seasonTeamRepository.existsBySeasonIdAndTeamId(season.getId(), team.getId())) {
//            throw new IllegalArgumentException("Câu lạc bộ này đã tham gia mùa giải");
//        }
//    }
//    private void validateBusinessRules(Team team, Season season) {
//        if (registrationTeamRepository.existsBySeasonIdAndTeamIdAndStatusIn(
//                season.getId(),
//                team.getId(),
//                List.of(RegistrationStatus.PENDING, RegistrationStatus.APPROVED))) {
//            throw new IllegalArgumentException("Câu lạc bộ này đã có đơn đăng ký trong mùa giải");
//        }
//
//        if (seasonTeamRepository.existsBySeasonIdAndTeamId(season.getId(), team.getId())) {
//            throw new IllegalArgumentException("Câu lạc bộ này đã tham gia mùa giải");
//        }
//
//        SystemRule rule = season.getSystemRule();
//
//        if (rule == null) {
//            throw new IllegalArgumentException("Mùa giải chưa được cấu hình bộ luật");
//        }
//
//        if (!"ACTIVE".equalsIgnoreCase(rule.getStatus())) {
//            throw new IllegalArgumentException("Bộ luật của mùa giải đang tạm ngưng");
//        }
//
//        if (rule.getMaxTeams() != null) {
//            long approvedTeamCount = seasonTeamRepository.countBySeasonId(season.getId());
//            long pendingTeamCount = registrationTeamRepository.countBySeasonIdAndStatus(
//                    season.getId(),
//                    RegistrationStatus.PENDING
//            );
//
//            if (approvedTeamCount + pendingTeamCount >= rule.getMaxTeams()) {
//                throw new IllegalArgumentException(
//                        "Mùa giải đã đạt số đội tối đa theo luật: " + rule.getMaxTeams()
//                );
//            }
//        }
//    }

    private void validateBusinessRules(Team team, Season season, SystemRule rule) {
        if (registrationTeamRepository.existsBySeasonIdAndTeamIdAndStatusIn(
                season.getId(),
                team.getId(),
                List.of(RegistrationStatus.PENDING, RegistrationStatus.APPROVED))) {
            throw new IllegalArgumentException("Câu lạc bộ này đã có đơn đăng ký trong mùa giải");
        }

        if (seasonTeamRepository.existsBySeasonIdAndTeamId(season.getId(), team.getId())) {
            throw new IllegalArgumentException("Câu lạc bộ này đã tham gia mùa giải");
        }

        if (rule.getMaxTeams() != null) {
            long approvedTeamCount = seasonTeamRepository.countBySeasonId(season.getId());

            long pendingTeamCount = registrationTeamRepository.countBySeasonIdAndStatus(
                    season.getId(),
                    RegistrationStatus.PENDING
            );

            if (approvedTeamCount + pendingTeamCount >= rule.getMaxTeams()) {
                throw new IllegalArgumentException(
                        "Mùa giải đã đạt số đội tối đa theo luật: " + rule.getMaxTeams()
                );
            }
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

        if (rule.getMinRegistrationPlayers() != null
                && squadSize < rule.getMinRegistrationPlayers()) {
            throw new IllegalArgumentException(
                    "Số lượng cầu thủ đăng ký (" + squadSize + ") chưa đạt tối thiểu theo luật ("
                            + rule.getMinRegistrationPlayers() + ")"
            );
        }

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

    private void validateCoachList(List<CoachRegistrationDTO> coaches) {
        Set<Long> coachIds = new HashSet<>();

        for (CoachRegistrationDTO coach : coaches) {
            if (coach.getCoachId() == null) {
                throw new IllegalArgumentException("ID Huấn luyện viên không được để trống");
            }
            if (!coachIds.add(coach.getCoachId())) {
                throw new IllegalArgumentException("Huấn luyện viên ID " + coach.getCoachId() + " bị chọn nhiều lần trong đơn");
            }
        }
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
                reg.getNote()
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

                // Nếu không đăng ký sân riêng, lấy sân mặc định của CLB
                stadium != null ? stadium.getName() : (team != null && team.getStadium() != null ? team.getStadium().getName() : null),
                stadium != null ? stadium.getAddress() : (team != null && team.getStadium() != null ? team.getStadium().getAddress() : null),
                stadium != null ? stadium.getCapacity() : (team != null && team.getStadium() != null ? team.getStadium().getCapacity() : null),
                stadium != null ? stadium.getGrass() : (team != null && team.getStadium() != null ? team.getStadium().getGrass() : null),

                reg.getStatus(),
                reg.getNote(),
                reg.getCreatedAt(),

                // Map thông tin cầu thủ từ Object Player gốc
                registrationPlayers == null ? List.of() : registrationPlayers.stream()
                        .map(rp -> {
                            Player p = rp.getPlayer();
                            return new RegistrationPlayerViewDTO(
                                    p.getName(),
                                    p.getIDCode(),
                                    p.getDateOfBirth(),
                                    rp.getPosition(), // Vị trí đăng ký trong giải
                                    rp.getShirtNumber(), // Số áo trong giải
                                    p.getNationality(),
                                    p.getHeight(),
                                    p.getWeight(),
                                    false // is_official có thể set mặc định
                            );
                        }).toList(),

                // Map thông tin HLV từ Object Coach gốc
                registrationCoaches == null ? List.of() : registrationCoaches.stream()
                        .map(rc -> {
                            Coach c = rc.getCoach();
                            return new RegistrationCoachViewDTO(
                                    c.getName(),
                                    c.getNationality(),
                                    c.getIDCode(),
                                    c.getBirthDay(),
                                    rc.getTournamentRole(), // Vai trò trong giải
                                    c.getDes()
                            );
                        }).toList()
        );
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }
}
