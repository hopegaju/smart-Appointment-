package com.example.queue_service.repository;

import com.example.queue_service.model.QueueToken;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface QueueTokenRepository extends CrudRepository<QueueToken, String> {
    List<QueueToken> findByDoctorIdAndDateOrderByTokenNumberAsc(String doctorId, LocalDate date);
    List<QueueToken> findByDoctorIdAndDateAndStatus(String doctorId, LocalDate date,
                                                    QueueToken.TokenStatus status);
    Optional<QueueToken> findByPatientIdAndDoctorIdAndDate(String patientId, String doctorId,
                                                           LocalDate date);
    Long countByDoctorIdAndDate(String doctorId, LocalDate date);
}
