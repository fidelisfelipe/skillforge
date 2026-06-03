# Configurando SSH para o GitHub

Guia rápido para membros que estão acessando o repositório pela primeira vez.

---

## Pré-requisitos

- [Git for Windows](https://git-scm.com/download/win) instalado
- Conta no GitHub com acesso ao repositório da guild

---

## Passo 1 — Verificar se já existe uma chave SSH

Abra o **PowerShell** e execute:

```powershell
ls ~/.ssh
```

Se você ver `id_ed25519.pub` ou `id_rsa.pub`, pule para o **Passo 3**.

---

## Passo 2 — Gerar uma nova chave SSH

Substitua pelo seu e-mail do GitHub:

```powershell
ssh-keygen -t ed25519 -C "seu-email@exemplo.com"
```

- Pressione **Enter** para aceitar o caminho padrão (`~/.ssh/id_ed25519`)
- Defina uma senha (recomendado) ou pressione **Enter** para deixar sem

---

## Passo 3 — Adicionar a chave ao agente SSH

```powershell
Get-Service -Name ssh-agent | Set-Service -StartupType Manual
Start-Service ssh-agent
ssh-add ~/.ssh/id_ed25519
```

---

## Passo 4 — Copiar a chave pública

```powershell
Get-Content ~/.ssh/id_ed25519.pub | clip
```

A chave foi copiada para a área de transferência.

---

## Passo 5 — Adicionar a chave no GitHub

1. Acesse **github.com → Settings → SSH and GPG keys**
2. Clique em **New SSH key**
3. Dê um nome (ex: `notebook-trabalho`) e cole a chave copiada
4. Clique em **Add SSH key**

---

## Passo 6 — Testar a conexão

```powershell
ssh-keyscan -t ed25519 github.com >> ~/.ssh/known_hosts
ssh -T git@github.com
```

Resultado esperado:

```
Hi <seu-usuario>! You've successfully authenticated, but GitHub does not provide shell access.
```

---

## Passo 7 — Clonar o repositório

```powershell
git clone git@github.com:fidelisfelipe/skillforge.git
```

Se você já clonou via HTTPS, atualize a URL remota:

```powershell
git remote set-url origin git@github.com:fidelisfelipe/skillforge.git
git remote -v
```

---

## Problemas comuns

| Sintoma | Solução |
|---|---|
| `Permission denied (publickey)` | A chave não foi adicionada ao GitHub — revise o Passo 5 |
| `Could not open a connection to your authentication agent` | Execute `Start-Service ssh-agent` novamente |
| `Host key verification failed` | Execute o `ssh-keyscan` do Passo 6 antes de testar |

---

Dúvidas? Chame um membro da guild no canal de onboarding.
