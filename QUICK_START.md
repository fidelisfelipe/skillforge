---
ai-context: "quickstart"
ai-audience: ["junior-developers", "AI-tools", "new-heroes"]
ai-scope: "5 minutos para entender o conceito de SkillForge"
---

# ⚡ Quick Start — 5 Minutos para Entender SkillForge

> Você tem 5 minutos? Vamos te colocar rápido no ritmo.

---

## 🎮 O Que É SkillForge?

SkillForge é uma **plataforma gamificada** onde developers formam uma "guilda" de heróis, cada um com suas skills (habilidades técnicas).

**O conceito em 3 frases:**
1. Você registra suas skills (Java, REST API, React, etc)
2. Guild Hub cruza quests (tarefas) com suas skills
3. Você resolve, ganha XP, avança de nível (Apprentice → Expert)

---

## 🧙 Role seu Herói (2 min)

### Opção A: Fazer Fork (Recomendado)
```bash
# 1. Clique em "Fork" no GitHub
# 2. Clone seu fork
git clone https://github.com/SEU-LOGIN/skillforge.git

# 3. Configure seu manifest
vi manifest.json
```

**Arquivo `manifest.json`:**
```json
{
  "heroId": "alice",
  "heroName": "Alice",
  "heroClass": "Mage",
  "skills": ["java", "rest-api", "system-design"],
  "endpoint": "http://localhost:8081",
  "model": "phi3:mini"
}
```

Pronto! Guild Hub descobre você automaticamente em 10 minutos.

### Opção B: Apenas Registrar (1 min)
Abra uma issue no GitHub com label `hero`:
```
Título: HERO-REGISTRATION — [Seu Nome]
Body: [Preencha template automático]
```

---

## ⚔️ Sua Primeira Quest (2 min)

### Criar SPARK (Micro-Quest)
Vá para [docs/onboarding/PRIMEIRO_PASSO.md](docs/onboarding/PRIMEIRO_PASSO.md) e siga.

**TL;DR:**
```
1. Abra issue com label "spark"
2. Título: SPARK-NNN — [O que você quer fazer]
3. Define Definition of Done (4 checkboxes)
4. Submit!
```

✅ **Pronto!** Sua quest está aberta. Alguém resolve, ganha 100 XP.

---

## 📊 Entender Progressão

```
XP 0        100        300        600
│           │          │          │
Apprentice──Journeyman──Expert──Master
    ↓           ↓           ↓        ↓
  SPARK      SPARK+     QUEST   LEGEND
```

- **SPARK** (100 XP): Quest pequena, <1 dia, sem domain profile
- **QUEST** (200-600 XP): Quest estruturada, com validação de domínio

---

## 🎯 Próximos Passos

- **Junior?** → [docs/onboarding/](docs/onboarding/) (criar primeira quest)
- **Senior?** → [docs/progression/](docs/progression/) (expandir guilda)
- **Lost?** → [INDEX.md](INDEX.md) (mapa completo)

---

## 🚀 Começar Agora

```bash
# 1. Fork o repo
# 2. Crie manifest.json
# 3. Abra primeira issue "spark"
# 4. Ganhe 100 XP

# Tempo total: 10 minutos
# Resultado: Você é herói da guilda 🧙
```

---

**Mais detalhes:** Leia [IND