---
ai-context: "guide"
ai-audience: ["senior-developers", "architects"]
ai-scope: "Como configurar e usar o MVP de Kata (Phase 5.0) com RabbitMQ"
---

# 🥋 Kata MVP Setup Guide

> Guia para rodar o sistema de Kata (Phase 5.0) localmente

---

## 📋 Pré-requisitos

- Java 21 + Maven
- Git
- **RabbitMQ Centralizado** (infra já disponível na empresa)

---

## 🚀 Quick Start

### **1. Configurar Credenciais RabbitMQ**

RabbitMQ já está rodando. Configure seu acesso:

```bash
# Crie arquivo de configuração
cat > ~/.skillforge/env.properties << EOF
RABBITMQ_HOST=rabbitmq.internal.company.com
RABBITMQ_PORT=5672
RABBITMQ_USERNAME=skillforge-dev
RABBITMQ_PASSWORD=<senha-do-seu-team>
RABBITMQ_VHOST=/skillforge
GITHUB_TOKEN=<seu-token>
EOF

# Ou exporte como variáveis
export RABBITMQ_HOST=rabbitmq.internal.company.com
export RABBITMQ_PORT=5672
export RABBITMQ_USERNAME=skillforge-dev
export RABBITMQ_PASSWORD=<senha>
```

**Nota:** As queues (`kata.requests`, `kata.responses`, etc) já devem existir na infra centralizada.

### **2. Subir Hub**

```bash
cd ~/skillforge/hub

# Build
mvn clean install

# Run com credenciais RabbitMQ
mvn spring-boot:run \
  -Dspring.rabbitmq.host=$RABBITMQ_HOST \
  -Dspring.rabbitmq.port=$RABBITMQ_PORT \
  -Dspring.rabbitmq.username=$RABBITMQ_USERNAME \
  -Dspring.rabbitmq.password=$RABBITMQ_PASSWORD \
  -Dspring.rabbitmq.virtual-host=$RABBITMQ_VHOST
```

Hub estará em: http://localhost:8080

### **3. Subir Hero-Template**

```bash
cd ~/skillforge/heroes/hero-template

# Build
mvn clean install

# Run com credenciais RabbitMQ
mvn spring-boot:run \
  -Dspring-boot.run.arguments="--server.port=8081" \
  -Dspring.rabbitmq.host=$RABBITMQ_HOST \
  -Dspring.rabbitmq.port=$RABBITMQ_PORT \
  -Dspring.rabbitmq.username=$RABBITMQ_USERNAME \
  -Dspring.rabbitmq.password=$RABBITMQ_PASSWORD \
  -Dspring.rabbitmq.virtual-host=$RABBITMQ_VHOST
```

Hero estará em: http://localhost:8081

---

## 🔄 Testar Fluxo Completo

### **Cenário: Hero escolhe Kata**

#### **1. Frontend: Hero acessa Dashboard**

```
http://localhost:8081/dojo
```

Vê botão: `[🥋 Choose Kata]`

#### **2. Frontend: Publica `LIST_THEMES`**

Quando clica, o JavaScript publica na MQ:

```java
KataThemeRequestMessage {
  heroId: "alice",
  requestType: "LIST_THEMES",
  timestamp: 1717321200000
}
// Publica em: kata.requests
```

#### **3. Hub Consumer: Consome e Responde**

```
[HUB LOG] 🎯 Processing kata request from hero: alice | Type: LIST_THEMES
[HUB LOG] 📋 Listed 3 themes for hero: alice
[HUB LOG] 📤 Response published for hero: alice
```

#### **4. Hero Consumer: Recebe Temas**

```
[HERO LOG] 📨 Received kata response | Status: THEMES_LISTED
[HERO LOG] 💾 Cached 3 available themes
[HERO LOG] ✅ Themes list sent to frontend
```

#### **5. Frontend: Renderiza Temas**

SSE recebe:

```json
{
  "id": "themes-loaded",
  "name": "THEMES_LOADED",
  "data": {
    "status": "success",
    "themesCount": 3,
    "themes": [
      {
        "id": "virtual-threads",
        "name": "Virtual Threads & Concurrency",
        "difficulty": "intermediate"
      },
      ...
    ]
  }
}
```

Frontend renderiza:

```html
<div class="theme-card">
  <h3>Virtual Threads & Concurrency</h3>
  <p>Master Java 21 Virtual Threads...</p>
  <button onclick="dojoClient.chooseTheme('virtual-threads')">
    Choose
  </button>
</div>
```

#### **6. Frontend: Hero Clica Theme → Publica `CHOOSE_THEME`**

```java
KataThemeRequestMessage {
  heroId: "alice",
  requestType: "CHOOSE_THEME",
  themeId: "virtual-threads",
  timestamp: 1717321250000
}
// Publica em: kata.requests
```

#### **7. Hub: Atribui Kata**

```
[HUB LOG] 🎯 Processing kata request from hero: alice | Type: CHOOSE_THEME
[HUB LOG] 🎯 Assigned kata KATA-001-virtual-threads from theme virtual-threads to hero: alice
[HUB LOG] 📤 Response published for hero: alice
```

#### **8. Hero: Recebe Kata**

```
[HERO LOG] 📨 Received kata response | Status: KATA_DELIVERED
[HERO LOG] 💾 Cached current kata: KATA-001-virtual-threads
[HERO LOG] ✅ Kata delivered: KATA-001-virtual-threads
```

#### **9. Frontend: Exibe Kata**

```html
<div id="kata-section">
  <h2>Your Kata</h2>
  <h3>Virtual Threads: Concurrency Made Simple</h3>
  <p>Problem: Process 10,000 concurrent HTTP requests...</p>
  <button onclick="dojoClient.acceptKata('KATA-001-virtual-threads')">
    ✅ Accept
  </button>
</div>
```

---

## 📝 Testar Submissão de Kata

### **Cenário: Hero Implementa e Submete**

#### **1. Hero: Implementa Localmente**

```bash
cd ~/your-hero/dojo/KATA-001-virtual-threads

# Edit src/main/java/VirtualThreadKata.java
# Implementar solução

# Validar localmente
mvn verify
# ✅ All tests pass
# Score: 95/100
# Ready to submit!
```

#### **2. Hero: Push para GitHub**

```bash
git checkout -b feat/KATA-001-virtual-threads
git add dojo/
git commit -m "feat: Complete KATA-001-virtual-threads

Skill: java-21-virtual-threads
Score: 95/100
Tests: All passing"

git push origin feat/KATA-001-virtual-threads
```

#### **3. GitHub: Abre PR**

PR criado: `feat/KATA-001-virtual-threads`

#### **4. Hero: Clica "Submit to Hub"**

Frontend publica:

```java
KataSubmissionMessage {
  heroId: "alice",
  kataId: "KATA-001-virtual-threads",
  gitBranch: "feat/KATA-001-virtual-threads",
  commitSha: "abc123def456...",
  timestamp: 1717321300000
}
// Publica em: kata.submission
```

#### **5. Hub: Valida Automaticamente**

```
[HUB LOG] 📨 Received kata submission | Kata: KATA-001-virtual-threads
[HUB LOG] 📥 Cloning repository...
[HUB LOG] ✅ KATA VALIDATED: KATA-001-virtual-threads | Score: 95 | XP: 150
[HUB LOG] 📤 Validation result published
```

#### **6. Hero: Recebe Resultado**

```
[HERO LOG] 📨 Kata validation result received | Kata: KATA-001-virtual-threads | Passed: true
[HERO LOG] ✅ KATA VALIDATED: KATA-001-virtual-threads | Score: 95 | XP: 150 | Skill: java-21-virtual-threads
```

#### **7. Frontend: Exibe Sucesso**

SSE:

```json
{
  "id": "kata-validated",
  "name": "KATA_VALIDATED",
  "data": {
    "status": "success",
    "kataId": "KATA-001-virtual-threads",
    "skill": "java-21-virtual-threads",
    "score": 95,
    "xpEarned": 150,
    "message": "🎉 Kata validated! Skill unlocked: java-21-virtual-threads"
  }
}
```

---

## 🔍 Monitorar Fluxo via Logs

### **Terminal 1: Hub Logs**

```bash
cd ~/skillforge/hub
mvn spring-boot:run | grep -E "KATA|kata|🎯|📨|✅"
```

### **Terminal 2: Hero Logs**

```bash
cd ~/skillforge/heroes/hero-template
mvn spring-boot:run | grep -E "KATA|kata|📨|✅|💾"
```

### **Terminal 3: Verificar Queues RabbitMQ (via Management UI)**

```bash
# Acessar UI centralizada
open http://<rabbitmq-server>:15672

# Ou usar CLI se tiver acesso
rabbitmqctl list_queues name messages

# Esperado:
# Queues                     Messages
# kata.requests              0
# kata.responses             0
# kata.submission            0
# kata.validation.results    0
```

---

## 🛠️ Troubleshooting

### **RabbitMQ não sobe**

```bash
# Ver logs
docker-compose logs rabbitmq

# Remover e tentar novamente
docker-compose down
docker volume rm skillforge_rabbitmq-data
docker-compose up -d rabbitmq
```

### **Mensagens não chegam ao Hero**

1. Verificar que `heroId` está correto em ambos os lados
2. Verificar que filas existem:

```bash
docker exec skillforge-rabbitmq rabbitmqctl list_queues
```

3. Ver mensagens na fila:

```bash
docker exec skillforge-rabbitmq rabbitmqctl list_queue_details name messages consumers
```

### **Hub não consegue clonar GitHub**

```bash
# Verificar token
echo $GITHUB_TOKEN

# Ou definir
export GITHUB_TOKEN="seu-token-aqui"
docker-compose up -d hub
```

---

## 📊 Message Flow Diagram

```
Hero Browser
    │
    ├─ [🥋 Choose Kata]
    │    │
    │    └─ JS: KataThemeRequestMessage(LIST_THEMES)
    │         │
    │         ├─→ RabbitMQ: kata.requests
    │         │
    │         ├─→ Hub Consumer (KataMQConsumer)
    │         │    │
    │         │    └─→ RabbitMQ: kata.responses
    │         │
    │         └─→ Hero Consumer (DojoMQConsumer)
    │              │
    │              └─→ SSE: THEMES_LOADED
    │
    ├─ [Select: Virtual Threads]
    │    │
    │    └─ JS: KataThemeRequestMessage(CHOOSE_THEME)
    │         │
    │         └─→ [Mesmo fluxo]
    │              └─→ SSE: KATA_DELIVERED
    │
    ├─ [✅ Accept]
    │    │
    │    └─ Instr: Clone template, implement, mvn verify
    │
    ├─ [git push]
    │    │
    │    └─ GitHub: Create PR
    │
    └─ [Submit to Hub]
         │
         └─ JS: KataSubmissionMessage
              │
              ├─→ RabbitMQ: kata.submission
              │
              ├─→ Hub Consumer (KataSubmissionConsumer)
              │    │
              │    ├─ Clone código
              │    ├─ Validar
              │    └─→ RabbitMQ: kata.validation.results
              │
              └─→ Hero Consumer (DojoMQConsumer)
                   │
                   └─→ SSE: KATA_VALIDATED
```

---

## ✅ Success Criteria

Fluxo completo funciona quando:

- [ ] RabbitMQ sobe sem erros
- [ ] Queues estão criadas
- [ ] Hero recebe temas
- [ ] Hero consegue escolher tema
- [ ] Hero recebe kata entregue
- [ ] Hero consegue submeter
- [ ] Hub valida sem erros
- [ ] Hero recebe resultado de validação
- [ ] Frontend exibe sucesso com SSE

---

## 📚 Próximos Passos

1. Criar KATA-001 (Virtual Threads) real
2. Popularf THEMES.json com 5+ temas
3. Implementar KataService (lógica de atribuição)
4. Implementar validação local (mvn plugin)
5. Integrar com GitHub API para validação

