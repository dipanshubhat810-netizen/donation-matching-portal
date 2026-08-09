package com.sevasahayog.donationmatching.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.charset.StandardCharsets;

@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(String secret, long expirationMs) {

    public JwtProperties {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("JWT secret is not configured. Set the JWT_SECRET environment variable.");
        }
        if (secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("JWT secret must be at least 32 bytes long (HS256 requires a 256-bit key).");
        }
        if (expirationMs <= 0) {
            throw new IllegalStateException("JWT expiration must be a positive number of milliseconds.");
        }
    }
}
