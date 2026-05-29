package com.skillforge.hub.onboarding;

import com.github.alexdlaird.ngrok.NgrokClient;
import com.github.alexdlaird.ngrok.conf.JavaNgrokConfig;
import com.github.alexdlaird.ngrok.protocol.CreateTunnel;
import com.github.alexdlaird.ngrok.protocol.Tunnel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class NgrokService {

    private static final Logger log = LoggerFactory.getLogger(NgrokService.class);

    private final HubUrlProvider hubUrlProvider;
    private final int serverPort;
    private final String ngrokDomain;
    private final String ngrokAuthToken;

    public NgrokService(
            HubUrlProvider hubUrlProvider,
            @Value("${server.port:8080}") int serverPort,
            @Value("${guild.ngrok.domain:}") String ngrokDomain,
            @Value("${guild.ngrok.auth-token:}") String ngrokAuthToken) {
        this.hubUrlProvider = hubUrlProvider;
        this.serverPort = serverPort;
        this.ngrokDomain = ngrokDomain;
        this.ngrokAuthToken = ngrokAuthToken;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void start() {
        if (ngrokAuthToken.isBlank() && ngrokDomain.isBlank()) {
            log.info("NGROK_AUTHTOKEN não configurado — ngrok não iniciado.");
            return;
        }

        try {
            JavaNgrokConfig.Builder configBuilder = new JavaNgrokConfig.Builder();
            if (!ngrokAuthToken.isBlank()) {
                configBuilder.withAuthToken(ngrokAuthToken);
            }

            NgrokClient client = new NgrokClient.Builder()
                    .withJavaNgrokConfig(configBuilder.build())
                    .build();

            CreateTunnel.Builder tunnelBuilder = new CreateTunnel.Builder()
                    .withAddr(serverPort);
            if (!ngrokDomain.isBlank()) {
                tunnelBuilder.withDomain(ngrokDomain);
            }

            Tunnel tunnel = client.connect(tunnelBuilder.build());
            String publicUrl = tunnel.getPublicUrl();

            hubUrlProvider.update(publicUrl);

            log.info("=============================================================");
            log.info("  HUB público via ngrok: {}", publicUrl);
            log.info("  Callback OAuth App:    {}/onboard/amqp/callback", publicUrl);
            log.info("=============================================================");

        } catch (Exception e) {
            log.warn("ngrok não iniciado — links de onboarding usarão localhost. ({})", e.getMessage());
        }
    }
}
