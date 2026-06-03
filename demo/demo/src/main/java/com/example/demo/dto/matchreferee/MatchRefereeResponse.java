package com.example.demo.dto.matchreferee;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MatchRefereeResponse {
    private Long id;

    private Long matchId;

    private Long refereeId;
    private String refereeName;
    private String refereeNationality;

    private String role;
}