# Templates de email — Auth (Supabase + Resend)

Templates PT-BR, on-brand, pra colar no **Supabase Dashboard → Authentication →
Email Templates**. Cada seção tem o *Subject* e o *Body (HTML)*.

> **Pré-requisito (ação sua):** configurar SMTP custom (Resend) em
> Authentication → Emails → SMTP Settings — passo-a-passo em
> [supabase-auth-setup.md](supabase-auth-setup.md). O free tier do Resend
> (100 emails/dia) cobre a beta sem custo. Sem isso, o SMTP default do Supabase
> tem rate limit agressivo e os emails de reset falham em produção.

Variáveis do Supabase usadas: `{{ .ConfirmationURL }}` (link de ação),
`{{ .Token }}` (código OTP, se usar), `{{ .SiteURL }}`. Cores da marca: terracota
`#C4693D`, oliva `#6B7248`, tinta `#2B2723`, creme `#F5EFE6`.

O HTML abaixo é inline-styled (email não suporta `<style>` externo) e usa fontes
serifadas/sans do sistema como fallback de Literata/Inter.

---

## 1. Confirmar cadastro (Confirm signup)

**Subject:** `Confirme seu email no Rodapé`

```html
<div style="margin:0;padding:0;background:#F5EFE6;font-family:Georgia,'Times New Roman',serif;">
  <div style="max-width:520px;margin:0 auto;padding:40px 28px;">
    <div style="font-size:26px;font-weight:700;color:#2B2723;letter-spacing:-0.5px;">Rodapé</div>
    <div style="height:3px;width:44px;background:#C4693D;margin:14px 0 28px;"></div>
    <h1 style="font-size:22px;color:#2B2723;margin:0 0 12px;">Bem-vindo(a) ao clube 📚</h1>
    <p style="font-size:16px;line-height:1.6;color:#4A443C;font-family:Helvetica,Arial,sans-serif;">
      Falta um passo pra você começar a ler junto: confirme seu email tocando no botão abaixo.
    </p>
    <a href="{{ .ConfirmationURL }}"
       style="display:inline-block;margin:24px 0;padding:14px 28px;background:#C4693D;color:#fff;
              text-decoration:none;border-radius:999px;font-size:16px;font-family:Helvetica,Arial,sans-serif;font-weight:600;">
      Confirmar email
    </a>
    <p style="font-size:13px;line-height:1.6;color:#8A8175;font-family:Helvetica,Arial,sans-serif;">
      Se você não criou uma conta no Rodapé, pode ignorar este email.
    </p>
    <p style="font-size:12px;color:#B0A99C;font-family:Helvetica,Arial,sans-serif;margin-top:28px;">
      Feito com 💚 pra clubes de leitura.
    </p>
  </div>
</div>
```

---

## 2. Redefinir senha (Reset password)

**Subject:** `Redefinir sua senha do Rodapé`

```html
<div style="margin:0;padding:0;background:#F5EFE6;font-family:Georgia,'Times New Roman',serif;">
  <div style="max-width:520px;margin:0 auto;padding:40px 28px;">
    <div style="font-size:26px;font-weight:700;color:#2B2723;letter-spacing:-0.5px;">Rodapé</div>
    <div style="height:3px;width:44px;background:#C4693D;margin:14px 0 28px;"></div>
    <h1 style="font-size:22px;color:#2B2723;margin:0 0 12px;">Redefinir senha</h1>
    <p style="font-size:16px;line-height:1.6;color:#4A443C;font-family:Helvetica,Arial,sans-serif;">
      Recebemos um pedido pra redefinir a senha da sua conta. Toque no botão pra criar uma nova.
      O link expira em 1 hora.
    </p>
    <a href="{{ .ConfirmationURL }}"
       style="display:inline-block;margin:24px 0;padding:14px 28px;background:#C4693D;color:#fff;
              text-decoration:none;border-radius:999px;font-size:16px;font-family:Helvetica,Arial,sans-serif;font-weight:600;">
      Criar nova senha
    </a>
    <p style="font-size:13px;line-height:1.6;color:#8A8175;font-family:Helvetica,Arial,sans-serif;">
      Se você não pediu isso, ignore este email — sua senha continua a mesma.
    </p>
    <p style="font-size:12px;color:#B0A99C;font-family:Helvetica,Arial,sans-serif;margin-top:28px;">
      Feito com 💚 pra clubes de leitura.
    </p>
  </div>
</div>
```

---

## 3. Magic Link (login sem senha) — *se habilitar*

**Subject:** `Seu link de acesso ao Rodapé`

```html
<div style="margin:0;padding:0;background:#F5EFE6;font-family:Georgia,'Times New Roman',serif;">
  <div style="max-width:520px;margin:0 auto;padding:40px 28px;">
    <div style="font-size:26px;font-weight:700;color:#2B2723;letter-spacing:-0.5px;">Rodapé</div>
    <div style="height:3px;width:44px;background:#C4693D;margin:14px 0 28px;"></div>
    <h1 style="font-size:22px;color:#2B2723;margin:0 0 12px;">Entrar no Rodapé</h1>
    <p style="font-size:16px;line-height:1.6;color:#4A443C;font-family:Helvetica,Arial,sans-serif;">
      Toque no botão pra entrar. O link vale por 1 hora e só funciona uma vez.
    </p>
    <a href="{{ .ConfirmationURL }}"
       style="display:inline-block;margin:24px 0;padding:14px 28px;background:#6B7248;color:#fff;
              text-decoration:none;border-radius:999px;font-size:16px;font-family:Helvetica,Arial,sans-serif;font-weight:600;">
      Entrar no Rodapé
    </a>
    <p style="font-size:13px;line-height:1.6;color:#8A8175;font-family:Helvetica,Arial,sans-serif;">
      Não pediu esse link? Ignore este email.
    </p>
  </div>
</div>
```

---

## 4. Trocar email (Change email address)

**Subject:** `Confirme seu novo email no Rodapé`

```html
<div style="margin:0;padding:0;background:#F5EFE6;font-family:Georgia,'Times New Roman',serif;">
  <div style="max-width:520px;margin:0 auto;padding:40px 28px;">
    <div style="font-size:26px;font-weight:700;color:#2B2723;letter-spacing:-0.5px;">Rodapé</div>
    <div style="height:3px;width:44px;background:#C4693D;margin:14px 0 28px;"></div>
    <h1 style="font-size:22px;color:#2B2723;margin:0 0 12px;">Confirmar novo email</h1>
    <p style="font-size:16px;line-height:1.6;color:#4A443C;font-family:Helvetica,Arial,sans-serif;">
      Você pediu pra trocar o email da sua conta. Confirme o novo endereço tocando abaixo.
    </p>
    <a href="{{ .ConfirmationURL }}"
       style="display:inline-block;margin:24px 0;padding:14px 28px;background:#C4693D;color:#fff;
              text-decoration:none;border-radius:999px;font-size:16px;font-family:Helvetica,Arial,sans-serif;font-weight:600;">
      Confirmar novo email
    </a>
    <p style="font-size:13px;line-height:1.6;color:#8A8175;font-family:Helvetica,Arial,sans-serif;">
      Se não foi você, ignore este email e considere trocar sua senha.
    </p>
  </div>
</div>
```

---

## Checklist de configuração (ação sua no Dashboard)

- [ ] SMTP custom (Resend) configurado + domínio verificado (SPF/DKIM).
- [ ] Sender name = "Rodapé", sender email = algo como `nao-responda@rodape.app`.
- [ ] Colar os 4 templates acima em Authentication → Email Templates.
- [ ] Site URL e Redirect URLs corretos (deep link `app.rodape://` pro reset).
- [ ] Enviar 1 email de teste de cada tipo pra uma caixa real (checar spam/DKIM).
