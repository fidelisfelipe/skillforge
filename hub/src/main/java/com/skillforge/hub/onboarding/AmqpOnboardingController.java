package com.skillforge.hub.onboarding;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

@Controller
@RequestMapping("/onboard/amqp")
public class AmqpOnboardingController {

    private static final Logger log = LoggerFactory.getLogger(AmqpOnboardingController.class);

    private static final String CERT_CHARS = "ABCDEFGHJKMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789";
    private static final SecureRandom RNG = new SecureRandom();

    private final OnboardingTokenStore tokenStore;
    private final OnboardingTokenValidator tokenValidator;
    private final GitHubOAuthClient oauthClient;
    private final CloudAmqpService cloudAmqp;
    private final HubUrlProvider hubUrlProvider;
    private final String amqpUrl;
    private final String amqpExchange;

    // Optional — only present when guild.mtls.enabled=true
    @Autowired(required = false) @Lazy
    private MtlsCaService mtlsCa;

    public AmqpOnboardingController(
            OnboardingTokenStore tokenStore,
            OnboardingTokenValidator tokenValidator,
            GitHubOAuthClient oauthClient,
            CloudAmqpService cloudAmqp,
            HubUrlProvider hubUrlProvider,
            @Value("${spring.rabbitmq.addresses:}") String amqpUrl,
            @Value("${guild.amqp.exchange:skillforge}") String amqpExchange) {
        this.tokenStore = tokenStore;
        this.tokenValidator = tokenValidator;
        this.oauthClient = oauthClient;
        this.cloudAmqp = cloudAmqp;
        this.hubUrlProvider = hubUrlProvider;
        this.amqpUrl = amqpUrl;
        this.amqpExchange = amqpExchange;
    }

    /** Dev clica o link → valida token (HMAC ou UUID) → redireciona para GitHub OAuth */
    @GetMapping
    public String start(@RequestParam String token, Model model) {
        if (!oauthClient.isConfigured()) {
            model.addAttribute("error", "GitHub OAuth não configurado no hub.");
            return "onboarding-result";
        }
        boolean valid = tokenStore.isValid(token) || tokenValidator.validate(token).isPresent();
        if (!valid) {
            model.addAttribute("error", "Link inválido ou expirado. Solicite um novo na issue do GitHub.");
            return "onboarding-result";
        }
        String callbackUrl = hubUrlProvider.get() + "/onboard/amqp/callback";
        return "redirect:" + oauthClient.buildAuthorizationUrl(token, callbackUrl);
    }

    /** GitHub redireciona aqui após autenticação */
    @GetMapping("/callback")
    public String callback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String error,
            Model model) {

        if (error != null || code == null || state == null) {
            model.addAttribute("error", "Autorização negada ou cancelada.");
            return "onboarding-result";
        }

        // Tenta UUID (local) primeiro, depois HMAC (Actions)
        OnboardingTokenStore.OnboardingRequest request = tokenStore.consume(state)
                .or(() -> tokenValidator.validate(state))
                .orElse(null);

        if (request == null) {
            model.addAttribute("error", "Link expirado ou já utilizado. Solicite um novo na issue do GitHub.");
            return "onboarding-result";
        }

        String accessToken = oauthClient.exchangeCodeForToken(code).orElse(null);
        if (accessToken == null) {
            model.addAttribute("error", "Falha na autenticação com GitHub. Tente novamente.");
            return "onboarding-result";
        }

        String login = oauthClient.getUserLogin(accessToken).orElse("");
        if (!login.equalsIgnoreCase(request.githubLogin())) {
            log.warn("Login mismatch no onboarding: esperado '{}', recebido '{}'", request.githubLogin(), login);
            model.addAttribute("error", "Autentique com o GitHub login '@%s' para acessar este convite.".formatted(request.githubLogin()));
            return "onboarding-result";
        }

        String email = oauthClient.getPrimaryEmail(accessToken).orElse(null);
        if (email == null) {
            model.addAttribute("error", "Não foi possível obter seu e-mail verificado do GitHub. Verifique as configurações da sua conta.");
            return "onboarding-result";
        }

        log.info("Onboarding AMQP: @{} → {} (hero: {})", login, email, request.heroId());
        boolean invited = cloudAmqp.inviteTeamMember(email);

        model.addAttribute("heroId", request.heroId());
        model.addAttribute("login", login);
        model.addAttribute("email", email);
        model.addAttribute("invited", invited);
        model.addAttribute("amqpUrl", amqpUrl);
        model.addAttribute("amqpExchange", amqpExchange);

        if (mtlsCa != null) {
            try {
                String password = randomPassword(12);
                byte[] p12 = mtlsCa.issuePkcs12(login, password);
                model.addAttribute("clientCertB64", Base64.getEncoder().encodeToString(p12));
                model.addAttribute("clientCertPassword", password);
                model.addAttribute("clientCertFilename", login + "-skillforge.p12");
            } catch (Exception e) {
                log.warn("⚠️ Falha ao emitir cert mTLS para @{}: {}", login, e.getMessage());
            }
        }

        return "onboarding-result";
    }

    @GetMapping("/ca.pem")
    public ResponseEntity<byte[]> downloadCa() {
        if (mtlsCa == null) return ResponseEntity.notFound().build();
        try {
            byte[] pem = mtlsCa.getCaCertPem().getBytes(StandardCharsets.UTF_8);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, "application/x-pem-file")
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=skillforge-ca.pem")
                    .body(pem);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    private static String randomPassword(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++)
            sb.append(CERT_CHARS.charAt(RNG.nextInt(CERT_CHARS.length())));
        return sb.toString();
    }
}
