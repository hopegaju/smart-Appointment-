package com.example.queue_service.model;

import lombok.Data;
import lombok.Builder;
import java.time.LocalDate;

@Data
@Builder
public class QueueStats {
    private String doctorName;
    private LocalDate date;
    private Integer totalTokens;
    private Integer waitingTokens;
    private Integer completedTokens;
    private Integer averageTokens;
    private Integer currentTokenNumber;
}