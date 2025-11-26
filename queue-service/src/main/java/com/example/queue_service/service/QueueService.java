package com.example.queue_service.service;

import com.example.queue_service.dto.*;
import com.example.queue_service.exception.TokenAlreadyExistsException;
import com.example.queue_service.model.QueueToken;
import com.example.queue_service.repository.QueueTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class QueueService {

    private final QueueTokenRepository tokenRepository;
    private static final int AVERAGE_CONSULTATION_MINUTES = 15;

    public TokenResponse generateToken(TokenGenerationRequest request) {
        Optional<QueueToken> existing = tokenRepository.findByPatientIdAndDoctorIdAndDate(
                request.getPatientId(),
                request.getDoctorId(),
                request.getDate()
        );

        if (existing.isPresent() &&
                existing.get().getStatus() == QueueToken.TokenStatus.WAITING) {
            throw new TokenAlreadyExistsException("Token already exists for this date");
        }

        Integer nextTokenNumber = getNextTokenNumber(request.getDoctorId(), request.getDate());
        int position = calculatePosition(request.getDoctorId(), request.getDate());
        int estimatedWaitMinutes = position * AVERAGE_CONSULTATION_MINUTES;
        LocalDateTime estimatedTime = LocalDateTime.now().plusMinutes(estimatedWaitMinutes);

        QueueToken token = QueueToken.builder()
                .tokenId(UUID.randomUUID().toString())
                .patientId(request.getPatientId())
                .doctorId(request.getDoctorId())
                .departmentId(request.getDepartmentId())
                .tokenNumber(nextTokenNumber)
                .date(request.getDate())
                .issueTime(LocalDateTime.now())
                .estimatedTime(estimatedTime)
                .status(QueueToken.TokenStatus.WAITING)
                .position(position)
                .estimatedWaitMinutes(estimatedWaitMinutes)
                .appointmentId(request.getAppointmentId())
                .priority(parsePriority(request.getPriority()))
                .build();

        QueueToken savedToken = tokenRepository.save(token);
        log.info("Generated token: {} for patient: {}", nextTokenNumber, request.getPatientId());

        return mapToResponse(savedToken);
    }

    public TokenResponse getTokenStatus(String tokenId) {
        QueueToken token = tokenRepository.findById(tokenId)
                .orElseThrow(() -> new RuntimeException("Token not found"));

        updateTokenPosition(token);

        return mapToResponse(token);
    }

    public QueueStatusResponse getQueueStatus(String doctorId, LocalDate date) {
        List<QueueToken> waitingTokens = tokenRepository.findByDoctorIdAndDateAndStatus(
                doctorId, date, QueueToken.TokenStatus.WAITING
        );

        List<QueueToken> allTokens = tokenRepository.findByDoctorIdAndDateOrderByTokenNumberAsc(
                doctorId, date
        );

        Integer currentToken = allTokens.stream()
                .filter(t -> t.getStatus() == QueueToken.TokenStatus.IN_PROGRESS)
                .map(QueueToken::getTokenNumber)
                .findFirst()
                .orElse(0);

        Integer lastServed = allTokens.stream()
                .filter(t -> t.getStatus() == QueueToken.TokenStatus.COMPLETED)
                .map(QueueToken::getTokenNumber)
                .max(Integer::compareTo)
                .orElse(0);

        return QueueStatusResponse.builder()
                .doctorId(doctorId)
                .date(date)
                .currentToken(currentToken)
                .totalWaiting(waitingTokens.size())
                .averageWaitMinutes(waitingTokens.size() * AVERAGE_CONSULTATION_MINUTES)
                .lastServedToken(lastServed)
                .build();
    }

    public List<TokenResponse> getPatientTokens(String patientId) {
        List<QueueToken> tokens = new ArrayList<>();
        tokenRepository.findAll().forEach(token -> {
            if (token.getPatientId().equals(patientId)) {
                tokens.add(token);
            }
        });

        return tokens.stream()
                .sorted(Comparator.comparing(QueueToken::getIssueTime).reversed())
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public TokenResponse callNextToken(String doctorId, LocalDate date) {
        List<QueueToken> waitingTokens = tokenRepository.findByDoctorIdAndDateAndStatus(
                doctorId, date, QueueToken.TokenStatus.WAITING
        );

        if (waitingTokens.isEmpty()) {
            throw new RuntimeException("No tokens in queue");
        }

        QueueToken nextToken = waitingTokens.stream()
                .sorted(Comparator
                        .comparing(QueueToken::getPriority,
                                Comparator.comparingInt(QueueToken.TokenPriority::getValue))
                        .thenComparing(QueueToken::getTokenNumber))
                .findFirst()
                .orElseThrow();

        nextToken.setStatus(QueueToken.TokenStatus.CALLED);
        nextToken.setActualCallTime(LocalDateTime.now());
        tokenRepository.save(nextToken);

        log.info("Called token: {} for doctor: {}", nextToken.getTokenNumber(), doctorId);

        return mapToResponse(nextToken);
    }

    public void startConsultation(String tokenId) {
        QueueToken token = tokenRepository.findById(tokenId)
                .orElseThrow(() -> new RuntimeException("Token not found"));

        token.setStatus(QueueToken.TokenStatus.IN_PROGRESS);
        tokenRepository.save(token);
    }

    public void completeToken(String tokenId) {
        QueueToken token = tokenRepository.findById(tokenId)
                .orElseThrow(() -> new RuntimeException("Token not found"));

        token.setStatus(QueueToken.TokenStatus.COMPLETED);
        tokenRepository.save(token);

        updateQueuePositions(token.getDoctorId(), token.getDate());
    }

    private Integer getNextTokenNumber(String doctorId, LocalDate date) {
        Long count = tokenRepository.countByDoctorIdAndDate(doctorId, date);
        return count.intValue() + 1;
    }

    private int calculatePosition(String doctorId, LocalDate date) {
        List<QueueToken> waiting = tokenRepository.findByDoctorIdAndDateAndStatus(
                doctorId, date, QueueToken.TokenStatus.WAITING
        );
        return waiting.size() + 1;
    }

    private void updateTokenPosition(QueueToken token) {
        List<QueueToken> waitingBefore = tokenRepository
                .findByDoctorIdAndDateAndStatus(token.getDoctorId(), token.getDate(),
                        QueueToken.TokenStatus.WAITING)
                .stream()
                .filter(t -> t.getTokenNumber() < token.getTokenNumber())
                .collect(Collectors.toList());

        int newPosition = waitingBefore.size() + 1;
        int newWaitTime = newPosition * AVERAGE_CONSULTATION_MINUTES;

        token.setPosition(newPosition);
        token.setEstimatedWaitMinutes(newWaitTime);
        token.setEstimatedTime(LocalDateTime.now().plusMinutes(newWaitTime));

        tokenRepository.save(token);
    }

    private void updateQueuePositions(String doctorId, LocalDate date) {
        List<QueueToken> waitingTokens = tokenRepository.findByDoctorIdAndDateAndStatus(
                doctorId, date, QueueToken.TokenStatus.WAITING
        );

        for (int i = 0; i < waitingTokens.size(); i++) {
            QueueToken token = waitingTokens.get(i);
            updateTokenPosition(token);
        }
    }

    private QueueToken.TokenPriority parsePriority(String priority) {
        try {
            return QueueToken.TokenPriority.valueOf(priority.toUpperCase());
        } catch (Exception e) {
            return QueueToken.TokenPriority.NORMAL;
        }
    }

    private TokenResponse mapToResponse(QueueToken token) {
        return TokenResponse.builder()
                .tokenId(token.getTokenId())
                .patientId(token.getPatientId())
                .doctorId(token.getDoctorId())
                .tokenNumber(token.getTokenNumber())
                .date(token.getDate())
                .issueTime(token.getIssueTime())
                .estimatedTime(token.getEstimatedTime())
                .status(token.getStatus().name())
                .position(token.getPosition())
                .estimatedWaitMinutes(token.getEstimatedWaitMinutes())
                .priority(token.getPriority().name())
                .message(generateStatusMessage(token))
                .build();
    }

    private String generateStatusMessage(QueueToken token) {
        switch (token.getStatus()) {
            case WAITING:
                return String.format("You are #%d in queue. Estimated wait: %d minutes",
                        token.getPosition(), token.getEstimatedWaitMinutes());
            case CALLED:
                return "Your turn! Please proceed to the doctor.";
            case IN_PROGRESS:
                return "Consultation in progress.";
            case COMPLETED:
                return "Consultation completed.";
            case CANCELED:
                return "Token cancelled.";
            default:
                return "Unknown status";
        }
    }
}