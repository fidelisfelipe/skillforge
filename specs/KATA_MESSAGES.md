---
ai-context: "reference"
ai-audience: ["architects", "senior-developers", "AI-tools"]
ai-scope: "Message contracts para comunicação Hero ↔ Hub via RabbitMQ"
---

# 📨 Kata Message Contracts

> Definições de Records para fluxo de Kata via message queue (RabbitMQ/AMQP)

---

## 🔄 Message Flow

```
Hero Browser
    ↓
Hero Frontend (Thymeleaf)
    ↓ publishes
RabbitMQ: kata.requests
    ↓
Hub Consumer (KataMQConsumer)
    ↓ processes
    ↓ publishes
RabbitMQ: kata.responses + kata.validation.results
    ↓ subscribes
Hero Backend (DojoMQConsumer)
    ↓ updates cache
    ↓ sends SSE
Hero Browser (updated UI)
```

---

## 📋 Message Definitions (Java Records)

### **1. KataThemeRequestMessage**

Request: Hero pede temas ou escolhe um

```java
package com.skillforge.dojo.message;

public record KataThemeRequestMessage(
    String heroId,
    String requestType,      // LIST_THEMES, CHOOSE_THEME, NEXT_KATA, REQUEST_DIFFERENT
    String themeId,          // null if LIST_THEMES
    long timestamp
) {
    
    // Validation
    public KataThemeRequestMessage {
        if (heroId == null || heroId.isBlank()) {
            throw new IllegalArgumentException("heroId required");
        }
        if (requestType == null) {
            throw new IllegalArgumentException("requestType required");
        }
        if (timestamp <= 0) {
            throw new IllegalArgumentException("timestamp must be positive");
        }
    }
}
```

### **2. KataThemeResponseMessage**

Response: Hub envia temas ou kata

```java
package com.skillforge.dojo.message;

import java.util.List;

public record KataThemeResponseMessage(
    String heroId,
    List<KataTheme> themes,
    KataDelivery currentKata,
    String status,           // THEMES_LISTED, KATA_DELIVERED, ERROR
    String errorMessage,     // null if success
    long timestamp
) {
    
    public record KataTheme(
        String id,
        String name,
        String description,
        String difficulty,          // beginner, intermediate, advanced
        String certReference,       // e.g., "Oracle Java 21 - Module 7"
        List<String> kataIds
    ) {}
    
    public record KataDelivery(
        String kataId,
        String themeId,
        String title,
        String spec,                // Problem statement (markdown)
        String solutionTemplatePath,// Where to clone template
        int xpReward,
        String difficulty,
        long timestamp
    ) {}
}
```

### **3. KataSubmissionMessage**

Request: Hero submete kata para validação

```java
package com.skillforge.dojo.message;

public record KataSubmissionMessage(
    String heroId,
    String kataId,
    String gitBranch,       // e.g., "feat/kata-001-virtual-threads"
    String commitSha,       // Latest commit
    long timestamp
) {
    
    public KataSubmissionMessage {
        if (heroId == null || heroId.isBlank()) {
            throw new IllegalArgumentException("heroId required");
        }
        if (kataId == null || kataId.isBlank()) {
            throw new IllegalArgumentException("kataId required");
        }
        if (gitBranch == null || gitBranch.isBlank()) {
            throw new IllegalArgumentException("gitBranch required");
        }
        if (commitSha == null || commitSha.isBlank()) {
            throw new IllegalArgumentException("commitSha required");
        }
    }
}
```

### **4. KataValidationResultMessage**

Response: Hub envia resultado de validação

```java
package com.skillforge.dojo.message;

import java.util.List;

public record KataValidationResultMessage(
    String heroId,
    String kataId,
    boolean passed,
    int score,              // 0-100
    int xpEarned,
    String skill,           // e.g., "java-21-virtual-threads" (null if failed)
    List<String> feedback,  // Error messages or success notes
    long timestamp
) {
    
    public KataValidationResultMessage {
        if (heroId == null || heroId.isBlank()) {
            throw new IllegalArgumentException("heroId required");
        }
        if (kataId == null || kataId.isBlank()) {
            throw new IllegalArgumentException("kataId required");
        }
        if (score < 0 || score > 100) {
            throw new IllegalArgumentException("score must be 0-100");
        }
        if (passed && (skill == null || skill.isBlank())) {
            throw new IllegalArgumentException("skill required if passed");
        }
    }
}
```

### **5. SkillValidatedEvent** (SSE Broadcast)

Broadcast: Hub notifica guilda inteira

```java
package com.skillforge.dojo.message;

public record SkillValidatedEvent(
    String heroId,
    String kataId,
    String skill,
    int xpEarned,
    long timestamp
) {
    
    public SkillValidatedEvent {
        if (heroId == null || heroId.isBlank()) {
            throw new IllegalArgumentException("heroId required");
        }
        if (skill == null || skill.isBlank()) {
            throw new IllegalArgumentException("skill required");
        }
    }
}
```

---

## 🔌 RabbitMQ Queue Configuration

### Queues

```
kata.requests              → Hub consome (hero publica)
kata.responses             → Hero consome (hub publica)
kata.submission            → Hub consome (hero submete kata)
kata.validation.results    → Hero consome (hub publica resultado)
```

### Topic Exchanges (SSE Broadcast)

```
skill.events (topic exchange)
  └─ routing-key: skill.validated.{heroId}
     → Todos os heróis interessados recebem
```

---

## 📝 Sequence Diagram

```
Hero                          Hub
  │                           │
  ├─ Publica                  │
  │  KataThemeRequestMessage  │
  │  (LIST_THEMES)            │
  │─────────────────────────→ │
  │                           │
  │                    Consome│
  │                    Processa
  │                    Publica │
  │  ← KataThemeResponseMessage
  │    (THEMES_LISTED)
  │
  ├─ Seleciona tema           │
  │  Publica                  │
  │  KataThemeRequestMessage  │
  │  (CHOOSE_THEME)           │
  │─────────────────────────→ │
  │                           │
  │                    Consome│
  │                    Atribui kata
  │                    Publica │
  │  ← KataThemeResponseMessage
  │    (KATA_DELIVERED)
  │
  ├─ Implementa código        │
  │  Roda: mvn verify         │
  │  Commits: git push        │
  │                           │
  │  Publica                  │
  │  KataSubmissionMessage    │
  │─────────────────────────→ │
  │                           │
  │                    Consome│
  │                    Clone código
  │                    Valida │
  │                    Publica │
  │  ← KataValidationResultMessage
  │    (passed: true)
  │
  │                    Broadcast:
  │  ← SkillValidatedEvent
  │    (SSE para toda guilda)
  │
  └─ Recebe notificação      │
     "Você validou skill!"
```

---

## 🧪 Exemplo: Serialize/Deserialize

```java
// Jackson configuration (Spring Boot auto)
@Configuration
public class KataMessageConfig {
    
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .setDefaultPropertyInclusion(JsonInclude.Include.NON_NULL);
    }
}

// Uso
KataThemeRequestMessage msg = new KataThemeRequestMessage(
    "alice",
    "LIST_THEMES",
    null,
    System.currentTimeMillis()
);

String json = objectMapper.writeValueAsString(msg);
KataThemeRequestMessage restored = objectMapper.readValue(
    json,
    KataThemeRequestMessage.class
);
```

---

## 📚 Referências

- **RabbitMQ**: [www.rabbitmq.com](https://www.rabbitmq.com)
- **Spring AMQP**: [spring.io/projects/spring-amqp](https://spring.io/projects/spring-amqp)
- **Jackson Records**: [github.com/FasterXML/jackson-databind](https://github.com/FasterXML/jackson-databind)
