package com.example.demo.dto;

import com.example.demo.entity.MatchStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MatchStatusUpdateDTO {
    private MatchStatus status;
}