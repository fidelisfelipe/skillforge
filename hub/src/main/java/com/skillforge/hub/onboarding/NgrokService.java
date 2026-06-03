package com.skillforge.hub.onboarding;

import com.github.alexdlaird.ngrok.NgrokClient;
import com.github.alexdlaird.ngrok.conf.JavaNgrokConfig;
import com.github.alexdlaird.ngrok.protocol.CreateTunnel;
import com.github.alexdlaird.ngrok.protocol.Proto;
import com.github.alexdlaird.ngrok.protocol.Tunnel;
import jakarta.annotation.PreDestroy;
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
    private final boolean sslEnabled;
    private final boolean mtlsEnabled;
    private final String certsDir;

    private NgrokClient client;

    public NgrokService(
            HubUrlProvider hubUrlProvider,
            @Value("${server.port:8080}") int serverPort,
            @Value("${guild.ngrok.domain:}") String ngrokDomain,
            @Value("${guild.ngrok.auth-token:}") String ngrokAuthToken,
            @Value("${server.ssl.enabled:false}") boolean sslEnabled,
            @Value("${guild.mtls.enabled:false}") boolean mtlsEnabled,
            @Value("${guild.mtls.certs-dir:./certs}") String certsDir) {
        this.hubUrlProvider = hubUrlProvider;
        this.serverPort = serverPort;
        this.ngrokDomain = ngrokDomain;
        this.ngrokAuthToken = ngrokAuthToken;
        this.sslEnabled = sslEnabled;
        this.mtlsEnabled = mtlsEnabled;
        this.certsDir = certsDir;
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

            client = new NgrokClient.Builder()
                    .withJavaNgrokConfig(configBuilder.build())
                    .build();

            // Sempre protocolo HTTP no túnel ngrok — o ngrok termina SSL na edge
            // e faz forward HTTP para o hub local, evitando SSL passthrough
            // que exporia o cert local (localhost) ao browser do dev.
            String backendAddr = (sslEnabled ? "https" : "http") + "://localhost:" + serverPort;
            CreateTunnel.Builder tunnelBuilder = new CreateTunnel.Builder()
                    .withAddr(backendAddr)
                    .withProto(Proto.HTTP);
            if (!ngrokDomain.isBlank()) {
                tunnelBuilder.withDomain(ngrokDomain);
            }
            if (mtlsEnabled) {
                String caPath = certsDir + "/ca.crt.pem";
                tunnelBuilder.withMutualTlsCas(caPath);
                log.info("mTLS habilitado no túnel ngrok — CA: {}", caPath);
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

    @PreDestroy
    void stop() {
        if (client == null) return;
        try {
            client.kill();
            log.info("🛑 ngrok tunnel encerrado.");
        } catch (Exception e) {
            log.warn("⚠️ Erro ao encerrar ngrok: {}", e.getMessage());
        }
    }
}
