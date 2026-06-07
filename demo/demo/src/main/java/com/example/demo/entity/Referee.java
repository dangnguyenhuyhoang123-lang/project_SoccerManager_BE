package com.example.demo.entity;

import com.example.demo.entity.match.MatchReferee;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDate;
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

    @Column(name = "name")
    private String name;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "birth_year")
    private Integer birthYear;

    @Column(name = "nationality")
    private String nationality;

    @Column(name = "phone")
    private String phone;

    @Column(name = "email")
    private String email;

    @Column(name = "level")
    private String level;

    @Column(name = "certification")
    private String certification;

    @Column(columnDefinition = "LONGTEXT")
    @Lob
    private String avatar;

    @Column(name = "status")
    private String status = "ACTIVE";

    @Column(columnDefinition = "TEXT")
    private String note;

    @OneToMany(mappedBy = "referee", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @ToString.Exclude
    private List<MatchReferee> matchAssignments;
}
