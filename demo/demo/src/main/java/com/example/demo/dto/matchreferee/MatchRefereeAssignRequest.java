package com.example.demo.dto.matchreferee;

import lombok.Data;

@Data
public class MatchRefereeAssignRequest {
    private Long matchId;
    private Long refereeId;
    private String role;
}
