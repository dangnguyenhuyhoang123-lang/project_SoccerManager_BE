package com.example.demo.dto;

import com.example.demo.entity.registerclub.InvitationStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SeasonInvitationResponse {
    private Long id;
    private Long seasonId;
    private String seasonName;

    private Long teamId;
    private String teamName;

    private InvitationStatus status;

    private LocalDateTime invitedAt;
    private LocalDateTime responseDeadline;
    private LocalDateTime respondedAt;


    private String responseNote;
}
