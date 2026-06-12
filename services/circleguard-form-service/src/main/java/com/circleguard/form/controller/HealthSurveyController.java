package com.circleguard.form.controller;

import com.circleguard.form.model.HealthSurvey;
import com.circleguard.form.service.HealthSurveyService;
import io.micrometer.core.instrument.Metrics;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/surveys")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class HealthSurveyController {
    private final HealthSurveyService surveyService;

    @PostMapping
    public ResponseEntity<HealthSurvey> submit(@RequestBody HealthSurvey survey) {
        HealthSurvey submitted = surveyService.submitSurvey(survey);
        Metrics.counter("circleguard.form.surveys.submitted").increment();
        return ResponseEntity.ok(submitted);
    }
}
