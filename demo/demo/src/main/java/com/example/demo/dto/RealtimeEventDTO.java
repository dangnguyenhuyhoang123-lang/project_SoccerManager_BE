package com.example.demo.dto;

import java.time.LocalDateTime;

public record RealtimeEventDTO(
        String type,
        Long referenceId,
        String referenceType,
        String action,
        Object payload,
        LocalDateTime createdAt
) {}
