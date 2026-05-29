package com.example.demo.dto.crawl;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class VpfSeasonSyncRequest {
    private String leagueName;       // V.League 1, V.League 2
    private String leagueSlug;       // v-league-2026, hang-nhat-2026...
    private String seasonName;       // LPBank V.League 1-2025/26
    private String seasonYear;       // 2025-2026

    private String calendarUrl;      // URL lịch thi đấu VPF
    private String teamsUrl;         // URL danh sách đội
    private String teamSid;          // sid=121994

    private LocalDate startDate;
    private LocalDate endDate;

    private String country;          // Vietnam
    private String scale;            // NATIONAL
}
