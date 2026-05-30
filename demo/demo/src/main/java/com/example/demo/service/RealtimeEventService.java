package com.example.demo.service;

import com.example.demo.dto.RealtimeEventDTO;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Collection;

@Service
public class RealtimeEventService {

    private final SimpMessagingTemplate messagingTemplate;

    public RealtimeEventService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

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

    public void sendToUsers(Collection<Long> userIds, RealtimeEventDTO event) {
        if (userIds == null || userIds.isEmpty()) {
            return;
        }

        for (Long userId : userIds) {
            sendToUser(userId, event);
        }
    }
}
