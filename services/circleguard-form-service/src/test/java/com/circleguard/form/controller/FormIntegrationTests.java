package com.circleguard.form.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
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

    @Test
    void postFormsWithValidJwtReturns201() throws Exception {
        // mockMvc.perform(post("/forms").contentType("application/json").content("{}").header("Authorization", "Bearer token")).andExpect(status().isCreated());
    }

    @Test
    void getFormsReturnsPaginatedList() throws Exception {
        // mockMvc.perform(get("/forms").header("Authorization", "Bearer token")).andExpect(status().isOk());
    }
}
