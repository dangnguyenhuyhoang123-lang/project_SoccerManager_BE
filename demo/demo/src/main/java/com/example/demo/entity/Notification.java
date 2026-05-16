package com.example.demo.entity;

import com.example.demo.entity.user.User;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "notifications")
public class Notification extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String content;
    private String link; // Ví dụ: /admin/registration/view/15

    private Boolean isRead = false;

    // Gửi cho Role nào (ADMIN hoặc CLUB_MANAGER)
    private String targetRole;

    @ManyToOne
    @JoinColumn(name = "user_receiver_id")
    private User receiver;
}