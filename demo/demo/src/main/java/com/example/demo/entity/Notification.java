package com.example.demo.entity;

import com.example.demo.entity.user.User;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "notifications")
public class Notification extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long receiverId;

    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    private String type;

    private Boolean isRead = false;

    private Long referenceId;

    private String referenceType;

    private LocalDateTime createdAt = LocalDateTime.now();

    public Notification() {
    }

    public Notification(Long receiverId, String title, String content, String type, Long referenceId, String referenceType) {
        this.receiverId = receiverId;
        this.title = title;
        this.content = content;
        this.type = type;
        this.referenceId = referenceId;
        this.referenceType = referenceType;
        this.isRead = false;
        this.createdAt = LocalDateTime.now();
    }

}