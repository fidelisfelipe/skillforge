---
ai-context: "decision-record"
ai-audience: ["architects", "senior-developers"]
ai-scope: "Arquitetura do módulo compartilhado para Message Contracts do Kata MVP"
---

# 📦 Shared Module — Kata MVP Architecture

> **Status**: ✅ **IMPLEMENTED** — Módulo compartilhado criado para evitar acoplamento
>
> **Data**: 2026-06-01  
> **Resolução**: Hero não depende mais do Hub via imports

---

## 🎯 O Problema (Resolvido)

### ❌ ANTES

```
hero-template/
  └─ DojoMQConsumer.java
      └─ import com.skillforge.hub.dojo.message.*

❌ Acoplamento: Hero importa do Hub!
```

### ✅ DEPOIS

```
shared/
  └─ src/main/java/com/skillforge/dojo/message/
      ├─ KataThemeRequestMessage.java
      ├─ KataThemeResponseMessage.java
      ├─ KataSubmissionMessage.java
      ├─ KataValidationResultMessage.java
      └─ SkillValidatedEvent.java

hub/ → depende de shared
hero-template/ → depende de shared

✅ Sem acoplamento direto!
```

---

## 📦 Estrutura Criada

### **shared/pom.xml**

```xml
<project>
    <artifactId>skillforge-shared</artifactId>
    <description>Shared message contracts and domain models</description>
    
    <dependencies>
        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-databind</artifactId>
        </dependency>
    </dependencies>
</project>
```

**Apenas Jackson** (para serialização RabbitMQ), sem Spring Boot, sem Lombok.

### **Message Records** (5 arquivos)

```
shared/src/main/java/com/skillforge/dojo/message/

✅ KataThemeRequestMessage.java
✅ KataThemeResponseMessage.java
✅ KataSubmissionMessage.java
✅ KataValidationResultMessage.java
✅ SkillValidatedEvent.java
```

**Package**: `com.skillforge.dojo.message.*` (neutro, compartilhado)

---

## 🔄 Dependências

### Parent `pom.xml` (Atualizado)

```xml
<modules>
    <module>shared</module>          ← Agora primeiro
    <module>hub</module>
    <module>heroes/hero-template</module>
    <!-- ... -->
</modules>
```

### Hub `pom.xml` (Adicionado)

```xml
<dependency>
    <groupId>com.skillforge</groupId>
    <artifactId>skillforge-shared</artifactId>
    <version>${project.version}</version>
</dependency>
```

### Hero-Template `pom.xml` (Adicionado)

```xml
<dependency>
    <groupId>com.skillforge</groupId>
    <artifactId>skillforge-shared</artifactId>
    <version>${project.version}</version>
</dependency>
```

---

## 📝 Imports Atualizados

### **KataMQConsumer.java** (Hub)

```java
// ✅ CORRETO
import com.skillforge.dojo.message.*;

// ❌ NÃO MAIS
// import com.skillforge.hub.dojo.message.*;
```

### **DojoMQConsumer.java** (Hero-Template)

```java
// ✅ CORRETO
import com.skillforge.dojo.message.*;

// ❌ NÃO MAIS
// import com.skillforge.hub.dojo.message.*;
```

### **DojoCacheService.java** (Hero-Template)

```java
// ✅ CORRETO
import com.skillforge.dojo.message.KataThemeResponseMessage.KataTheme;
import com.skillforge.dojo.message.KataThemeResponseMessage.KataDelivery;
```

### **KataService.java** (Hub)

```java
// ✅ CORRETO
import com.skillforge.dojo.message.KataThemeResponseMessage.KataTheme;
import com.skillforge.dojo.message.KataThemeResponseMessage.KataDelivery;
```

---

## 🏗️ Topologia Final

```
parent/pom.xml
├── shared/pom.xml (sem dependências Spring)
│   ├── com.skillforge.dojo.message.*
│   └── KataThemeRequestMessage.java (5 records)
│
├── hub/pom.xml (depende de shared)
│   ├── com.skillforge.hub.dojo.*
│   ├── KataService.java
│   ├── *ServiceStub.java
│   └── KataMQConsumer.java (import shared)
│
├── heroes/hero-template/pom.xml (depende de shared)
│   ├── com.skillforge.template.dojo.*
│   ├── DojoCacheService.java
│   ├── KataLocalService.java
│   └── DojoMQConsumer.java (import shared)
│
└── [outros módulos]
```

---

## ✅ Checklist de Conformidade

```
MÓDULO COMPARTILHADO
[✅] shared/pom.xml criado
[✅] shared/ adicionado ao parent <modules>
[✅] Apenas Jackson como dependência

MESSAGE RECORDS
[✅] 5 records em com.skillforge.dojo.message.*
[✅] Validação em compact constructors
[✅] Documentação de queues RabbitMQ

HUB
[✅] Depende de skillforge-shared
[✅] KataMQConsumer imports corretos
[✅] KataService imports corretos
[✅] GitHubServiceStub imports corretos

HERO-TEMPLATE
[✅] Depende de skillforge-shared
[✅] DojoMQConsumer imports corretos
[✅] DojoCacheService imports corretos
[✅] KataLocalService imports corretos

DEPENDÊNCIAS
[✅] Hero NÃO depende de Hub
[✅] Ambos dependem de shared (neutro)
[✅] Sem acoplamento circular
```

---

## 🚀 Build

Agora é seguro compilar:

```bash
# Build completo
mvn clean install

# Resultado esperado
[INFO] Building skillforge-shared 0.1.0-SNAPSHOT
[INFO] Building guild-hub 0.1.0-SNAPSHOT
[INFO] Building hero-template 0.1.0-SNAPSHOT
[INFO] BUILD SUCCESS ✅
```

---

## 🎯 Benefícios da Arquitetura

| Aspecto | Benefício |
|---------|-----------|
| **Acoplamento** | ✅ Reduzido: Hero não importa do Hub |
| **Reutilização** | ✅ Messages compartilhadas entre hub e hero |
| **Manutenção** | ✅ Mudanças em records afetam só shared |
| **Testabilidade** | ✅ Hero pode ser testado sem Hub |
| **Escalabilidade** | ✅ Novos heróis podem depender de shared |

---

## 📚 Referências

- **KATA_MVP_BUILD_EVALUATION.md** — Avaliação técnica
- **KATA_MVP_COMPILATION_FIXES.md** — Correções de compilação
- **KATA_MVP_PACKAGE_CORRECTION.md** — Correção de packages
- **KATA_MESSAGES.md** — Specs de messages

---

**Gerado**: 2026-06-01  
**Versão**: Shared Module Implementation
