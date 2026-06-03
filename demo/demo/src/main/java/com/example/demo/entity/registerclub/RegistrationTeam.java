package com.example.demo.entity.registerclub;

import com.example.demo.entity.*;
import com.example.demo.entity.team.Team;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Data
@Table(
        name = "team_registration",
        indexes = {
                @Index(name = "idx_registration_team_season", columnList = "season_id"),
                @Index(name = "idx_registration_team_club", columnList = "team_id"),
                @Index(name = "idx_registration_team_status", columnList = "status")
        }
)
public class RegistrationTeam extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Liên kết với CLB gốc và Mùa giải
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "season_id", nullable = false)
    private Season season;

    // Trạng thái hồ sơ: PENDING, APPROVED, REJECTED
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RegistrationStatus status = RegistrationStatus.PENDING;

    // Quan trọng: Lý do từ chối để Quản lý CLB biết đường sửa
    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    // Các thông tin "chụp ảnh" lúc đăng ký (có thể thay đổi so với mặc định)
    @Column
    private String nameAtRegistration; // Tên đội lúc đăng ký giải này
    @Column
    private String logoAtRegistration;

    @OneToMany(mappedBy = "registrationTeam", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RegistrationPlayer> players;

    @OneToMany(mappedBy = "registrationTeam", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RegistrationCoach> coaches;

    @Embedded
    private RegistrationStadium stadium;

    @Column
    private String note;


    @Column(name = "fee_amount")
    private BigDecimal feeAmount;

    @Column(name = "fee_status")
    @Enumerated(EnumType.STRING)
    private FeeStatus feeStatus; // UNPAID, PAID, WAIVED

    @Column(name = "payment_proof_url")
    private String paymentProofUrl;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;


    @Column
    private String homeKitColor;

    @Column
    private String awayKitColor;

    @Column
    private String homeKitImageUrl;

    @Column
    private String awayKitImageUrl;
}
