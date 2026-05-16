package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

@Entity
@Table(name = "referee")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Referee {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private String name;

    @Column
    private String nationality;

    @Column
    private Integer birthYear;


    @Column(columnDefinition = "LONGTEXT")
    @Lob
    private String avatar;

    @OneToMany(mappedBy = "referee", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @ToString.Exclude // Quan trọng: Tránh lỗi StackOverflow khi in log
    private List<MatchReferee> matchAssignments;

}