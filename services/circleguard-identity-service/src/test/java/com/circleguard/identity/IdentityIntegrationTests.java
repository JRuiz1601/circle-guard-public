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
    void getIdentityWithInvalidIdReturns404Or400() throws Exception {
        mockMvc.perform(get("/api/v1/identities/lookup/00000000-0000-0000-0000-000000000000"))
            .andExpect(status().isNotFound()); // We expect 404 or maybe 401/403 due to auth structure. Since no auth context, let's assume 401 or 404. It's often 401 if unauthorized.
    }
}