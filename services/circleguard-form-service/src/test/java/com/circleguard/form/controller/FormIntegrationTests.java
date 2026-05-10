package com.circleguard.form.controller;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FormIntegrationTests {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Test
    void getQuestionnairesReturnsOk() throws Exception {
        mockMvc.perform(get("/api/v1/questionnaires"))
            .andExpect(status().isOk());
    }

    @Test
    void postQuestionnaireWithValidBodyReturnsOk() throws Exception {
        mockMvc.perform(post("/api/v1/questionnaires")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"title\":\"Test\",\"description\":\"Desc\",\"active\":false}"))
            .andExpect(status().isOk());
    }
}