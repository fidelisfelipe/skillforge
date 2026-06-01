---
ai-context: "guide"
ai-audience: ["senior-developers", "qa-engineers"]
ai-scope: "Plano de validação em 4 fases para Kata MVP Phase 5.0"
---

# ✅ Kata MVP Phase 5.0 — Validation Plan

> **Objetivo**: Validar implementação MVP antes de Phase 5.1
>
> **Escopo**: Compilação → Unit Tests → Integration Tests → E2E
>
> **Esforço Estimado**: ~4-6 horas

---

## 📋 4 Fases de Validação

```
Phase 1: Compilação     [1h]    ← START HERE
    ↓
Phase 2: Unit Tests     [1h]    ← Services, Consumers
    ↓
Phase 3: Integration    [2h]    ← Mock RabbitMQ
    ↓
Phase 4: End-to-End     [2h]    ← RabbitMQ Real
    ↓
✅ READY FOR 5.1
```

---

# **Phase 1: Compilação Maven** (1h)

## 1.1 Build Completo

```bash
cd ~/skillforge

# Limpar e compilar tudo
mvn clean install

# Esperado:
# [INFO] Building skillforge-shared 0.1.0-SNAPSHOT
# [INFO] BUILD SUCCESS ✅

# [INFO] Building guild-hub 0.1.0-SNAPSHOT
# [INFO] BUILD SUCCESS ✅

# [INFO] Building hero-template 0.1.0-SNAPSHOT
# [INFO] BUILD SUCCESS ✅
```

### ✅ Sucesso = Nenhum erro de compilação

### ❌ Falha = Verificar

```bash
# Detalhes do erro
mvn clean compile -X | grep ERROR

# Ou compilar módulo individual
cd hub && mvn clean install -DskipTests
cd ../heroes/hero-template && mvn clean install -DskipTests
```

## 1.2 Verificar Dependências

```bash
# Ver árvore de dependências
mvn dependency:tree

# Verificar se shared está correto
mvn dependency:tree | grep skillforge-shared

# Esperado:
# +- com.skillforge:skillforge-shared:jar:0.1.0-SNAPSHOT:compile
```

## 1.3 Verificar Package Imports

```bash
# Procurar por imports inválidos
grep -r "io.skillforge" hub/src/main/java/
grep -r "io.skillforge" heroes/hero-template/src/main/java/

# Esperado: NENHUMA saída — todos os packages devem ser com.skillforge.*
```

---

# **Phase 2: Unit Tests** (1h)

## 2.1 Testes de Message Records

### Criar: `shared/src/test/java/com/skillforge/dojo/message/KataMessageTests.java`

```java
package com.skillforge.dojo.message;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class KataMessageTests {

    @Test
    void testKataThemeRequestMessage_Valid() {
        KataThemeRequestMessage msg = new KataThemeRequestMessage(
            "alice",
            "LIST_THEMES",
            null,
            System.currentTimeMillis()
        );
        
        assertEquals("alice", msg.heroId());
        assertEquals("LIST_THEMES", msg.requestType());
    }

    @Test
    void testKataThemeRequestMessage_InvalidHeroId() {
        assertThrows(IllegalArgumentException.class, () ->
            new KataThemeRequestMessage(
                "",  // ← Invalid
                "LIST_THEMES",
                null,
                System.currentTimeMillis()
            )
        );
    }

    @Test
    void testKataValidationResultMessage_InvalidScore() {
        assertThrows(IllegalArgumentException.class, () ->
            new KataValidationResultMessage(
                "alice",
                "KATA-001",
                false,
                101,  // ← Invalid (> 100)
                0,
                null,
                "Failed",
                System.currentTimeMillis()
            )
        );
    }

    @Test
    void testKataValidationResultMessage_PassedRequiresSkill() {
        assertThrows(IllegalArgumentException.class, () ->
            new KataValidationResultMessage(
                "alice",
                "KATA-001",
                true,  // ← Passed
                95,
                100,
                null,  // ← Missing skill
                "OK",
                System.currentTimeMillis()
            )
        );
    }
}
```

### Executar

```bash
cd shared && mvn test -Dtest=KataMessageTests

# Esperado:
# Tests run: 4, Failures: 0, Errors: 0 ✅
```

## 2.2 Testes de Services

### Criar: `hub/src/test/java/com/skillforge/hub/dojo/KataServiceTests.java`

```java
package com.skillforge.hub.dojo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class KataServiceTests {

    @Autowired
    private KataService kataService;

    @Test
    void testGetAvailableThemes() {
        var themes = kataService.getAvailableThemes("alice");
        
        assertNotNull(themes);
        assertFalse(themes.isEmpty());
        assertEquals(3, themes.size());  // Mock data: 3 temas
    }

    @Test
    void testThemeExists() {
        assertTrue(kataService.themeExists("virtual-threads"));
        assertFalse(kataService.themeExists("nonexistent"));
    }

    @Test
    void testAssignKata() {
        var kata = kataService.assignKata("alice", "virtual-threads");
        
        assertNotNull(kata);
        assertEquals("KATA-001A", kata.kataId());
        assertEquals("virtual-threads", kata.themeId());
    }
}
```

### Executar

```bash
cd hub && mvn test -Dtest=KataServiceTests

# Esperado:
# Tests run: 3, Failures: 0, Errors: 0 ✅
```

## 2.3 Testes de Consumers (Mock RabbitMQ)

### Criar: `hub/src/test/java/com/skillforge/hub/dojo/KataMQConsumerTests.java`

```java
package com.skillforge.hub.dojo;

import com.skillforge.dojo.message.KataThemeRequestMessage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import static org.mockito.Mockito.*;

@SpringBootTest
class KataMQConsumerTests {

    @Autowired
    private KataMQConsumer consumer;

    @MockBean
    private RabbitTemplate rabbitTemplate;

    @Test
    void testHandleListThemesRequest() {
        KataThemeRequestMessage msg = new KataThemeRequestMessage(
            "alice",
            "LIST_THEMES",
            null,
            System.currentTimeMillis()
        );

        consumer.handleKataRequest(msg);

        // Verificar que respondeu
        verify(rabbitTemplate).convertAndSend(
            eq("kata.responses"),
            any()
        );
    }

    @Test
    void testHandleChooseThemeRequest() {
        KataThemeRequestMessage msg = new KataThemeRequestMessage(
            "alice",
            "CHOOSE_THEME",
            "virtual-threads",
            System.currentTimeMillis()
        );

        consumer.handleKataRequest(msg);

        verify(rabbitTemplate).convertAndSend(
            eq("kata.responses"),
            any()
        );
    }
}
```

### Executar

```bash
cd hub && mvn test -Dtest=KataMQConsumerTests

# Esperado:
# Tests run: 2, Failures: 0, Errors: 0 ✅
```

---

# **Phase 3: Integration Tests** (2h)

## 3.1 Setup: Testcontainers com RabbitMQ

### Adicionar ao `pom.xml` (todos os módulos)

```xml
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>testcontainers</artifactId>
    <version>1.19.8</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>rabbitmq</artifactId>
    <version>1.19.8</version>
    <scope>test</scope>
</dependency>
```

## 3.2 Criar: `hub/src/test/java/com/skillforge/hub/dojo/KataFlowIntegrationTest.java`

```java
package com.skillforge.hub.dojo;

import com.skillforge.dojo.message.*;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import java.util.concurrent.TimeUnit;

@SpringBootTest
@Testcontainers
class KataFlowIntegrationTest {

    @Container
    static RabbitMQContainer rabbitmq = new RabbitMQContainer("rabbitmq:3.13-management");

    @DynamicPropertySource
    static void rabbitmqProperties(org.springframework.test.context.DynamicPropertyRegistry registry) {
        registry.add("spring.rabbitmq.host", rabbitmq::getHost);
        registry.add("spring.rabbitmq.port", rabbitmq::getAmqpPort);
    }

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private KataMQConsumer consumer;

    @Test
    void testListThemesFlow() throws InterruptedException {
        // 1. Hero publica pedido
        KataThemeRequestMessage request = new KataThemeRequestMessage(
            "alice",
            "LIST_THEMES",
            null,
            System.currentTimeMillis()
        );

        rabbitTemplate.convertAndSend("kata.requests", request);

        // 2. Hub consome e responde
        // (simulado via consumer.handleKataRequest)
        consumer.handleKataRequest(request);

        // 3. Verificar resposta (em queue kata.responses)
        KataThemeResponseMessage response = (KataThemeResponseMessage)
            rabbitTemplate.receiveAndConvert("kata.responses", 5, TimeUnit.SECONDS);

        assertNotNull(response);
        assertEquals("alice", response.heroId());
        assertEquals("THEMES_LISTED", response.status());
    }
}
```

### Executar

```bash
cd hub && mvn test -Dtest=KataFlowIntegrationTest

# Esperado:
# Tests run: 1, Failures: 0, Errors: 0 ✅
# (pode demorar alguns segundos - Testcontainers inicia RabbitMQ)
```

---

# **Phase 4: End-to-End Test** (2h)

## 4.1 Setup RabbitMQ Real

```bash
# Verifique que RabbitMQ centralizado está rodando
ping rabbitmq.internal.company.com

# Ou inicie localmente
docker run -d --name rabbitmq \
  -p 5672:5672 \
  -p 15672:15672 \
  rabbitmq:3.13-management

# Management UI
open http://localhost:15672
# default: guest/guest
```

## 4.2 Criar Queues

```bash
# Via Management UI:
# Admin → Queues → Add Queue
# Names: kata.requests, kata.responses, kata.submission, kata.validation.results

# Ou via CLI:
docker exec rabbitmq rabbitmqctl declare_queue kata.requests
docker exec rabbitmq rabbitmqctl declare_queue kata.responses
docker exec rabbitmq rabbitmqctl declare_queue kata.submission
docker exec rabbitmq rabbitmqctl declare_queue kata.validation.results
```

## 4.3 Rodar Aplicações

### Terminal 1: Hub

```bash
cd ~/skillforge/hub

export RABBITMQ_HOST=localhost
export RABBITMQ_PORT=5672
export RABBITMQ_USERNAME=guest
export RABBITMQ_PASSWORD=guest
export RABBITMQ_VHOST=/

mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=kata"

# Esperado:
# [INFO] o.s.b.w.e.t.TomcatWebServer - Tomcat started on port 8080
```

### Terminal 2: Hero-Template

```bash
cd ~/skillforge/heroes/hero-template

export RABBITMQ_HOST=localhost
export RABBITMQ_PORT=5672
export RABBITMQ_USERNAME=guest
export RABBITMQ_PASSWORD=guest
export RABBITMQ_VHOST=/
export HERO_ID=alice
export HERO_NAME="Alice"

mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=kata --server.port=8081"

# Esperado:
# [INFO] o.s.b.w.e.t.TomcatWebServer - Tomcat started on port 8081
```

### Terminal 3: Simular Hero (usando curl ou script)

```bash
# Criar arquivo: test-kata.sh

#!/bin/bash

# 1. Hero pede temas
curl -X POST http://localhost:8081/dojo/request-themes \
  -H "Content-Type: application/json" \
  -d '{"heroId": "alice", "requestType": "LIST_THEMES"}'

sleep 2

# Verificar em RabbitMQ UI: kata.responses deve ter mensagem

# 2. Hero escolhe tema
curl -X POST http://localhost:8081/dojo/choose-theme \
  -H "Content-Type: application/json" \
  -d '{"heroId": "alice", "themeId": "virtual-threads"}'

sleep 2

# Verificar em RabbitMQ UI: kata.responses deve ter new message

# 3. Verificar logs
echo "✅ Check Hub logs for:"
echo "   🎯 Processing kata request"
echo "   📋 Listed 3 themes"
echo "   🎯 Assigned kata"

echo "✅ Check Hero logs for:"
echo "   📨 Received kata response"
echo "   💾 Cached current kata"
echo "   ✅ Themes list sent to frontend"
```

### Executar

```bash
bash test-kata.sh
```

## 4.4 Verificações Manual

### Hub Logs

```
🎯 Processing kata request from hero: alice | Type: LIST_THEMES
📋 Listed 3 themes for hero: alice
📤 Response published for hero: alice

🎯 Processing kata request from hero: alice | Type: CHOOSE_THEME
🎯 Assigned kata KATA-001A from theme virtual-threads to hero: alice
```

### Hero Logs

```
📨 Received kata response | Status: THEMES_LISTED
💾 Cached 3 available themes
✅ Themes list sent to frontend

📨 Received kata response | Status: KATA_DELIVERED
💾 Cached current kata: KATA-001A
✅ Kata delivered: KATA-001A
```

### RabbitMQ Management UI

```
Queue: kata.requests
  Messages: 0 (consumidas ✅)

Queue: kata.responses
  Messages: 2 (respostas ✅)

Queue: kata.submission
  Messages: 0

Queue: kata.validation.results
  Messages: 0
```

---

# ✅ Sucesso Criteria

| Fase | Critério | Status |
|------|----------|--------|
| **Compilação** | `mvn clean install` sem erros | ✅ |
| **Unit Tests** | Message + Service + Consumer tests passam | ✅ |
| **Integration** | Testcontainers RabbitMQ com fluxo completo | ✅ |
| **E2E** | Hub ↔ Hero comunicam via RabbitMQ real | ✅ |

---

# 🚀 Se Tudo Passar

```bash
# Commit e push
git add .
git commit -m "feat: Kata MVP Phase 5.0 validated"
git push origin feature/kata-mvp-phase-5

# Status
✅ Compilação OK
✅ Unit Tests OK
✅ Integration Tests OK
✅ E2E OK

🎉 READY FOR PHASE 5.1
```

---

# 📚 Referências

- **KATA_MVP_SETUP.md** — Setup manual
- **run-kata-mvp.sh** — Script de bootstrap
- **RabbitMQ Docs** — https://www.rabbitmq.com/
- **Spring AMQP** — https://spring.io/projects/spring-amqp

---

**Gerado**: 2026-06-01  
**Versão**: Validation Plan Phase 5.0
