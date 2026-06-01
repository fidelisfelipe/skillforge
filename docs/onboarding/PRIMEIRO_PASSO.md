---
ai-context: "guide"
ai-audience: ["junior-developers", "new-heroes"]
ai-scope: "Criar primeira SPARK quest em 20 minutos"
---

# 🚀 Seu Primeiro Passo: Criar uma Micro-Quest

> Este é o caminho mais direto para um junior criar sua primeira quest no SkillForge.
> **Tempo:** ~20 minutos. **Resultado:** 1 Micro-Quest aberta + 100 XP em jogo.

---

## 1️⃣ Antes de Começar (2 min)

**Você precisa:**
- Conta GitHub (já tem)
- Fork ou acesso de write no repositório SkillForge
- Entender 3 palavras:
  - **Issue:** Problema/convite no GitHub
  - **PR:** Pull Request (sua solução para uma issue)
  - **Spark:** Label de Micro-Quest (fácil e rápido)

---

## 2️⃣ Escolha Seu Tópico (3 min)

**Não pode ser qualquer coisa.** Precisa responder uma destas 7 perguntas:

| # | Pergunta | Exemplo |
|---|----------|---------|
| 1 | **Quem é o ator?** | "Criar um serviço que estude padrões de herói" |
| 2 | **O que está acontecendo?** | "Os heróis não conseguem ver suas skills em tempo real" |
| 3 | **Quão urgente é?** | "Guild Hub cai quando 10+ heróis conectam" |
| 4 | **Qual contexto histórico?** | "Dashboard tá quebrado desde que atualizou Spring 3.3" |
| 5 | **O que pode dar errado?** | "Herói perde XP se Ollama desconecta" |
| 6 | **O que fazemos AGORA?** | "Adicionar suporte a Virtual Threads em hero-template" |
| 7 | **Como sabemos que funcionou?** | "Quando 100% das quests têm testes JUnit5" |

**Escolha uma. Exemplo real:**

> Pergunta 6: "Quero adicionar um endpoint `/api/hero-stats` que mostre estatísticas do herói em tempo real"

---

## 3️⃣ Abra uma Issue no GitHub (5 min)

### Passo 3.1: Ir para Issues
```
https://github.com/fidelisfelipe/skillforge/issues
Click "New Issue"
```

### Passo 3.2: Preencher a Issue
```
TÍTULO: (copie exatamente este formato)
SPARK-NNN — [Seu Título em Imperativo] | +100 XP | COMMON

Exemplo:
SPARK-048 — Criar endpoint /api/hero-stats | +100 XP | COMMON
```

### Passo 3.3: Descrição
```markdown
## 🎯 O Que Fazer

[1-2 frases explicando a mudança]

Exemplo:
Criar um novo endpoint GET `/api/hero-stats` que retorna as estatísticas 
acumuladas de um herói (XP total, nível, skills desbloqueadas, quests completadas).

## ✅ Definition of Done

- [ ] Endpoint GET /api/hero-stats/{heroId} implementado
- [ ] Retorna JSON válido com: xp_total, level, skills[], quests_completed
- [ ] 1+ teste unitário cobrindo caso feliz
- [ ] Documentação no README ou OpenAPI
- [ ] Merge no main sem quebrar testes existentes

## 🏆 Skills Usadas
- REST API Development
- SQL & Database

## 📍 Severidade
COMMON (pequena mudança, 1-3 dias de trabalho)
```

### Passo 3.4: Labels
Na direita, procure por "Labels":
- ✅ Adicione: `spark` (este é o magic label)
- ✅ Adicione: `junior-friendly` (se apropriado)
- ✅ Adicione: (opcional) `good-first-issue`

### Passo 3.5: Submit
```
Click "Submit new issue"
```

✅ **Pronto! Sua primeira quest aberta!**

Você acaba de:
- Criar uma **Issue pública** que qualquer um pode ver
- Declarar **skills necessárias** para resolvê-la
- Oferecer um **convite aberto** (não uma ordem)

---

## 4️⃣ Workflow Automático (o GitHub cuida) (2 min)

**O que acontece automaticamente:**

1. **Bot adiciona comment:**
   ```
   ✅ SPARK-048 detectada!
   Status: [ ] Aberta
   Skills: REST API Development, SQL & Database
   XP: 100 (COMMON)
   ```

2. **Aparecem no QUEST_BOARD.md** (automático)

3. **Herói vê e comenta:**
   ```
   @seu-login /claim
   ```
   (Isso significa "vou fazer essa")

4. **Herói abre PR:**
   ```
   GitHub → "New Pull Request"
   Title: Closes #NNN
   ```

5. **Você faz code review:**
   - Comenta: "Bom! Falta teste"
   - Não é "mande fazer", é "considere..."

6. **Herói ajusta**

7. **Merge → XP creditado** (você ganha +100 XP também!)

---

## 5️⃣ Se Quiser Resolver SUA PRÓPRIA QUEST (Opcional)

Você pode resolver o que criou:

```bash
# Clone se ainda não tem
git clone https://github.com/fidelisfelipe/skillforge.git
cd skillforge

# Crie branch local
git checkout -b local/spark-048-hero-stats

# Implemente (20 min)
# Teste localmente (10 min)
# Commit
git add .
git commit -m "feat: Add GET /api/hero-stats endpoint"

# Push
git push origin local/spark-048-hero-stats

# Abra PR no GitHub
# Titulo: Closes #NNN
# Descreva o que fez

# Mentor faz review + merge
```

---

## 6️⃣ Exemplos Reais no Repositório

Procure por issues abertas com label `spark`:
```
https://github.com/fidelisfelipe/skillforge/issues?q=label:spark
```

Veja também quests já criadas:
```
/quests/MICRO_QUEST.md          ← Formato oficial
/quests/QUEST_BOARD.md          ← Todas as quests (SPARK + QUEST)
/quests/catalog/QUEST-014*.md   ← Exemplo de quest completa (mais complexa)
```

---

## 7️⃣ Checklist: Você Está Pronto?

- [ ] Criou uma GitHub issue
- [ ] Tem label `spark`
- [ ] Tem um título em imperativo
- [ ] Tem pelo menos 2 itens em "Definition of Done"
- [ ] Respondeu uma das 7 perguntas universais
- [ ] Mencionou skills necessárias

✅ **Se checou todas → parabéns! Sua primeira quest saiu! 🎉**

---

## 8️⃣ Próximo Passo Natural

Depois que tiver 1-2 Micro-Quests criadas:

1. **Explore Issues abertas** (pegue uma que alguém else criou)
2. **Comente `/claim`** (significa "vou resolver essa")
3. **Abra um PR** com sua solução
4. **Receba feedback** (code review = mentoria)
5. **Aprenda** e melhore

Você ganha XP **criando** e **resolvendo**. Sem dependência. No seu ritmo.

---

## ❓ Dúvidas Frequentes

### "Mas e se minha quest for ruim?"
> Não existe quest "ruim" em SkillForge. Existe quest que ninguém resolve (ok, volta para brainstorm) ou quest que é resolvida (XP pra todo mundo). Zero risco.

### "Preciso resolver a quest que criei?"
> Não! Você criou um convite aberto. Qualquer herói pode pegar. Você ganha XP por **criar** (não por resolver).

### "Quanto tempo para aparecer no QUEST_BOARD?"
> Automático quando a issue é criada com label `spark`. Apareça em <1 min.

### "Posso mudar depois?"
> Sim! Edite a issue, o QUEST_BOARD atualiza automaticamente.

### "Não sei se minha ideia é boa"
> Sem problema! Crie a issue, discuta em comentários. É discussão aberta (opensource). Se não fizer sentido, fecha e pronto.

---

## 🎯 Filosofia

> Em SkillForge, **criar uma quest é um ato de generosidade**.
> Você não está "mandando fazer".
> Você está **oferecendo um convite** ao time para resolver algo que você acha importante.

Sem pressão. Sem prazos. Sem hierarquia.

Só você + GitHub Iss