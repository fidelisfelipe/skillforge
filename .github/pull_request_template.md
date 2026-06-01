## 📋 Descrição

<!-- Descreva as mudanças em poucas palavras -->

Closes #(issue)

---

## ✅ Checklist de Qualidade

### Geral
- [ ] Testei as mudanças localmente (`mvn verify` para código Java)
- [ ] Não quebrei nenhum link existente
- [ ] Não introduzi referências a caminhos antigos (`agents/`, caminhos desatualizados)

### Se Adicionei/Modifiquei Documentação (`.md`)

#### Estrutura
- [ ] Arquivo está no diretório correto?
  - Onboarding → `/docs/onboarding/`
  - Progressão → `/docs/progression/`
  - Catálogos → `/docs/reference/`
  - Specs de heróis → `/specs/heroes/`
  - Arquitetura → `/docs/`
- [ ] Links internos usam paths **relativos** (ex: `[Link](../path/to/file.md)`, não URLs absolutas)

#### Metadata (Frontmatter YAML)
- [ ] Arquivo tem frontmatter YAML no início?
  ```yaml
  ---
  ai-context: "..."
  ai-audience: [...]
  ai-scope: "..."
  ---
  ```
- [ ] `ai-context` é um valor válido? (navigation, quickstart, guide, reference, decision-record, hero-spec, quest, domain-spec)
- [ ] `ai-audience` é um array com valores válidos? (all, junior-developers, senior-developers, architects, maintainers, AI-tools, new-heroes)
- [ ] `ai-scope` é uma descrição concisa (<120 caracteres)?

#### Indexação
- [ ] Se é um documento novo de **nível principal**, atualizei [INDEX.md](../INDEX.md)?
- [ ] Se adicionei subseção, atualizei o README.md da pasta pai?
- [ ] Links testaram (sem 404)?

### Se Modifiquei Código Java
- [ ] Build local passou: `mvn verify`
- [ ] Nenhum warning de compilação
- [ ] Se criou novo módulo, atualizei `pom.xml`?
- [ ] Se criou novo hero, adicionei arquivo `.http` em `src/test/resources/`?

### Se Criou Novo Hero
- [ ] Estrutura: `/heroes/{hero-id}/` com `pom.xml` e `src/`
- [ ] Especificação: `/specs/heroes/{hero-id}/README.md` e `system-prompt.md`
- [ ] Skills declaradas em manifest.json
- [ ] Arquivo `.http` para testes rápidos
- [ ] Pom.xml (raiz) referencia novo módulo?

### Se Modificou Contratos (Records em `/specs/`)
- [ ] Mudança é backward-compatible?
- [ ] Todos os heroes que usam este contrato foram testados?
- [ ] Documentação de `/specs/heroes/*/` foi atualizada?

### Se Criou Issue ou Quest
- [ ] Usou labels corretos? (`hero`, `registered`, `quest`, `spark`, `skill-validated:{skill}`, etc)
- [ ] Definition of Done está clara?
- [ ] Problema está bem-definido (não ambíguo)?

---

## 🧪 Teste Local

### Para documentação:
```bash
# Valide frontmatter (se você tem o script)
bash scripts/validate-frontmatter.sh
```

### Para código Java:
```bash
# Build completo com testes
mvn verify

# Se novo hero: teste endpoint HTTP
# Use arquivo src/test/resources/{hero-id}.http no IDE
```

---

## 📸 Screenshots (se relevante)

<!-- Adicione screenshots se mudou UI, dashboard, etc -->

---

## 🔍 Notas para Revisores

<!-- Contexto extra que ajuda o reviewer:
- Decisões de design
- Alternativas consideradas
- Dependências de outras PRs
- Aviso de breaking changes
-->

---

## 🚀 Pronto para Merge?

Seu PR será mergeado quando:

✅ Todos os checkboxes acima estiverem marcados  
✅ Código foi revisado (pelo menos 1 arquiteto ou maintainer)  
✅ Não há conflitos com `main`  

**Labels de prioridade:**
- `priority/urgent` — Bloqueia outra quest (merge antes de tudo)
- `priority/high` — Feature importante
- `priority/normal` — Manutenção regular
- `priority/low` — Nice-to-have

---

**Obrigado por contribuir ao SkillForge! 🧙**
