package com.example.demo.entity.registerclub;

import com.example.demo.entity.GrassType;
import jakarta.persistence.*;
import lombok.Data;

@Embeddable
@Data
public class RegistrationStadium {

    @Column(name = "stadium_name")
    private String name;

    @Column(name = "stadium_address")
    private String address;

    @Column(name = "stadium_capacity")
    private Integer capacity;

    @Enumerated(EnumType.STRING)
    @Column(name = "grass_type")
    private GrassType grass;
}
