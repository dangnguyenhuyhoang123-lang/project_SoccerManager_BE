package com.example.demo.service.season;

import com.example.demo.dao.season.SeasonInvitationRepository;
import com.example.demo.dao.season.SeasonRepository;
import com.example.demo.dao.season.SeasonTeamRepository;
import com.example.demo.dao.team.TeamRepository;
import com.example.demo.dao.user.UserRepository;
import com.example.demo.dto.RealtimeEventDTO;
import com.example.demo.dto.SeasonInvitationCreateRequest;
import com.example.demo.dto.SeasonInvitationResponse;
import com.example.demo.entity.season.Season;
import com.example.demo.entity.registerclub.InvitationStatus;
import com.example.demo.entity.registerclub.SeasonInvitation;
import com.example.demo.entity.team.Team;
import com.example.demo.entity.user.User;
import com.example.demo.service.realtime.NotificationService;
import com.example.demo.service.realtime.RealtimeEventService;
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
    private final SeasonTeamRepository seasonTeamRepository;



    // ==================== QUERY METHODS ====================

    /**
     * Lấy danh sách lời mời của một mùa giải.
     * Dùng cho admin xem các CLB đã được mời và trạng thái phản hồi.
     */
    public List<SeasonInvitationResponse> getBySeason(Long seasonId) {
        return invitationRepository.findBySeasonId(seasonId)
                .stream()
                .map(this::toResponse)
                .toList();
    }


    //    Tìm lơì mời theo id
    private SeasonInvitation getInvitation(Long id) {
        return invitationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lời mời"));
    }

    // Lấy danh sách lời mời theo câu lạc bô
    public List<SeasonInvitationResponse> getMyInvitations() {
        Long teamId = getCurrentUserTeamId();

        return invitationRepository.findByTeamIdOrderByInvitedAtDesc(teamId)
                .stream()
                .map(this::toResponse)
                .toList();
    }




    // ==================== COMMAND METHODS ====================
    @Transactional
    public SeasonInvitationResponse invite(Long seasonId, SeasonInvitationCreateRequest request) {
        Season season = getSeasonOrThrow(seasonId);

        Team team = getTeamOrThrow(request.getTeamId());

        validateCanInviteTeam(season, team);
        SeasonInvitation invitation = invitationRepository.save(
                buildInvitation(season, team, request)
        );

         SeasonInvitation saved =invitationRepository.save(invitation);

        notifyClubManagerAboutInvitation(invitation);

        return toResponse(saved);
}
    @Transactional
    public SeasonInvitationResponse accept(Long id) {
        SeasonInvitation invitation = getActiveInvitationOrThrow(id);

        invitation.setStatus(InvitationStatus.ACCEPTED);
        invitation.setRespondedAt(LocalDateTime.now());

        SeasonInvitation saved = invitationRepository.save(invitation);

        notifyAdminsAboutInvitationAccepted(saved);
        notifyClubManagerInvitationChanged(saved, "SEASON_INVITATION_ACCEPTED");

        return toResponse(saved);
    }

    @Transactional
    public SeasonInvitationResponse decline(Long id, String note) {
        SeasonInvitation invitation = getActiveInvitationOrThrow(id);

        invitation.setStatus(InvitationStatus.DECLINED);
        invitation.setRespondedAt(LocalDateTime.now());
        invitation.setResponseNote(note);

        SeasonInvitation saved = invitationRepository.save(invitation);

        notifyAdminsAboutInvitationDeclined(saved);
        notifyClubManagerInvitationChanged(saved, "SEASON_INVITATION_DECLINED");

        return toResponse(saved);
    }

// ==================== QUERY HELPERS ====================

    //   Lấy mùa giải theo id.
    private Season getSeasonOrThrow(Long seasonId) {
        return seasonRepository.findById(seasonId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy mùa giải"));
    }


    //      Lấy CLB theo id.
    private Team getTeamOrThrow(Long teamId) {
        return teamRepository.findById(teamId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đội bóng"));
    }


    //      Lấy lời mời theo id.

    private SeasonInvitation getInvitationOrThrow(Long id) {
        return invitationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lời mời"));
    }

    /**
     * Lấy lời mời còn hiệu lực.
     * Nếu lời mời đã quá hạn thì cập nhật sang EXPIRED và dừng xử lý.
     */
    private SeasonInvitation getActiveInvitationOrThrow(Long id) {
        SeasonInvitation invitation = getInvitationOrThrow(id);

        validateInvitationIsInvited(invitation);
        expireInvitationIfNeeded(invitation);

        return invitation;
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


// ==================== VALIDATION HELPERS ====================
    /**
     * Kiểm tra CLB có thể được mời vào mùa giải hay không.
     *
     * Điều kiện:
     * - CLB chưa tham gia mùa giải.
     * - CLB chưa có lời mời còn hiệu lực hoặc đã chấp nhận.
     */
    private void validateCanInviteTeam(Season season, Team team) {
        validateTeamNotAlreadyInSeason(season, team);
        validateNoActiveInvitation(season, team);
    }

    /**
     * Đảm bảo CLB chưa có trong danh sách đội tham gia mùa giải.
     */
    private void validateTeamNotAlreadyInSeason(Season season, Team team) {
        if (seasonTeamRepository.existsBySeasonIdAndTeamId(season.getId(), team.getId())) {
            throw new RuntimeException("CLB này đã tham gia mùa giải");
        }
    }

    /**
     * Đảm bảo CLB chưa có lời mời đang còn hiệu lực hoặc đã chấp nhận.
     */
    private void validateNoActiveInvitation(Season season, Team team) {
        boolean existed = invitationRepository.existsBySeasonIdAndTeamIdAndStatusIn(
                season.getId(),
                team.getId(),
                List.of(InvitationStatus.INVITED, InvitationStatus.ACCEPTED)
        );

        if (existed) {
            throw new RuntimeException("Đội bóng đã được mời hoặc đã chấp nhận tham gia");
        }
    }

    /**
     * Chỉ cho phép phản hồi lời mời đang ở trạng thái INVITED.
     */
    private void validateInvitationIsInvited(SeasonInvitation invitation) {
        if (invitation.getStatus() != InvitationStatus.INVITED) {
            throw new RuntimeException("Lời mời không còn hiệu lực");
        }
    }

    /**
     * Nếu lời mời quá hạn thì cập nhật trạng thái EXPIRED và dừng xử lý.
     */
    private void expireInvitationIfNeeded(SeasonInvitation invitation) {
        if (invitation.getResponseDeadline() == null) {
            return;
        }

        if (LocalDateTime.now().isAfter(invitation.getResponseDeadline())) {
            invitation.setStatus(InvitationStatus.EXPIRED);
            invitationRepository.save(invitation);

            notifyAdminsAboutInvitationExpired(invitation);
            notifyClubManagerInvitationChanged(invitation, "SEASON_INVITATION_EXPIRED");

            throw new RuntimeException("Lời mời đã quá hạn");
        }
    }



// ==================== BUSINESS HELPERS ====================

    /**
     * Tạo entity lời mời tham gia mùa giải.
     */
    private SeasonInvitation buildInvitation(
            Season season,
            Team team,
            SeasonInvitationCreateRequest request
    ) {
        SeasonInvitation invitation = new SeasonInvitation();

        invitation.setSeason(season);
        invitation.setTeam(team);
        invitation.setStatus(InvitationStatus.INVITED);
        invitation.setInvitedAt(LocalDateTime.now());
        invitation.setResponseDeadline(resolveResponseDeadline(request));

        return invitation;
    }

    /**
     * Xác định hạn phản hồi lời mời.
     * Nếu admin không nhập hạn phản hồi thì mặc định là 2 tuần.
     */
    private LocalDateTime resolveResponseDeadline(SeasonInvitationCreateRequest request) {
        return request.getResponseDeadline() != null
                ? request.getResponseDeadline()
                : LocalDateTime.now().plusWeeks(2);
    }


// ==================== NOTIFICATION / REALTIME HELPERS ====================
    /**
     * Gửi notification và realtime cho quản lý CLB khi admin gửi lời mời.
     */
    private void notifyClubManagerAboutInvitation(SeasonInvitation invitation) {
        User manager = findClubManagerByTeamId(invitation.getTeam().getId());

        if (manager == null) {
            return;
        }

        notificationService.sendToUser(
                manager.getId(),
                "Bạn nhận được lời mời tham gia mùa giải",
                "CLB " + invitation.getTeam().getName()
                        + " được mời tham gia mùa giải "
                        + invitation.getSeason().getName() + ".",
                "SEASON_INVITATION_SENT",
                invitation.getId(),
                "SEASON_INVITATION"
        );

        RealtimeEventDTO event = realtimeEvent(
                "SEASON_INVITATION_SENT",
                invitation.getId(),
                "SEASON_INVITATION",
                "REFETCH_INVITATIONS"
        );

        realtimeEventService.sendToUser(manager.getId(), event);
    }


    /**
     * Gửi notification và realtime cho admin khi CLB chấp nhận lời mời.
     */
    private void notifyAdminsAboutInvitationAccepted(SeasonInvitation invitation) {
        notifyAdminsAboutInvitationResult(
                invitation,
                "CLB đã chấp nhận lời mời",
                invitation.getTeam().getName()
                        + " đã chấp nhận tham gia mùa giải "
                        + invitation.getSeason().getName() + ".",
                "SEASON_INVITATION_ACCEPTED"
        );
    }


    /**
     * Gửi notification và realtime cho admin khi lời mời quá hạn.
     */
    private void notifyAdminsAboutInvitationExpired(SeasonInvitation invitation) {
        notifyAdminsAboutInvitationResult(
                invitation,
                "Lời mời mùa giải đã quá hạn",
                "Lời mời CLB " + invitation.getTeam().getName()
                        + " tham gia mùa giải "
                        + invitation.getSeason().getName()
                        + " đã quá hạn.",
                "SEASON_INVITATION_EXPIRED"
        );
    }

    /**
     * Gửi thông báo và realtime cho admin khi CLB từ chối lời mời.
     */
    private void notifyAdminsAboutInvitationDeclined(SeasonInvitation invitation) {
        notifyAdminsAboutInvitationResult(
                invitation,
                "CLB đã từ chối lời mời",
                invitation.getTeam().getName()
                        + " đã từ chối tham gia mùa giải "
                        + invitation.getSeason().getName() + ".",
                "SEASON_INVITATION_DECLINED"
        );
    }

    /**
     * Gửi thông báo kết quả phản hồi lời mời cho toàn bộ admin.
     */
    private void notifyAdminsAboutInvitationResult(
            SeasonInvitation invitation,
            String title,
            String message,
            String type
    ) {
        RealtimeEventDTO event = realtimeEvent(
                type,
                invitation.getId(),
                "SEASON_INVITATION",
                "REFETCH_INVITATIONS"
        );

        for (User admin : userRepository.findUsersByRoleName("ROLE_ADMIN")) {
            notificationService.sendToUser(
                    admin.getId(),
                    title,
                    message,
                    type,
                    invitation.getId(),
                    "SEASON_INVITATION"
            );

            realtimeEventService.sendToUser(admin.getId(), event);
        }
    }

    /**
     * Gửi realtime để club manager reload danh sách lời mời của CLB.
     */
    private void notifyClubManagerInvitationChanged(SeasonInvitation invitation, String type) {
        User manager = findClubManagerByTeamId(invitation.getTeam().getId());

        if (manager == null) {
            return;
        }

        RealtimeEventDTO event = realtimeEvent(
                type,
                invitation.getId(),
                "SEASON_INVITATION",
                "REFETCH_INVITATIONS"
        );

        realtimeEventService.sendToUser(manager.getId(), event);
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


// ==================== MAPPING HELPERS ====================


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



}