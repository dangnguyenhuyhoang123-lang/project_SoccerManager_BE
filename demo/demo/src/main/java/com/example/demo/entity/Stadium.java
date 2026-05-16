package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.util.List;

@Entity
@Table(name = "stadiums")
@Data
public class Stadium {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column
    private String name;

    @Column
    private String address;

    @Column
    private Integer capacity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GrassType grass;

    @OneToMany(mappedBy = "stadium")
    private List<Team> homeTeams;

}