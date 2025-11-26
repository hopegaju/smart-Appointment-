package com.example.queue_service.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.index.Indexed;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@RedisHash("QueueToken")
public class QueueToken {
    @Id
    private String id;

    @Indexed
    private String patientId;

    @Indexed
    private String doctorId;

    @Indexed
    private String departmentId;
    private String tokenNumber;

    @Indexed
    private LocalDate date;
    private LocalDateTime issueTime;
    private LocalDateTime estimatedTime;
    private LocalDateTime actualCallTime;

    @Indexed
    private TokenStatus status;
    private Integer position;
    private Integer estimatedWaitMinutes;
    private String appointmentId;
    private TokenPriority priority;
    public enum TokenStatus {
        WAITING,
        CALLED,
        IN_PROGRESS,
        COMPLETED,
        CANCELED,
        NO_SHOW
    }
    public enum TokenPriority {
        EMERGENCY(1),
        HIGH(2),
        NORMAL(3),
        LOW(4);
        private int value;
        TokenPriority(int value) {
            this.value = value;
        }
        public int getValue() {
            return value;
        }
    }
}
