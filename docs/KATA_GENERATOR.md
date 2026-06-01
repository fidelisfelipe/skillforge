---
ai-context: "guide"
ai-audience: ["senior-developers", "guild-masters"]
ai-scope: "Kata Generator — criação automatizada de katas com Claude API e skill /create-kata-theme"
---

# Kata Generator

Sistema de criação automatizada de katas para o SkillForge Dojo.
Usa a API do Claude para gerar specs, testes reais (JUnit 5) e branches no `skillforge-katas`.

---

## Visão Geral

```
/create-kata-theme                ← skill interativa no Claude Code
       │
       ▼
scripts/kata-generator/
  generate.mjs                    ← gera tema novo (specs + testes via Claude API)
  generate-from-catalog.mjs       ← gera testes para katas existentes no catálogo
  list-katas.cjs                  ← helper: lê summary.json e imprime id|className
       │
       ▼
scripts/kata-generator/output/
  catalog-additions.yml           ← YAML pronto para append no catálogo
  summary.json                    ← metadados de todos os katas gerados
  katas/
    KATA-001B_BlockingIoServiceTest.java
    ...                           ← um arquivo por kata com testes reais
       │
       ▼
scripts/
  create-kata.sh                  ← cria branch via clone + push para GitHub
  create-kata-local.sh            ← cria branch no repo local (sem push)
  create-all-katas.sh             ← orquestra a criação em lote
       │
       ▼
skillforge-katas/
  kata-001a-template/             ← branch orphan com pom.xml + classe + testes
  kata-002a-template/
  ...
```

---

## Pré-requisitos

| Variável | Onde configurar | Para que serve |
|----------|----------------|----------------|
| `ANTHROPIC_API_KEY` | `.env` | Chamadas à API do Claude |
| `GITHUB_TOKEN` | `.env` | Push de branches para o GitHub |

```bash
# .env (na raiz do projeto)
ANTHROPIC_API_KEY=sk-ant-...
GITHUB_TOKEN=ghp_...
```

---

## Fluxo 1 — Criar katas para um chapter (interativo)

Use a skill dentro do Claude Code:

```
/create-kata-theme
```

O agente vai:
1. Perguntar: tema, dificuldade, quantidade (1–5), referência de certificação
2. Chamar `generate.mjs` → Claude gera specs e testes
3. Mostrar preview dos katas e arquivos de teste
4. Pedir confirmação
5. Fazer append no catálogo, copiar para o hub e criar as branches

> Use o ID do chapter como referência de certificação, ex: `OCP Java 21 (1Z0-830) — Chapter 5`

---

## Fluxo 2 — Gerar testes para katas existentes no catálogo

```bash
# Todos os chapters (pula KATA-001A que já tem testes manuais)
cd scripts/kata-generator
node generate-from-catalog.mjs --skip KATA-001A

# Só um chapter específico
node generate-from-catalog.mjs --theme ch13-concurrency

# Pular múltiplos katas
node generate-from-catalog.mjs --skip KATA-001A,KATA-007A

# Criar branches localmente
bash ../create-all-katas.sh --from-generator --local

# Revisar e fazer push
cd ../../skillforge-katas
git branch
git show kata-007a-template:src/test/java/com/skillforge/kata/SalesReporterTest.java
git push origin --all
```

---

## Fluxo 3 — Criar uma branch avulsa

```bash
# Com testes gerados:
bash scripts/create-kata-local.sh KATA-007A SalesReporter \
  scripts/kata-generator/output/katas/KATA-007A_SalesReporterTest.java

# Sem testes (placeholder):
bash scripts/create-kata-local.sh KATA-007A SalesReporter

# Push direto para GitHub:
bash scripts/create-kata.sh KATA-007A SalesReporter \
  scripts/kata-generator/output/katas/KATA-007A_SalesReporterTest.java
```

---

## Estrutura de cada branch de kata

Cada branch no `skillforge-katas` é orphan (sem histórico compartilhado):

```
kata-007a-template/
  pom.xml                                    ← standalone, JUnit Jupiter 5.10.0
  .gitignore
  README.md                                  ← instruções para o hero
  src/
    main/java/com/skillforge/kata/
      SalesReporter.java                     ← classe vazia (TODO)
    test/java/com/skillforge/kata/
      SalesReporterTest.java                 ← testes reais gerados pelo Claude
```

Package sempre `com.skillforge.kata`. IDs de kata nunca mudam — as branches são permanentes.

---

## Catálogo

Fonte: `quests/dojo/java-21-certified/catalog.yml`
Destino (hub): `hub/src/main/resources/kata-catalog.yml`

O catálogo segue os 14 chapters do guia **OCP Java 21 (1Z0-830)**:

```yaml
themes:
  - id: ch10-streams
    name: "Ch.10 — Streams"
    description: "Optional, Stream pipelines, primitive streams and advanced collectors"
    difficulty: intermediate
    certReference: "OCP Java 21 (1Z0-830) — Chapter 10"
    katas:
      - id: KATA-007A
        title: "Streams: Collectors e Agrupamento"
        difficulty: intermediate
        xpReward: 90
        templateBranch: kata-007a-template
        className: SalesReporter
        spec: |
          Dado um stream de vendas...
```

Após editar o catálogo, sincronize para o hub:

```bash
cp quests/dojo/java-21-certified/catalog.yml hub/src/main/resources/kata-catalog.yml
```

Reinicie o hub para recarregar o `KataCatalogLoader`.

---

## Estado do catálogo — OCP Java 21 (1Z0-830)

| Chapter | Theme ID | Katas | Status |
|---------|----------|-------|--------|
| Ch.01 — Building Blocks | `ch01-building-blocks` | — | ⏳ pendente |
| Ch.02 — Operators | `ch02-operators` | — | ⏳ pendente |
| Ch.03 — Making Decisions | `ch03-making-decisions` | KATA-006A, 006B | ✅ |
| Ch.04 — Core APIs | `ch04-core-apis` | KATA-005A, 014A, 014B | ✅ |
| Ch.05 — Methods | `ch05-methods` | — | ⏳ pendente |
| Ch.06 — Class Design | `ch06-class-design` | — | ⏳ pendente |
| Ch.07 — Beyond Classes | `ch07-beyond-classes` | KATA-002A/B/C, 003A/B/C, 004A/B | ✅ |
| Ch.08 — Lambdas | `ch08-lambdas-functional` | KATA-008A, 008B | ✅ |
| Ch.09 — Collections & Generics | `ch09-collections-generics` | KATA-010A, 010B | ✅ |
| Ch.10 — Streams | `ch10-streams` | KATA-007A/B/C, 009A | ✅ |
| Ch.11 — Exceptions | `ch11-exceptions-localization` | KATA-013A | ✅ |
| Ch.12 — Modules | `ch12-modules` | KATA-012A | ✅ |
| Ch.13 — Concurrency | `ch13-concurrency` | KATA-001A/B/C, 011A/B | ✅ |
| Ch.14 — I/O | `ch14-io` | — | ⏳ pendente |

Para preencher os chapters pendentes use `/create-kata-theme` com a referência do chapter.

---

## Branches no skillforge-katas

| Branch | Kata | Chapter | Classe | Testes |
|--------|------|---------|--------|--------|
| `kata-001a-template` | KATA-001A | Ch.13 | ConcurrentExecutor | ✅ manual |
| `kata-001b-template` | KATA-001B | Ch.13 | BlockingIoService | ✅ gerado |
| `kata-001c-template` | KATA-001C | Ch.13 | AggregatorService | ✅ gerado |
| `kata-002a-template` | KATA-002A | Ch.07 | MoneyRecord | ✅ gerado |
| `kata-002b-template` | KATA-002B | Ch.07 | Temperature | ✅ gerado |
| `kata-002c-template` | KATA-002C | Ch.07 | ExprEvaluator | ✅ gerado |
| `kata-003a-template` | KATA-003A | Ch.07 | ShapeClassifier | ✅ gerado |
| `kata-003b-template` | KATA-003B | Ch.07 | ShapeAnalyzer | ✅ gerado |
| `kata-003c-template` | KATA-003C | Ch.07 | EventHandler | ✅ gerado |
| `kata-004a-template` | KATA-004A | Ch.07 | PaymentProcessor | ✅ gerado |
| `kata-004b-template` | KATA-004B | Ch.07 | ResultHandler | ✅ gerado |
| `kata-005a-template` | KATA-005A | Ch.04 | TemplateRenderer | ✅ gerado |
| `kata-006a-template` | KATA-006A | Ch.03 | DayClassifier | ✅ gerado |
| `kata-006b-template` | KATA-006B | Ch.03 | PricingEngine | ✅ gerado |
| `kata-007a-template` | KATA-007A | Ch.10 | SalesReporter | ✅ gerado |
| `kata-007b-template` | KATA-007B | Ch.10 | LibraryCatalog | ✅ gerado |
| `kata-007c-template` | KATA-007C | Ch.10 | RunningAverageCollector | ✅ gerado |
| `kata-008a-template` | KATA-008A | Ch.08 | DataPipeline | ✅ gerado |
| `kata-008b-template` | KATA-008B | Ch.08 | TriFunction | ✅ gerado |
| `kata-009a-template` | KATA-009A | Ch.10 | UserProfileService | ✅ gerado |
| `kata-010a-template` | KATA-010A | Ch.09 | NumberStats | ✅ gerado |
| `kata-010b-template` | KATA-010B | Ch.09 | MultiIndexMap | ✅ gerado |
| `kata-011a-template` | KATA-011A | Ch.13 | AsyncOrderPipeline | ✅ gerado |
| `kata-011b-template` | KATA-011B | Ch.13 | BoundedBlockingCache | ✅ gerado |
| `kata-012a-template` | KATA-012A | Ch.12 | ModuleConsumer | ✅ gerado |
| `kata-013a-template` | KATA-013A | Ch.11 | PaymentException | ✅ gerado |
| `kata-014a-template` | KATA-014A | Ch.04 | TextProcessor | ✅ gerado |
| `kata-014b-template` | KATA-014B | Ch.04 | TextFormatter | ✅ gerado |

---

## Referência dos scripts

### `generate.mjs` — novo tema com specs e testes

```bash
node scripts/kata-generator/generate.mjs \
  --theme "I/O: Files e NIO.2" \
  --difficulty intermediate \
  --count 3 \
  --cert "OCP Java 21 (1Z0-830) — Chapter 14"
```

### `generate-from-catalog.mjs` — testes para katas existentes

```bash
node scripts/kata-generator/generate-from-catalog.mjs
node scripts/kata-generator/generate-from-catalog.mjs --theme ch14-io
node scripts/kata-generator/generate-from-catalog.mjs --skip KATA-001A,KATA-007A
```

### `create-all-katas.sh`

```bash
bash scripts/create-all-katas.sh --from-generator --local   # local, sem push
bash scripts/create-all-katas.sh --from-generator           # push para GitHub
bash scripts/create-all-katas.sh --local                    # lista hardcoded, local
bash scripts/create-all-katas.sh --skip-existing            # lista hardcoded, pula existentes
```

### `create-kata-local.sh` / `create-kata.sh`

```bash
bash scripts/create-kata-local.sh <KATA-ID> <ClassName> [test-file]
bash scripts/create-kata.sh       <KATA-ID> <ClassName> [test-file]
```

---

## Fluxo do hero

1. Hero acessa o Dojo UI e escolhe um chapter
2. Hub entrega `KataDelivery` via AMQP → SSE exibe o kata
3. Hero clona a branch template:
   ```bash
   git clone -b kata-007a-template https://github.com/fidelisfelipe/skillforge-katas
   ```
4. `mvn verify` → testes falham (classe vazia — esperado)
5. Implementa a classe até todos os testes passarem
6. Abre PR com a solução:
   ```bash
   git checkout -b kata-007a-{heroId}-solution
   git commit -am "kata-007a: implement SalesReporter"
   git push origin kata-007a-{heroId}-solution
   # PR body: heroId: {heroId}
   ```
7. Hub recebe webhook → clona → `mvn verify` → publica resultado via AMQP → fecha PR
