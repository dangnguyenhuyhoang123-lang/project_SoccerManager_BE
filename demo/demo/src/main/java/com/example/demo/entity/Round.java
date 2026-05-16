package com.example.demo.entity;


import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(
        name = "rounds",
        uniqueConstraints = @UniqueConstraint(name = "uk_round_season_number", columnNames = {"season_id", "round_number"}),
        indexes = {
                @Index(name = "idx_round_season", columnList = "season_id"),
                @Index(name = "idx_round_dates", columnList = "start_date,end_date")
        }
)
@Data
public class Round extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "round_number")
    private Integer roundNumber;

    @Column
    private String name;

    @Column(name = "start_date")
    private LocalDateTime startDate;

    @Column(name = "end_date")
    private LocalDateTime endDate;

    @Column(name = "max_matches")
    private Integer maxMatches;

    @Column
    private String status;

    @Column
    private Boolean notifyTeams;

//    @Column
//    private Boolean autoAssignReferee;
//    option tự phân công trọng tài

    @ManyToOne
    @JoinColumn(name = "season_id", nullable = false)
    private Season season;

    @OneToMany(mappedBy = "round")
    private List<Match> matches;
}
