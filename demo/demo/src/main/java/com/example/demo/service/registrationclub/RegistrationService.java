package com.example.demo.service.registrationclub;

import com.example.demo.dao.CoachRepository;
import com.example.demo.dao.player.PlayerRepository;
import com.example.demo.dao.registerteam.RegistrationTeamRepo;
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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RegistrationService {

    private final RegistrationTeamRepo teamRegRepo;
    private final SeasonRepository seasonRepository;
    private final TeamRepository teamRepository;
    private final SeasonTeamRepository seasonTeamRepository;
    private final RegistrationTeamRepo registrationTeamRepository;
    private final PlayerRepository playerRepository;
    private  final CoachRepository coachRepository;

    @Transactional
    public RegistrationSummaryDTO submitRegistration(FullRegistrationDTO dto) {
        // 1. Kiểm tra hình dáng DTO
        validateRequestShape(dto);

        // 2. Lấy Season và Team
        Season season = seasonRepository.findById(dto.getSeasonID()).orElseThrow();
        Team team = teamRepository.findById(dto.getTeamInfo().getId()).orElseThrow();

        // 3. Kiểm tra luật của giải (có bị trùng đơn không)
        validateBusinessRules(team, season);

        // 4. KIỂM TRA HLV VÀ CẦU THỦ
        validateCoachList(dto.getListCoachInfo());

        // Tạo list Entity để hứng dữ liệu từ DB
        List<Player> dbPlayersForValidation = new ArrayList<>();
        for (PlayerRegistrationDTO pDto : dto.getListPlayerInfo()) {
            Player p = playerRepository.findById(pDto.getPlayerId()).orElseThrow();
            dbPlayersForValidation.add(p);
        }

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

        return toDetailDto(registration);
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
    private void validateBusinessRules(Team team, Season season) {
        // 1. Check xem Team này đã có đơn nào đang chờ duyệt hoặc đã duyệt chưa (Bạn cần viết thêm hàm này trong Repo)
        if (registrationTeamRepository.existsBySeasonIdAndTeamIdAndStatusIn(
                season.getId(),
                team.getId(),
                List.of(RegistrationStatus.PENDING, RegistrationStatus.APPROVED))) {
            throw new IllegalArgumentException("Câu lạc bộ này đã có đơn đăng ký trong mùa giải");
        }

        // 2. Check xem Team đã chính thức nằm trong mùa giải chưa
        if (seasonTeamRepository.existsBySeasonIdAndTeamId(season.getId(), team.getId())) {
            throw new IllegalArgumentException("Câu lạc bộ này đã tham gia mùa giải");
        }
    }

    // Nhận vào danh sách DTO để check trùng, và danh sách Entity gốc (lấy từ DB) để check tuổi
    private void validatePlayerList(List<PlayerRegistrationDTO> dtoList, List<Player> dbPlayers, SystemRule rule, Season season) {
        Set<Integer> shirtNumbers = new HashSet<>();
        Set<Long> playerIds = new HashSet<>();
        LocalDate referenceDate = season.getStartDate() != null ? season.getStartDate() : LocalDate.now();

        // 1. Validate trên DTO (Check trùng lặp số áo và ID cầu thủ trong cùng 1 đơn)
        for (PlayerRegistrationDTO pDto : dtoList) {
            if (pDto.getShirtNumber() == null) {
                throw new IllegalArgumentException("Số áo cầu thủ không được để trống");
            }
            if (!shirtNumbers.add(pDto.getShirtNumber())) {
                throw new IllegalArgumentException("Có cầu thủ bị trùng số áo (" + pDto.getShirtNumber() + ") trong đơn đăng ký");
            }
            if (!playerIds.add(pDto.getPlayerId())) {
                throw new IllegalArgumentException("Cầu thủ ID " + pDto.getPlayerId() + " bị chọn nhiều lần trong đơn");
            }
        }

        // 2. Validate trên Object thực tế (Rule độ tuổi, số lượng)
        if (rule != null) {
            int squadSize = dbPlayers.size();
            if (rule.getMinPlayers() != null && squadSize < rule.getMinPlayers()) {
                throw new IllegalArgumentException("Số lượng cầu thủ (" + squadSize + ") chưa đạt tối thiểu (" + rule.getMinPlayers() + ")");
            }
            if (rule.getMaxPlayers() != null && squadSize > rule.getMaxPlayers()) {
                throw new IllegalArgumentException("Số lượng cầu thủ (" + squadSize + ") vượt quá tối đa (" + rule.getMaxPlayers() + ")");
            }

            // Tính tuổi từ DB
            for (Player player : dbPlayers) {
                if (player.getDateOfBirth() != null) {
                    int age = Period.between(player.getDateOfBirth(), referenceDate).getYears();
                    if (rule.getMinAge() != null && age < rule.getMinAge()) {
                        throw new IllegalArgumentException("Cầu thủ " + player.getName() + " (" + age + " tuổi) chưa đủ tuổi quy định");
                    }
                    if (rule.getMaxAge() != null && age > rule.getMaxAge()) {
                        throw new IllegalArgumentException("Cầu thủ " + player.getName() + " (" + age + " tuổi) vượt quá tuổi quy định");
                    }
                }
            }
        }
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
                reg.getPlayers() != null ? reg.getPlayers().size() : 0,
                reg.getCoaches() != null ? reg.getCoaches().size() : 0,
                reg.getCreatedAt(),
                reg.getNote()
        );
    }

    private RegistrationDetailDTO toDetailDto(RegistrationTeam reg) {
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
                reg.getPlayers() == null ? List.of() : reg.getPlayers().stream()
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
                reg.getCoaches() == null ? List.of() : reg.getCoaches().stream()
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
