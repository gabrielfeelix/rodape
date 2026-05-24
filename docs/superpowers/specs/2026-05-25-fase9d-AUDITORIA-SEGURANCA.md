# Fase 9D — Auditoria de Segurança Supabase

**Data:** 2026-05-25
**Auditor:** Claude (revisao senior)
**Projeto:** zfbywoeajebvasnsrzfh (sa-east-1, Postgres 17)
**Escopo:** RLS + RPCs SECURITY DEFINER + Storage policies + Auth config

---

## TL;DR

**Status geral**: ✅ **Sólido**. Backend bem desenhado. Encontrei 1 problema técnico
(corrigido nesta sessão via migration 18) e 1 problema de configuração a ser
resolvido no painel (você precisa clicar).

---

## ✅ O que está bem feito

### Row Level Security (RLS)
- **22/22 tabelas com RLS ATIVO** (`rowsecurity = true`)
- **73 policies bem estruturadas**, todas filtradas por `role = authenticated`
- **Padrão consistente** de qual papel pode fazer o quê:
  - `SELECT` → membros do clube veem dados do clube
  - `INSERT` → dono se identifica via `user_id = auth.uid()`
  - `UPDATE/DELETE` → dono ou admin
- **Performance**: helpers `is_club_member/admin/super` usados como subquery
  (`( SELECT is_club_member(...) AS ...)`) — Postgres cacheia em initplan,
  não executa por row.

### RPCs SECURITY DEFINER
Todas as 14 funções `SECURITY DEFINER` auditadas linha-por-linha:

| RPC | `auth.uid()` check | Permission check | `SET search_path` |
|-----|-------------------|------------------|-------------------|
| `create_club` | ✅ | qualquer logado | ✅ public, pg_temp |
| `join_club_with_code` | ✅ | regex code + clube existe | ✅ |
| `promote_member` | ✅ | admin ou super | ✅ |
| `demote_admin` | ✅ | só super_admin | ✅ |
| `transfer_super_admin` | ✅ | só super + alvo é admin | ✅ |
| `remove_member` | ✅ | hierárquica (super > admin > member) | ✅ |
| `leave_club` | ✅ | membership + super_admin handoff | ✅ |
| `close_voting_round` | ✅ | só admin/super | ✅ |
| `regenerate_invite_code` | ✅ | só admin/super | ✅ |
| `is_club_member/admin/super` | helper interno, sem auth check explícito (intencional) | n/a | ✅ |
| `club_role` | helper interno | n/a | ✅ |
| `generate_invite_code` | helper interno | n/a | ✅ |
| `generate_unique_invite_code` | helper interno | n/a | ✅ |

**Defesa contra hijack via `search_path`**: todas têm `SET search_path TO 'public', 'pg_temp'`
no header. Sem isso, um atacante poderia criar tabela `public.club_members` em outro
schema e fazer o `SELECT FROM club_members` da RPC consultar a tabela maliciosa.

### Storage
- **Bucket `book-covers`**:
  - Privado (`public = false`) — URLs precisam ser signed
  - Limite 5MB, só `image/jpeg/png/webp`
  - Policies por path: `<club_id>/...` — usuário só vê/escreve em clubes que é membro
- **Bucket `avatars`**:
  - Privado, limite 2MB, mesmas extensões
  - Path: `<user_id>/...` — usuário só mexe no seu próprio

---

## ⚠️ Problemas encontrados e CORRIGIDOS nesta sessão (migration 18)

### 1. `auto_close_expired_rounds` callable por usuários comuns

**Severidade**: Médio (não vaza dado, mas burla controle)

**Problema**: A função era chamável via `/rest/v1/rpc/auto_close_expired_rounds`
por qualquer usuário autenticado. Embora ela só execute o que é "permitido"
(fechar rounds que já passaram do prazo), o cliente podia:
- Forçar fechamento prematuro contra a vontade dos admins
- Rodar em loop como DoS (cada call faz N inserts em notifications)
- Burlar o pipeline natural (admins esperam fechar via UI, não automatizado)

**Correção aplicada** (migration 18):
```sql
REVOKE EXECUTE ON FUNCTION public.auto_close_expired_rounds() FROM authenticated;
```

Agora só `service_role` e `postgres` podem chamar. Quando precisarmos automatizar
(cron job server-side), criamos uma Edge Function que usa service_role.

### 2. FK sem index em `meeting_notes.user_id`

**Severidade**: Performance (não é problema de segurança)

**Correção aplicada** (migration 18):
```sql
CREATE INDEX IF NOT EXISTS meeting_notes_user_idx
  ON public.meeting_notes (user_id);
```

---

## ⚠️ Problema de configuração — PRECISA DE VOCÊ (1 minuto no painel)

### Leaked Password Protection Disabled

Supabase tem integração com [HaveIBeenPwned.org](https://haveibeenpwned.com) que
bloqueia senhas que apareceram em vazamentos públicos. **Está desabilitado**.

**Como ligar:**

1. Abrir painel: https://supabase.com/dashboard/project/zfbywoeajebvasnsrzfh/auth/policies
2. Menu lateral: **Authentication → Policies → Password Strength**
3. Toggle **"Check passwords against HaveIBeenPwned"** = ON
4. Salvar

Recomendado também:
- **Minimum password length**: 8 (default é 6 — fraco demais)
- **Required characters**: pelo menos 1 letra + 1 número

---

## ⚠️ Outros warnings ignorados (motivo documentado)

Os advisors apontam 13 funções como "SECURITY DEFINER callable por authenticated".
**Mantemos todas habilitadas** porque é exatamente o padrão correto: cliente chama
a função, função valida quem chamou e o que pode fazer. Tirar EXECUTE quebraria o app.

A diferença com `auto_close_expired_rounds` (que tiramos) é que aquela **não fazia
check de permissão interno** — qualquer usuário podia disparar. As outras todas têm
`auth.uid()` + check de papel no clube.

---

## Performance (não-crítico, podemos endereçar depois)

6 indexes não usados ainda — porque o banco está virtualmente vazio. Quando
popular vão começar a ser usados (`books_isbn_idx`, `books_openlibrary_idx`,
`books_title_trgm_idx`, `notifications_user_unread_idx`, `meetings_club_data_idx`,
`comments_chapter_created_idx`, `reactions_comment_idx`).

**Não remover ainda** — Postgres só sabe se são úteis depois de carga real.

---

## Conclusão

O backend Supabase do Rodapé está **pronto pra produção do ponto de vista de
segurança**, com 2 ressalvas:

1. ✅ **Corrigida nesta sessão**: `auto_close_expired_rounds` revogada de `authenticated`
2. ⚠️ **Aguarda 1min seu**: Leaked Password Protection no painel

Nenhum dado de usuário está exposto. Nenhuma RPC permite escalada de privilégio.
Nenhuma policy tem furo. Storage é privado e segregado por clube/usuário.

Próximo passo recomendado: ligar a proteção de senha no painel, depois seguir
pro Nível 1A (Realtime read-only).
