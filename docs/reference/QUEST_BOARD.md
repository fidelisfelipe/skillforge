---
ai-context: "reference"
ai-audience: ["all"]
ai-scope: "Catálogo de quests disponíveis com raridade, XP e requirements"
---

# Quest Board

> Quests são tarefas reais do projeto. Completar uma quest avança o sistema e gera XP para o herói.

---

## Como funcionam as quests

- Cada quest tem **skills requeridas** (você deve declará-las no manifesto)
- Cada quest tem **critérios de aceitação** claros — sem critério, sem XP
- Pull Requests são a prova de entrega — título deve referenciar o ID da quest
- XP é distribuído ao merge, conforme a [fórmula do README](README.md#distribuição-de-xp)
- Quests podem ser feitas em paralelo por heróis diferentes

---

## Status das quests

| Símbolo | Significado |
|---------|-------------|
| `[ ]` | Aberta — ninguém pegou ainda |
| `[~]` | Em progresso — verificar se tem assignee |
| `[x]` | Completada |
| `[!]` | Bloqueada — falta skill na guilda |

---

## Apprentice Track — Nível 1–2

*Para devs juniores. Quests concretas, escopo fechado, sem dependência de arquitetura.*

---

### `QUEST-001` — Hero Health Endpoint `COMMON` `+100 XP`

**Status:** `[ ]`  
**Skills requeridas:** `java`, `spring-boot`  
**Módulo:** `hero-template`

**O que fazer:**  
Implementar o endpoint `GET /health` no template do herói. Deve retornar o estado atual do nó.

**Contrato esperado:**
```json
{
  "heroId": "string",
  "status": "UP | DOWN | DEGRADED",
  "uptime": "seconds",
  "timestamp": "ISO-8601"
}
```

**Critérios de aceitação:**
- [ ] Endpoint responde 200 com o contrato acima
- [ ] `status` é `DOWN` se o modelo Ollama não estiver acessível
- [ ] `uptime` conta desde o início da aplicação
- [ ] Teste de integração cobrindo os três estados de `status`

---

### `QUEST-002` — Hero Manifest Endpoint `COMMON` `+150 XP`

**Status:** `[ ]`  
**Skills requeridas:** `java`, `spring-boot`, `java-records`  
**Módulo:** `hero-template`

**O que fazer:**  
Implementar `GET /manifest` que devolve o manifesto completo do herói em runtime.

**Contrato esperado:**
```java
record HeroManifest(
    String heroId,
    String heroName,
    String heroClass,
    List<String> skills,
    String endpoint,
    String model,
    String specialty,
    String version
)
```

**Critérios de aceitação:**
- [ ] Endpoint devolve o record serializado como JSON
- [ ] Dados carregados do `manifest.json` em `resources/`
- [ ] `version` corresponde ao `git rev-parse --short HEAD` no momento do build
- [ ] Teste unitário para o carregamento do manifesto

---

### `QUEST-003` — Persist Hero Registration `COMMON` `+200 XP`

**Status:** `[ ]`  
**Skills requeridas:** `java`, `sqlite`, `jdbc`  
**Módulo:** `hub`

**O que fazer:**  
Quando um herói se registra no hub, persistir o registo em SQLite. Suportar re-registro (upsert por `heroId`).

**Schema esperado:**
```sql
CREATE TABLE heroes (
    hero_id     TEXT PRIMARY KEY,
    hero_name   TEXT NOT NULL,
    hero_class  TEXT,
    skills      TEXT,  -- JSON array
    endpoint    TEXT NOT NULL,
    model       TEXT,
    specialty   TEXT,
    registered_at TEXT,
    last_seen   TEXT,
    status      TEXT DEFAULT 'ACTIVE'
);
```

**Critérios de aceitação:**
- [ ] Registro persiste após restart do hub
- [ ] Re-registro atualiza `last_seen` e mantém `registered_at` original
- [ ] `status` é marcado como `INACTIVE` se `/health` falhar após 3 tentativas
- [ ] Teste de integração com banco real (não mock)

---

### `QUEST-004` — Integration Tests for Hero Endpoints `COMMON` `+150 XP`

**Status:** `[ ]`  
**Skills requeridas:** `java`, `testing`, `spring-boot-test`  
**Módulo:** `hero-template`

**O que fazer:**  
Escrever testes de integração para `/health`, `/manifest` e `/solve` usando `@SpringBootTest` e `MockMvc`.

**Critérios de aceitação:**
- [ ] Cobertura mínima de 80% nos controllers
- [ ] Testes para cenários de erro (Ollama down, input inválido)
- [ ] Testes rodam com `mvn test` sem configuração extra
- [ ] Sem mocks do banco de dados — usar H2 em memória ou SQLite de teste

---

## Journeyman Track — Nível 3–4

*Para devs plenos. Requer entender a arquitetura. Quests com decisões de design.*

---

### `QUEST-005` — Capability Graph `RARE` `+350 XP`

**Status:** `[!]` *(requer QUEST-003 completa)*  
**Skills requeridas:** `java`, `data-structures`, `graph-algorithms`  
**Módulo:** `hub`

**O que fazer:**  
Implementar o `CapabilityGraph` — estrutura em memória que mantém o mapeamento `skill → List<Hero>` e permite consultar quais heróis resolvem um conjunto de skills.

**Interface esperada:**
```java
class CapabilityGraph {
    void register(HeroRegistration hero);
    void deregister(String heroId);
    List<HeroRegistration> findBySkills(List<String> required, List<String> optional);
    Set<String> availableSkills();
    Map<String, List<String>> skillToHeroes(); // para debug/dashboard
}
```

**Critérios de aceitação:**
- [ ] `findBySkills` devolve heróis que cobrem **todas** as skills required
- [ ] Heróis com skills optional são priorizados no resultado
- [ ] Graph é thread-safe (Virtual Threads do Java 21)
- [ ] Reconstruído a partir do banco ao iniciar o hub
- [ ] Testes unitários cobrindo: registro, deregistro, busca por subset de skills

---

### `QUEST-006` — Hero Registration API `RARE` `+300 XP`

**Status:** `[ ]`  
**Skills requeridas:** `java`, `spring-boot`, `rest-api`  
**Módulo:** `hub`

**O que fazer:**  
Implementar `POST /heroes/register` que aceita o manifesto do herói, persiste e atualiza o Capability Graph.

**Contrato:**
```java
// Request body
record HeroRegistration(String heroId, String heroName, String heroClass,
    List<String> skills, String endpoint, String model, String specialty)

// Response
record RegistrationResult(String heroId, String status, int questsUnlocked,
    List<String> unlockedQuestIds)
```

**Critérios de aceitação:**
- [ ] Valida que `heroId`, `endpoint` e pelo menos uma `skill` estão presentes
- [ ] Retorna 409 se herói já ativo com mesmo `heroId` e `endpoint` diferente
- [ ] Retorna `questsUnlocked` com quests que ficaram disponíveis por causa deste registro
- [ ] Teste de integração end-to-end: registrar → verificar no graph → verificar no banco

---

### `QUEST-007` — Parallel Fan-out com CompletableFuture `RARE` `+400 XP`

**Status:** `[!]` *(requer QUEST-005 e QUEST-006 completas)*  
**Skills requeridas:** `java`, `concurrency`, `completable-future`  
**Módulo:** `hub`

**O que fazer:**  
Implementar o mecanismo de fan-out paralelo: dado um problema, o hub consulta o Capability Graph, seleciona os heróis relevantes e dispara as chamadas em paralelo com `CompletableFuture.allOf()`.

**Comportamento esperado:**
- Heróis respondem ou timeout em 10s (configurável)
- Respostas parciais são aceitas — timeout de um herói não cancela os outros
- Resultado final agrega `List<AgentOutput>` de todos que responderam

**Critérios de aceitação:**
- [ ] Fan-out usa Virtual Threads (não platform threads)
- [ ] Herói que excede timeout retorna `AgentOutput` com `confidence: 0.0` e `reason: "timeout"`
- [ ] Herói que retorna erro HTTP retorna `AgentOutput` com `confidence: 0.0` e o erro no `reason`
- [ ] Teste com 3 heróis mock — um normal, um lento, um com erro — verificar que todos chegam no agregado
- [ ] Logs estruturados com `heroId`, `duration_ms` e `status` por chamada

---

### `QUEST-008` — SSE Quest Unlock Notifications `RARE` `+350 XP`

**Status:** `[!]` *(requer QUEST-006 completa)*  
**Skills requeridas:** `java`, `spring-boot`, `sse`  
**Módulo:** `hub`

**O que fazer:**  
Implementar endpoint SSE `GET /guild/events` que envia notificações em tempo real quando: herói registra, quest desbloqueia, herói sobe de nível.

**Formato dos eventos:**
```
event: HERO_JOINED
data: {"heroId":"MLOracle","skills":["ml-inference"],"questsUnlocked":3}

event: QUEST_UNLOCKED
data: {"questId":"QUEST-011","title":"Pipeline ML","rarity":"EPIC","xpReward":800}

event: HERO_LEVEL_UP
data: {"heroId":"CodeBlade","newLevel":"Expert","xp":3000}
```

**Critérios de aceitação:**
- [ ] SSE mantém conexão aberta — heartbeat a cada 30s
- [ ] Novo herói que conecta recebe o estado atual da guilda como primeiro evento (`GUILD_STATE`)
- [ ] Múltiplos clientes recebem o mesmo evento simultaneamente
- [ ] Teste: registrar herói via API → verificar que evento `HERO_JOINED` chega no SSE

---

## Expert Track — Nível 5–7

*Para devs seniors. Requer decisões de arquitetura, algoritmos não-triviais, liderança técnica.*

---

### `QUEST-009` — XP Calculation Engine `EPIC` `+700 XP`

**Status:** `[!]` *(requer QUEST-007 completa)*  
**Skills requeridas:** `java`, `domain-design`, `algorithms`  
**Módulo:** `core`

**O que fazer:**  
Implementar o motor de cálculo de XP conforme as regras do sistema. Deve ser determinístico, auditável e persistido por quest.

**Regras:**
```
XP_base = rarity.baseXp
XP_required_hero = (XP_base / heroes_required.size) * 1.2
XP_optional_hero = (XP_base / heroes_optional.size) * 0.6
XP_synthesizer = XP_base * 0.15
Speed_bonus = top 25% mais rápidos recebem XP * 1.10
```

**Critérios de aceitação:**
- [ ] Cálculo é determinístico dado o mesmo input
- [ ] Resultado persiste em tabela `xp_events` com `heroId`, `questId`, `xp`, `reason`, `timestamp`
- [ ] Bônus de velocidade só aplica quando 4+ heróis participam
- [ ] Endpoint `GET /heroes/{heroId}/xp` retorna XP total e histórico
- [ ] Testes unitários para cada regra e para edge cases (1 herói, sem optional, etc.)

---

### `QUEST-010` — Quest Evaluation Engine `EPIC` `+600 XP`

**Status:** `[!]` *(requer QUEST-005 completa)*  
**Skills requeridas:** `java`, `algorithms`, `domain-modeling`  
**Módulo:** `hub`

**O que fazer:**  
Implementar o motor que avalia, dado o estado atual do Capability Graph, quais quests estão **AVAILABLE**, **BLOCKED** ou **COMPLETABLE**.

**Lógica:**
- `AVAILABLE`: todas as `required_skills` da quest estão no graph
- `BLOCKED`: uma ou mais `required_skills` não existem na guilda
- `COMPLETABLE`: available + heróis suficientes para cobrir as skills (sem overlap total)

**Critérios de aceitação:**
- [ ] Estado das quests recalculado sempre que o Capability Graph muda
- [ ] Endpoint `GET /quests` retorna todas as quests com status atual
- [ ] Endpoint `GET /quests?status=AVAILABLE` filtra por status
- [ ] Quando herói registra, response inclui lista de quests que mudaram de estado
- [ ] Testes: adicionar skill que desbloqueia quest → verificar status muda

---

### `QUEST-011` — Synthesizer Node `EPIC` `+800 XP`

**Status:** `[!]` *(requer QUEST-007 completa)*  
**Skills requeridas:** `java`, `ollama`, `ai-integration`  
**Módulo:** `synthesizer`

**O que fazer:**  
Implementar o nó sintetizador que recebe `List<AgentOutput>` de múltiplos heróis e produz uma resposta coesa usando `qwen2.5:7b` via Ollama.

**Contrato:**
```java
record SynthesisRequest(String questId, String originalProblem, List<AgentOutput> heroOutputs)
record SynthesisResult(String questId, String synthesis, double confidence,
    List<String> contributingHeroes, int tokensUsed)
```

**Critérios de aceitação:**
- [ ] Prompt enviado ao Ollama inclui as respostas de todos os heróis com atribuição
- [ ] Respostas com `confidence < 0.3` são descartadas antes da síntese
- [ ] Se menos de 2 respostas válidas, retorna a melhor sem síntese (flag `synthesized: false`)
- [ ] `tokensUsed` é registado para auditoria
- [ ] Teste de integração com Ollama local (skip se Ollama não disponível no CI)

---

## Master Track — Nível 8+

*Para tech leads e seniors com visão de produto. Quests definem o futuro do sistema.*

---

### `QUEST-012` — Skill Taxonomy Definition `LEGENDARY` `+1200 XP`

**Status:** `[ ]`  
**Skills requeridas:** `technical-leadership`, `domain-design`  
**Módulo:** `core` + documentação

**O que fazer:**  
Definir e documentar a taxonomia canónica de skills da guilda — o vocabulário controlado que todos os heróis usam para declarar capacidades.

**Entregáveis:**
- `SKILL_TAXONOMY.md` com categorias, skills e descrições
- `skills.json` em `core/src/main/resources/` com a lista validada
- Validação no endpoint de registro que avisa (sem bloquear) skills não reconhecidas
- Proposta de processo de evolução da taxonomia (quem pode adicionar skills)

**Critérios de aceitação:**
- [ ] Mínimo 40 skills em pelo menos 6 categorias
- [ ] Cada skill tem: `id`, `name`, `category`, `description`, `aliases`
- [ ] Aprovado por pelo menos 2 Experts ou 1 Master via PR review
- [ ] Hub usa a taxonomia para sugerir correções de typos no registro

---

### `QUEST-013` — Guild Progress Dashboard `LEGENDARY` `+1500 XP`

**Status:** `[!]` *(requer QUEST-008, QUEST-009, QUEST-010 completas)*  
**Skills requeridas:** `architecture`, `frontend`, `sse`, `technical-leadership`  
**Módulo:** novo módulo `dashboard`

**O que fazer:**  
Criar um dashboard web em tempo real que mostra o estado atual da guilda: heróis ativos, skills disponíveis, quests abertas, leaderboard e eventos recentes.

**Páginas/seções:**
- `/` — visão geral: heróis ativos, quests disponíveis, XP total da guilda
- `/heroes` — lista de heróis com skills, nível e XP
- `/quests` — quest board com filtros por raridade e status
- `/events` — feed em tempo real via SSE

**Critérios de aceitação:**
- [ ] Página principal carrega em < 500ms (dados do hub, não LLM)
- [ ] Feed de eventos atualiza sem refresh via SSE
- [ ] Dashboard funciona sem JavaScript framework — HTML + CSS + EventSource API
- [ ] Responsivo (mobile-first)
- [ ] Serve como documentação viva do estado da guilda

---

## Como pegar uma quest

1. Verifique se você tem as skills requeridas no seu manifesto
2. Comente na issue correspondente (será criada no GitHub por quest)
3. Crie branch: `quest/QUEST-NNN-descricao-curta`
4. Abra PR com título `[QUEST-NNN] Descrição` qu