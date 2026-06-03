package com.example.demo.entity;

import com.example.demo.entity.team.Team;
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

    @Column
    private Integer fifaStarRating;

    @Column
    private String country;

    @Column
    private String city;

    @Column
    private String certificateUrl;

    @OneToMany(mappedBy = "stadium")
    private List<Team> homeTeams;


    @Column(name = "source_name", length = 100)
    private String sourceName;

    @Column(name = "normalized_name", length = 255)
    private String normalizedName;

    @Column(name = "image_url", length = 255)
    private String imageUrl;

}