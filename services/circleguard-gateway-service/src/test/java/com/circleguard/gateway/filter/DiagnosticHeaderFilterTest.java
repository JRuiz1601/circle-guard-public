package com.circleguard.gateway.filter;

import com.circleguard.gateway.controller.GateController;
import com.circleguard.gateway.service.QrValidationService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;

class DiagnosticHeaderFilterTest {

    @Nested
    @WebMvcTest(GateController.class)
    @TestPropertySource(properties = "feature.diagnostic-header.enabled=true")
    class WhenEnabled {

        @Autowired MockMvc mockMvc;
        @MockBean  QrValidationService validationService;

        @Test
        void headerIsPresent() throws Exception {
            when(validationService.validateToken(any()))
                .thenReturn(new QrValidationService.ValidationResult(true, "GREEN", "Welcome"));

            mockMvc.perform(post("/api/v1/gate/validate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"token\":\"test\"}"))
                .andExpect(header().string("X-CircleGuard-Debug", "enabled"));
        }
    }

    @Nested
    @WebMvcTest(GateController.class)
    @TestPropertySource(properties = "feature.diagnostic-header.enabled=false")
    class WhenDisabled {

        @Autowired MockMvc mockMvc;
        @MockBean  QrValidationService validationService;

        @Test
        void headerIsAbsent() throws Exception {
            when(validationService.validateToken(any()))
                .thenReturn(new QrValidationService.ValidationResult(true, "GREEN", "Welcome"));

            mockMvc.perform(post("/api/v1/gate/validate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"token\":\"test\"}"))
                .andExpect(header().doesNotExist("X-CircleGuard-Debug"));
        }
    }
}
