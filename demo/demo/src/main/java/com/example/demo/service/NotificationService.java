package com.example.demo.service;

import com.example.demo.dao.NotificationRepository;
import com.example.demo.dto.NotificationDTO;
import com.example.demo.entity.Notification;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import com.example.demo.entity.NotificationType;
import java.util.List;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public NotificationService(
            NotificationRepository notificationRepository,
            SimpMessagingTemplate messagingTemplate
    ) {
        this.notificationRepository = notificationRepository;
        this.messagingTemplate = messagingTemplate;
    }

    public NotificationDTO sendToUser(
            Long receiverId,
            String title,
            String content,
            String type,
            Long referenceId,
            String referenceType
    ) {
        Notification notification = new Notification(
                receiverId,
                title,
                content,
                type,
                referenceId,
                referenceType
        );

        Notification saved = notificationRepository.save(notification);
        NotificationDTO dto = toDTO(saved);

        // Gửi realtime riêng cho user đó
        messagingTemplate.convertAndSend(
                "/topic/users/" + receiverId + "/notifications",
                dto
        );

        return dto;
    }

    public List<NotificationDTO> getNotificationsByUser(Long userId) {
        return notificationRepository.findByReceiverIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    public Long countUnread(Long userId) {
        return notificationRepository.countByReceiverIdAndIsReadFalse(userId);
    }

    public void markAsRead(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông báo"));

        notification.setIsRead(true);
        notificationRepository.save(notification);
    }

    private NotificationDTO toDTO(Notification notification) {
        return new NotificationDTO(
                notification.getId(),
                notification.getReceiverId(),
                notification.getTitle(),
                notification.getContent(),
                notification.getType(),
                notification.getIsRead(),
                notification.getReferenceId(),
                notification.getReferenceType(),
                notification.getCreatedAt()
        );
    }

    public void notifyLineupSubmittedToAdmin(
            Long adminUserId,
            String teamName,
            String matchName,
            Long matchId
    ) {
        sendToUser(
                adminUserId,
                "CLB đã nộp đội hình",
                teamName + " đã nộp đội hình cho trận " + matchName + ".",
                NotificationType.LINEUP_SUBMITTED.name(),
                matchId,
                "MATCH_LINEUP"
        );
    }

    public void notifyLineupUpdatedToAdmin(
            Long adminUserId,
            String teamName,
            String matchName,
            Long matchId
    ) {
        sendToUser(
                adminUserId,
                "CLB đã cập nhật đội hình",
                teamName + " đã chỉnh sửa đội hình cho trận " + matchName + ".",
                NotificationType.LINEUP_UPDATED.name(),
                matchId,
                "MATCH_LINEUP"
        );
    }

    public void notifyRegistrationApprovedToClub(
            Long clubManagerUserId,
            String teamName,
            String seasonName,
            Long registrationId
    ) {
        sendToUser(
                clubManagerUserId,
                "Đơn đăng ký đã được duyệt",
                "Đơn đăng ký của " + teamName + " cho mùa giải " + seasonName + " đã được duyệt.",
                NotificationType.REGISTRATION_APPROVED.name(),
                registrationId,
                "REGISTRATION_TEAM"
        );
    }

    public void notifyRegistrationRejectedToClub(
            Long clubManagerUserId,
            String teamName,
            String seasonName,
            Long registrationId,
            String reason
    ) {
        String content = "Đơn đăng ký của " + teamName + " cho mùa giải " + seasonName + " đã bị từ chối.";

        if (reason != null && !reason.isBlank()) {
            content += " Lý do: " + reason;
        }

        sendToUser(
                clubManagerUserId,
                "Đơn đăng ký bị từ chối",
                content,
                NotificationType.REGISTRATION_REJECTED.name(),
                registrationId,
                "REGISTRATION_TEAM"
        );
    }
}
