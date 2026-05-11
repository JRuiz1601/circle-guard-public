package com.circleguard.auth.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.UUID;

@Component
public class IdentityClient {

    private final RestTemplate restTemplate = new RestTemplate();
    private final String mapUrl;

    public IdentityClient(
            @Value("${circleguard.identity.map-url:http://localhost:8083/api/v1/identities/map}") String mapUrl) {
        this.mapUrl = mapUrl;
    }

    public UUID getAnonymousId(String realIdentity) {
        Map<String, String> request = Map.of("realIdentity", realIdentity);
        Map<?, ?> response = restTemplate.postForObject(mapUrl, request, Map.class);
        if (response == null || response.get("anonymousId") == null) {
            throw new IllegalStateException("identity service returned no anonymousId");
        }
        return UUID.fromString(response.get("anonymousId").toString());
    }
}
