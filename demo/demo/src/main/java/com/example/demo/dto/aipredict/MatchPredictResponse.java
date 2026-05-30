package com.example.demo.dto.aipredict;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class MatchPredictResponse {

    private Integer homeScore;
    private Integer awayScore;
}
