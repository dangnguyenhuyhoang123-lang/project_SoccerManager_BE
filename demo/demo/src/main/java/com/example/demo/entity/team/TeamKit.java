package com.example.demo.entity.team;

import com.example.demo.entity.SeasonTeam;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TeamKit {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private SeasonTeam seasonTeam;

    @Enumerated(EnumType.STRING)
    private KitType kitType; // HOME, AWAY, THIRD

    private String shirtColor;
    private String shortColor;
    private String sockColor;
    private String imageUrl;
}
