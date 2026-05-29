package com.example.demo.dao;

import com.example.demo.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByReceiverIdOrderByCreatedAtDesc(Long receiverId);
    Long countByReceiverIdAndIsReadFalse(Long receiverId);
}
