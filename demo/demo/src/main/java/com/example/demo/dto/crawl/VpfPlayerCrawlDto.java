package com.example.demo.dto.crawl;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class VpfPlayerCrawlDto {
    private String fullName;
    private String normalizedName;
    private String position;
    private Integer shirtNumber;
    private Integer heightCm;
    private Integer weightKg;
    private LocalDate dateOfBirth;
    private String imageUrl;
    private String sourceUrl;

    private Integer goals;
    private Integer penalties;
    private Integer ownGoals;
    private Integer yellowCards;
    private Integer redCards;
}