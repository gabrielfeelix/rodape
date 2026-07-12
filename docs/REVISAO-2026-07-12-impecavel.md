# Revisão "impecável" — 2026-07-12

Auditoria de 30 personas (8 agentes em paralelo, um por cluster de fluxo) + auditoria
do banco Supabase ao vivo (Management API). Objetivo: caçar TODO bug real de
fluxo/estado/runtime e gaps de UX/acessibilidade, corrigir front + back + DB.

Baseline: o app **compilava** e os testes unitários passavam — os bugs eram de
runtime/estado, não de compilação. Backend confirmado sólido (RLS em 22 tabelas,
RPCs SECURITY DEFINER com search_path, invariante super_admin como CONSTRAINT
TRIGGER DEFERRED). A fonte do "muito bugado" estava na camada do app + alguns gaps
de backend (payloads pobres, sem cron).

## P0 corrigidos (crash / perda de dados)

1. **Room sem Migration** — `@Database(version=2)` sem migration; fallback destrutivo
   só em DEBUG → crash no upgrade. Adicionadas `MIGRATION_1_2` (pending_mutations) e
   `MIGRATION_2_3` (PK de votes + coluna dataEpoch). Version → 3.
2. **PK local de `votes`** — omitia `votingRoundId`; votos de rodadas diferentes
   colapsavam/sumiam. PK agora `(votingRoundId, clubBookId, userId)`.
3. **Editar capítulos apagava TODA a discussão** — `upsertChapters` fazia
   delete-all+reinsert e o `ON DELETE CASCADE` levava comentários+reações. Agora é
   DIFF (`saveChapters`): upsert in-place, deleta só os capítulos removidos.

## Data-layer / sync / realtime

- **Vazamento de Realtime** — o guard de liveness checava o job de setup (que morria
  após `subscribe()`), então cada revisita criava um canal novo; reloaders num
  `CopyOnWriteArraySet` cresciam sem limite. Agora o Job rastreado COLETA o flow
  (liveness real) e reloaders são indexados por (table,filter). `close()` cancela
  tudo; `MainViewModel.onCleared()` e o worker chamam.
- **Fila offline** — drain com mutex (sem drain duplicado), para na 1ª falha
  transitória (preserva ordem/dependências), 408/429 tratados como transitórios,
  drain ANTES do signOut, troca de conta limpa SEM drenar (não reenvia sob a sessão
  errada). Novos handlers: perfil, voto (delete), comentário editar/apagar/moderar,
  notificação lida.
- **Optimistic/local-first** onde faltava: perfil, desfazer voto, moderar comentário,
  marcar notificação lida, padrão de encontro.
- **Timezone dos encontros** — util `MeetingTime` centraliza tudo no fuso local
  (antes gravava hora local como UTC). Encontros guardam `dataEpoch` (instante real)
  e ordenam por ele; "próximo encontro" = agendado futuro mais próximo.
- **Recorrência gera encontros de verdade** — `generateMeetingsFromPattern` cria as
  próximas 8 ocorrências (semanal/quinzenal/mensal_dia_semana/mensal_dia_mes/
  personalizado), idempotente por id de data. Matemática coberta por testes.
- **Clubes escopados por membership** — parou de baixar clubes públicos que o usuário
  nunca entrou (poluíam switcher/empty-state/clube-ativo).
- **RPCs propagam erro** — sair/entrar/remover/regenerar não engolem mais a exceção
  (mensagem específica em vez de "sucesso" falso ou texto genérico).
- **`lastActiveClubId`** persistido em create/join/leave/archive; papel reseta ao
  trocar de clube (guards de admin não misfire).

## Backend (migrations 0003 / 0004)

- **0003** — payloads de notificação enriquecidos (actorName/clubName/bookTitle/
  titulos em vez de UUID cru); triggers novos para `comment_on_chapter` (avisa quem
  já leu o capítulo), `voting_open`, `member_finished`; policy `comments delete self`.
- **0004** — `pg_cron` habilitado; núcleo `_do_close_round` (sem auth) usado pela RPC
  (com auth) e pelo cron; job `rodape-auto-close-rounds` (*/15) fecha votações
  expiradas; `rodape-meeting-reminders` (hora em hora) envia lembrete 24h antes.

## Telas (fluxo + acessibilidade)

Notificações (parse por JSON real), discussão (editar/apagar próprio comentário,
limite 4000, spoiler espera progress), capítulos (confirmação + draft estável),
encontros (DatePicker sem off-by-one, TimePicker, loading gates, RSVP remember),
clubes (esconde "Remover" inválido, erros em snackbar, confirmar restaurar), auth
(deep-link de recovery one-shot, cancelar no reset, rememberSaveable, live regions,
IME, erros pt-BR do Google), MainTabs (nome vem do Room, BackHandler, estado de
tab/scroll preservado, 1º capítulo destravado, touch targets 48dp, semantics de
tab/avatar), sugerir/manual (parse OL robusto, estado de erro≠vazio, câmera com
settings-fallback), RatingStars (arredonda, selectableGroup).

## Testes

`MeetingTimeTest` cobre timezone (local≠UTC) e a matemática das 6 recorrências —
o ponto que antes "só falhava em runtime" agora é verificável estático.

## A confirmar no emulador (runtime visual)

Os fixes de recorrência/timezone e o ciclo de vida do Realtime são corretos por
código+teste, mas o comportamento visual final (encontros nas datas certas, canais
sem leak sob uso real) só se confirma com o app rodando.
