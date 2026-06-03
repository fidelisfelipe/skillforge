package com.skillforge.kata;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * KATA-AI01A: Triagem de Issues com LLM Local
 *
 * Implemente os três métodos marcados com TODO para fazer os testes passarem.
 * Execute: mvn verify
 *
 * Não modifique os tipos (records/enums) nem o construtor — apenas os métodos.
 */
public class IssueTriager {

    // ── Tipos fornecidos — não alterar ────────────────────────────────────────

    public record IssueInput(String title, String body) {}

    public enum Category { BUG, FEATURE, QUESTION, DOCS, OTHER }

    public enum Priority { LOW, MEDIUM, HIGH }

    public record TriageResult(Category category, Priority priority, String suggestedLabel) {}

    // ── Infraestrutura — não alterar ──────────────────────────────────────────

    private final String ollamaBaseUrl;
    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();

    public IssueTriager() {
        this("http://localhost:11434");
    }

    /** Construtor usado pelos testes para apontar para o WireMock. */
    public IssueTriager(String ollamaBaseUrl) {
        this.ollamaBaseUrl = ollamaBaseUrl;
    }

    // ── Métodos a implementar ─────────────────────────────────────────────────

    /**
     * Classifica a issue recebida usando o LLM local.
     *
     * Passos:
     * 1. Construir o prompt com buildPrompt(issue).
     * 2. Fazer POST {ollamaBaseUrl}/api/generate com body JSON:
     *    { "model": "llama3.2", "prompt": "<prompt>", "stream": false }
     * 3. Ler o campo "response" do JSON retornado.
     * 4. Chamar parseResult(llmResponse) e retornar o resultado.
     *
     * Use java.net.http.HttpClient + HttpRequest.BodyPublishers.ofString().
     * Content-Type da requisição deve ser "application/json".
     */
    public TriageResult triage(IssueInput issue) {
        // TODO: implementar
        throw new UnsupportedOperationException("TODO: implementar triage()");
    }

    /**
     * Constrói o prompt enviado ao LLM.
     *
     * O prompt deve:
     * - Estar em inglês
     * - Incluir issue.title() e issue.body() como contexto
     * - Instruir o LLM a responder SOMENTE com JSON válido no formato:
     *   { "category": "BUG|FEATURE|QUESTION|DOCS|OTHER",
     *     "priority": "LOW|MEDIUM|HIGH",
     *     "label": "texto-curto-com-hifens" }
     *
     * Use text block Java (""" ... """) para o template.
     */
    public String buildPrompt(IssueInput issue) {
        // TODO: implementar usando text block
        throw new UnsupportedOperationException("TODO: implementar buildPrompt()");
    }

    /**
     * Extrai o TriageResult da resposta bruta do LLM.
     *
     * A resposta pode vir:
     * - Como JSON puro: {"category":"BUG","priority":"HIGH","label":"bug-critical"}
     * - Dentro de bloco markdown: ```json\n{...}\n```
     *
     * Em qualquer caso de falha (JSON inválido, enum desconhecido, campo ausente),
     * retornar: new TriageResult(Category.OTHER, Priority.LOW, "needs-triage")
     */
    public TriageResult parseResult(String llmResponse) {
        // TODO: implementar — extrair JSON, desserializar, tratar falhas
        throw new UnsupportedOperationException("TODO: implementar parseResult()");
    }
}
