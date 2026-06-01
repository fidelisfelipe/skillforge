---
ai-context: "navigation"
ai-audience: ["all"]
ai-scope: "Mapa de navegação central do SkillForge para todos os públicos"
---

# 🗺️ SkillForge: Mapa de Navegação

> Bem-vindo à guilda! Este arquivo ajuda você a encontrar exatamente o que procura.

---

## 🎯 Eu sou um...

### 🟢 **Junior Developer** (Novo na guilda)
Você quer **criar sua primeira quest** ou **se registrar como herói**?

1. Leia [QUICK_START.md](QUICK_START.md) (5 minutos)
2. Vá para [docs/onboarding/PRIMEIRO_PASSO.md](docs/onboarding/PRIMEIRO_PASSO.md)
3. Abra uma issue GitHub com label `spark`
4. **Pronto!** Você criou uma quest. Ganhe 100 XP.

**Não precisa ler:** AGENTS.md, QUEST_FRAMEWORK, system-prompt.md

---

### 🔴 **Senior Developer** (Expandindo a guilda)
Você quer **criar quest complexa**, **novo domínio** ou **novo herói**?

1. Leia [AGENTS.md](docs/AGENTS.md) (decisões arquiteturais)
2. Vá para [docs/progression/QUEST_FRAMEWORK.md](docs/progression/QUEST_FRAMEWORK.md)
3. Se criar domínio novo, estude [docs/progression/DOMAIN_PROFILE_GUIDE.md](docs/progression/DOMAIN_PROFILE_GUIDE.md)
4. Especializa em `/quests/domains/{domain}/DOMAIN_PROFILE.md`
5. **Pronto!** Você expandiu SkillForge.

**Leia tudo**, aprofunde onde especializar.

---

### 🔵 **IA Tool** (GitHub Copilot, IntelliJ, Claude)
Você precisa **navegar o contexto sem poluição**?

1. Cada arquivo tem `ai-context` no frontmatter YAML
2. `/docs/` está organizado por audience
3. `/specs/heroes/*/system-prompt.md` tem instruções
4. `/quests/domains/*/DOMAIN_PROFILE.md` tem specs

**Entry point:** Procure arquivos com `ai-context: "decision-record"` ou seu contexto específico.

---

## 📁 Estrutura Rápida

```
skillforge/
│
├── 📖 INÍCIO (leia primeiro)
│   ├── README.md                    ← Overview geral
│   ├── QUICK_START.md               ← 5 min kickoff
│   ├── INDEX.md                     ← Você está aqui
│   ├── CONTRIBUTING.md              ← Como contribuir
│   │
│   └── docs/                        ← Documentação organizada
│       ├── AGENTS.md                ← Decisões técnicas
│       ├── STACK.md                 ← Stack técnico
│       │
│       ├── onboarding/              ← Para juniors
│       │   ├── PRIMEIRO_PASSO.md
│       │   ├── GUILD_ONBOARDING.md
│       │   └── README.md
│       │
│       ├── progression/             ← Para seniors
│       │   ├── QUEST_FRAMEWORK.md
│       │   ├── DOMAIN_PROFILE_GUIDE.md
│       │   ├── MICRO_QUEST.md
│       │   └── README.md
│       │
│       └── reference/               ← Geral
│           ├── QUEST_BOARD.md
│           ├── SKILL_MANIFEST_GUIDE.md
│           ├── HERO_REGISTRATION.md
│           └── README.md
│
├── 🎯 specs/                       ← Specs de heróis (SEM código)
│   ├── README.md
│   └── heroes/
│       ├── quest-scribe/
│       ├── hero-reviewer/
│       └── hero-template/
│
├── 🚀 heroes/                       ← Implementação (COM código)
│   ├── README.md
│   ├── hero-template/
│   ├── quest-scribe/
│   └── [outros heróis]
│
├── 🏰 hub/                          ← Orquestrador central
│   ├── README.md
│   ├── pom.xml
│   └── src/
│
├── ⚔️ quests/                       ← Framework + catálogo
│   ├── README.md
│   ├── catalog/                     ← Quests implementadas
│   └── domains/                     ← Por domínio
│       ├── medical/
│       └── agency/
│
└── 🛠️ tools/                        ← Ferramentas auxiliares
    ├── tools/guild-quest/
    └── [scripts]
```

---

## 🔗 Links Úteis por Tarefa

### Criar Uma Quest
- Micro-Quest (rápido): [docs/onboarding/PRIMEIRO_PASSO.md](docs/onboarding/PRIMEIRO_PASSO.md)
- Quest Completa (estruturada): [docs/progression/QUEST_FRAMEWORK.md](docs/progression/QUEST_FRAMEWORK.md)
- Ver exemplos: [docs/reference/QUEST_BOARD.md](docs/reference/QUEST_BOARD.md)

### Registrar Como Herói
- Passo a passo: [docs/onboarding/GUILD_ONBOARDING.md](docs/onboarding/GUILD_ONBOARDING.md)
- Manifest schema: [docs/reference/SKILL_MANIFEST_GUIDE.md](docs/reference/SKILL_MANIFEST_GUIDE.md)

### Criar Novo Domínio
- Como fazer: [docs/progression/DOMAIN_PROFILE_GUIDE.md](docs/progression/DOMAIN_PROFILE_GUIDE.md)
- Exemplo (médico): [quests/domains/medical/DOMAIN_PROFILE.md](quests/domains/medical/DOMAIN_PROFILE.md)

### Implementar Novo Herói
- Template: [specs/heroes/hero-template/README.md](specs/heroes/hero-template/README.md)
- Padrões: [specs/README.md](specs/README.md)

### Entender Arquitetura
- Decisões técnicas: [docs/AGENTS.md](docs/AGENTS.md)
- Stack completo: [docs/STACK.md](docs/STACK.md)

### Contribuir
- Guia: [CONTRIBUTING.md](CONTRIBUTING.md)
- Manutenção contínua: [docs/MAINTENANCE.md](docs/MAINTENANCE.md)
- Frontmatter metadata: [docs/FRONTMATTER_GUIDE.md](docs/FRONTMATTER_GUIDE.md)
- Workflow: Pull Request → Code Review → Merge

---

## 📊 Estatísticas

| Item | Valor |
|------|-------|
| **Heróis registrados** | [veja `/specs/heroes/`] |
| **Quests abertas** | [veja `/quests/catalog/`] |
| **Domínios ativos** | 2 (medical, agency) |
| **XP total guilda** | [veja `/docs/reference/QUEST_BOARD.md`] |

---

## ❓ 