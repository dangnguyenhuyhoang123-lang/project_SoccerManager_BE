package com.example.demo.dto.aipredict;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class MatchPredictRequest {
    private String homeTeam;
    private String awayTeam;
}
