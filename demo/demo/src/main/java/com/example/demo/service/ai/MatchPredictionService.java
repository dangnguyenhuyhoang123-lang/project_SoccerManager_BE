package com.example.demo.service.ai;

import com.example.demo.dto.aipredict.MatchPredictRequest;
import com.example.demo.dto.aipredict.MatchPredictResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class MatchPredictionService {

    private final RestTemplate restTemplate;

    private static final String AI_PREDICT_URL = "http://localhost:5000/api/predict";

    public MatchPredictionService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public MatchPredictResponse predict(String homeTeamName, String awayTeamName) {
        MatchPredictRequest request = new MatchPredictRequest(
                homeTeamName,
                awayTeamName
        );

        return restTemplate.postForObject(
                AI_PREDICT_URL,
                request,
                MatchPredictResponse.class
        );
    }
}
