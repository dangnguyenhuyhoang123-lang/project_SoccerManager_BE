package com.example.demo.service;

import com.example.demo.dao.match.MatchLineupRepository;
import com.example.demo.dao.match.MatchRepository;
import com.example.demo.dao.match.MatchTacticsRepository;
import com.example.demo.dao.player.PlayerRepository;
import com.example.demo.dao.team.TeamRepository;
import com.example.demo.dto.LineUpSubmit.MatchLineupSubmitDTO;
import com.example.demo.entity.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MatchLineupService {

    private final MatchTacticsRepository tacticsRepo;
    private final MatchLineupRepository lineupRepo;
    private final MatchRepository matchRepo;
    private final TeamRepository teamRepo;
    private final PlayerRepository playerRepo;

    @Transactional
    public void submitLineup(MatchLineupSubmitDTO dto) {
        // 1. Kiểm tra Match và Team có hợp lệ không
        Match match = matchRepo.findById(dto.getMatchId())
                .orElseThrow(() -> new RuntimeException("Trận đấu không tồn tại"));
        Team team = teamRepo.findById(dto.getTeamId())
                .orElseThrow(() -> new RuntimeException("Đội bóng không tồn tại"));

        // 2. XÓA DỮ LIỆU CŨ (Nếu đã nộp rồi mà muốn chỉnh sửa lại)
        // Tìm và xóa Tactics cũ của đội này trong trận này (Cascade sẽ tự xóa Lineup cũ)
        tacticsRepo.deleteByMatchIdAndTeamId(dto.getMatchId(), dto.getTeamId());

        // 3. TẠO CHIẾN THUẬT MỚI (MatchTactics)
        MatchTactics tactics = new MatchTactics();
        tactics.setMatch(match);
        tactics.setTeam(team);
        tactics.setFormationName(dto.getFormationName());
        MatchTactics savedTactics = (MatchTactics) tacticsRepo.save(tactics);

        // 4. LƯU DANH SÁCH VỊ TRÍ CẦU THỦ (MatchLineup)
        List<MatchLineup> lineups = dto.getPlayers().stream().map(pDto -> {
            Player player = playerRepo.findById(pDto.getPlayerId())
                    .orElseThrow(() -> new RuntimeException("Cầu thủ không tồn tại"));

            MatchLineup lineup = new MatchLineup();
            lineup.setMatchTactics(savedTactics); // Gắn vào tactics vừa tạo
            lineup.setPlayer(player);
            lineup.setRole(pDto.getRole());
            lineup.setPosition(pDto.getPosition());
            lineup.setShirtNumber(pDto.getShirtNumber());
            lineup.setLineupOrder(pDto.getLineupOrder());
            lineup.setIsStarting(pDto.getIsStarting());

            // Lấy số áo hiện tại của cầu thủ để lưu vào trận đấu
            lineup.setShirtNumber(player.getShirtNumber());

            return lineup;
        }).collect(Collectors.toList());

        lineupRepo.saveAll(lineups);
    }
}