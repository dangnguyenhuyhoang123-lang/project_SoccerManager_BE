package com.example.demo.entity;

import com.example.demo.entity.registerclub.RegistrationTeam;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Entity
@Data
@Table(name = "season")
public class Season extends BaseEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private String year;

    @Column
    private String name;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    // --- LIÊN KẾT VỚI BỘ QUY TẮC HỆ THỐNG ---
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "system_rule_id")
    private SystemRule systemRule;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "league_id")
    private League league;

    @OneToMany(mappedBy = "season", cascade = CascadeType.ALL)
    private List<Round> rounds;

    @OneToMany(mappedBy = "season")
    private List<SeasonTeam> seasonTeams;

    @OneToMany(mappedBy = "season")
    private List<RegistrationTeam> registrationTeams;

    // Lấy toàn bộ lịch sử thay đổi HLV của tất cả các đội trong mùa giải này
    @OneToMany(mappedBy = "season")
    private List<SeasonTeamCoach> coachAssignments;


    @Column(name = "vpf_season_url", length = 1000)
    private String vpfSeasonUrl;

    @Column(name = "sportsdb_season", length = 50)
    private String sportsDbSeason;

    @Column(name = "source_name", length = 100)
    private String sourceName;

}
