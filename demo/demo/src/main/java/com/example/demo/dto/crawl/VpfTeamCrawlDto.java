package com.example.demo.dto.crawl;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VpfTeamCrawlDto {
    private String seasonYear;
    private String seasonName;
    private String teamName;
    private String normalizedName;
    private String logoUrl;
    private String sourceUrl;
    private String sourceName;
}
