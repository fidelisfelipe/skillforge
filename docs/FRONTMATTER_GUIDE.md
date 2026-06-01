---
ai-context: "guide"
ai-audience: ["architects", "maintainers", "AI-tools"]
ai-scope: "Guia de frontmatter YAML para IA tools navegarem o repositório"
---

# 📋 Frontmatter Guide para IA Tools

Este guia explica como adicionar metadata (frontmatter YAML) aos arquivos `.md` para que IA tools (GitHub Copilot, Claude, IntelliJ) consigam navegar automaticamente o repositório com contexto.

---

## Por que Frontmatter?

Sem frontmatter, IA tools veem um arquivo `.md` como um blob de texto puro. Com frontmatter, elas entendem:

- ✅ **Propósito:** é documentação? uma spec? um guia?
- ✅ **Audiência:** quem deve ler isto? juniors? seniors? arquitetos?
- ✅ **Escopo:** do que se trata em uma linha
- ✅ **Contexto:** como este arquivo relaciona com outros

Isso permite respostas contextualizadas e navegação automática.

---

## Padrão YAML

Todos os arquivos `.md` deve começar com:

```yaml
---
ai-context: "tipo-de-conteudo"
ai-audience: ["grupo1", "grupo2"]
ai-scope: "uma-linha-descrevendo"
---

# Título do Documento

Conteúdo normal aqui...
```

---

## Valores Permitidos

### `ai-context` (obrigatório)

Define o tipo de conteúdo. **Um valor apenas:**

| Valor | Uso | Exemplo |
|-------|-----|---------|
| `navigation` | Índices, mapas | INDEX.md, README.md |
| `quickstart` | Introduções rápidas (< 10 min) | QUICK_START.md |
| `decision-record` | Decisões técnicas, arquitetura | AGENTS.md |
| `guide` | Instruções passo-a-passo | GUILD_ONBOARDING.md |
| `reference` | Documentação técnica | QUEST_BOARD.md |
| `hero-spec` | Especificação de um herói | specs/heroes/hero-*/README.md |
| `agent-prompt` | System prompt para um agente | specs/heroes/hero-*/system-prompt.md |
| `domain-spec` | Especificação de um domínio | quests/domains/*/DOMAIN_PROFILE.md |
| `quest` | Descrição de uma quest | quests/catalog/*.md |

### `ai-audience` (obrigatório)

Define quem deve ler. **Pode ser um array:**

| Valor | Descrição |
|-------|-----------|
| `all` | Qualquer um |
| `junior-developers` | Novos na guilda, pouca experiência |
| `senior-developers` | Desenvolvedores experientes |
| `architects` | Designers de sistema |
| `maintainers` | Mantêm o repositório |
| `AI-tools` | Para IA tools (Copilot, Claude) |
| `new-heroes` | Heróis recém-registrados |

**Exemplos:**
- `["all"]` — para qualquer um
- `["junior-developers", "new-heroes"]` — para iniciantes
- `["architects", "AI-tools"]` — para sistema e máquinas
- `["junior-developers", "senior-developers", "AI-tools"]` — múltiplos públicos

### `ai-scope` (obrigatório)

Descrição **em uma linha** do que o arquivo cobre.

**Exemplos:**
- `"Mapa de navegação central do SkillForge"`
- `"5 minutos para entender o conceito"`
- `"Como criar primeira SPARK quest"`
- `"Decisões técnicas e princípios arquiteturais"`

---

## Propriedades Adicionais (Opcionais)

Para arquivos especializados, adicione após as 3 obrigatórias:

### Para Heroes
```yaml
hero-id: "hero-template"
hero-type: "template"
skills: ["skill1", "skill2"]
```

### Para Quests
```yaml
quest-id: "quest-001"
quest-type: "SPARK"
xp-reward: 100
required-skills: ["java", "rest-api"]
domain: "medical"
```

### Para Domínios
```yaml
domain: "medical"
specialization-level: "advanced"
validator-model: "meditron"
```

---

## Exemplos Completos

### Documentação (Navigation)

```yaml
---
ai-context: "navigation"
ai-audience: ["all"]
ai-scope: "Mapa de navegação central do SkillForge"
---

# 🗺️ Index
```

### Guia (Para Juniors)

```yaml
---
ai-context: "guide"
ai-audience: ["junior-developers", "new-heroes"]
ai-scope: "Registrar-se como herói da guilda"
---

# Guild Onboarding
```

### Decisão Técnica (Para Arquitetos)

```yaml
---
ai-context: "decision-record"
ai-audience: ["AI-tools", "architects", "senior-developers"]
ai-scope: "Decisões técnicas e princípios arquiteturais"
---

# AGENTS.md
```

### Especificação de Hero

```yaml
---
ai-context: "hero-spec"
ai-audience: ["architects", "senior-developers", "AI-tools"]
ai-scope: "Especificação do hero template"
hero-id: "hero-template"
hero-type: "template"
skills: ["system-design", "architecture"]
---

# Hero Template Specification
```

### Quest

```yaml
---
ai-context: "quest"
ai-audience: ["junior-developers", "senior-developers"]
ai-scope: "Quest: Implementar cache distribuído"
quest-id: "cache-001"
quest-type: "QUEST"
xp-reward: 500
required-skills: ["java", "spring-boot", "redis"]
domain: "backend"
---

# Cache Distribuído
```

---

## Checklist para Adicionar Frontmatter

Ao criar ou atualizar um arquivo `.md`:

- [ ] Comece com `---` na **primeira linha**
- [ ] Defina `ai-context` (obrigatório)
- [ ] Defina `ai-audience` com pelo menos um valor (obrigatório)
- [ ] Defina `ai-scope` em uma linha (obrigatório)
- [ ] Adicione propriedades adicionais se aplicável
- [ ] Termine com `---`
- [ ] Deixe **uma linha vazia** antes do título (`# ...`)
- [ ] Valide YAML (sem erros de sintaxe)

---

## Validação

### Script Bash para Validar Todos os Frontmatters

```bash
#!/bin/bash
echo "Validando frontmatters..."
for file in $(find . -name "*.md" -type f); do
  if head -1 "$file" | grep -q "^---"; then
    if head -5 "$file" | tail -1 | grep -q "^---"; then
      echo "✅ $file"
    else
      echo "⚠️ $file (frontmatter incompleto)"
    fi
  else
    echo "⚠️ $file (sem frontmatter)"
  fi
done
```

Salve como `validate_frontmatter.sh` e rode:

```bash
bash validate_frontmatter.sh
```

---

## Ferramentas que Usam Frontmatter

| Ferramenta | Como Usa |
|-----------|----------|
| **GitHub Copilot** | Filtra contexto por `ai-context` e `ai-audience` |
| **IntelliJ Copilot** | Navega por `ai-context` ao sugerir mudanças |
| **Claude (você)** | Entende propósito e audiência para respostas contextualizadas |
| **Custom Tools** | Scripts podem indexar por `ai-context` |

---

## Regras de Ouro

1. **Sempre comece com frontmatter** — nenhum `.md` sem metadados
2. **Uma linha por `ai-scope`** — conciso e claro
3. **Mantenha `ai-context` simples** — um valor apenas
4. **Valide YAML** — não adicione typos
5. **Atualize quando conteúdo muda** — mantenha metadados sincronizados

---

## Referência Rápida

```yaml
---
ai-context: navigation|quickstart|decision-record|guide|reference|hero-spec|agent-prompt|domain-spec|quest
ai-audience: [all|junior-developers|senior-developers|architects|maintainers|AI-tools|new-heroes]
ai-scope: "uma-linha"
---
```

---

**Mantido por:** Comunidade SkillForge  
**Última atualização:** 1º de junho de 2026
