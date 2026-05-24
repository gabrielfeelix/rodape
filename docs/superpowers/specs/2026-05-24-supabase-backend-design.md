# Fase 8 — Backend Supabase (Auth + Postgres + Storage)

**Status:** Banco aplicado e validado em 2026-05-24. App ainda não migrado.
**Data:** 2026-05-24
**Autor:** brainstorm com Claude

## Status atual (atualizado pós-execução)

✅ Schema completo aplicado no projeto Supabase `zfbywoeajebvasnsrzfh` (sa-east-1, Postgres 17). 17 migrations idempotentes, todas em `list_migrations`. 22 tabelas com RLS ativo, ~80 policies, 12 RPCs `SECURITY DEFINER`, 3 triggers (handle_new_user, enforce_super_admin_invariant, set_updated_at), 2 storage buckets (book-covers, avatars), Realtime em 13 tabelas. Smoke test ponta-a-ponta passou: criar clube → entrar por código → criar voting round → votar → fechar round (livro vira current, notificações criadas) → promover member → transferir super_admin atomicamente. Banco está vazio após cleanup.

❌ Pendente nesta fase (próxima sessão):
- Configurar Google OAuth no Supabase Auth (depende do Google Cloud Console).
- Confirmar que "Confirm email" está ON no painel.
- Adicionar `supabase-kt` no Android, criar SupabaseClient, repos remotos, telas de auth reais, remover Room/seed/demo, ligar UI nos dados reais.

## Decisões finais (após confirmação 2026-05-24)

- **Votação:** 1 voto por round (PK em `votes` é `(voting_round_id, user_id)`).
- **Comentários removidos:** soft delete via flag `removido`. UI deve renderizar placeholder "[mensagem removida pela moderação]".
- **Catálogo books:** global, dedup entre clubes via `club_books`.
- **Email de confirmação:** ATIVO (configurar no painel Supabase Auth → Email Settings).
- **Push FCM:** fora desta fase.
- **Convite:** só por código de 6 chars `[A-Z0-9]`. Tab "Com link" do `JoinClubScreen` deve ser escondida até comprar domínio.
- **Bug corrigido durante smoke test:** trigger `enforce_super_admin_invariant` precisa permitir cascata de delete quando o clube em si está sendo deletado (migration 16).

## Objetivo

Trocar a fundação local (Room + DataStore session + seeds hardcoded + login fake) por um backend Supabase real: autenticação Email/Senha + Google Sign-In, Postgres como source of truth de tudo, Storage pras capas customizadas. App nasce vazio em produção — sem seeds, sem mockups, sem dados de demo. Schema desenhado client-agnóstico pra futuro app iOS conectar no mesmo backend.

## Escopo

**Dentro:**
- Projeto Supabase configurado (URL/keys vivem em `.env` + `BuildConfig` via plugin Secrets — já existe).
- Schema completo no Postgres: 22 tabelas espelhando o domínio Room atual, com FKs reais, índices, timestamps, e tipos nativos (UUID, timestamptz, enums).
- Row-Level Security em todas as tabelas com policies por papel (membro do clube, admin, super-admin, dono do registro).
- Triggers/funções: fechar voting round por tempo, manter invariante "1 super_admin por clube ativo", auto-promover admin mais antigo quando super-admin sai, propagar `current → finished` na votação.
- Realtime habilitado em `comments`, `reactions`, `votes`, `meeting_rsvps`, `notifications`, `book_summaries`, `meeting_minutes`.
- Buckets de Storage: `book-covers` (upload de capas manuais), `avatars` (upload customizado futuro — opcional na fase 1).
- Auth: Email/Senha habilitado. Google Sign-In configurado (OAuth no Google Cloud + Provider no Supabase + Credential Manager no Android).
- SDK Supabase Kotlin (`supabase-kt`) adicionado, `SupabaseClient` único exposto via `RodapeApp` (já existe Application).
- Camada `data/remote/` (substitui `data/repository/` Room) com classes por agregado: `AuthRepository`, `ProfileRepository`, `ClubRepository`, `BookRepository`, `ChapterRepository`, `MeetingRepository`, `VotingRepository`, `CommentRepository`, `QuoteRepository`, `RatingRepository`, `NotificationRepository`. Usam o cliente Supabase para REST + Realtime.
- Telas de auth reais: `LoginScreen` reescrita (sem botão de demo, validação real, erro de credencial), nova `SignUpScreen`, fluxo de "esqueci a senha" (envio de magic link de reset).
- Remoção integral de:
  - `RodapeRepository.seedDatabase()` e `seedNewClubData()`.
  - `RodapeDatabase` (Room) e todos os DAOs.
  - Sessão em DataStore (substituída pela sessão do Supabase Auth).
  - Lista de "demo users" e auto-login do MainViewModel.

**Fora (planejado pra fases futuras):**
- Modo offline / cache local pra ler sem internet. (App vai exigir conexão na primeira versão Supabase. Voltaremos a discutir cache depois que o backend estiver estável.)
- Apple Sign-In (entra junto com o porte iOS).
- Notificações push (FCM). Por ora notificações vivem só no banco e aparecem ao abrir o app.
- Importação de dados antigos do Room pra Supabase. Não há base instalada — app só rodou local em dev.
- App iOS em si. O schema é desenhado pra suportar, mas o cliente Swift fica pra outro projeto.

## Arquitetura

```
Android Compose UI (Jetpack)
        │
        ▼
ViewModels (existem hoje, mantidos — refatorados pra chamar novos repos)
        │
        ▼
data/remote/*Repository.kt  ← novo
        │ (Kotlin coroutines + Flow vindo de Realtime)
        ▼
SupabaseClient (supabase-kt)
        │
        ▼
┌─────────────────────────────────────────────┐
│            Supabase (cloud)                  │
│  Auth (GoTrue) ── identidade do usuário      │
│  PostgREST ────── leituras/escritas REST     │
│  Realtime ─────── WebSocket pra UI viva      │
│  Storage ──────── capas e avatares           │
│  Postgres ─────── tabelas + RLS + triggers   │
└─────────────────────────────────────────────┘
```

**Princípios:**
- Source of truth = Postgres. Cliente nunca calcula nada que possa ser derivado/validado por trigger (votação, transições de status, contagem de admins).
- RLS é a única autorização. Servidor não tem regra "extra" fora do banco.
- Realtime entrega mudanças (não polling). Cada repo expõe `Flow` que combina `select` inicial + `channel` ao vivo.
- Cliente nunca segura `service_role`. Só `anon` key no app. Operações privilegiadas (rotação de convite, criação de clube, transferência de super-admin) usam **RPCs Postgres** (`SECURITY DEFINER`) chamadas via PostgREST com sessão do usuário — o `SECURITY DEFINER` permite checar `auth.uid()` e bypassar RLS de forma controlada.

## Schema do Banco

### Convenções globais

- Todas as PKs são `uuid` com `default gen_random_uuid()`, exceto pivots que mantêm composite PK (`(a_id, b_id)`).
- Todas as tabelas têm `created_at timestamptz not null default now()` e (onde fizer sentido) `updated_at timestamptz not null default now()` mantido por trigger `set_updated_at`.
- Strings de enum atuais (`papel`, `status`, `cadencia`, etc.) viram **tipos enum nativos do Postgres** (mais seguros, validados pelo banco).
- FKs com `on delete cascade` quando o filho não faz sentido sem o pai (ex: `meeting_rsvps` → `meetings`); `on delete restrict` quando preservar dado é importante (ex: `votes` → `users`).
- IDs textuais antigos (`book_metamorfose`, `user_voce`) ficam em strings na história; o schema novo usa UUID em tudo.

### Enums Postgres

```sql
create type member_role as enum ('member', 'admin', 'super_admin');
create type club_privacy as enum ('convidados', 'publico');
create type club_book_status as enum ('current', 'finished', 'suggested', 'next');
create type meeting_status as enum ('agendado', 'concluido', 'cancelado');
create type rsvp_status as enum ('vou', 'talvez', 'nao_vou');
create type voting_round_status as enum ('aberta', 'fechada');
create type voting_cadence as enum ('unica', 'semanal', 'quinzenal', 'mensal_dia_semana', 'mensal_dia_mes', 'personalizado_dias');
create type notification_type as enum (
  'comment_on_chapter', 'next_book_decided', 'meeting_reminder',
  'member_finished', 'voting_open', 'voting_closed', 'member_removed',
  'promoted_to_admin', 'super_admin_transferred'
);
```

### Tabelas

#### `profiles`
> Ligado 1-1 com `auth.users.id`. Substitui a entidade `User` atual.

| coluna | tipo | nota |
|---|---|---|
| `id` | `uuid` PK | = `auth.users.id` |
| `nome` | `text not null` | |
| `sobrenome` | `text` | nullable |
| `avatar_key` | `text not null default 'preset:leitor'` | um dos 12 presets, ou `custom:<storage_path>` no futuro |
| `font_scale` | `numeric(3,2) not null default 1.0` | preferência de fonte (era DataStore) |
| `created_at`, `updated_at` | `timestamptz` | |

Trigger: `on auth.user create → insert into profiles (id, nome) values (new.id, coalesce(new.raw_user_meta_data->>'nome', split_part(new.email,'@',1)))`.

#### `clubs`
| coluna | tipo | nota |
|---|---|---|
| `id` | `uuid` PK | |
| `nome` | `text not null` | check length ≤ 40 |
| `descricao` | `text` | check length ≤ 140 |
| `codigo` | `text not null unique` | invite code 6 chars, gerado por função |
| `cor` | `text not null default '0'` | índice "0"-"4" ou hex `#RRGGBB` |
| `privacidade` | `club_privacy not null default 'convidados'` | |
| `criador_id` | `uuid not null references profiles(id)` | |
| `arquivado` | `boolean not null default false` | |
| `created_at`, `updated_at` | | |

Índice: `(arquivado)`.

#### `club_members`
| coluna | tipo | nota |
|---|---|---|
| `club_id` | `uuid references clubs(id) on delete cascade` | |
| `user_id` | `uuid references profiles(id) on delete cascade` | |
| `papel` | `member_role not null default 'member'` | |
| `entrou_em` | `timestamptz not null default now()` | |
| PK | `(club_id, user_id)` | |

Índices: `(user_id)` (pra "meus clubes"), `(club_id, papel)` (pra contar admins).
Constraint via **trigger**: cada `clubs` ativo tem exatamente 1 `papel = 'super_admin'`.

#### `books`
| coluna | tipo | nota |
|---|---|---|
| `id` | `uuid` PK | |
| `title` | `text not null` | |
| `author` | `text not null` | |
| `cover_url` | `text` | https://... ou `storage://book-covers/<path>` resolvido pelo cliente |
| `openlibrary_id` | `text` | |
| `isbn` | `text` | índice |
| `is_manual` | `boolean not null default false` | |
| `total_paginas` | `int` | |
| `editora` | `text` | |
| `ano_publicacao` | `int` | check 1000-2100 |
| `idioma` | `text not null default 'pt'` | |
| `created_at` | | |

Observação: books é **global**, compartilhado entre clubes. Não há FK pra `clubs` — a relação vive em `club_books`. Isso permite "o mesmo livro foi lido por 3 clubes" sem duplicar.

#### `club_books`
| coluna | tipo | nota |
|---|---|---|
| `club_id` | `uuid references clubs(id) on delete cascade` | |
| `book_id` | `uuid references books(id) on delete cascade` | |
| `status` | `club_book_status not null` | |
| `ordem` | `int not null default 0` | |
| `data_encontro` | `timestamptz` | data do encontro de fechamento (preenchido quando vira `finished`) |
| `created_at`, `updated_at` | | |
| PK | `(club_id, book_id)` | |

Constraint via trigger: no máximo 1 `(club_id, status='current')`.

#### `chapters`
| coluna | tipo |
|---|---|
| `id uuid PK` | |
| `book_id uuid references books(id) on delete cascade` | |
| `numero int not null` | |
| `titulo text not null` | |
| `created_at` | |
| unique `(book_id, numero)` | |

#### `user_progress`
| coluna | tipo |
|---|---|
| `user_id uuid references profiles(id) on delete cascade` | |
| `club_id uuid references clubs(id) on delete cascade` | |
| `book_id uuid references books(id) on delete cascade` | |
| `current_chapter int not null default 0` | |
| `updated_at` | |
| PK `(user_id, club_id, book_id)` | |

#### `comments`
| coluna | tipo |
|---|---|
| `id uuid PK` | |
| `chapter_id uuid references chapters(id) on delete cascade` | |
| `club_id uuid references clubs(id) on delete cascade` | |
| `user_id uuid references profiles(id)` | restrict (preservar autoria) |
| `texto text not null` | |
| `removido boolean not null default false` | |
| `removido_por uuid references profiles(id)` | |
| `motivo_remocao text` | |
| `created_at` | |

Índice: `(chapter_id, created_at desc)`.

#### `reactions`
| coluna | tipo |
|---|---|
| `comment_id uuid references comments(id) on delete cascade` | |
| `user_id uuid references profiles(id) on delete cascade` | |
| `emoji text not null` | |
| `created_at` | |
| PK `(comment_id, user_id, emoji)` | |

#### `voting_rounds`
| coluna | tipo |
|---|---|
| `id uuid PK` | |
| `club_id uuid references clubs(id) on delete cascade` | |
| `criado_por uuid references profiles(id)` | |
| `aberta_em timestamptz not null default now()` | |
| `fecha_em timestamptz not null` | |
| `n_livros int not null check (n_livros between 1 and 12)` | |
| `cadencia voting_cadence not null` | |
| `status voting_round_status not null default 'aberta'` | |
| `vencedores jsonb` | preenchido pelo trigger de fechamento |
| `fechada_em timestamptz` | |

#### `book_suggestions`
| coluna | tipo |
|---|---|
| `id uuid PK` | |
| `club_id uuid references clubs(id) on delete cascade` | |
| `book_id uuid references books(id) on delete restrict` | |
| `sugerido_por uuid references profiles(id)` | |
| `voting_round_id uuid references voting_rounds(id) on delete cascade` | |
| `justificativa text` | |
| `created_at` | |
| unique `(voting_round_id, book_id)` | |

#### `votes`
| coluna | tipo |
|---|---|
| `voting_round_id uuid references voting_rounds(id) on delete cascade` | |
| `book_id uuid references books(id) on delete cascade` | |
| `user_id uuid references profiles(id) on delete cascade` | |
| `voted_at timestamptz not null default now()` | |
| PK `(voting_round_id, user_id, book_id)` | usuário pode dar até `n_livros` votos? Não — 1 voto por round (validação em trigger). PK permite múltiplos *apenas se trocarmos a regra depois* — por enquanto trigger garante 1. |

Reverter: se decidirmos "1 voto por round, 1 livro só", trocar PK pra `(voting_round_id, user_id)` e remover `book_id` da PK.
**Decisão a confirmar com você na seção FAQ.**

#### `meetings`
| coluna | tipo |
|---|---|
| `id uuid PK` | |
| `club_id uuid references clubs(id) on delete cascade` | |
| `data timestamptz not null` | era string ("DOMINGO, 24 DE OUTUBRO"); agora timestamp real, cliente formata na exibição |
| `local text` | |
| `agenda text` | |
| `book_id uuid references books(id)` | nullable |
| `chapter_start int` | |
| `chapter_end int` | |
| `status meeting_status not null default 'agendado'` | |
| `created_at`, `updated_at` | |

#### `meeting_rsvps`
| coluna | tipo |
|---|---|
| `meeting_id uuid references meetings(id) on delete cascade` | |
| `user_id uuid references profiles(id) on delete cascade` | |
| `status rsvp_status not null` | |
| `updated_at` | |
| PK `(meeting_id, user_id)` | |

#### `meeting_patterns`
| coluna | tipo |
|---|---|
| `id uuid PK` | |
| `club_id uuid references clubs(id) on delete cascade` | |
| `dia_semana int not null check (dia_semana between 1 and 7)` | |
| `hora text not null` | "19:00" |
| `local text` | |
| `agenda_template text` | |
| `ativo boolean not null default true` | |
| `tipo_recorrencia voting_cadence not null` | reaproveita enum (mesmos valores) |
| `valor_recorrencia int not null default 0` | |

#### `meeting_minutes`
| coluna | tipo |
|---|---|
| `meeting_id uuid PK references meetings(id) on delete cascade` | |
| `texto text not null default ''` | |
| `last_editor_id uuid references profiles(id)` | |
| `updated_at` | |

#### `meeting_notes`
| coluna | tipo |
|---|---|
| `meeting_id uuid references meetings(id) on delete cascade` | |
| `user_id uuid references profiles(id) on delete cascade` | |
| `texto text not null default ''` | |
| `updated_at` | |
| PK `(meeting_id, user_id)` | |

#### `saved_quotes`
| coluna | tipo |
|---|---|
| `id uuid PK` | |
| `user_id uuid references profiles(id) on delete cascade` | |
| `club_id uuid references clubs(id) on delete cascade` | |
| `book_id uuid references books(id) on delete cascade` | |
| `texto text not null` | |
| `capitulo_ref text` | |
| `created_at` | |

Índice `(user_id, created_at desc)`.

#### `book_summaries`
| coluna | tipo |
|---|---|
| `book_id uuid references books(id) on delete cascade` | |
| `club_id uuid references clubs(id) on delete cascade` | |
| `texto text not null default ''` | |
| `last_editor_id uuid references profiles(id)` | |
| `updated_at` | |
| PK `(book_id, club_id)` | |

#### `book_ratings`
| coluna | tipo |
|---|---|
| `book_id uuid references books(id) on delete cascade` | |
| `club_id uuid references clubs(id) on delete cascade` | |
| `user_id uuid references profiles(id) on delete cascade` | |
| `stars int not null check (stars between 1 and 5)` | |
| `comment text not null default ''` | |
| `updated_at` | |
| PK `(book_id, club_id, user_id)` | |

#### `notifications`
| coluna | tipo |
|---|---|
| `id uuid PK` | |
| `user_id uuid references profiles(id) on delete cascade` | |
| `club_id uuid references clubs(id) on delete cascade` | |
| `tipo notification_type not null` | |
| `payload jsonb not null default '{}'::jsonb` | |
| `lida boolean not null default false` | |
| `created_at` | |

Índice `(user_id, created_at desc) where not lida` (pra unread count rápido).

#### `member_removals`
| coluna | tipo |
|---|---|
| `id uuid PK` | |
| `club_id uuid references clubs(id) on delete cascade` | |
| `user_id uuid references profiles(id)` | restrict |
| `removed_by uuid references profiles(id)` | |
| `motivo text` | |
| `removed_at timestamptz not null default now()` | |

## Row-Level Security

### Pattern principal

Para 90% das tabelas que pertencem a um clube, a policy é:

```sql
create policy "members can read"
  on <tabela> for select
  using (
    exists (
      select 1 from club_members cm
      where cm.club_id = <tabela>.club_id
        and cm.user_id = auth.uid()
    )
  );
```

Função helper pra evitar repetição:

```sql
create or replace function is_club_member(target_club uuid) returns boolean
language sql stable security definer as $$
  select exists (
    select 1 from club_members
    where club_id = target_club and user_id = auth.uid()
  );
$$;

create or replace function club_role(target_club uuid) returns member_role
language sql stable security definer as $$
  select papel from club_members
  where club_id = target_club and user_id = auth.uid();
$$;
```

### Policies por tabela (resumo)

| tabela | SELECT | INSERT | UPDATE | DELETE |
|---|---|---|---|---|
| `profiles` | qualquer autenticado lê nome+avatar | só o dono | só o dono | nunca (cascata via `auth.users`) |
| `clubs` | membros + público se `privacidade='publico'` | qualquer autenticado (vira super_admin via trigger) | admin/super | super (via RPC) |
| `club_members` | membros do clube | só via RPC `join_club_with_code` | só admin/super altera `papel` | super remove qualquer; member remove a si próprio |
| `books` | qualquer autenticado | qualquer autenticado | só `is_manual` + criador (a definir) | nunca |
| `club_books` | membros | admin/super | admin/super (status) | admin/super |
| `chapters` | qualquer autenticado | admin/super do clube que tem o livro como `current`/`finished` | admin/super | admin/super |
| `user_progress` | só dono | só dono | só dono | só dono |
| `comments` | membros (mesmo `removido=true`) | membros | só autor (texto); admin/super (campo `removido`) | nunca (soft delete) |
| `reactions` | membros | só dono | — | só dono |
| `voting_rounds` | membros | admin/super | admin/super | admin/super |
| `book_suggestions` | membros | membros | só sugerente | só sugerente; admin/super |
| `votes` | membros | só dono em round `aberta` | — | só dono em round `aberta` |
| `meetings` | membros | admin/super | admin/super | admin/super |
| `meeting_rsvps` | membros | só dono | só dono | só dono |
| `meeting_patterns` | membros | admin/super | admin/super | admin/super |
| `meeting_minutes` | membros | admin/super | admin/super | admin/super |
| `meeting_notes` | só dono | só dono | só dono | só dono |
| `saved_quotes` | membros (pra exibir nas frases do livro) | só dono | só dono | só dono |
| `book_summaries` | membros | membros | membros (last_editor) | nunca |
| `book_ratings` | membros | só dono | só dono | só dono |
| `notifications` | só destinatário | trigger/server | só destinatário (marca lida) | só destinatário |
| `member_removals` | admin/super | trigger/RPC | nunca | nunca |

### RPCs `SECURITY DEFINER`

Operações que envolvem múltiplas tabelas com invariantes:

- `create_club(nome, descricao, cor, privacidade) returns clubs` — cria clube, insere criador como `super_admin`, gera código único.
- `join_club_with_code(codigo) returns club_members` — valida código, insere como `member`.
- `leave_active_club(club_id)` — valida invariante de super_admin, auto-promove admin mais antigo se necessário.
- `promote_member(club_id, target_user_id, novo_papel)` — checa que quem chama é super (ou admin pra promover member).
- `transfer_super_admin(club_id, target_user_id)` — atômico: chamador vira `admin`, target vira `super_admin`.
- `remove_member(club_id, target_user_id, motivo)` — bloqueia auto-remoção do único super; insere em `member_removals`; cria notificação.
- `close_voting_round(round_id)` — calcula vencedores, grava `vencedores jsonb`, atualiza status, promove top-1 a `current` e demais a `next`, cria notificações.
- `regenerate_invite_code(club_id)` — gera novo código aleatório único; invalida o antigo.

### Triggers automáticos

- `set_updated_at` — em toda tabela com `updated_at`.
- `clubs_after_insert` — insere criador como `super_admin` em `club_members`.
- `voting_rounds_cron` — *opcional fase 2*: pg_cron ou Edge Function que roda a cada 5min e fecha rounds onde `now() >= fecha_em and status = 'aberta'`.
- `comments_notify` — insere `notifications` nos demais participantes do thread.

## Storage

### Bucket `book-covers`
- Privado por padrão; URLs assinadas curtas (1h) servidas pelo cliente quando precisar exibir.
- Path: `<club_id>/<book_id>/<uuid>.jpg`
- Policy: leitura/escrita só por membros do `club_id` no path.

### Bucket `avatars` (fase futura)
- Por ora só usamos os 12 presets (drawables locais). Bucket criado vazio pra quando habilitarmos upload customizado.

## Auth

### Email/Senha
- Habilitado nativo no Supabase. Tela `LoginScreen` (existente) reescrita: remover botão dev, validar email/senha de verdade, mostrar erro.
- Nova `SignUpScreen`: nome + email + senha (≥8 chars).
- Reset de senha: link "Esqueci senha" envia OTP/magic link (`auth.resetPasswordForEmail`).
- Email de confirmação: **desabilitado na primeira versão** (atrito alto pra MVP). Habilitar quando tivermos domínio e SMTP custom.

### Google Sign-In (Android)
1. Google Cloud Console: criar OAuth Client ID **tipo Android** (SHA-1 da debug key + da release key).
2. Google Cloud Console: criar OAuth Client ID **tipo Web** (esse é o `WEB_CLIENT_ID` que vai pro código + Supabase).
3. Supabase: Auth → Providers → Google → ativar, colar Web Client ID e Client Secret.
4. Android: adicionar `androidx.credentials:credentials` + `googleid` (Credential Manager moderno, substitui o `GoogleSignInClient` antigo deprecado).
5. Fluxo no app: `CredentialManager.getCredential(GetGoogleIdOption)` → recebe `idToken` → `supabase.auth.signInWith(IDToken) { provider = Google; token = idToken }`.

### Configurações de projeto Supabase
- Site URL: `app.rodape://callback` (deep link).
- Redirect URLs adicionais: nenhuma (sem web por ora).
- JWT expiry: padrão (1h). Refresh: padrão.

## Migração do app (passos macro)

Esse spec será destrinchado num plano de implementação separado (writing-plans). Aqui só o esqueleto:

1. **Setup do projeto Supabase** (via MCP): criar enums, tabelas, índices, RLS, funções, triggers, buckets.
2. **Dependências Android**: adicionar `supabase-kt` (auth, postgrest, realtime, storage), Credential Manager Google.
3. **`SupabaseClient` singleton** em `RodapeApp` lendo URL/anon-key do `BuildConfig` (já temos plugin Secrets).
4. **Camada `data/remote/`** com 11 repos novos.
5. **`MainViewModel` refatorado**: usa novos repos via injeção manual; remove `seedDatabase`, `seedNewClubData`, lista de demo users, lógica de auto-login.
6. **Telas de Auth**: reescrever `LoginScreen`, criar `SignUpScreen`, adicionar fluxo de reset.
7. **Telas existentes**: nenhuma muda visualmente, só trocam a fonte de dados (Flow vem do novo repo).
8. **Upload de capa manual**: `AddBookManualScreen` envia bytes pro `book-covers` em vez de gravar `file://` local.
9. **Apagar Room**: remover `AppDatabase`, `RodapeDao`, `RodapeRepository`, e a entrada `androidx.room.*` do gradle.
10. **Apagar DataStore de sessão**: manter só `font_scale` (migra pra `profiles.font_scale`) e flags de engagement.
11. **Testes**: smoke test do fluxo login → criar clube → sugerir livro → votar → fechar round → marcar encontro → comentar → reagir.

## Decisões abertas (preciso confirmar com você)

> **FAQ** — itens marcados aqui precisam de resposta sua antes de eu sair criando tabelas/escrevendo código.

1. **Votação: 1 voto por round ou múltiplos votos (até `n_livros`)?**
   Olhei o código atual: `Vote` tem PK `(clubBookId, userId)` o que sugere "1 voto por livro por usuário" mas sem limite global de N livros. Na prática hoje o app permite "votar e desvotar" num livro. Quero confirmar: cada pessoa **vota em 1 livro só** por round, ou pode marcar até `n_livros` favoritos?

2. **Comentários removidos: membros veem com texto borrado, ou some?**
   Hoje há flag `removido`. O ModerationLogScreen mostra strikethrough. Manter assim na UI ou esconder do feed pros não-admins?

3. **Catálogo `books`: global ou por clube?**
   Estou propondo global (1 "Hora da Estrela" pra todo mundo, ligado via `club_books`). Vantagem: deduplica. Desvantagem: livros manuais ficam visíveis em queries globais (não em UI, mas em SQL). Confirma?

4. **`auth.users` email confirmation**: desabilitado na primeira versão (atrito menor) — concorda?

5. **Notificações push**: confirma que ficam pra outra fase (sem FCM agora)?

## Não-objetivos

- Não vamos refatorar UI (visual e UX permanecem idênticos).
- Não vamos refatorar lógica de votação/admin além do necessário pra trocar a fonte de dados.
- Não vamos suportar offline na primeira versão.
- Não vamos migrar dados antigos (não há base).
