package com.example.demo.dto.registrationclub;

import com.example.demo.entity.team.GrassType;
import lombok.Data;

@Data
public class StadiumRegistrationDTO {

    private String name;


    private String address;


    private Integer capacity;


    private GrassType grass;


    private Integer fifaStarRating;

    private String country;

    private String city;

    private String certificateUrl;


}
