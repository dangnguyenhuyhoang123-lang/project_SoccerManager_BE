package com.example.demo.dto.registrationclub;

import lombok.Data;

import java.util.List;

@Data
public class FullRegistrationDTO {

    private long seasonID;
    private TeamRegistrationDTO teamInfo;

    private StadiumRegistrationDTO stadiumInfo;

    private List<PlayerRegistrationDTO> listPlayerInfo;

    private List<CoachRegistrationDTO> listCoachInfo;

}
