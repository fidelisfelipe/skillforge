package com.skillforge.hub.onboarding;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Service
public class CloudAmqpService {

    private static final Logger log = LoggerFactory.getLogger(CloudAmqpService.class);
    private static final String INVITE_URL = "https://customer.cloudamqp.com/api/team/invite";

    private final HttpClient http = HttpClient.newHttpClient();
    private final String apiKey;

    public CloudAmqpService(@Value("${cloudamqp.api-key:}") String apiKey) {
        this.apiKey = apiKey;
    }

    public boolean inviteTeamMember(String email) {
        if (apiKey.isBlank()) {
            log.warn("CLOUDAMQP_API_KEY não configurado — convite não enviado para {}", email);
            return false;
        }
        try {
            String credentials = Base64.getEncoder().encodeToString(
                (apiKey + ":").getBytes(StandardCharsets.UTF_8));
            String body = "{\"email\":\"%s\",\"roles\":[\"monitor\"]}".formatted(email);

            HttpResponse<String> response = http.send(
                HttpRequest.newBuilder()
                    .uri(URI.create(INVITE_URL))
                    .header("Authorization", "Basic " + credentials)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build(),
                HttpResponse.BodyHandlers.ofString());

            boolean ok = response.statusCode() < 300;
            if (ok) log.info("Convite CloudAMQP enviado para {}", email);
            else log.warn("Falha no convite CloudAMQP para {} — status {}: {}", email, response.statusCode(), response.body());
            return ok;
        } catch (Exception e) {
            log.error("Erro ao convidar {} para o CloudAMQP: {}", email, e.getMessage());
            return false;
        }
    }
}
