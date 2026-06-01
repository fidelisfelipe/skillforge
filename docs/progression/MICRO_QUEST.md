---
ai-context: "guide"
ai-audience: ["junior-developers", "senior-developers"]
ai-scope: "Criar SPARK quests simplificadas para onboarding rápido"
---

# Micro-Quest

> Formato simplificado para features pequenas de alto potencial.  
> Nenhum domain profile, nenhum fixture, nenhuma validação clínica.  
> Basta implementar e abrir o PR.

---

## Quando usar Micro-Quest

Use micro-quest quando a feature:
- Cabe em 1–3 dias de trabalho
- Não exige conhecimento profundo de domínio (médico, financeiro, etc.)
- Pode ser validada automaticamente ou por revisão de código simples
- É uma melhoria clara para o projeto sem análise de impacto extensa

Use a [Quest Framework completa](QUEST_FRAMEWORK.md) quando a feature:
- Exige fixtures de domínio validadas
- Precisa de um modelo validador especializado (Meditron, etc.)
- Tem critérios de aceite complexos com camada de domínio e camada técnica

---

## Schema da Micro-Quest

```markdown
### `SPARK-{NNN}` — {Título} `COMMON` `+{XP} XP`

**Status:** `[ ]` aberta | `[~]` em progresso | `[x]` concluída
**Proposta por:** @{githubLogin}
**XP:** 100 | 150 | 200

**Descrição**
O que deve acontecer. Uma ou duas frases.
Quem se beneficia? O que muda?

**Definition of Done**
- [ ] Critério técnico 1
- [ ] Critério técnico 2
```

### Campos obrigatórios

| Campo | Obrigatório | Notas |
|---|---|---|
| `SPARK-{NNN}` | Sim | Número da issue GitHub |
| Título | Sim | Imperativo: "Adicionar X", "Corrigir Y" |
| Status | Sim | `[ ]` aberta por padrão |
| Descrição | Sim | Mínimo 1 frase clara |
| Definition of Done | Sim | Mínimo 1 critério verificável |
| XP | Sim | 100 / 150 / 200 |

### Campos opcionais

| Campo | Quando usar |
|---|---|
| `Skills sugeridas` | Se a feature exige skill específica |
| `Contexto adicional` | Links, screenshots, issues relacionadas |

---

## Como criar uma Micro-Quest

Abra uma issue no GitHub com o label `spark`.  
O workflow cria a estrutura automaticamente e posta como comentário.

**Título da issue:** o título vira o nome da quest.  
**Corpo da issue:** vira a descrição. Pode editar depois.

---

## Fluxo completo

```
1. Abrir issue com label spark
   └── Workflow posta micro-quest formatada como comentário
   └── Labels: micro-quest + open

2. Hero comenta /claim
   └── Issue atribuída ao hero
   └── Label: in-progress

3. Hero abre PR com closes #NNN
   └── PR review normal
   └── Merge → issue fechada automaticamente

4. XP creditado manualmente pelo guild master (por enquanto)
```

---

## Escala de XP

| XP | Critério |
|---|---|
| 100 | Feature simples: um endpoint, um fix de UI, uma melhoria de config |
| 150 | Feature média: integração com serviço externo, nova rota AMQP, novo utilitário |
| 200 | Feature complexa mas ainda sem domain profile: novo hero simples, refactor significativo |

> Para quests com XP acima de 200, use o schema completo com domain profile.

---

## Diferenças entre Micro-Quest e Quest

| Aspecto | Micro-Quest | Quest completa |
|---|---|---|
| Domain profile | Não exigido | Obrigatório |
| Fixtures | Não exigido | Mínimo 3 casos |
| Validação de domínio | Não | Sim (SLM especializado) |
| Camada domínio no DoD | Não | Obrigatória |
|