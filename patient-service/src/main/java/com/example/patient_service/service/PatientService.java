package com.example.patient_service.service;


import com.example.patient_service.dto.AddressDTO;
import com.example.patient_service.dto.PatientRegistrationRequest;
import com.example.patient_service.dto.PatientResponse;
import com.example.patient_service.exception.PatientAlreadyExistsException;
import com.example.patient_service.exception.PatientNotFoundException;
import com.example.patient_service.model.Address;
import com.example.patient_service.model.Patient;
import com.example.patient_service.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PatientService {

    private final PatientRepository patientRepository;
        private final PasswordEncoder passwordEncoder;

    @Transactional
    public PatientResponse registerPatient(PatientRegistrationRequest request) {
        if (patientRepository.existsByEmail(request.getEmail())) {
            throw new PatientAlreadyExistsException("Email already registered");
        }
        if (patientRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new PatientAlreadyExistsException("Phone number already registered");
        }

        Patient patient = Patient.builder()
                .patientId("PT" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .dateOfBirth(request.getDateOfBirth())
                .gender(Patient.Gender.valueOf(request.getGender().toUpperCase()))
                .phoneNumber(request.getPhoneNumber())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .bloodGroup(request.getBloodGroup())
                .medicalHistory(request.getMedicalHistory())
                .allergies(request.getAllergies())
                .status(Patient.PatientStatus.ACTIVE)
                .build();

        if (request.getAddress() != null) {
            patient.setAddress(new Address(
                    request.getAddress().getStreet(),
                    request.getAddress().getCity(),
                    request.getAddress().getState(),
                    request.getAddress().getZipCode(),
                    request.getAddress().getCountry()
            ));
        }

        Patient savedPatient = patientRepository.save(patient);
        return mapToResponse(savedPatient);
    }

    public PatientResponse getPatientById(String patientId) {
        Patient patient = patientRepository.findByPatientId(patientId)
                .orElseThrow(() -> new PatientNotFoundException("Patient not found: " + patientId));
        return mapToResponse(patient);
    }

    @Transactional
    public PatientResponse updatePatient(String patientId, PatientRegistrationRequest request) {
        Patient patient = patientRepository.findByPatientId(patientId)
                .orElseThrow(() -> new PatientNotFoundException("Patient not found: " + patientId));

        patient.setFirstName(request.getFirstName());
        patient.setLastName(request.getLastName());
        patient.setBloodGroup(request.getBloodGroup());
        patient.setMedicalHistory(request.getMedicalHistory());
        patient.setAllergies(request.getAllergies());

        Patient updatedPatient = patientRepository.save(patient);
        return mapToResponse(updatedPatient);
    }

    private PatientResponse mapToResponse(Patient patient) {
        AddressDTO addressDTO = null;
        if (patient.getAddress() != null) {
            addressDTO = new AddressDTO(
                    patient.getAddress().getStreet(),
                    patient.getAddress().getCity(),
                    patient.getAddress().getState(),
                    patient.getAddress().getZipCode(),
                    patient.getAddress().getCountry()
            );
        }

        return PatientResponse.builder()
                .id(patient.getId())
                .patientId(patient.getPatientId())
                .firstName(patient.getFirstName())
                .lastName(patient.getLastName())
                .dateOfBirth(patient.getDateOfBirth())
                .gender(patient.getGender().name())
                .phoneNumber(patient.getPhoneNumber())
                .email(patient.getEmail())
                .address(addressDTO)
                .bloodGroup(patient.getBloodGroup())
                .status(patient.getStatus().name())
                .build();
    }
}
