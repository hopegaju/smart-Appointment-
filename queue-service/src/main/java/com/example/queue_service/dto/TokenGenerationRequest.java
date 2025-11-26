package com.example.queue_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TokenGenerationRequest {
    private String patientId;
    private String doctorId;
    private String departmentId;
    private LocalDate date;
    private String appointmentId;
    private String priority;
}
