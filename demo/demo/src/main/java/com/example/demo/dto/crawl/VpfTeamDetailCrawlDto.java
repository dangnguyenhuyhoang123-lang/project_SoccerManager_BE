package com.example.demo.dto.crawl;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class VpfTeamDetailCrawlDto {
    private String teamName;
    private String normalizedName;
    private String logoUrl;
    private String sourceUrl;

    private String homeStadiumName;
    private Integer stadiumCapacity;
    private String coachName;

    private String stadiumAddress;
    private String stadiumImageUrl;

    private String description;

    private List<VpfPlayerCrawlDto> players = new ArrayList<>();
}
