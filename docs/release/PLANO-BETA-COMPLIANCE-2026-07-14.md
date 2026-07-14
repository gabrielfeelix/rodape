# Plano de execução — Compliance & operação para beta pública

**Data:** 2026-07-14 · **Contexto:** app entra em beta (sem monetização por ~1 mês,
depois assinatura). Objetivo: passar na revisão da Play, operar como empresa, sem
gastar em tier pago agora. Cobre moderação (denúncia/bloqueio/remoção), Termos/EULA,
Crashlytics, In-App Review, age gate, auditoria RLS, SMTP grátis e prontidão de
Play Console.

> **Achados que mudaram o plano** (recon no código real):
> - Schema versionado em `supabase/schema/` (tables/policies/functions/triggers) +
>   `supabase/migrations/` (até `0009`). RLS = **89 policies**, helpers
>   `is_club_member` / `is_club_admin` / `is_club_super` / `club_role`.
> - `comments` **já tem** `removido` / `removido_por` / `motivo_remocao` — infra de
>   moderação parcial já existe. Padrão a espelhar nas outras tabelas UGC.
> - Roles: `member` / `admin` / `super_admin` (`club_members.papel`).
> - Firebase já integrado (só messaging) → Crashlytics pluga barato.
> - `CrashLogger` já persiste stack traces locais e foi desenhado pra "Exportar logs".
> - SMTP/Resend já documentado em `supabase-auth-setup.md` (falta só executar dashboard).
> - UGC real: `comments` (chat/capítulo), `saved_quotes`, `meeting_notes`,
>   `book_suggestions`, `reactions`, `profiles.nome` (texto livre 60 chars).

---

## Ordem de execução (calendário)

| Onda | O quê | Bloqueia? | Quem |
|---|---|---|---|
| **0 — hoje** | Abrir teste fechado (12 testers/14 dias) · criar conta Resend | Calendário | Você (dashboard) |
| **1 — código** | W1 Moderação · W2 Termos · W3 Crashlytics · W4 In-App Review · W5 Age gate | Revisão Play | Eu |
| **2 — código+forms** | W6 Auditoria RLS · W8 Data Safety/IARC/listing | Revisão Play | Eu (draft) + você (submit) |
| **3 — release** | Promover pra produção após 14 dias de teste | — | Você |

Onda 0 roda em paralelo com Onda 1 (é gargalo de tempo, não de código).

---

## W1 — Moderação de UGC (denúncia + bloqueio + remoção) 🔴 P0

O maior furo. Play/Apple exigem, pra app com conteúdo de usuário: denunciar, bloquear,
EULA com tolerância zero, e agir sobre denúncia.

### W1.1 — Banco (`supabase/migrations/0010_moderation.sql`)
- **`content_reports`**: `id`, `reporter_id→profiles`, `club_id→clubs`,
  `target_type` (enum: `comment|quote|meeting_note|book_suggestion|profile|reaction`),
  `target_id uuid`, `motivo` (enum: `spam|abuso|assedio|conteudo_improprio|outro`),
  `detalhe text(≤1000)`, `status` (enum: `pendente|resolvido|descartado`),
  `resolved_by`, `resolved_at`, `created_at`.
- **`user_blocks`**: `blocker_id`, `blocked_id`, `created_at`, PK(blocker, blocked).
- **Espelhar moderação** (`removido`/`removido_por`/`motivo_remocao`) em
  `saved_quotes`, `meeting_notes`, `book_suggestions`, `reactions` (comments já tem).
- **Helper** `is_blocked(a uuid, b uuid)` + fn `report_content(...)` e
  `set_content_removed(...)` (SECURITY DEFINER, `search_path` pinado).
- **RLS**:
  - `content_reports`: INSERT se `is_club_member(club_id)`; SELECT/UPDATE se
    `is_club_admin(club_id)`.
  - `user_blocks`: dono gerencia os próprios (`blocker_id = auth.uid()`).
  - Ajustar SELECT das tabelas UGC: esconder linhas onde
    `removido = true` (exceto pra autor/admin) **e** onde autor está na minha lista
    de bloqueio (`NOT is_blocked(auth.uid(), user_id)`).

### W1.2 — Camada de dados (Kotlin)
- `data/model/Entities.kt`: `ContentReport`, `UserBlock` (+ campos `removido` nas
  entidades espelhadas).
- `RemoteRepository`: `reportContent(...)`, `blockUser(id)`, `unblockUser(id)`,
  `removeContent(type,id,motivo)` (admin), `pendingReports(clubId)` (admin),
  `resolveReport(...)`.
- `RodapeDao` + `PendingMutation`: cache local de **meus bloqueios** (filtro offline)
  e enfileirar report/block quando offline (segue padrão da fila existente).
- `MainViewModel`: expor `blockedUserIds` (Flow) e filtrar listas de UGC por ele.

### W1.3 — UI (pontos de inserção confirmados)

> **Já existe** (lado admin): remoção de comentário em `DiscussionScreen.kt:583-608`
> ("Remover (moderação)") + `ModerationLogScreen.kt` (log de removidos) + DAO
> `markCommentRemoved`/`Restored`. Falta o **lado do usuário** (denunciar/bloquear) e
> estender remoção às outras tabelas.

- **`Denunciar`** (menu do item, caminho não-próprio) em:
  - `DiscussionScreen.kt:413-640` — comentário do chat (encaixa no menu já existente).
  - `BookDetailScreen.kt:606-614` (comentário de capítulo), `:742-775` (**resenha** =
    `BookRating.comment`, texto livre), `:507-525` (citação).
  - `SuggestScreen.kt` / autor da sugestão em `BookDetailScreen.kt:848`.
- **`Bloquear usuário`** (exceto próprio): no avatar/nome de qualquer autor + no sheet
  de membro `ManageClubScreen.kt:655-694`.
- **`Remover`** (admin): estender o padrão de `DiscussionScreen` a citação/resenha/
  sugestão/nota (espelha `markCommentRemoved`).
- **`ReportSheet`**: motivo + detalhe opcional. **`BlockConfirmDialog`**: explica efeito.
- **Fila de moderação** (`ModerationQueueScreen`, novo): denúncias `pendente` dos meus
  clubes; remover/descartar. Cumpre "agir em 24h". Vizinho do `ModerationLogScreen`.
- **Filtrar bloqueados**: `FrasesScreen` mostra só citações próprias (`MainViewModel.kt:251`)
  → sem report ali; aplicar filtro de `blockedUserIds` nas listas cross-user.
- **Gate de EULA no cadastro**: checkbox "Li e aceito os Termos e a Política" (liga W2).

> **Room**: nova entidade em `Entities.kt` + bump `AppDatabase` (`MIGRATION_n`) + DAO +
> DTO `@Serializable` + `from("...").upsert` + `registerHandler(...)` na fila offline
> (`RemoteRepository.kt:959+`, padrão `insert_comment`/`delete_reaction`).

**Entrega:** denúncia + bloqueio + remoção admin + fila. Satisfaz política UGC.

---

## W2 — Termos de Uso / EULA 🔴 P0

- `docs/legal/termos-de-uso.md` (PT-BR): regras de conduta, **cláusula de tolerância
  zero a conteúdo abusivo/ofensivo**, licença de uso, isenção de responsabilidade,
  conteúdo de terceiros (livros/APIs), encerramento de conta, foro. Inclui as
  cláusulas padrão de EULA que a Apple exige (boa prática mesmo no Android).
- Hospedar junto do privacy (GitHub Pages ou `rodape.app/termos`). Linkar em: listing
  da Play, Ajustes › Sobre no app, checkbox do cadastro.
- Atualizar `privacy-policy.md`: mencionar moderação, bloqueio, retenção de denúncias,
  "sem publicidade / sem rastreio de terceiros".

---

## W3 — Crashlytics + observabilidade 🟡 P1

- Deps: `firebase-crashlytics` + plugin gradle `com.google.firebase.crashlytics`
  (google-services já aplicado).
- Manter `CrashLogger` local (offline/export) **e** encaminhar o throwable pro
  Crashlytics no handler.
- Init em `RodapeApp`. `recordException` nos `catch` de rede/repo (não-fatais).
- **In-app "Reportar bug"** (Ajustes): tela que abre email pra `feedback@rodape.app`
  com os `.txt` do CrashLogger anexados (usa `ShareImage`/`AppIntents` já existentes).
- Android Vitals (crash/ANR) vem de graça no Play Console assim que publicar.

---

## W4 — Play In-App Review 🟡 P1

- Dep `com.google.android.play:review-ktx`. (Hoje `RatingStars` é nota de **livro**,
  não review da Play.)
- Disparar `ReviewManager` num **momento bom**: após fechar encontro / marco 100% /
  N-ésima abertura. Flag no DataStore pra não repetir. Google já limita frequência.

---

## W5 — Age gate + criança (LGPD/COPPA) 🟡 P1

- Confirmar/hardenizar §9 da privacy. No cadastro, confirmar idade mínima
  (≥13, ou ≥16 p/ LGPD-criança). Bloquear abaixo. Guardar mínimo (ex.: ano de
  nascimento ou flag). Declarar no Data Safety + "Público-alvo" da Play.

---

## W6 — Auditoria de RLS 🟡 P1

RLS é a **única** autorização (cliente usa publishable key). Um policy frouxo vaza
clube privado inteiro.

- Ler as 89 policies + `functions.sql`. Checar por tabela: RLS habilitado (idealmente
  `FORCE`), sem SELECT que ignore `club_id`, sem `USING (true)` indevido.
- `SECURITY DEFINER` com `search_path` pinado (anti-hijack); `service_role` nunca no
  cliente (confirmado: só publishable).
- Testes negativos: membro do clube A não lê/escreve dados do clube B; não-membro não
  lê nada; não-admin não modera.
- Saída: relatório + `0011_rls_fixes.sql` se houver buraco.

---

## W7 — SMTP de produção (Resend free) 🔴 P0 · *já documentado*

- Free tier Resend = 100 emails/dia — suficiente pra beta. Sem custo.
- Config no dashboard Supabase (passo-a-passo em `supabase-auth-setup.md`): SMTP custom
  + DNS (SPF/DKIM) do domínio. **Ação sua.**
- Eu: revisar/branding dos templates (reset de senha, confirmação) em PT-BR.
- Sem isso, reset de senha **falha** em produção (rate limit do SMTP default).

---

## W8 — Prontidão Play Console 🔴 P0 (processo)

- **Teste fechado**: conta pessoal nova exige **12 testers × 14 dias** antes de
  produção. **Abrir na Onda 0.**
- **Data Safety**: eu redijo as respostas exatas batendo com a privacy policy (coleta:
  email, nome, conteúdo de clube; sem ads; sem rastreio). Você submete.
- **Classificação IARC**: eu redijo (UGC com moderação, sem violência/sexo). Você submete.
- **Listing**: eu escrevo descrição curta/longa PT-BR; ícone/feature graphic/screenshots
  = design/humano.
- Técnico ok: `targetSdk 36`, `minSdk 24`, Play App Signing, AAB assinado (já no repo).

---

## Ações que só você faz (dashboards/humano)

1. Abrir **teste fechado** na Play + recrutar 12 testers (Onda 0).
2. Criar conta **Resend** + DNS do domínio (Onda 0).
3. Submeter **Data Safety / IARC / listing** com meus drafts (Onda 2).
4. **MEI/CNPJ** — só quando for cobrar (não agora).
5. (Opcional) revisão dos Termos por advogado — eu entrego o draft.

## O que eu entrego (código + drafts)

W1 moderação completa · W2 Termos + update privacy · W3 Crashlytics + bug report ·
W4 In-App Review · W5 age gate · W6 auditoria+fix RLS · W7 templates email ·
W8 textos de Data Safety/IARC/listing.

---

## Sequência sugerida de implementação (Onda 1 → 2)

1. `0010_moderation.sql` (banco) → data layer → UI de report/block → fila admin.
2. Termos + privacy update + gate de cadastro.
3. Crashlytics + "Reportar bug".
4. In-App Review.
5. Age gate.
6. Auditoria RLS + `0011_rls_fixes.sql`.
7. Drafts de Play Console (Data Safety/IARC/listing) + templates Resend.
