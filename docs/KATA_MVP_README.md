---
ai-context: "guide"
ai-audience: ["senior-developers", "architects"]
ai-scope: "Resumo executivo do MVP de Kata (Phase 5.0) — arquivos, configuração e próximos passos"
---

# 🥋 Kata MVP (Phase 5.0) — Executive Summary

> Implementação do fluxo **Hero → Hub** via **RabbitMQ** (infra centralizada)

---

## 📦 O Que Foi Criado

### **1. Message Contracts (Specs)**

📄 `specs/KATA_MESSAGES.md`

Define os Records Java para comunicação:
- `KataThemeRequestMessage` — Hero solicita temas/kata
- `KataThemeResponseMessage` — Hub responde com temas/kata
- `KataSubmissionMessage` — Hero submete kata para validação
- `KataValidationResultMessage` — Hub envia resultado
- `SkillValidatedEvent` — Broadcast SSE para guilda

### **2. Hub Backend**

📄 `hub/src/main/java/io/skillforge/dojo/KataMQConsumer.java`

Consumer que:
- Escuta fila `kata.requests`
- Processa: LIST_THEMES, CHOOSE_THEME, NEXT_KATA, REQUEST_DIFFERENT
- Publica em `kata.responses` (resposta específica para hero)
- Cria issues no GitHub para rastreamento

### **3. Hero Backend**

📄 `heroes/hero-template/src/main/java/io/skillforge/dojo/DojoMQConsumer.java`

Consumer que:
- Escuta fila `kata.responses` e `kata.validation.results`
- Atualiza cache local
- Envia notificações SSE para frontend

### **4. Configuração**

📄 `hub/src/main/resources/application-kata.yml`  
📄 `heroes/hero-template/src/main/resources/application-kata.yml`

Spring Boot profiles para conectar à infra RabbitMQ centralizada

### **5. Documentação**

📄 `docs/KATA_MVP_SETUP.md` (atualizado)

Guia prático de setup e teste **sem docker-compose**

---

## 🔄 Fluxo Implementado

```
Hero Browser
    ↓
[Clica "Choose Kata"]
    ↓ JS publica
KataThemeRequestMessage → RabbitMQ: kata.requests
    ↓
Hub Consumer (KataMQConsumer)
    ↓ processa
    ↓ publica
KataThemeResponseMessage → RabbitMQ: kata.responses
    ↓
Hero Consumer (DojoMQConsumer)
    ↓ recebe
    ↓ atualiza cache
    ↓ envia SSE
Hero Browser [renderiza temas]
```

---

## 🚀 Como Usar

### **1. Configurar RabbitMQ (Infra Centralizada)**

```bash
export RABBITMQ_HOST=<seu-rabbitmq-server>
export RABBITMQ_PORT=5672
export RABBITMQ_USERNAME=skillforge-dev
export RABBITMQ_PASSWORD=<sua-senha>
export RABBITMQ_VHOST=/skillforge
```

**Queues pré-requisito:**
- `kata.requests`
- `kata.responses`
- `kata.submission`
- `kata.validation.results`

### **2. Subir Hub**

```bash
cd ~/skillforge/hub

mvn spring-boot:run \
  --spring-boot.run.arguments="--spring.profiles.active=kata" \
  -Dspring.rabbitmq.host=$RABBITMQ_HOST \
  -Dspring.rabbitmq.port=$RABBITMQ_PORT \
  -Dspring.rabbitmq.username=$RABBITMQ_USERNAME \
  -Dspring.rabbitmq.password=$RABBITMQ_PASSWORD \
  -Dspring.rabbitmq.virtual-host=$RABBITMQ_VHOST
```

Hub em: http://localhost:8080

### **3. Subir Hero**

```bash
export HERO_ID=alice
export HERO_NAME="Alice"

cd ~/skillforge/heroes/hero-template

mvn spring-boot:run \
  --spring-boot.run.arguments="--spring.profiles.active=kata --server.port=8081" \
  -Dspring.rabbitmq.host=$RABBITMQ_HOST \
  [... outras vars]
```

Hero em: http://localhost:8081

---

## 📋 Próximos Passos

### **Fase 5.1: Criar KATA-001 Real**

1. Definir `quests/dojo/java-21-certified/THEMES.json`
2. Criar `quests/dojo/java-21-certified/KATA-001-virtual-threads/`
   - `SPEC.md` (problema)
   - `pom.xml` (template)
   - `src/main/java/VirtualThreadKata.java` (gaps)
   - `src/test/java/VirtualThreadKataTest.java` (validação)
   - `validation-rules.json` (regras)

### **Fase 5.2: Frontend Thymeleaf**

1. Criar template HTML: `hub/src/main/resources/templates/dojo.html`
2. JavaScript client: `hub/src/main/resources/static/dojo.js`
3. Conectar ao MQ consumer

### **Fase 5.3: Maven Plugin Kata Validator**

1. Criar Maven plugin que roda `mvn verify` localmente
2. Validações:
   - Testes passam
   - Coverage > threshold
   - Sem APIs deprecated
   - Memory < limite

### **Fase 5.4: GitHub Integration**

1. Webhook que detecta PR com label `kata:{kataId}`
2. Hub valida automaticamente
3. Merge automático se passou

---

## 🧪 Testar Fluxo MVP

```bash
# Terminal 1: Hub logs
cd hub && mvn spring-boot:run | grep KATA

# Terminal 2: Hero logs
cd heroes/hero-template && mvn spring-boot:run | grep KATA

# Terminal 3: Browser
open http://localhost:8081/dojo

# Ver logs de MQ
# (via RabbitMQ Management UI centralizado)
```

---

## 📝 Arquivos Não Necessários

Os seguintes arquivos criados **podem ser removidos** pois você tem infra centralizada:

- ❌ `docker-compose.yml` (removido)
- ❌ `infrastructure/rabbitmq/init.sh` (removido)

Apenas **mantenha:**
- ✅ `specs/KATA_MESSAGES.md`
- ✅ `hub/src/main/java/io/skillforge/dojo/KataMQConsumer.java`
- ✅ `heroes/hero-template/src/main/java/io/skillforge/dojo/DojoMQConsumer.java`
- ✅ `hub/src/main/resources/application-kata.yml`
- ✅ `heroes/hero-template/src/main/resources/application-kata.yml`
- ✅ `docs/KATA_MVP_SETUP.md`

---

## 🎯 Status

| Componente | Status | Próximo |
|-----------|--------|---------|
| Message Contracts | ✅ Done | N/A |
| Hub Consumer | ✅ Done | Testar |
| Hero Consumer | ✅ Done | Testar |
| RabbitMQ Config | ✅ Done | Usar infra existente |
| Frontend | ❌ TODO | Criar Thymeleaf |
| KATA-001 | ❌ TODO | Implementar |
| Validator Plugin | ❌ TODO | Criar Maven plugin |
| GitHub Integration | ❌ TODO | Webhooks |

---

## 🔗 Referências

- [KATA_MESSAGES.md](KATA_MESSAGES.md) — Message contracts
- [KATA_MVP_SETUP.md](KATA_MVP_SETUP.md) — Setup guide
- [PRINCIPLES.md](PRINCIPLES.md) — Arquitetura (SLM-local-first, MQ-driven)

