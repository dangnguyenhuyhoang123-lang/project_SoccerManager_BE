package com.example.demo.dto.registrationclub;

import jakarta.persistence.Column;
import lombok.Data;


@Data
public class TeamRegistrationDTO {

    private Long id;

    private String managingOrganization;


    private String organizationCountry;


    private String organizationAddress;


    private String businessLicenseNo;
    private String note;
}
