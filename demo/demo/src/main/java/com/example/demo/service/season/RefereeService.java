package com.example.demo.service.season;

import com.example.demo.dao.RefereeRepository;
import com.example.demo.dao.match.MatchRefereeRepository;
import com.example.demo.dto.RealtimeEventDTO;
import com.example.demo.dto.referee.RefereeRequest;
import com.example.demo.dto.referee.RefereeResponse;
import com.example.demo.entity.Referee;
import com.example.demo.service.realtime.RealtimeEventService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RefereeService {
    private final RefereeRepository refereeRepository;
    private final MatchRefereeRepository matchRefereeRepository;
    private final RealtimeEventService realtimeEventService;

    public List<RefereeResponse> getAll() {
        return refereeRepository.findAll().stream().map(this::toResponse).toList();
    }

    public RefereeResponse getById(Long id) {
        return toResponse(findReferee(id));
    }

    @Transactional
    public RefereeResponse create(RefereeRequest request) {
        Referee referee = new Referee();
        apply(referee, request);
        Referee saved = refereeRepository.save(referee);
        sendRefereeRealtimeEvent(saved.getId(), "REFEREE_CREATED");
        return toResponse(saved);
    }

    @Transactional
    public RefereeResponse update(Long id, RefereeRequest request) {
        Referee referee = findReferee(id);
        apply(referee, request);
        Referee saved = refereeRepository.save(referee);
        sendRefereeRealtimeEvent(saved.getId(), "REFEREE_CREATED");
        return toResponse(saved);
    }

    @Transactional
    public void delete(Long id) {
        Referee referee = findReferee(id);
        if (matchRefereeRepository.existsByRefereeId(id)) {
            referee.setStatus("INACTIVE");
            Referee saved = refereeRepository.save(referee);
            sendRefereeRealtimeEvent(saved.getId(), "REFEREE_UPDATED");
            return;
        }
        refereeRepository.delete(referee);
    }

    private Referee findReferee(Long id) {
        if (id == null || id <= 0) throw new RuntimeException("id trọng tài không hợp lệ");
        return refereeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy trọng tài id = " + id));
    }

    private void apply(Referee referee, RefereeRequest request) {
        if (request == null) throw new RuntimeException("Dữ liệu trọng tài không được để trống");
        if (request.getName() == null || request.getName().isBlank()) {
            throw new RuntimeException("Tên trọng tài không được để trống");
        }
        referee.setName(request.getName().trim());
        referee.setDateOfBirth(request.getDateOfBirth());
        referee.setBirthYear(request.getBirthYear());
        referee.setNationality(trim(request.getNationality()));
        referee.setPhone(trim(request.getPhone()));
        referee.setEmail(trim(request.getEmail()));
        referee.setLevel(trim(request.getLevel()));
        referee.setCertification(trim(request.getCertification()));
        referee.setAvatar(trim(request.getAvatar()));
        referee.setStatus(normalizeStatus(request.getStatus()));
        referee.setNote(trim(request.getNote()));
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) return "ACTIVE";
        String value = status.trim().toUpperCase();
        return "INACTIVE".equals(value) ? "INACTIVE" : "ACTIVE";
    }

    private String trim(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private RefereeResponse toResponse(Referee referee) {
        return new RefereeResponse(
                referee.getId(), referee.getName(), referee.getDateOfBirth(), referee.getBirthYear(),
                referee.getNationality(), referee.getPhone(), referee.getEmail(), referee.getLevel(),
                referee.getCertification(), referee.getAvatar(), referee.getStatus(), referee.getNote()
        );
    }

//    =======REALTIME DTO===========

    private void sendRefereeRealtimeEvent(Long refereeId, String type) {
        RealtimeEventDTO event = realtimeEvent(
                type,
                refereeId,
                "REFEREE",
                "REFETCH_REFEREES"
        );

        realtimeEventService.sendToAdmins(event);
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
