package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Entity
@Data
@Table(name = "coach")
public class Coach extends BaseEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column
    private Long id;

    @Column
    private String name;

    @Column
    private String nationality;

    @Column(name = "id_code")
    private String IDCode;

    @Column(columnDefinition = "LONGTEXT")
    @Lob
    private String avatar;

    @Column(name = "birth_day")
    private LocalDate birthDay;

    @Column
    private String des;

    @Column
    private String status;


    // Xem sự nghiệp của HLV này đã qua những đội bóng nào
    @OneToMany(mappedBy = "coach")
    private List<SeasonTeamCoach> careerHistory;
}