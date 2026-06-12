package com.circleguard.gateway.controller;

import com.circleguard.gateway.service.QrValidationService;
import io.micrometer.core.instrument.Metrics;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/gate")
@RequiredArgsConstructor
public class GateController {
    private final QrValidationService validationService;

    @PostMapping("/validate")
    public ResponseEntity<QrValidationService.ValidationResult> validate(@RequestBody Map<String, String> request) {
        String token = request.get("token");
        QrValidationService.ValidationResult result = validationService.validateToken(token);
        Metrics.counter("circleguard.gateway.qr.validations", "result", validationResultLabel(result)).increment();
        return ResponseEntity.ok(result);
    }

    private String validationResultLabel(QrValidationService.ValidationResult result) {
        if (result.valid()) {
            return "allowed";
        }
        if (result.message() != null && result.message().toLowerCase().contains("denied")) {
            return "denied";
        }
        return "invalid";
    }
}
