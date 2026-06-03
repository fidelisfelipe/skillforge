# KATA-AI01A: Triagem de Issues com LLM Local

> Parte do dojo **AI Dev — LLM Local** no SkillForge.

## O que você vai construir

Uma classe `IssueTriager` que usa um modelo de linguagem local (Ollama) para
classificar automaticamente issues de repositórios GitHub em categoria, prioridade
e label sugerida — tudo via chamada HTTP direta à API REST do Ollama.

## Pré-requisitos

| Ferramenta | Versão | Descrição |
|---|---|---|
| Java | 21 | `java --version` |
| Maven | 3.9+ | `mvn --version` |
| Ollama | qualquer | `ollama --version` (só necessário para rodar manualmente) |

> Os **testes** não precisam de Ollama instalado — usam WireMock para simular as respostas.

## Como resolver

1. Abra `src/main/java/com/skillforge/kata/IssueTriager.java`
2. Leia os testes em `src/test/java/com/skillforge/kata/IssueTriagerTest.java`
3. Implemente os três métodos marcados com `// TODO`
4. Verifique: `mvn verify`

### Métodos a implementar

| Método | Responsabilidade |
|---|---|
| `triage(IssueInput)` | Orquestra: chama Ollama, lê resposta, retorna resultado |
| `buildPrompt(IssueInput)` | Constrói o prompt com text block, inclui título e corpo |
| `parseResult(String)` | Extrai JSON da resposta (com ou sem wrapper markdown) |

### API do Ollama (para referência)

```http
POST http://localhost:11434/api/generate
Content-Type: application/json

{
  "model": "llama3.2",
  "prompt": "...",
  "stream": false
}
```

Resposta:
```json
{ "response": "...", "done": true }
```

### Testar com Ollama real (opcional)

```bash
ollama pull llama3.2
# em outra aba:
ollama serve
# rodar apenas a classe main (não os testes):
mvn compile exec:java -Dexec.mainClass="com.skillforge.kata.IssueTriagerMain"
```

## Como submeter

```bash
git checkout -b kata-ai01a-{heroId}-solution
git commit -am "kata-ai01a: implement IssueTriager"
git push origin kata-ai01a-{heroId}-solution
# Abra PR com body: heroId: {heroId}
```

## Critérios de aceite (resumo dos testes)

- `triage()` faz POST `/api/generate` e retorna resultado correto para JSON simples
- `triage()` extrai JSON quando a resposta vem dentro de bloco markdown
- `triage()` retorna `TriageResult(OTHER, LOW, "needs-triage")` para resposta inválida
- `buildPrompt()` inclui título e corpo da issue no texto gerado
- `buildPrompt()` menciona `JSON` e `category` para guiar o formato da resposta
- `parseResult()` lida com JSON puro, bloco markdown e enum desconhecido
