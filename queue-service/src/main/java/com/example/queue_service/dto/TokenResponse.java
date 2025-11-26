package com.example.queue_service.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Builder
public class TokenResponse {
    private String tokenId;
    private String patientId;
    private String doctorId;
    private Integer tokenNumber;
    private LocalDate date;
    private LocalTime issueTime;
    private LocalTime estimatedTime;
    private String status;
    private Integer position;
    private Integer estimatedWaitingMinutes;
    private String priority;
    private String message;
}
