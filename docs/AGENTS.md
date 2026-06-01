---
ai-context: "decision-record"
ai-audience: ["AI-tools", "architects", "senior-developers"]
---

# 🏗️ AGENTS.md — Decisões Arquiteturais do SkillForge

> Este arquivo descreve **por quê** certas decisões foram tomadas.
> 
> Leia este arquivo se você é:
> - ✅ Uma ferramenta de IA (Copilot, IntelliJ, Claude)
> - ✅ Um senior developer projetando novo herói ou feature
> - ✅ Alguém querendo entender trade-offs arquiteturais

---

## 1️⃣ SLM Local Antes de API Paga

**Decisão:** Qualquer funcionalidade com LLM deve primeiro avaliar um modelo local via Ollama.

**Hierarquia de decisão:**
```
1. SLM local via Ollama          (custo zero, privacidade total)
2. SLM especializado via Ollama  (ex: meditron para médico)
3. API paga como fallback        (Claude, OpenAI — apenas se SLM não atinge qualidade)
4. API paga + SLM em conjunto    (casos onde estrutura + validação de domínio são necessários)
```

**Quando API paga é justificável:**
- Raciocínio estruturado complexo que SLMs 7B não entregam consistentemente
- Geração de conteúdo com formato estrito (JSON, código, docs técnicas)
- Validação cruzada entre modelos (SLM valida domínio, API estrutura output)

**Exemplo aprovado:** Quest Scribe usa Claude API para estrutura + Meditron (Ollama) para validação clínica.

---

## 2️⃣ GitHub Issues como Banco de Dados Intencional

**Decisão:** Heroes, quests, XP e validações vivem em GitHub Issues + Labels.

**Não adicionar** banco relacional externo sem discussão explícita.

**Motivo:** A transparência do estado da guilda via GitHub é uma feature, não limitação.

**Labels como ledger:**
- `xp:{total}` — XP acumulado do herói
- `skill-validated:{skill}` — Skill confirmada por desafio real
- `level:{n}` — Nível atual
- `hero` + `registered` — Herói validado
- `quest` + `spark` — Tipo de quest

---

## 3️⃣ Zero Frameworks de Frontend

**Decisão:** Dashboards usam HTML + CSS + SSE nativo. Sem React, Vue, HTMX.

**Exceção:** Apenas com justificativa de produto clara.

**Tecnologia:**
- Thymeleaf renderiza server-side
- EventSource API cuida de tempo real
- JavaScript vanilla para interatividade mínima

---

## 4️⃣ Java 21 + Virtual Threads — Sem Platform Threads

**Decisão:** Toda concorrência usa `Executors.newVirtualThreadPerTaskExecutor()` ou `@Async` com executor configurado para virtual threads.

**Prohibited:** `CompletableFuture` com pool de platform threads = regressão.

**Motivo:** Virtual threads (Project Loom) escalam melhor, reduzem context switching.

---

## 5️⃣ Módulos são Heróis — Heróis são Módulos

**Decisão:** Cada capacidade nova é avaliada como um hero node antes de virar código acoplado ao hub.

**Fluxo:**
1. Especificar hero: `/specs/heroes/{id}/` (manifest + system-prompt)
2. Implementar hero: `/heroes/{id}/` (código Spring Boot)
3. Registrar no hub: Issue com label `hero` + `registered`
4. Hub descobre via ForkWatcher

**Benefício:** Heróis são independentes, escaláveis, testáveis isoladamente.

---

## 6️⃣ Guardrails são Funções Puras — Repositório é o Único Estado

**Decisão:** Mecanismos de validação (domain profile, quest schema, agent checklist) leem estado do repositório e retornam aprovado/reprovado, **sem persistir** resultado em banco.

**Contrato de Guardrail:**
```
guardrail(repo_state) → { valid: bool, missing: [], warnings: [] }
```

**O "estado aprovado" é expresso pelo que existe no repo:**
- `fixtures/ps-cases.json` existe → fixtures criadas ✅
- Checklist em `DOMAIN_PROFILE.md` sem `[ ]` bloqueantes → domínio aprovado
- Quest em `/quests/catalog/` → quest publicada

**Consequência:** Rodar guardrail 2x no mesmo repo produz mesmo resultado (idempotência).

**Proibido:** Tabela de "domínios aprovados", cache de validação, flag em banco.

---

## 📊 Stack Canônica

| Camada | Tecnologia | Notas |
|---|---|---|
| **Runtime** | Java 21 | Records, virtual threads, sealed classes |
| **Framework** | Spring Boot 3.3.x | Sem Jakarta EE externo |
| **Messaging** | RabbitMQ (AMQP) | Topic exchange `skillforge` |
| **LLM local** | Ollama | Modelos configuráveis por hero |
| **LLM remoto** | Claude API | Fallback ou casos justificados |
| **Persistência estado** | GitHub Issues + Labels | Ledger público e auditável |
| **Cache local** | SQLite via JDBC | Apenas estado efêmero |
| **Frontend** | Thymeleaf + SSE | Sem SPA |
| **Build** | Maven multi-módulo | Parent `pom.xml` na raiz |

---

## 🔌 Padrão AMQP Para Todos os Heróis

**Obrigatório em `application.yml`:**
```yaml
spring:
  rabbitmq:
    addresses: ${AMQP_URL}
    ssl:
      enabled: ${AMQP_SSL_ENABLED:false}
```

**Não usar em heróis novos:**
- `spring.rabbitmq.host`
- `spring.rabbitmq.port`
- `spring.rabbitmq.username`
- `spring.rabbitmq.password`

**Motivo:** Padronização de deploy e configuração única por URL AMQP.

---

## 📝 .http File Obrigatório para Testes

**Regra:** Todo hero novo deve fornecer arquivo de teste HTTP.

**Obrigatório:**
- Arquivo em `src/test/resources/`
- Nome: `{hero-id}.http`
- Conteém: GET health check + POST principal com payload válido

**Objetivo:** Padronizar validação rápida, onboarding e troubleshooting.

---

## 🌿 Branches Locais (local/*) Nunca em Remote

**Regra:** Branches prefixados com `local/` são exclusivamente locais.

**Quando usar `local/`:**
- Trabalho experimental, WIP
- Heroes em desenvolvimento inicial

**Quando publicar (`feature/`):**
- Pronto para revisão/merge

**Hook `pre-push` ativo** bloqueia automaticamente `local/*` push.

Para publicar, renomeie antes:
```bash
git branch -m local/meu-hero feature/meu-hero
git push origin feature/meu-hero
```

---

## 🎯 Decisões em Aberto (Brainstorm)

### Quest Framework e Domínios
Ver [`quests/QUEST_FRAMEWORK.md`](../quests/QUEST_FRAMEWORK.md) — define schema invariante.

Cada domínio de negócio vive em `quests/domains/{domain}/DOMAIN_PROFILE.md` antes de qualquer quest.

**Domínios prontos:**
- Medical (pronto-socorro) — `/quests/domains/medical/`
- Agency (pitch/marketing) — `/quests/domains/agency/`

### Quest Scribe Hero
Hero especializado em design de quests usando modelo dual:
- **Claude API** — escreve estrutura, formato, critérios técnicos
- **Meditron (Ollama)** — valida plausibilidade clínica dos casos

Entrada: `{domain, questType, heroLevel, context}`
Saída: Rascunho em `quests/{domain}/{QUEST-ID}.md`

Skills declaradas: `quest-design`, `clinical-reasoning`, `domain-modeling`

---

## ⛔ O Que NÃO Fazer

- ❌ Mockar banco em testes de integração (aprendi com incidente anterior)
- ❌ Usar `@Transactional` sem entender escopo — SQLite tem limitações
- ❌ Adicionar dependências se Java 21 stdlib já resolve
- ❌ Fazer push para main sem build local passando (`mvn verify`)
- ❌ Armazenar XP/validações em banco, não em GitHub labels

---

**Versão:** 1.0  
**Data:** 1 de junho de 2026  
**Mantido por:** Comunidade SkillForge

Referência: [QUICK_START.md](../QUICK_START.md) | [INDEX.md](../INDEX.md)
