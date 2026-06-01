---
ai-context: "guide"
ai-audience: ["maintainers", "architects", "AI-tools"]
ai-scope: "Práticas contínuas para manter a organização e consistência pós-reorganização"
---

# 🔧 Manutenção Contínua — Fase 4

> Este documento codifica as práticas de manutenção necessárias para evitar degradação da estrutura após a reorganização Fases 1–3.
>
> **Quem lê?** Maintainers, arquitetos, ferramentas de IA (Copilot, Claude, IntelliJ) que gerenciam o repositório ao longo do tempo.

---

## 1. Monitoramento de Novos Arquivos

Sempre que um novo arquivo de **documentação** ou **especificação** é adicionado ao repositório, ele deve passar por checklist de conformidade **antes de ser mergeado**.

### Checklist para MDs em `/docs/`, `/specs/`, `/quests/`

```
[ ] Arquivo tem extensão .md?
[ ] Contém frontmatter YAML com ai-context, ai-audience, ai-scope?
[ ] ai-context é um valor válido? (navigation, quickstart, guide, reference, decision-record, hero-spec, quest)
[ ] ai-audience é um array com valores válidos? (all, junior-developers, senior-developers, architects, maintainers, AI-tools, new-heroes)
[ ] ai-scope é uma descrição concisa (<120 caracteres)?
[ ] Arquivo está no diretório correto?
  - Manuais de onboarding → /docs/onboarding/
  - Progressão e leveling → /docs/progression/
  - Catálogos e boards → /docs/reference/
  - Especificações de heróis → /specs/heroes/
  - Arquitetura e decisões → /docs/AGENTS.md (ou docs/reference/ARCHITECTURE.md se expandir)
[ ] Links internos usam paths relativos corretos?
[ ] Não há referências a "agents/" (apenas "specs/")?
```

### Validação Automática (Git Hook)

Adicionar um `pre-commit` hook que valida frontmatter em novos .md files:

```bash
#!/bin/bash
# .git/hooks/pre-commit (executável)

YAML_PATTERN="^---$"
REQUIRED_FIELDS=("ai-context" "ai-audience" "ai-scope")
MD_FILES=$(git diff --cached --name-only --diff-filter=A | grep '\.md$')

for file in $MD_FILES; do
  if [[ ! "$file" =~ ^(README|\.git) ]]; then
    # Verifica se é um dos arquivos de raiz (podem não ter frontmatter)
    if [[ ! "$file" =~ ^[^/]*\.md$ ]]; then
      # Arquivo em subdiretório — deve ter frontmatter
      head -1 "$file" | grep -q "$YAML_PATTERN" || {
        echo "❌ $file: missing frontmatter YAML"
        exit 1
      }
      
      for field in "${REQUIRED_FIELDS[@]}"; do
        grep -q "^$field:" "$file" || {
          echo "❌ $file: missing required field '$field'"
          exit 1
        }
      done
    fi
  fi
done

exit 0
```

**Para instalar:**
```bash
cd .git/hooks
curl https://raw.githubusercontent.com/SEU-REPO/skillforge/main/.git-hooks/pre-commit > pre-commit
chmod +x pre-commit
```

---

## 2. Mantendo INDEX.md Atualizado

Sempre que um novo documento de **navegação principal** é criado em `/docs/` ou `/specs/`, ele deve ser indexado em:

- **[INDEX.md](../INDEX.md)** — mapa completo, organizado por audience + context
- **[docs/README.md](./README.md)** — índice de docs apenas

### Padrão de Entrada em INDEX.md

```markdown
| 🔗 | [Título Descritivo](path/to/file.md) | Contexto | Público | Resumo de 1 linha |
```

**Exemplo:**
```markdown
| 🚀 | [Quest Scribe Hero Setup](specs/heroes/quest-scribe/SETUP.md) | hero-spec | architects, senior-developers | Como configurar o hero que desenha quests |
```

### Checklist para Manutenção de INDEX.md

```
[ ] Novo arquivo de nível 1 (em /docs/*, /specs/heroes/*, etc)?
[ ] INDEX.md atualizado com entrada na seção certa?
[ ] docs/README.md atualizado com link?
[ ] Link testa corretamente (sem 404)?
[ ] Emoji escolhido combina com contexto? (🗺️ nav, ⚡ quick, 📚 docs, 🚀 hero, 🏰 infra, 🎮 quest, 🔧 maint)
```

---

## 3. Validação de Compatibilidade com IA Tools

Quando ferramentas como Copilot, Claude ou IntelliJ Baseline lerem este repositório, elas precisam de:

1. **Frontmatter legível** — YAML válido, nenhum syntax error
2. **ai-context sem typos** — valores exatos (case-sensitive)
3. **Paths relativos consistentes** — nenhuma referência a `/agents/`
4. **Estrutura determinística** — mesmos diretórios sempre, sem renomeações ad-hoc

### Validação Manual (Rodar mensalmente)

```bash
#!/bin/bash
# scripts/validate-frontmatter.sh

echo "🔍 Validando frontmatter em .md files..."

ERRORS=0
for file in $(find docs specs quests -name "*.md" -type f); do
  # Extrai ai-context
  CONTEXT=$(grep "^ai-context:" "$file" | cut -d'"' -f2)
  VALID_CONTEXTS="navigation|quickstart|guide|reference|decision-record|hero-spec|quest|domain-spec|agent-prompt"
  
  if [[ ! "$CONTEXT" =~ ^($VALID_CONTEXTS)$ ]]; then
    echo "❌ $file: invalid ai-context '$CONTEXT'"
    ((ERRORS++))
  fi
  
  # Verifica se ai-audience é array
  if ! grep -q "^ai-audience: \[" "$file"; then
    echo "❌ $file: ai-audience must be array (use [ ])"
    ((ERRORS++))
  fi
  
  # Verifica se tem ai-scope
  if ! grep -q "^ai-scope:" "$file"; then
    echo "❌ $file: missing ai-scope"
    ((ERRORS++))
  fi
done

if [ $ERRORS -eq 0 ]; then
  echo "✅ Todos os frontmatters validados com sucesso"
  exit 0
else
  echo "❌ $ERRORS erro(s) encontrado(s)"
  exit 1
fi
```

**Executar:**
```bash
bash scripts/validate-frontmatter.sh
```

---

## 4. Governança de Renomeações e Reorganizações

Mudanças estruturais (pastas, nomes de arquivos, paths) afetam:
- Links em todos os demais arquivos
- Referencias em pom.xml (para módulos Maven)
- Git hooks e scripts automatizados
- Metadados de ferramentas (índices de Copilot, etc)

### Protocolo de Mudança Estrutural

**Antes de fazer:**

1. Abra uma **issue de Discussão** descrevendo:
   - O que muda e por quê
   - Quantos arquivos/referências são afetadas
   - Plano de atualização de links

2. Aguarde feedback de **3 arquitetos** (ou maintainers)

3. **Depois de aprovação:**
   - Crie um branch `feature/refactor-<descrição>`
   - Documente cada mudança em um script de migração (ex: `scripts/migrate-agents-to-specs.sh`)
   - Execute bulk-update de links usando `sed` com validação
   - Teste links com `find . -name "*.md" -exec grep -l "broken-path" {} \;`
   - Commit com mensagem descritiva das mudanças

**Exemplo de mensagem de commit:**
```
refactor: rename agents/ → specs/ for clarity

- Move /agents/ → /specs/
- Update 22 .md files with new paths (agents/ → specs/)
- Update pom.xml module references
- Verify 0 remaining "agents/" references

Refs: #issue-number
```

---

## 5. Estabilidade de Contratos (Records, APIs)

Arquivos em `/specs/` descrevem contratos Java (Records) que podem ser usados em múltiplos heroes.

### Quando Você Muda um Contrato em `/specs/`

```java
// ANTES: docs/reference/HERO_MANIFEST.md
record HeroManifest(String heroId, String heroName, List<String> skills)

// DEPOIS: Você adiciona um campo
record HeroManifest(String heroId, String heroName, List<String> skills, int level)
```

**Checklist:**

```
[ ] Mudança é backward-compatible? (novo campo tem default?)
[ ] Todos os heroes que usam este contrato foram testados?
[ ] /specs/heroes/*/README.md atualizado?
[ ] Git blame mostra quem e quando foi a mudança?
[ ] Issue aberta para migração de dados (se stateful)?
```

---

## 6. Integração com GitHub Issues (Ledger)

Quests e progresso de heróis vivem em GitHub Issues com labels.

### Labels Obrigatórios (Não Renomear!)

```
hero           — identifica uma issue de hero registration
registered     — hero foi validado e está ativo
quest          — um questão estruturada
spark          — mini-quest (<100 XP)
level:{n}      — nível atual (level:1, level:3, etc)
xp:{total}     — XP acumulado
skill-validated:{skill}  — skill verificada em quest real
```

**Se você precisar adicionar novo label:**

1. Crie issue de Discussão propondo o novo label
2. Aguarde consenso
3. Documente em `docs/reference/GITHUB_LABELS.md`
4. Atualize scripts que usam labels (ex: leaderboard, quest-matching)

---

## 7. Monitoramento de Dependências e Stack

Stack definida em [PRINCIPLES.md](../PRINCIPLES.md) está congelada até discussão explícita.

### Antes de Adicionar Qualquer Dependência

- ❌ Não adicione React, Vue, ou SPA framework sem aprovação de arquitetos
- ❌ Não adicione banco SQL externo (SQLite é local-first)
- ❌ Não adicione OpenAI/Claude API sem testar SLM local primeiro
- ✅ Atualizar `pom.xml` com versões novas é automático (depende de renovabot)
- ✅ Adicionar novos modelos Ollama requer atualizar `/specs/models.md`

**Onde documentar decisões de stack:**
- `docs/AGENTS.md` — decisões de arquitetura
- `/specs/README.md` — contratos e especificações
- `PRINCIPLES.md` — princípios inegociáveis

---

## 8. Cadência de Revisão

### Semanal (Todo Maintainer)
- [ ] Revisar [Pull Requests](https://github.com/SEU-REPO/skillforge/pulls) em aberto
- [ ] Verificar se frontmatters estão presentes em novos .md files
- [ ] Validar que nenhuma referência a paths antigos entrou

### Mensal (Time Principal)
- [ ] Executar `bash scripts/validate-frontmatter.sh`
- [ ] Revisar [Discussions](https://github.com/SEU-REPO/skillforge/discussions) abertas
- [ ] Atualizar MAINTENANCE.md se novos padrões emergirem
- [ ] Revisar [INDEX.md](../INDEX.md) — adicionar novas entradas?

### Trimestral (Arquitetos)
- [ ] Avaliar se estrutura atual ainda faz sentido
- [ ] Revisar se há inconsistências semânticas em ai-context/ai-audience
- [ ] Planejar melhorias na próxima fase

---

## 9. Troubleshooting Comum

### "Quebrei um link interno — como achar todas as referências?"

```bash
# Procura por um path antigo
grep -r "agents/" . --include="*.md"

# Substitui em todos os .md files
find . -name "*.md" -exec sed -i 's|agents/|specs/|g' {} \;
```

### "Copilot não enxerga um arquivo novo que criei"

Checklist:
```
[ ] Arquivo tem extensão .md?
[ ] Está em um diretório que Copilot indexa? (docs/, specs/, quests/)
[ ] Tem frontmatter YAML válido?
[ ] ai-context é um valor exato do conjunto permitido?
[ ] Esperar 5 minutos para Copilot reindexar
[ ] Limpar cache local: Cmd+Shift+P → "Developer: Reload Window"
```

### "Adicionei um novo hero — qual arquivo devo criar?"

1. **Se é um hero node (Spring Boot):**
   - Local: `/heroes/{hero-id}/`
   - Documentação: `/specs/heroes/{hero-id}/SETUP.md`
   - Pom.xml: adicione `<module>heroes/{hero-id}</module>`
   - INDEX.md: adicione entrada com contexto `hero-spec`

2. **Se é uma capacidade (agent, validator, orquestrador):**
   - Local: `/tools/{tool-id}/`
   - Documentação: `/specs/{tool-id}/SPEC.md`
   - Pom.xml: adicione `<module>tools/{tool-id}</module>`

3. **Se é uma documentação de domínio (ex: pronto-socorro médico):**
   - Local: `/quests/domains/{domain}/`
   - Documentação: `/quests/domains/{domain}/DOMAIN_PROFILE.md`
   - Não entra em pom.xml (é conteúdo, não código)

---

## 10. Próximas Fases (Roadmap de Manutenção)

### Fase 4.1 — Automação (próximos 2 sprints)
- [ ] GitHub Action que valida frontmatter em PRs
- [ ] Renovabot para atualizar dependências automaticamente
- [ ] Script que gera leaderboard a partir de labels

### Fase 4.2 — Observabilidade (próximos 4 sprints)
- [ ] Dashboard que mostra "saúde" da estrutura (quantos arquivos sem frontmatter, links quebrados)
- [ ] Métrica: "Cobertura de IA Tools" (% de arquivos com metadata válida)
- [ ] Alertas se alguma pasta fica vazia inesperadamente

### Fase 4.3 — Evolução Semântica (roadmap de longo prazo)
- [ ] Análise de quando `ai-context` deveria ter um novo tipo
- [ ] Quando `ai-audience` deve ser expandida
- [ ] Avaliação de se existe "padrão emergente" em como heróis se organizam

---

## Referências

- [FRONTMATTER_GUIDE.md](./FRONTMATTER_GUIDE.md) — Especificação completa de metadata
- [INDEX.md](../INDEX.md) — Mapa navegável de todos os documentos
- [PRINCIPLES.md](../PRINCIPLES.md) — Princípios inegociáveis de arquitetura
- [.github/pull_request_template.md](../.github/pull_request_template.md) — Template para PRs (deve referenciar checklist acima)

---

**Última atualização:** 2026-06-01  
**Status:** Fase 4 — Definição de Processos de Manutenção
