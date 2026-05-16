package com.example.demo.entity.registerclub;

import com.example.demo.entity.Coach;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;

@Entity
@Data
@Table(
        name = "registration_coach",
        uniqueConstraints = @UniqueConstraint(name = "uk_registration_coach_unique", columnNames = {"registration_id", "coach_id"}),
        indexes = {
                @Index(name = "idx_registration_coach_registration", columnList = "registration_id"),
                @Index(name = "idx_registration_coach_coach", columnList = "coach_id")
        }
)
public class RegistrationCoach {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registration_id", nullable = false)
    private RegistrationTeam registrationTeam;

    // CHỈ CẦN THAM CHIẾU TỚI COACH GỐC
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coach_id", nullable = false)
    private Coach coach;



    // Vai trò trong giải đấu này (HLV trưởng, Trợ lý...)
    @Column(nullable = false)
    private String tournamentRole;
}
