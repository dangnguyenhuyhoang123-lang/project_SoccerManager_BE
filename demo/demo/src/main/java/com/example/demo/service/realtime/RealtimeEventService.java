package com.example.demo.service.realtime;

import com.example.demo.dao.user.UserRepository;
import com.example.demo.dto.RealtimeEventDTO;
import com.example.demo.entity.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RealtimeEventService {

    private final SimpMessagingTemplate messagingTemplate;
    private final UserRepository userRepository;

//  ==================== USER / PRIVATE EVENTS ====================
    //    Thưc hiện gửi realtime cho 1 user cụ thể
    public void sendToUser(Long userId, RealtimeEventDTO event) {
        if (userId == null || event == null) {
            return;
        }

        String topic = "/topic/users/" + userId + "/events";

        try {
            System.out.println(
                    "Send realtime event topic=" + topic
                            + ", type=" + event.type()
                            + ", action=" + event.action()
                            + ", referenceId=" + event.referenceId()
            );
            messagingTemplate.convertAndSend(topic, event);
        } catch (Exception ex) {
            System.out.println("Cannot send realtime event: " + ex.getMessage());
        }
    }
    //    Thưc hiện gửi cùng 1 realtime cho nhiều user cụ thể
    public void sendToUsers(Collection<Long> userIds, RealtimeEventDTO event) {
        if (userIds == null || userIds.isEmpty()) {
            return;
        }

        for (Long userId : userIds) {
            sendToUser(userId, event);
        }
    }
//     ==================== ROLE-BASED EVENTS ====================

    //    Thực hiện gửi realtime cho toàn bộ user là admin
    public void sendToAdmins(RealtimeEventDTO event) {
        if (event == null) {
            return;
        }

        List<User> admins = userRepository.findUsersByRoleName("ROLE_ADMIN");

        for (User admin : admins) {
            sendToUser(admin.getId(), event);
        }
    }
//    ==================== CLUB MANAGER EVENTS ====================

    //  Gửi realtime event cho quản lý câu loạc bộ theo team ID
    public void sendToClubManagerByTeamId(Long teamId, RealtimeEventDTO event) {
        if (teamId == null || event == null) {
            return;
        }

        userRepository.findClubManagerByTeamIdAndRoleName(teamId, "ROLE_CLUB_MANAGER")
                .or(() -> userRepository.findClubManagerByTeamIdAndRoleName(teamId, "CLUB_MANAGER"))
                .or(() -> userRepository.findFirstByTeamId(teamId))
                .ifPresent(manager -> sendToUser(manager.getId(), event));
    }


//    ==================== PUBLIC EVENTS ====================
    //    Gửi realtime event cho 1 pulic topic
    public void sendToPublic(String topicSuffix, RealtimeEventDTO event) {
        if (topicSuffix == null || topicSuffix.isBlank() || event == null) {
            return;
        }

        String topic = "/topic/public/" + topicSuffix;

        try {
            System.out.println(
                    "Send public realtime event topic=" + topic
                            + ", type=" + event.type()
                            + ", action=" + event.action()
                            + ", referenceId=" + event.referenceId()
            );
            messagingTemplate.convertAndSend(topic, event);
        } catch (Exception ex) {
            System.out.println("Cannot send public realtime event: " + ex.getMessage());
        }
    }


    //  Gửi event cho danh sách trận đấu ( các trang public)
    public void sendToPublicMatches(RealtimeEventDTO event) {
        sendToPublic("matches", event);
    }

    // Gửi event cho chi tiết 1 trận đấu
    public void sendToPublicMatch(Long matchId, RealtimeEventDTO event) {
        if (matchId == null) return;
        sendToPublic("matches/" + matchId, event);
    }

    //    Gửi event cho bảng xếp hạng
    public void sendToPublicStandings(Long seasonId, RealtimeEventDTO event) {
        if (seasonId == null) return;
        sendToPublic("seasons/" + seasonId + "/standings", event);
    }

    //    Gửi event cho giải đấu , mùa giải , vòng đấu
    public void sendToPublicLeagues(RealtimeEventDTO event) {
        sendToPublic("leagues", event);
    }
    //    Gửi event cho trang tin tức
    public void sendToPublicNews(RealtimeEventDTO event) {
        sendToPublic("news", event);
    }



}
