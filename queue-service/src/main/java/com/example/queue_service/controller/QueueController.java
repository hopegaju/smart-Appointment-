package com.example.queue_service.controller;

import com.example.queue_service.dto.QueueStatusResponse;
import com.example.queue_service.dto.TokenGenerationRequest;
import com.example.queue_service.dto.TokenResponse;
import com.example.queue_service.service.QueueService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/API/QUEUE")
@RequiredArgsConstructor
public class QueueController {
    private final QueueService queueService;

    @PostMapping("/generate")
    public ResponseEntity<TokenResponse> generateQueue(@RequestBody TokenGenerationRequest request) {
        TokenResponse response= queueService.generateToken(request);
        return ResponseEntity.ok(response);
    }
    @GetMapping("/token/{tokenId}")
    public ResponseEntity<TokenResponse> getToken(@PathVariable String tokenId) {
        TokenResponse response = queueService.getTokenStatus(tokenId);
        return ResponseEntity.ok(response);
    }
    @GetMapping("/status")
    public ResponseEntity<QueueStatusResponse> getQueueStatus(
            @RequestParam String doctorId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
            ){
        QueueStatusResponse response = queueService.getQueueStatus(doctorId, date);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/patient/{patientId}")
    public ResponseEntity<List<TokenResponse>> getPatientTokens(
            @PathVariable String patientId) {
        List<TokenResponse> tokens = queueService.getPatientTokens(patientId);
        return ResponseEntity.ok(tokens);
    }

    @PostMapping("/call-next")
    public ResponseEntity<TokenResponse> callNextToken(
            @RequestParam String doctorId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        TokenResponse response = queueService.callNextToken(doctorId, date);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/token/{tokenId}/start")
    public ResponseEntity<Void> startConsultation(@PathVariable String tokenId) {
        queueService.startConsultation(tokenId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/token/{tokenId}/complete")
    public ResponseEntity<Void> completeToken(@PathVariable String tokenId) {
        queueService.completeToken(tokenId);
        return ResponseEntity.ok().build();
    }
}
