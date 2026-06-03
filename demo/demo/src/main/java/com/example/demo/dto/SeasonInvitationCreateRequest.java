package com.example.demo.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SeasonInvitationCreateRequest {
    private Long teamId;
    private LocalDateTime responseDeadline;
}
