# Supabase Auth — setup do email + linking automático

Guia passo-a-passo das **3 configurações de Dashboard** que precisam ser feitas
manualmente pro fluxo de auth funcionar 100% em produção.

Projeto: **Rodapé** (pegar a URL no `.env` raiz, chave `SUPABASE_URL`).

---

## 1️⃣ SMTP customizado (Resend) — resolve rate limit e emails que não chegam

### Por que

Free tier do Supabase tem rate limit agressivo (~4 emails/hora por endpoint) e
o servidor SMTP padrão só envia emails pra endereços listados como "Authorized
Users". Em produção isso quebra: usuário comum não recebe email de confirmação
nem reset de senha.

Resend resolve ambos:
- 100 emails/dia grátis (3000/mês), sem cartão
- Bypassa o rate limit do Supabase
- DNS verification (SPF/DKIM) entrega na inbox em vez de spam

### Passos

#### 1.1 Criar conta Resend

1. Vai em https://resend.com → **Sign up** (use o Gmail do projeto)
2. No dashboard → **API Keys** → **Create API Key**
   - Nome: `Rodape Supabase Auth`
   - Permission: **Sending access** (não "Full access")
   - Domain: deixa **All domains** por enquanto
3. **Copia a key** (`re_...`) — só aparece uma vez. Guarda no `.env` raiz como:
   ```
   RESEND_API_KEY=re_xxxxxxxxxxxxxxxxxxxxx
   ```

#### 1.2 Domínio de envio (opcional pra MVP, recomendado pra produção)

- **MVP rápido (sem domínio próprio):** Resend dá `onboarding@resend.dev` que
  funciona out-of-the-box. Pula pra **1.3**.
- **Produção séria:** Adiciona seu domínio em Resend → **Domains** → Add Domain.
  Te dá registros DNS (MX, TXT, CNAME) pra colar no seu provedor de DNS. Após
  verificação (~10 min), passa a poder enviar de `no-reply@seudominio.com`.

#### 1.3 Configurar no Supabase Dashboard

1. Abre o projeto no Supabase Dashboard
2. **Authentication** → **Emails** → **SMTP Settings**
3. Ativa o toggle **Enable Custom SMTP**
4. Preenche:
   - **Sender email:** `onboarding@resend.dev` (ou o do seu domínio)
   - **Sender name:** `Rodapé`
   - **Host:** `smtp.resend.com`
   - **Port:** `465`
   - **Username:** `resend`
   - **Password:** cola a `RESEND_API_KEY` (re_...)
   - **Minimum interval between emails:** `60` (default ok)
5. **Save**

#### 1.4 Testar

- Cadastra uma conta nova no app com email externo (não-Gmail, ex: ProtonMail)
- Email de confirmação deve chegar em <30s
- No Resend dashboard → **Emails** confirma que saiu

---

## 2️⃣ Email confirmation obrigatória

### Por que

Garante que o user é dono do email antes de criar conta — pré-requisito de
segurança pra **automatic linking** (passo 3) não vazar contas.

### Passos

1. Supabase Dashboard → **Authentication** → **Providers** → **Email**
2. Confirma que **Confirm email** está ✅ ligado
3. Em **Authentication** → **URL Configuration**:
   - **Site URL:** `app.rodape://` (ou o domínio web se tiver)
   - **Redirect URLs:** adiciona `app.rodape://reset-password` na lista
     (já deve estar — verifica que continua)
4. **Authentication** → **Emails** → **Email Templates** → **Confirm signup**
   - Customiza o template pra ter a marca Rodapé (opcional, mas dá credibilidade)
   - O placeholder `{{ .ConfirmationURL }}` precisa estar presente

### Comportamento esperado

- User cadastra → recebe email com link → clica → confirma → pode logar
- Tentativa de login antes de confirmar → mensagem amigável já tratada no
  `AuthErrors.kt` ("Confirme seu email antes de entrar")

---

## 3️⃣ Automatic linking Google ↔ email

### Por que

Sem isso, usuário que cadastrou com email/senha **não consegue logar com Google
do mesmo email** — erro `account_not_linked` ou similar. Com linking automático,
Google detecta o email já registrado, valida que está confirmado, e liga as
identidades automaticamente.

⚠️ **Só funciona com segurança se passo 2 (email confirmation) estiver ativo**,
senão alguém pode criar conta com email alheio e Google "rouba" depois.

### Passos

1. Supabase Dashboard → **Authentication** → **Settings** (no menu lateral)
2. Procura a seção **Advanced** ou **Manual linking**
3. Liga o toggle **"Allow manual linking"** OU **"Automatic linking by email"**
   - Nome do toggle varia entre versões do Dashboard
   - Se não achar, vai em **SQL Editor** e roda:
     ```sql
     update auth.config
        set enable_manual_linking = true
      where id = 'default';
     ```
4. **Save**

### Comportamento esperado

- User cadastra com `joao@gmail.com` + senha → confirma email → loga normal
- Mais tarde, mesmo user clica em **Continuar com Google** com `joao@gmail.com`
- Supabase detecta email já registrado e confirmado → liga Google ao mesmo
  user_id → login bem-sucedido (mesma conta, agora com 2 providers)

---

## ✅ Checklist final

- [ ] Resend API key criada e salva no `.env`
- [ ] Supabase SMTP customizado configurado e salvou OK
- [ ] Teste: cadastro com email externo recebe confirmação em <30s
- [ ] Email confirmation está ON no provider Email
- [ ] Manual/automatic linking ligado em Advanced settings
- [ ] Teste: cadastra com email+senha → confirma → loga com Google do mesmo email → entra na mesma conta

---

## Troubleshooting

| Erro no app | Causa provável | Fix |
|-------------|----------------|-----|
| "Confirme seu email antes de entrar" | Email confirmation ligada + user não clicou no link | OK (esperado) |
| "Muitas tentativas" mesmo após config SMTP | Cache do Supabase do rate limit antigo | Espera 1h ou contata suporte |
| Email não chega no destino | DNS do domínio não verificado em Resend | Volta em 1.2 ou usa `onboarding@resend.dev` |
| "account_not_linked" mesmo após ligar linking | Email do user não está marcado confirmed | Confirma manualmente em Authentication → Users |
