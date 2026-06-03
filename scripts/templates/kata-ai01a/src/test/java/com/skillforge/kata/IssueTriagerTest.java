package com.skillforge.kata;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.skillforge.kata.IssueTriager.Category;
import com.skillforge.kata.IssueTriager.IssueInput;
import com.skillforge.kata.IssueTriager.Priority;
import com.skillforge.kata.IssueTriager.TriageResult;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("KATA-AI01A: IssueTriager")
class IssueTriagerTest {

    private static WireMockServer wireMock;
    private IssueTriager triager;

    @BeforeAll
    static void startWireMock() {
        wireMock = new WireMockServer(wireMockConfig().dynamicPort());
        wireMock.start();
    }

    @AfterAll
    static void stopWireMock() {
        wireMock.stop();
    }

    @BeforeEach
    void setUp() {
        wireMock.resetAll();
        triager = new IssueTriager("http://localhost:" + wireMock.port());
    }

    // ── triage() ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("triage retorna BUG/HIGH para resposta Ollama com JSON simples")
    void triage_plainJson_returnsBugHigh() {
        wireMock.stubFor(post(urlEqualTo("/api/generate"))
                .willReturn(okJson("""
                        { "response": "{\\"category\\":\\"BUG\\",\\"priority\\":\\"HIGH\\",\\"label\\":\\"bug-critical\\"}" }
                        """)));

        TriageResult result = triager.triage(new IssueInput(
                "NullPointerException no login",
                "O sistema crasha ao tentar logar com email vazio."
        ));

        assertEquals(Category.BUG, result.category());
        assertEquals(Priority.HIGH, result.priority());
        assertEquals("bug-critical", result.suggestedLabel());
    }

    @Test
    @DisplayName("triage extrai JSON quando Ollama responde com bloco markdown")
    void triage_markdownWrappedJson_returnsFeatureMedium() {
        wireMock.stubFor(post(urlEqualTo("/api/generate"))
                .willReturn(okJson("""
                        { "response": "```json\\n{\\"category\\":\\"FEATURE\\",\\"priority\\":\\"MEDIUM\\",\\"label\\":\\"enhancement\\"}\\n```" }
                        """)));

        TriageResult result = triager.triage(new IssueInput(
                "Adicionar dark mode",
                "Seria ótimo ter um modo escuro nas configurações do perfil."
        ));

        assertEquals(Category.FEATURE, result.category());
        assertEquals(Priority.MEDIUM, result.priority());
        assertEquals("enhancement", result.suggestedLabel());
    }

    @Test
    @DisplayName("triage retorna fallback quando Ollama responde com texto inválido")
    void triage_invalidJson_returnsFallback() {
        wireMock.stubFor(post(urlEqualTo("/api/generate"))
                .willReturn(okJson("""
                        { "response": "Não consigo classificar esta issue." }
                        """)));

        TriageResult result = triager.triage(new IssueInput("?", "???"));

        assertEquals(Category.OTHER, result.category());
        assertEquals(Priority.LOW, result.priority());
        assertEquals("needs-triage", result.suggestedLabel());
    }

    @Test
    @DisplayName("triage retorna QUESTION/LOW para issue de dúvida")
    void triage_questionIssue_returnsQuestionLow() {
        wireMock.stubFor(post(urlEqualTo("/api/generate"))
                .willReturn(okJson("""
                        { "response": "{\\"category\\":\\"QUESTION\\",\\"priority\\":\\"LOW\\",\\"label\\":\\"question\\"}" }
                        """)));

        TriageResult result = triager.triage(new IssueInput(
                "Como configurar o timeout de sessão?",
                "Não encontrei na documentação como ajustar o timeout."
        ));

        assertEquals(Category.QUESTION, result.category());
        assertEquals(Priority.LOW, result.priority());
        assertEquals("question", result.suggestedLabel());
    }

    // ── buildPrompt() ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("buildPrompt inclui o título da issue")
    void buildPrompt_containsTitle() {
        String prompt = triager.buildPrompt(new IssueInput(
                "Título do Bug Crítico",
                "Algum corpo"
        ));

        assertTrue(prompt.contains("Título do Bug Crítico"),
                "O prompt deve conter o título da issue");
    }

    @Test
    @DisplayName("buildPrompt inclui o corpo da issue")
    void buildPrompt_containsBody() {
        String prompt = triager.buildPrompt(new IssueInput(
                "Título",
                "Descrição detalhada do problema encontrado"
        ));

        assertTrue(prompt.contains("Descrição detalhada do problema encontrado"),
                "O prompt deve conter o corpo da issue");
    }

    @Test
    @DisplayName("buildPrompt instrui o LLM a retornar JSON com campo category")
    void buildPrompt_mentionsJsonAndCategory() {
        String prompt = triager.buildPrompt(new IssueInput("x", "y"));

        assertTrue(prompt.toLowerCase().contains("json"),
                "O prompt deve mencionar JSON para guiar o formato da resposta");
        assertTrue(prompt.contains("category") || prompt.contains("BUG"),
                "O prompt deve descrever os valores esperados do campo category");
    }

    // ── parseResult() ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("parseResult processa JSON puro corretamente")
    void parseResult_plainJson_parsed() {
        TriageResult result = triager.parseResult(
                "{\"category\":\"DOCS\",\"priority\":\"LOW\",\"label\":\"documentation\"}"
        );

        assertEquals(Category.DOCS, result.category());
        assertEquals(Priority.LOW, result.priority());
        assertEquals("documentation", result.suggestedLabel());
    }

    @Test
    @DisplayName("parseResult extrai JSON de bloco markdown triple-backtick")
    void parseResult_markdownBlock_extracted() {
        String raw = "```json\n{\"category\":\"DOCS\",\"priority\":\"LOW\",\"label\":\"documentation\"}\n```";

        TriageResult result = triager.parseResult(raw);

        assertEquals(Category.DOCS, result.category());
        assertEquals(Priority.LOW, result.priority());
        assertEquals("documentation", result.suggestedLabel());
    }

    @Test
    @DisplayName("parseResult retorna fallback para texto sem JSON")
    void parseResult_noJson_returnsFallback() {
        TriageResult result = triager.parseResult("Desculpe, não entendi.");

        assertEquals(Category.OTHER, result.category());
        assertEquals(Priority.LOW, result.priority());
        assertEquals("needs-triage", result.suggestedLabel());
    }

    @Test
    @DisplayName("parseResult retorna fallback para enum desconhecido")
    void parseResult_unknownEnum_returnsFallback() {
        TriageResult result = triager.parseResult(
                "{\"category\":\"SPAM\",\"priority\":\"URGENT\",\"label\":\"x\"}"
        );

        assertEquals(Category.OTHER, result.category());
        assertEquals(Priority.LOW, result.priority());
        assertEquals("needs-triage", result.suggestedLabel());
    }
}
