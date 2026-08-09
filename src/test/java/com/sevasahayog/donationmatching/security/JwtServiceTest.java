package com.sevasahayog.donationmatching.security;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private final JwtService jwtService = new JwtService(
            new JwtProperties("unit-test-secret-unit-test-secret-unit-test-secret", 3600000));

    @Test
    void tokenCarriesOnlySubjectAndTimeClaims() throws Exception {
        String token = jwtService.generateToken("alice@example.com");

        String[] parts = token.split("\\.");
        assertThat(parts).hasSize(3);
        byte[] payload = Base64.getUrlDecoder().decode(parts[1]);
        JsonNode json = new ObjectMapper().readTree(payload);

        assertThat(json.get("sub").asText()).isEqualTo("alice@example.com");
        assertThat(json.has("iat")).isTrue();
        assertThat(json.has("exp")).isTrue();
        assertThat(json.has("role")).isFalse();
        assertThat(json.toString()).doesNotContain("role");
    }
}
