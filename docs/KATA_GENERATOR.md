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
/create-kata-theme         ← skill interativa no Claude Code
       │
       ▼
scripts/kata-generator/
  generate.mjs             ← gera tema novo (specs + testes via Claude API)
  generate-from-catalog.mjs← gera testes para katas existentes no catálogo
  list-katas.cjs           ← helper: lê summary.json e imprime id|className
       │
       ▼
scripts/kata-generator/output/
  catalog-additions.yml    ← YAML pronto para append no catálogo
  summary.json             ← metadados de todos os katas gerados
  katas/
    KATA-001B_BlockingIoServiceTest.java
    ...                    ← um arquivo por kata com testes reais
       │
       ▼
scripts/
  create-kata.sh           ← cria branch via clone + push para GitHub
  create-kata-local.sh     ← cria branch no repo local (sem push)
  create-all-katas.sh      ← orquestra a criação em lote
       │
       ▼
skillforge-katas/
  kata-001a-template/      ← branch orphan com pom.xml + classe + testes
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

## Fluxo 1 — Criar um novo tema (interativo)

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

---

## Fluxo 2 — Criar branches para katas existentes no catálogo

Quando o catálogo já tem os katas definidos mas as branches ainda não existem:

```bash
# 1. Gerar testes para todos os katas sem branch
cd scripts/kata-generator
node generate-from-catalog.mjs --skip KATA-001A

# Opções:
#   --skip KATA-001A,KATA-002A   → pular katas específicos
#   --theme streams               → processar só um tema

# 2. Criar branches localmente
bash /caminho/para/skillforge/scripts/create-all-katas.sh --from-generator --local

# 3. Revisar e fazer push
cd /caminho/para/skillforge-katas
git branch            # ver todas as branches criadas
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

Cada branch no `skillforge-katas` é orphan (sem histórico compartilhado) e contém:

```
kata-007a-template/
  pom.xml                                          ← standalone, JUnit 5 only
  .gitignore
  README.md                                        ← instruções para o hero
  src/
    main/java/com/skillforge/kata/
      SalesReporter.java                           ← classe vazia (TODO)
    test/java/com/skillforge/kata/
      SalesReporterTest.java                       ← testes reais gerados pelo Claude
```

**`pom.xml`** — standalone, sem parent, só JUnit Jupiter 5.10.0:
```xml
<dependency>
  <groupId>org.junit.jupiter</groupId>
  <artifactId>junit-jupiter</artifactId>
  <version>5.10.0</version>
  <scope>test</scope>
</dependency>
```

**Package** — sempre `com.skillforge.kata` (independente do kata ID).

---

## Catálogo

O catálogo fonte fica em `quests/dojo/java-21-certified/catalog.yml` e é copiado para o hub em `hub/src/main/resources/kata-catalog.yml`.

Estrutura YAML:

```yaml
themes:
  - id: streams
    name: "Stream API"
    description: "Process collections with the Stream API"
    difficulty: intermediate
    certReference: "Oracle Java 21 - Module 8"
    katas:
      - id: KATA-007A
        title: "Streams: Collectors e Agrupamento"
        difficulty: intermediate
        xpReward: 90
        templateBranch: kata-007a-template
        className: SalesReporter
        spec: |
          Dado um stream de vendas, agrupe por região...
```

Após editar o catálogo, copie para o hub:

```bash
cp quests/dojo/java-21-certified/catalog.yml hub/src/main/resources/kata-catalog.yml
```

E reinicie o hub para recarregar o `KataCatalogLoader`.

---

## Estado atual do skillforge-katas

| Branch | Kata | Classe | Testes |
|--------|------|--------|--------|
| `kata-001a-template` | KATA-001A | ConcurrentExecutor | ✅ manual |
| `kata-001b-template` | KATA-001B | BlockingIoService | ✅ gerado |
| `kata-001c-template` | KATA-001C | AggregatorService | ✅ gerado |
| `kata-002a-template` | KATA-002A | MoneyRecord | ✅ gerado |
| `kata-002b-template` | KATA-002B | Temperature | ✅ gerado |
| `kata-002c-template` | KATA-002C | ExprEvaluator | ✅ gerado |
| `kata-003a-template` | KATA-003A | ShapeClassifier | ✅ gerado |
| `kata-003b-template` | KATA-003B | ShapeAnalyzer | ✅ gerado |
| `kata-003c-template` | KATA-003C | EventHandler | ✅ gerado |
| `kata-004a-template` | KATA-004A | PaymentProcessor | ✅ gerado |
| `kata-004b-template` | KATA-004B | ResultHandler | ✅ gerado |
| `kata-005a-template` | KATA-005A | TemplateRenderer | ✅ gerado |
| `kata-006a-template` | KATA-006A | DayClassifier | ✅ gerado |
| `kata-006b-template` | KATA-006B | PricingEngine | ✅ gerado |
| `kata-007a-template` | KATA-007A | SalesReporter | ✅ gerado |
| `kata-007b-template` | KATA-007B | LibraryCatalog | ✅ gerado |
| `kata-007c-template` | KATA-007C | RunningAverageCollector | ✅ gerado |
| `kata-008a-template` | KATA-008A | DataPipeline | ✅ gerado |
| `kata-008b-template` | KATA-008B | TriFunction | ✅ gerado |
| `kata-009a-template` | KATA-009A | UserProfileService | ✅ gerado |
| `kata-010a-template` | KATA-010A | NumberStats | ✅ gerado |
| `kata-010b-template` | KATA-010B | MultiIndexMap | ✅ gerado |
| `kata-011a-template` | KATA-011A | AsyncOrderPipeline | ✅ gerado |
| `kata-011b-template` | KATA-011B | BoundedBlockingCache | ✅ gerado |
| `kata-012a-template` | KATA-012A | ModuleConsumer | ✅ gerado |
| `kata-013a-template` | KATA-013A | PaymentException | ✅ gerado |
| `kata-014a-template` | KATA-014A | TextProcessor | ✅ gerado |
| `kata-014b-template` | KATA-014B | TextFormatter | ✅ gerado |

---

## Referência dos scripts

### `generate.mjs`

Cria um **tema novo** com specs e testes gerados do zero.

```bash
node scripts/kata-generator/generate.mjs \
  --theme "Reactive Streams" \
  --difficulty advanced \
  --count 3 \
  --cert "Oracle Java 21 - Module 12"
```

Saída em `scripts/kata-generator/output/`.

### `generate-from-catalog.mjs`

Gera testes para **katas existentes** no catálogo.

```bash
node scripts/kata-generator/generate-from-catalog.mjs
node scripts/kata-generator/generate-from-catalog.mjs --theme streams
node scripts/kata-generator/generate-from-catalog.mjs --skip KATA-001A,KATA-007A
```

### `create-all-katas.sh`

```bash
# Local + generator output
bash scripts/create-all-katas.sh --from-generator --local

# GitHub + generator output
bash scripts/create-all-katas.sh --from-generator

# Local + lista hardcoded do catálogo (sem testes gerados)
bash scripts/create-all-katas.sh --local

# GitHub + lista hardcoded (pula branches que já existem)
bash scripts/create-all-katas.sh --skip-existing
```

### `create-kata-local.sh`

```bash
bash scripts/create-kata-local.sh <KATA-ID> <ClassName> [test-file-path]
```

### `create-kata.sh`

```bash
bash scripts/create-kata.sh <KATA-ID> <ClassName> [test-file-path]
```

---

## Fluxo do hero após criação das branches

1. Hero acessa o Dojo UI e escolhe um tema
2. Hub entrega `KataDelivery` via AMQP → SSE mostra o kata
3. Hero clona a branch template:
   ```bash
   git clone -b kata-007a-template https://github.com/fidelisfelipe/skillforge-katas
   ```
4. Executa `mvn verify` (testes devem falhar — classe vazia)
5. Implementa `SalesReporter.java` até todos passarem
6. Cria branch de solução e abre PR:
   ```bash
   git checkout -b kata-007a-{heroId}-solution
   git commit -am "kata-007a: implement SalesReporter"
   git push origin kata-007a-{heroId}-solution
   # PR body deve conter: heroId: {heroId}
   ```
7. Hub recebe webhook do GitHub, clona a branch de solução, executa `mvn verify` e publica o resultado via AMQP
