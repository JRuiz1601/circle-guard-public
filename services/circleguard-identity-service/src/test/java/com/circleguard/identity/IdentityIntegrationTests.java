package com.circleguard.identity;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class IdentityIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getIdentityWithValidJwtReturnsIdentity() throws Exception {
        // mockMvc.perform(get("/identities/1").header("Authorization", "Bearer valid.jwt.token")).andExpect(status().isOk());
    }
}
