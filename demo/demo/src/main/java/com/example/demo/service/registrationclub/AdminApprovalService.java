package com.example.demo.service.registrationclub;

import com.example.demo.dao.player.PlayerSeasonRepository;
import com.example.demo.dao.registerteam.RegistrationTeamRepo;
import com.example.demo.dao.season.SeasonTeamCoachRepo;
import com.example.demo.dao.season.SeasonTeamRepository;
import com.example.demo.entity.*;
import com.example.demo.entity.registerclub.*;
import com.example.demo.service.StandingService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class AdminApprovalService {

    private final RegistrationTeamRepo registrationTeamRepository;
    private final SeasonTeamRepository seasonTeamRepository;
    private final PlayerSeasonRepository playerSeasonRepository;
    private final SeasonTeamCoachRepo seasonTeamCoachRepository;
    private final StandingService standingService;

    @Transactional
    public void approveRegistration(Long registrationId) {
        RegistrationTeam reg = registrationTeamRepository.findById(registrationId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn đăng ký"));

        if (reg.getStatus() != RegistrationStatus.PENDING) {
            throw new RuntimeException("Đơn này đã được xử lý");
        }

        Team team = reg.getTeam();
        Season season = reg.getSeason();

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
        registrationTeamRepository.save(reg);
    }

    @Transactional
    public void rejectRegistration(Long id, String reason) {
        RegistrationTeam reg = registrationTeamRepository.findById(id).get();
        reg.setStatus(RegistrationStatus.REJECTED);
        reg.setRejectionReason(reason); // Bạn nhớ thêm trường này vào Entity RegistrationTeam nhé
        registrationTeamRepository.save(reg);
    }
}