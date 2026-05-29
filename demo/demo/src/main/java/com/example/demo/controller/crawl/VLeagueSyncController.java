package com.example.demo.controller.crawl;

import com.example.demo.dto.crawl.VpfMatchCrawlDto;
import com.example.demo.dto.crawl.VpfSeasonSyncRequest;
import com.example.demo.dto.crawl.VpfTeamCrawlDto;
import com.example.demo.dto.crawl.VpfTeamDetailCrawlDto;
import com.example.demo.service.crawl.VpfVLeagueSyncService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vleague/sync")
public class VLeagueSyncController {

    private final VpfVLeagueSyncService vpfVLeagueSyncService;

    public VLeagueSyncController(VpfVLeagueSyncService vpfVLeagueSyncService) {
        this.vpfVLeagueSyncService = vpfVLeagueSyncService;
    }

    @PostMapping("/vpf-calendar")
    public String syncVpfCalendar() {
        vpfVLeagueSyncService.syncVLeagueCalendar();
        return "Sync VPF V.League calendar successfully";
    }

    @GetMapping("/vpf-calendar/preview")
    public List<VpfMatchCrawlDto> previewVpfCalendar() {
        return vpfVLeagueSyncService.previewVLeagueCalendar();
    }

//    TEAM
    @GetMapping("/vpf-teams/preview")
    public List<VpfTeamCrawlDto> previewVpfTeams() {
    return vpfVLeagueSyncService.previewVLeagueTeams();
    }

// TEAM DETAILS
    @GetMapping("/vpf-team-detail/preview")
    public VpfTeamDetailCrawlDto previewTeamDetail(@RequestParam String teamUrl) {
        return vpfVLeagueSyncService.previewTeamDetail(teamUrl);
    }
    @PostMapping("/vpf-teams")
    public String syncVpfTeams() {
        vpfVLeagueSyncService.syncVLeagueTeams();
        return "Sync VPF V.League teams successfully";
    }

    @PostMapping("/vpf-team-details")
    public String syncVpfTeamDetails() {
        vpfVLeagueSyncService.syncVLeagueTeamDetails();
        return "Sync VPF V.League team details successfully";
    }

//  =======

    @PostMapping("/vpf-season/teams")
    public String syncVpfTeamsBySeason(@RequestBody VpfSeasonSyncRequest request) {
        vpfVLeagueSyncService.syncVLeagueTeams(request);
        return "Sync VPF teams by season successfully";
    }

    @PostMapping("/vpf-season/team-details")
    public String syncVpfTeamDetailsBySeason(@RequestBody VpfSeasonSyncRequest request) {
        vpfVLeagueSyncService.syncVLeagueTeamDetails(request);
        return "Sync VPF team details by season successfully";
    }

    @PostMapping("/vpf-season/calendar")
    public String syncVpfCalendarBySeason(@RequestBody VpfSeasonSyncRequest request) {
        vpfVLeagueSyncService.syncVLeagueCalendar(request);
        return "Sync VPF calendar by season successfully";
    }

    @PostMapping("/vpf-season/full")
    public String syncFullVpfSeason(@RequestBody VpfSeasonSyncRequest request) {
        vpfVLeagueSyncService.syncFullSeason(request);
        return "Sync full VPF season successfully";
    }

}
