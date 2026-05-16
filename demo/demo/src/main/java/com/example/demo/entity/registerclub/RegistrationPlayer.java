package com.example.demo.entity.registerclub;

import com.example.demo.entity.Player;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;

@Entity
@Data
@Table(
        name = "registration_player",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_registration_player_player", columnNames = {"registration_id", "player_id"}),
                @UniqueConstraint(name = "uk_registration_player_shirt", columnNames = {"registration_id", "shirt_number"})
        },
        indexes = {
                @Index(name = "idx_registration_player_registration", columnList = "registration_id"),
                @Index(name = "idx_registration_player_player", columnList = "player_id")
        }
)
public class RegistrationPlayer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registration_id", nullable = false)
    private RegistrationTeam registrationTeam;

    // CHỈ CẦN THAM CHIẾU TỚI PLAYER GỐC LÀ ĐỦ
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;

    // Các thông tin thay đổi theo mùa giải
    @Column(name = "shirt_number", nullable = false)
    private Integer shirtNumber;

    @Column(name = "position_in_tournament")
    private String position;
}
