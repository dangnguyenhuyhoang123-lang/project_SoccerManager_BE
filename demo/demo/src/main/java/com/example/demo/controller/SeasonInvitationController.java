package com.example.demo.controller;

import com.example.demo.dto.InvitationResponseRequest;
import com.example.demo.dto.SeasonInvitationCreateRequest;
import com.example.demo.dto.SeasonInvitationResponse;
import com.example.demo.service.SeasonInvitationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@CrossOrigin
public class SeasonInvitationController {

    private final SeasonInvitationService invitationService;

    @PostMapping("/api/seasons/{seasonId}/invitations")
    public SeasonInvitationResponse invite(
            @PathVariable Long seasonId,
            @RequestBody SeasonInvitationCreateRequest request
    ) {
        return invitationService.invite(seasonId, request);
    }

    @GetMapping("/api/seasons/{seasonId}/invitations")
    public List<SeasonInvitationResponse> getBySeason(@PathVariable Long seasonId) {
        return invitationService.getBySeason(seasonId);
    }

    @GetMapping("/api/invitations/my")
    public List<SeasonInvitationResponse> getMyInvitations() {
        return invitationService.getMyInvitations();
    }

    @PostMapping("/api/invitations/{id}/accept")
    public SeasonInvitationResponse accept(@PathVariable Long id) {
        return invitationService.accept(id);
    }

    @PostMapping("/api/invitations/{id}/decline")
    public SeasonInvitationResponse decline(
            @PathVariable Long id,
            @RequestBody(required = false) InvitationResponseRequest request
    ) {
        return invitationService.decline(id, request != null ? request.getNote() : null);
    }
}