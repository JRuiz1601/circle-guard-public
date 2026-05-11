package com.circleguard.form.service;

import com.circleguard.form.model.HealthSurvey;
import com.circleguard.form.model.Questionnaire;
import com.circleguard.form.model.ValidationStatus;
import com.circleguard.form.repository.HealthSurveyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HealthSurveyServiceTest {

    @Mock
    private HealthSurveyRepository repository;

    @Mock
    private QuestionnaireService questionnaireService;

    @Mock
    private SymptomMapper symptomMapper;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private HealthSurveyService healthSurveyService;

    private HealthSurvey sampleSurvey;

    @BeforeEach
    void setUp() {
        sampleSurvey = new HealthSurvey();
        sampleSurvey.setId(UUID.randomUUID());
        sampleSurvey.setAnonymousId(UUID.randomUUID());
    }

    @Test
    void submitSurveyPersistsRecord() {
        when(questionnaireService.getActiveQuestionnaire()).thenReturn(Optional.empty());
        when(repository.save(any(HealthSurvey.class))).thenReturn(sampleSurvey);

        healthSurveyService.submitSurvey(sampleSurvey);

        verify(repository, times(1)).save(sampleSurvey);
    }

    @Test
    void submitSurveyEmitsKafkaEvent() {
        when(questionnaireService.getActiveQuestionnaire()).thenReturn(Optional.empty());
        when(repository.save(any(HealthSurvey.class))).thenReturn(sampleSurvey);

        healthSurveyService.submitSurvey(sampleSurvey);

        verify(kafkaTemplate, times(1)).send(eq("survey.submitted"), eq(sampleSurvey.getAnonymousId().toString()), any());
    }

    @Test
    void submitSurveyWithAttachmentSetsPendingStatus() {
        sampleSurvey.setAttachmentPath("/path/to/file.pdf");
        when(questionnaireService.getActiveQuestionnaire()).thenReturn(Optional.empty());
        when(repository.save(any(HealthSurvey.class))).thenReturn(sampleSurvey);

        HealthSurvey saved = healthSurveyService.submitSurvey(sampleSurvey);

        assertEquals(ValidationStatus.PENDING, saved.getValidationStatus());
        verify(repository, times(1)).save(sampleSurvey);
    }

    @Test
    void validateSurveyApprovedEmitsCertificateEvent() {
        UUID adminId = UUID.randomUUID();
        when(repository.findById(sampleSurvey.getId())).thenReturn(Optional.of(sampleSurvey));
        when(repository.save(any(HealthSurvey.class))).thenReturn(sampleSurvey);

        healthSurveyService.validateSurvey(sampleSurvey.getId(), ValidationStatus.APPROVED, adminId);

        verify(kafkaTemplate, times(1)).send(eq("certificate.validated"), eq(sampleSurvey.getAnonymousId().toString()), any());
    }

    @Test
    void validateSurveyRejectedDoesNotEmitCertificateEvent() {
        UUID adminId = UUID.randomUUID();
        when(repository.findById(sampleSurvey.getId())).thenReturn(Optional.of(sampleSurvey));
        when(repository.save(any(HealthSurvey.class))).thenReturn(sampleSurvey);

        healthSurveyService.validateSurvey(sampleSurvey.getId(), ValidationStatus.REJECTED, adminId);

        verify(kafkaTemplate, never()).send(eq("certificate.validated"), anyString(), any());
    }
}