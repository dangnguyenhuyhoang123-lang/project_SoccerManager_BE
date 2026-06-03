package com.example.demo.entity.registerclub;

import com.example.demo.entity.Season;
import com.example.demo.entity.team.Team;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SeasonInvitation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private Season season;



    @ManyToOne
    private Team team;

    @Enumerated(EnumType.STRING)
    private InvitationStatus status; // INVITED, ACCEPTED, DECLINED, EXPIRED

    @Column
    private LocalDateTime invitedAt;

    @Column
    private LocalDateTime responseDeadline;

    @Column
    private LocalDateTime respondedAt;

    @Column(columnDefinition = "TEXT")
    private String responseNote;
}
