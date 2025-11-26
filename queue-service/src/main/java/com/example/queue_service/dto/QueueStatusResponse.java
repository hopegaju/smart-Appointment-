package com.example.queue_service.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class QueueStatusResponse {
    private String doctorId;
    private LocalDate date;
    private Integer currentToken;
    private Integer totalWaiting;
    private Integer averageWaitMinutes;
    private Integer lastServedToken;
}
