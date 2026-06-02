# SkillForge

> Transformando as habilidades de uma equipa de developers em uma guilda capaz de descobrir, desbloquear e resolver problemas de negócio em tempo real.

![img_2.png](img_2.png)

---

## Antes de qualquer quest — treino no Dojo

Skills não se declaram. **Skills se provam.**

Após fazer o fork e registrar seu nó herói, o caminho começa no **Dojo**: um sistema de exercícios práticos (katas) calibrados pelos capítulos da certificação OCP Java 21. Cada kata resolvido é validado automaticamente pelo hub, desbloqueia XP real e adiciona uma skill verificada ao seu perfil — que o hub usa para rotear quests para você.

```
fork → registrar herói → Dojo (katas) → XP + skill desbloqueada → quests
```

Sem treino, sem skill. Sem skill, sem quest.

---

## O que é

O **SkillForge** é uma plataforma de capacidade coletiva orientada a skills.

Cada developer roda um **nó herói** localmente. Esse nó se registra num **Guild Hub** central, que mantém um grafo das capacidades da equipa, roteia problemas para os heróis certos em paralelo e emite notificações em tempo real quando novas skills desbloqueiam novas possibilidades.

O objetivo não é apenas gamificar colaboração — é transformar especialização técnica em infraestrutura operacional. E especialização sem evidência não conta.

---

## O problema

Em muitas equipas, capacidade técnica fica invisível:

- conhecimento individual não formalizado
- especializações implícitas que dependem de coordenação manual
- ninguém sabe exatamente o que o grupo consegue resolver agora
- onboarding lento, gargalos em especialistas, baixo paralelismo

O time possui capacidade. Mas não consegue usá-la como sistema.

---

## Como funciona

### Fluxo do herói

```
1. Fork skillforge → sobe hero-template localmente
2. Registra no Guild Hub (onboarding AMQP)
3. Abre o Dojo em http://localhost:8081/
4. Escolhe um tema (chapter do OCP Java 21)
5. Hub entrega um kata via AMQP → SSE exibe no hero-template
6. Clona a branch template do skillforge-katas
7. Implementa a classe até os testes passarem (mvn verify)
8. Abre PR com  heroId: <seu-id>  no body
9. Hub valida automaticamente: clone → mvn verify → resultado via AMQP
10. XP creditado, skill desbloqueada → quests disponíveis
```

### Fluxo da guilda

```
Dev sobe nó herói  →  herói se registra via AMQP
                   →  hub atualiza o Capability Graph
                   →  quests pendentes são reavaliadas
                   →  SSE notifica toda a guilda em tempo real

Problema submetido →  hub seleciona heróis com skills relevantes
                   →  fan-out paralelo
                   →  XP distribuído, audit trail persistido
```

---

## O Dojo

O Dojo é o sistema de treinamento do SkillForge. Sem passar por ele, um herói não tem skills verificadas e não é elegível para quests.

### Katas disponíveis

Os katas estão organizados pelos 14 chapters do guia **OCP Java 21 (1Z0-830)**:

| Chapter | Katas | Skills desbloqueadas |
|---------|-------|---------------------|
| Ch.01 — Building Blocks | KATA-015A … 015E | `java-21-building-blocks` |
| Ch.02 — Operators | KATA-016A … 016E | `java-21-operators` |
| Ch.03 — Making Decisions | KATA-006A, 006B | `java-21-flow-control` |
| Ch.04 — Core APIs | KATA-005A, 014A, 014B | `java-21-core-apis` |
| Ch.05 — Methods | KATA-017A … 017E | `java-21-methods` |
| Ch.07 — Beyond Classes | KATA-002A–C, 003A–C, 004A–B | `java-21-records`, `java-21-pattern-matching` |
| Ch.08 — Lambdas | KATA-008A, 008B | `java-21-lambdas` |
| Ch.09 — Collections | KATA-010A, 010B | `java-21-collections` |
| Ch.10 — Streams | KATA-007A–C, 009A | `java-21-streams` |
| Ch.11 — Exceptions | KATA-013A | `java-21-exceptions` |
| Ch.12 — Modules | KATA-012A | `java-21-modules` |
| Ch.13 — Concurrency | KATA-001A–C, 011A–B | `java-21-virtual-threads` |

Catálogo completo → [`quests/dojo/java-21-certified/catalog.yml`](quests/dojo/java-21-certified/catalog.yml)
Branches template → [github.com/fidelisfelipe/skillforge-katas](https://github.com/fidelisfelipe/skillforge-katas)

### Como resolver um kata

```bash
# 1. O hub entrega o kata via Dojo UI (hero-template:8081)
# 2. Clone a branch template
git clone -b kata-007a-template https://github.com/fidelisfelipe/skillforge-katas kata-007a

# 3. Crie sua branch de solução
cd kata-007a
git checkout -b kata-007a-{seuHeroId}-solution

# 4. Implemente até os testes ficarem verdes
mvn verify

# 5. Push e abra o PR
git push origin kata-007a-{seuHeroId}-solution
# Body do PR:
#   heroId: {seuHeroId}

# Hub valida automaticamente → fecha PR → XP creditado
```

---

## Mecânicas de jogo

### Raridade das quests

| Raridade | Skills necessárias | XP | Quem pode ver |
|---|---|---|---|
| COMMON | 1–2 | 50–200 | Apprentice+ |
| RARE | 3–4 | 200–500 | Journeyman+ |
| EPIC | 5–6 | 500–1000 | Expert+ |
| LEGENDARY | 6+ | 1000+ | Master+ |

### Progressão do herói

| Nível | Nome | XP mínimo | O que desbloqueia |
|---|---|---|---|
| 1 | Apprentice | 0 | Quests COMMON |
| 3 | Journeyman | 1.000 | Quests RARE |
| 5 | Expert | 3.000 | Quests EPIC |
| 8 | Master | 8.000 | Quests LEGENDARY |
| 10 | Archmage | 20.000 | Define skills, aprova quests |

---

## Arquitetura

```
hero-template :8081  ←→  AMQP  ←→  Guild Hub :8080
     │                              │
   Dojo UI                     KataValidator
   (SSE + fetch)                (clone → mvn verify)
                                     │
                            skillforge-katas (GitHub)
                            kata-NNNx-template branches
```

### Stack

- **Java 21** — Records, Virtual Threads, Pattern Matching
- **Spring Boot 3.3** — Web + Thymeleaf + AMQP
- **RabbitMQ / CloudAMQP** — mensageria entre herói e hub
- **GitHub** — fonte de verdade para katas, quests e registro de heróis
- **Maven multi-módulo** — `shared` · `hub` · `heroes/hero-template`

---

## Para começar

**1. Fork e sobe o hero-template**

```bash
git clone https://github.com/{seu-user}/skillforge
cd skillforge/heroes/hero-template
# configure application.yml com seu heroId e credenciais AMQP
mvn spring-boot:run
```

**2. Registra no Guild Hub**

Acesse `http://localhost:8081` e siga o fluxo de onboarding AMQP.
Guia completo → [docs/onboarding/GUILD_ONBOARDING.md](docs/onboarding/GUILD_ONBOARDING.md)

**3. Treine no Dojo**

Com o hero-template rodando, acesse `http://localhost:8081/` — o Dojo abre diretamente.
Escolha um chapter, receba um kata, implemente, abra o PR.

**4. Desbloqueie quests**

Após acumular skills verificadas, quests compatíveis aparecem automaticamente no dashboard da guilda.

---

## Navegação

| | Documento | Para quem |
|---|---|---|
| 🗺️ | [INDEX.md](INDEX.md) | Mapa de navegação completo |
| ⚡ | [QUICK_START.md](QUICK_START.md) | 5 minutos para entender |
| 🥋 | [skillforge-katas](https://github.com/fidelisfelipe/skillforge-katas) | Branches template dos katas |
| 📚 | [quests/dojo/java-21-certified/catalog.yml](quests/dojo/java-21-certified/catalog.yml) | Catálogo completo de katas |
| 🏰 | [hub/README.md](hub/README.md) | Guild Hub — API e fluxo |
| 🚀 | [docs/onboarding/GUILD_ONBOARDING.md](docs/onboarding/GUILD_ONBOARDING.md) | Onboarding para novos heróis |

---

## Resultado esperado

> O Guild Hub central conecta os Nós Heróis locais através de um Capability Graph — o conhecimento técnico da equipa, antes invisível, torna-se infraestrutura operacional estruturada. Skills acendem conforme são provadas no Dojo.

![img_1.png](img_1.png)

> Quando um problema é submetido, o hub identifica e aciona simultaneamente os heróis com as skills relevantes. Respostas emergem em paralelo e convergem na solução final.

![img.png](img.png)

> Esse é o ponto em que capacidade deixa de ser atributo humano e passa a ser comportamento emergente do sistema.

---

## Status

Dojo ativo · 43 katas · Ch.01–05, 07–13 cobertos · validação automática via PR
