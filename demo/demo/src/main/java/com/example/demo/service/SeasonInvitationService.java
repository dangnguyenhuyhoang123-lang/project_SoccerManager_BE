package com.example.demo.service;

import com.example.demo.dao.SeasonInvitationRepository;
import com.example.demo.dao.season.SeasonRepository;
import com.example.demo.dao.team.TeamRepository;
import com.example.demo.dao.user.UserRepository;
import com.example.demo.dto.RealtimeEventDTO;
import com.example.demo.dto.SeasonInvitationCreateRequest;
import com.example.demo.dto.SeasonInvitationResponse;
import com.example.demo.entity.Season;
import com.example.demo.entity.registerclub.InvitationStatus;
import com.example.demo.entity.registerclub.SeasonInvitation;
import com.example.demo.entity.team.Team;
import com.example.demo.entity.user.User;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SeasonInvitationService {

    private final SeasonInvitationRepository invitationRepository;
    private final SeasonRepository seasonRepository;
    private final TeamRepository teamRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final RealtimeEventService realtimeEventService;

    @Transactional
    public SeasonInvitationResponse invite(Long seasonId, SeasonInvitationCreateRequest request) {
        Season season = seasonRepository.findById(seasonId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy mùa giải"));

        Team team = teamRepository.findById(request.getTeamId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đội bóng"));

        boolean existed = invitationRepository.existsBySeasonIdAndTeamIdAndStatusIn(
                seasonId,
                team.getId(),
                List.of(InvitationStatus.INVITED, InvitationStatus.ACCEPTED)
        );

        if (existed) {
            throw new RuntimeException("Đội bóng đã được mời hoặc đã chấp nhận tham gia");
        }

        SeasonInvitation invitation = new SeasonInvitation();
        invitation.setSeason(season);
        invitation.setTeam(team);
        invitation.setStatus(InvitationStatus.INVITED);
        invitation.setInvitedAt(LocalDateTime.now());
        invitation.setResponseDeadline(
                request.getResponseDeadline() != null
                        ? request.getResponseDeadline()
                        : LocalDateTime.now().plusWeeks(2)
        );

        SeasonInvitation saved =invitationRepository.save(invitation);

//        Tìm user được mời tham gia lời giải và thông báo
        User manager = findClubManagerByTeamId(team.getId());

        RealtimeEventDTO event = realtimeEvent(
                "SEASON_INVITATION_SENT",
                saved.getId(),
                "SEASON_INVITATION",
                "REFETCH_INVITATIONS"
        );

        if (manager != null) {
            notificationService.sendToUser(
                    manager.getId(),
                    "Bạn nhận được lời mời tham gia mùa giải",
                    "CLB " + team.getName() + " được mời tham gia mùa giải " + season.getName() + ".",
                    "SEASON_INVITATION_SENT",
                    saved.getId(),
                    "SEASON_INVITATION"
            );

            realtimeEventService.sendToUser(manager.getId(), event);
        }


        return toResponse(saved);
    }

    public List<SeasonInvitationResponse> getBySeason(Long seasonId) {
        return invitationRepository.findBySeasonId(seasonId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public SeasonInvitationResponse accept(Long id) {
        SeasonInvitation invitation = getInvitation(id);



        if (invitation.getStatus() != InvitationStatus.INVITED) {
            throw new RuntimeException("Lời mời không còn hiệu lực");
        }

        if (LocalDateTime.now().isAfter(invitation.getResponseDeadline())) {
            invitation.setStatus(InvitationStatus.EXPIRED);
            invitationRepository.save(invitation);
            throw new RuntimeException("Lời mời đã quá hạn");
        }

        invitation.setStatus(InvitationStatus.ACCEPTED);
        invitation.setRespondedAt(LocalDateTime.now());

        SeasonInvitation saved = invitationRepository.save(invitation);

        RealtimeEventDTO adminEvent = realtimeEvent(
                "SEASON_INVITATION_ACCEPTED",
                saved.getId(),
                "SEASON_INVITATION",
                "REFETCH_INVITATIONS"
        );

        for (User admin : userRepository.findUsersByRoleName("ROLE_ADMIN")) {
            notificationService.sendToUser(
                    admin.getId(),
                    "CLB đã chấp nhận lời mời",
                    saved.getTeam().getName() + " đã chấp nhận tham gia mùa giải " + saved.getSeason().getName() + ".",
                    "SEASON_INVITATION_ACCEPTED",
                    saved.getId(),
                    "SEASON_INVITATION"
            );

            realtimeEventService.sendToUser(admin.getId(), adminEvent);
        }

        realtimeEventService.sendToPublicLeagues(
                realtimeEvent(
                        "SEASON_TEAM_UPDATED",
                        saved.getSeason().getId(),
                        "SEASON_TEAM",
                        "REFETCH_SEASON_TEAMS"
                )
        );


        return toResponse(saved);
    }

    @Transactional
    public SeasonInvitationResponse decline(Long id, String note) {
        SeasonInvitation invitation = getInvitation(id);



        if (invitation.getStatus() != InvitationStatus.INVITED) {
            throw new RuntimeException("Lời mời không còn hiệu lực");
        }

        if (LocalDateTime.now().isAfter(invitation.getResponseDeadline())) {
            invitation.setStatus(InvitationStatus.EXPIRED);
            invitationRepository.save(invitation);
            throw new RuntimeException("Lời mời đã quá hạn");
        }

        invitation.setStatus(InvitationStatus.DECLINED);
        invitation.setRespondedAt(LocalDateTime.now());
        invitation.setResponseNote(note);

        SeasonInvitation saved = invitationRepository.save(invitation);

        RealtimeEventDTO adminEvent = realtimeEvent(
                "SEASON_INVITATION_DECLINED",
                saved.getId(),
                "SEASON_INVITATION",
                "REFETCH_INVITATIONS"
        );

        for (User admin : userRepository.findUsersByRoleName("ROLE_ADMIN")) {
            notificationService.sendToUser(
                    admin.getId(),
                    "CLB đã từ chối lời mời",
                    saved.getTeam().getName() + " đã từ chối tham gia mùa giải " + saved.getSeason().getName() + ".",
                    "SEASON_INVITATION_DECLINED",
                    saved.getId(),
                    "SEASON_INVITATION"
            );

            realtimeEventService.sendToUser(admin.getId(), adminEvent);
        }


        return toResponse(saved);
    }

    private SeasonInvitation getInvitation(Long id) {
        return invitationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lời mời"));
    }


    public List<SeasonInvitationResponse> getMyInvitations() {
        Long teamId = getCurrentUserTeamId();

        return invitationRepository.findByTeamIdOrderByInvitedAtDesc(teamId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private Long getCurrentUserTeamId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("Bạn chưa đăng nhập");
        }

        String userName = authentication.getName();

        User user = userRepository.findByUserName(userName);

        if (user.getTeam() == null || user.getTeam().getId() == null) {
            throw new RuntimeException("Tài khoản hiện tại chưa được gắn với câu lạc bộ");
        }

        return user.getTeam().getId();
    }

    private SeasonInvitationResponse toResponse(SeasonInvitation invitation) {
        return new SeasonInvitationResponse(
                invitation.getId(),

                invitation.getSeason() != null ? invitation.getSeason().getId() : null,
                invitation.getSeason() != null ? invitation.getSeason().getName() : null,

                invitation.getTeam() != null ? invitation.getTeam().getId() : null,
                invitation.getTeam() != null ? invitation.getTeam().getName() : null,

                invitation.getStatus(),

                invitation.getInvitedAt(),
                invitation.getResponseDeadline(),
                invitation.getRespondedAt(),

                invitation.getResponseNote()
        );
    }

//  Tìm club manager
    private User findClubManagerByTeamId(Long teamId) {
        return userRepository.findClubManagerByTeamIdAndRoleName(teamId, "ROLE_CLUB_MANAGER")
                .or(() -> userRepository.findClubManagerByTeamIdAndRoleName(teamId, "CLUB_MANAGER"))
                .or(() -> userRepository.findFirstByTeamId(teamId))
                .orElse(null);
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