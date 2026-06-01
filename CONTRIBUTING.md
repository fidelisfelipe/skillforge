# Contribuindo com o SkillForge

Obrigado por querer melhorar a guilda. Este guia cobre o fluxo para contribuições de código ao repositório principal.

---

## Antes de começar

- Leia o [`PRINCIPLES.md`](PRINCIPLES.md) — ele define as decisões técnicas inegociáveis do projeto.
- Verifique se já existe uma [issue](https://github.com/fidelisfelipe/skillforge/issues) relacionada à sua contribuição. Se não existir, abra uma antes do PR.

---

## Fluxo de contribuição

### 1. Fork e clone

Quando você faz fork, o GitHub cria uma cópia do repositório na sua conta. Você passa a ter dois repositórios relacionados:

| Nome | O que é | URL |
|---|---|---|
| `origin` | **Seu fork** — onde você commita e faz push | `github.com/SEU-USUARIO/skillforge` |
| `upstream` | **Repo principal** — fonte da verdade do projeto | `github.com/fidelisfelipe/skillforge` |

O Git não configura `upstream` automaticamente. Você precisa registrá-lo uma única vez após clonar:

```bash
# Fork via GitHub, depois:
git clone https://github.com/SEU-USUARIO/skillforge.git
cd skillforge
git remote add upstream https://github.com/fidelisfelipe/skillforge.git

# Confirme que os dois remotes estão configurados:
git remote -v
# origin    https://github.com/SEU-USUARIO/skillforge.git (fetch)
# upstream  https://github.com/fidelisfelipe/skillforge.git (fetch)
```

### 2. Sincronize com o upstream antes de começar

O repo principal avança enquanto você trabalha no fork. Antes de criar uma branch, traga as novidades do `upstream` para o seu `main` local:

```bash
git fetch upstream          # baixa o histórico do repo principal
git checkout main
git merge upstream/main     # aplica no seu main local
```

> `upstream/main` é a branch `main` do repositório principal, como ela estava no último `git fetch`. Não é a sua — é a deles.

### 3. Crie uma branch

Use o prefixo adequado ao tipo de mudança:

| Prefixo | Quando usar |
|---|---|
| `feature/` | Nova funcionalidade ou hero |
| `fix/` | Correção de bug |
| `docs/` | Apenas documentação |
| `chore/` | Configs, dependências, build |

```bash
git checkout -b fix/nome-descritivo
```

> **Atenção:** branches com prefixo `local/` nunca devem ser enviadas ao remote — um hook `pre-push` bloqueia automaticamente. Use `local/` apenas para WIP local.

### 4. Faça as alterações

- Siga a stack canônica descrita no `PRINCIPLES.md`
- Rode o build antes de commitar:

```bash
mvn verify
```

### 5. Commit

Use [Conventional Commits](https://www.conventionalcommits.org/):

```
fix: corrigir configuração rabbitmq no hero-template
feat: adicionar hero de análise de logs
docs: atualizar guia de onboarding
chore: atualizar versão do spring-boot
```

### 6. Push e Pull Request

```bash
git push origin fix/nome-descritivo
```

Abra o PR em `github.com/fidelisfelipe/skillforge` apontando para a branch `main`.

**No PR, descreva:**
- O que foi alterado e por quê
- Como testar (endpoint, comando, arquivo `.http`)
- Issue relacionada (ex: `Closes #42`)

---

## Padrões obrigatórios

### Novo hero

Todo novo hero deve incluir:

- `application.yml` com `spring.rabbitmq.addresses` (não `host`/`port`)
- `manifest.json` preenchido
- Arquivo `src/test/resources/{hero-id}.http` com health check e POST principal

### AMQP

```yaml
spring:
  rabbitmq:
    addresses: ${AMQP_URL}
    ssl:
      enabled: ${AMQP_SSL_ENABLED:false}
```

### LLM

Avalie modelo local via Ollama antes de usar API paga. Consulte a hierarquia em `PRINCIPLES.md`.

---

## Dúvidas

Abra uma [issue](https://github.com/fidelisfelipe/skillforge/issues) com a label `question`.
