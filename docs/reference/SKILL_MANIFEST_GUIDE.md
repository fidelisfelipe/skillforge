---
ai-context: "guide"
ai-audience: ["all"]
ai-scope: "Como declarar e validar skills em manifest.json"
---

# Skill Manifest Guide

> O manifesto é o contrato entre você e a guilda. Quanto mais preciso, mais quests você pode participar.

---

## O que é um Skill Manifest

É o arquivo que define quem você é no sistema. Quando o seu herói registra no Guild Hub, ele envia este manifesto. O hub usa essas informações para:

- incluir você no Capability Graph da guilda
- calcular quais quests você pode participar
- rotear problemas para os heróis certos
- emitir notificações quando suas skills desbloqueiam algo novo

---

## Níveis de validação

Cada skill que você declara passa por um processo de validação progressivo:

| Nível | Badge | Como chegar |
|-------|-------|-------------|
| `DECLARED` | — | Você declarou no manifesto |
| `DEMONSTRATED` | ✓ | Pelo menos 1 quest completada com essa skill |
| `PROVEN` | ✓✓ | 3+ quests completadas ou validação explícita de um Master |

Skills `DECLARED` entram no graph mas com peso menor no roteamento.  
Skills `PROVEN` têm prioridade em quests EPIC e LEGENDARY.

Você não precisa esperar validação para começar. Declare o que você sabe e prove nas quests.

---

## Template do manifesto

Crie o arquivo em `heroes/seu-nome/src/main/resources/manifest.json`:

```json
{
  "heroId": "seu-nome-unico",
  "heroName": "Nome de exibição",
  "heroClass": "Backend | Frontend | Data | DevOps | Fullstack | ML | Security",
  "skills": [
    "java",
    "spring-boot",
    "postgresql"
  ],
  "endpoint": "http://localhost:8081",
  "model": "phi3:mini",
  "specialty": "Uma frase descrevendo em que você é especialista"
}
```

### Campos obrigatórios

| Campo | Tipo | Descrição |
|-------|------|-----------|
| `heroId` | string | Identificador único. Use kebab-case. Ex: `felipe-dev` |
| `heroName` | string | Nome de exibição na guilda |
| `heroClass` | string | Classe principal do herói |
| `skills` | array | Lista de skills. Mínimo: 1. Ver categorias abaixo. |
| `endpoint` | string | URL base do seu nó herói. Ex: `http://localhost:8081` |
| `model` | string | Modelo Ollama que você está rodando localmente |
| `specialty` | string | Uma frase. O que você resolve melhor? |

---

## Categorias de skills e exemplos

Use os IDs exatos na lista de `skills` do manifesto.

### Linguagens e runtimes
```
java              python           typescript        go
kotlin            rust             scala             clojure
java-21           python-3.11      node-20
```

### Frameworks e plataformas
```
spring-boot       spring-webflux   quarkus           micronaut
react             vue              nextjs             htmx
fastapi           django           flask              express
```

### Dados e persistência
```
postgresql        mysql            sqlite             mongodb
redis             elasticsearch    jdbc               jpa
data-modeling     schema-design    sql                nosql
```

### Infraestrutura e cloud
```
docker            kubernetes       terraform          ansible
aws               gcp              azure              linux
ci-cd             github-actions   nginx
```

### Concorrência e sistemas distribuídos
```
concurrency       virtual-threads  completable-future reactive
sse               websocket        kafka              rabbitmq
distributed-systems  event-driven  cqrs
```

### Inteligência artificial e ML
```
ollama            langchain        ml-inference       llm-integration
pytorch           scikit-learn     data-science       nlp
prompt-engineering fine-tuning     rag
```

### Qualidade e processo
```
testing           tdd              integration-tests  spring-boot-test
java-records      domain-design    clean-architecture ddd
rest-api          openapi          api-design
```

### Liderança técnica
```
technical-leadership  architecture  system-design    mentoring
code-review       algorithms       graph-algorithms  data-structures
```

> Não encontrou sua skill? Abra uma issue com o label `skill-request`.  
> Skills fora da lista são aceitas mas ficam como `UNRECOGNIZED` no graph.

---

## Como escrever boas skills

**Seja específico, não genérico.**

```json
// Ruim — genérico demais
"skills": ["programação", "backend", "banco de dados"]

// Bom — skills reais e verificáveis
"skills": ["java", "spring-boot", "postgresql", "jdbc", "rest-api"]
```

**Declare o que você consegue entregar, não o que já leu.**

A pergunta certa é: *"Consigo completar uma quest com esse requisito sem pedir ajuda?"*  
Se sim, declare. Se não, aguarde até ter mais confiança.

**Não exagere nem subestime.**

Skills sobredeclaradas geram respostas de baixa qualidade e XP negativo por quest mal entregue.  
Skills subdeclaradas fazem você perder quests onde poderia contribuir.

---

## Exemplos de manifestos reais

### Dev Backend Java

```json
{
  "heroId": "java-forge",
  "heroName": "JavaForge",
  "heroClass": "Backend",
  "skills": ["java", "java-21", "spring-boot", "postgresql", "jdbc", "rest-api", "testing", "docker"],
  "endpoint": "http://localhost:8081",
  "model": "phi3:mini",
  "specialty": "APIs REST robustas com Java 21 e Spring Boot, foco em contratos imutáveis"
}
```

### Dev Data/ML

```json
{
  "heroId": "ml-oracle",
  "heroName": "MLOracle",
  "heroClass": "ML",
  "skills": ["python", "pytorch", "scikit-learn", "ollama", "ml-inference", "data-science", "postgresql"],
  "endpoint": "http://localhost:8083",
  "model": "phi3:mini",
  "specialty": "Inferência local com modelos Ollama e análise preditiva com scikit-learn"
}
```

### Dev Frontend

```json
{
  "heroId": "ui-weaver",
  "heroName": "UIWeaver",
  "heroClass": "Frontend",
  "skills": ["typescript", "react", "htmx", "sse", "rest-api", "css"],
  "endpoint": "http://localhost:8084",
  "model": "phi3:mini",
  "specialty": "Interfaces reativas com SSE e foco em performance sem frameworks pesados"
}
```

---

## Como submeter ao hub

### 1. Validar localmente antes de registrar

```bash
# O herói deve responder em /manifest com os dados do seu manifest.json
curl http://localhost:808X/manifest

# Verifique se todos os campos obrigatórios estão presentes
# Verifique se as skills usam os IDs corretos da taxonomia
```

### 2. Registrar no Guild Hub

```bash
curl -X POST http://localhost:8080/heroes/register \
  -H "Content-Type: application/json" \
  -d @src/main/resources/manifest.json
```

### 3. Verificar o resultado

```json
// Resposta esperada
{
  "heroId": "java-forge",
  "status": "ACTIVE",
  "questsUnlocked": 2,
  "unlockedQuestIds": ["QUEST-001", "QUEST-002"],
  "warnings": ["Skill 'spring-webflux' não reconhecida na taxonomia — registrada como UNRECOGNIZED"]
}
```

### 4. Confirmar presença no graph

```bash
curl http://localhost:8080/guild/capabilities
# deve listar seu heroId nas skills que você declarou
```

---

## Processo de validação por peers

Para uma skill sair de `DECLARED` para `DEMONSTRATED`:
- Complete pelo menos 1 quest que a utilize como `required`
- O sistema atualiza automaticamente ao merge do PR

Para `PROVEN`:
- Complete 3+ quests com a skill como `required`, ou
- Peça review a um herói Master ou Archmage via comentário na sua issue de registro

---

## Evoluindo seu manifesto

Você pode atualizar seu manifesto a qualquer momento:

```bash
# Edite manifest.json, então re-registre
curl -X POST http://localhost:8080/heroes/register \
  -H "Content-Type: application/json" \
  -d @src/main/resources/manifest.json

# O hub faz upsert — mantém histórico, atualiza skills e recalcula quests
```

Novas skills adicionadas são imediatamente consideradas no Capability Graph.  
Skills removidas ficam como `RETIRED` no histórico (não ap