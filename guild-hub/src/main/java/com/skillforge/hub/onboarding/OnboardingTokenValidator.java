package com.skillforge.hub.onboarding;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import java.util.Optional;

@Component
public class OnboardingTokenValidator {

    private final String secret;
    private final ObjectMapper mapper = new ObjectMapper();

    public OnboardingTokenValidator(@Value("${guild.onboarding.secret:}") String secret) {
        this.secret = secret;
    }

    public boolean isConfigured() {
        return !secret.isBlank();
    }

    public Optional<OnboardingTokenStore.OnboardingRequest> validate(String token) {
        if (secret.isBlank()) return Optional.empty();
        try {
            JsonNode node = mapper.readTree(Base64.getUrlDecoder().decode(token));

            String heroId      = node.path("heroId").asText();
            String githubLogin = node.path("githubLogin").asText();
            int issueNumber    = node.path("issueNumber").asInt();
            long expiry        = node.path("expiry").asLong();
            String sig         = node.path("sig").asText();

            if (System.currentTimeMillis() > expiry) return Optional.empty();

            String payload   = heroId + ":" + githubLogin + ":" + issueNumber + ":" + expiry;
            String expected  = hmac(payload);
            if (!expected.equals(sig)) return Optional.empty();

            return Optional.of(new OnboardingTokenStore.OnboardingRequest(heroId, githubLogin, issueNumber));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private String hmac(String data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(), "HmacSHA256"));
        byte[] raw = mac.doFinal(data.getBytes());
        StringBuilder sb = new StringBuilder();
        for (byte b : raw) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}
