package com.example.demo.entity;

public enum EventType {
    GOAL,
    SUBSTITUTION,
    YELLOW_CARD,
    RED_CARD,
    FOUL,      // Để đếm "Phạm lỗi"
    OFFSIDE,   // Để đếm "Việt vị"
    CORNER,    // Để đếm "Phạt góc"
    SHOT,      // Để đếm "Số lần sút"
    SHOT_ON_TARGET // Để đếm "Sút trúng đích"
}
