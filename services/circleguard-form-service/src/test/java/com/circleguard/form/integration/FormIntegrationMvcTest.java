package com.circleguard.form.integration;

import com.circleguard.form.service.StorageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FormIntegrationMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private KafkaTemplate<String, Object> kafkaTemplate;

    @MockBean
    private StorageService storageService;

    @Test
    void getQuestionnairesReturnsOk() throws Exception {
        mockMvc.perform(get("/api/v1/questionnaires"))
            .andExpect(status().isOk());
    }

    @Test
    void postQuestionnaireCreatesRecord() throws Exception {
        mockMvc.perform(post("/api/v1/questionnaires")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"title\":\"Test Questionnaire\",\"description\":\"Daily check\",\"active\":true}"))
            .andExpect(status().isOk());
    }

    @Test
    void submitHealthSurveyReturnsOk() throws Exception {
        UUID anonymousId = UUID.randomUUID();
        mockMvc.perform(post("/api/v1/surveys")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"anonymousId\":\"" + anonymousId + "\",\"hasFever\":false,\"hasCough\":false}"))
            .andExpect(status().isOk());
    }

    @Test
    void uploadFileReturnsFilename() throws Exception {
        when(storageService.store(any())).thenReturn("test.pdf");
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.pdf", "application/pdf", "test data".getBytes()
        );
        mockMvc.perform(multipart("/api/v1/attachments").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.filename").exists());
    }

    @Test
    void requestToNonExistentEndpointReturns404() throws Exception {
        mockMvc.perform(get("/api/v1/nonexistent"))
            .andExpect(status().isNotFound());
    }
}