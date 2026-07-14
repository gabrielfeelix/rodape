-- 0010_moderation.sql — Moderação de UGC (denúncia + bloqueio + remoção)
-- Requisito de loja (Google Play UGC policy / Apple 1.2): denunciar conteúdo,
-- bloquear usuário, e admin poder agir sobre denúncias.
--
-- Convenções seguidas do schema existente:
--   * helpers is_club_member/is_club_admin/is_club_super(target_club uuid)
--   * RLS "TO authenticated", auth.uid() envolto em ( SELECT ... )
--   * SECURITY DEFINER com SET search_path TO 'public','pg_temp'
--   * comments já tinha removido/removido_por/motivo_remocao — espelhado aqui.

-- ─────────────────────────────────────────────────────────────────────────────
-- 1. Enums
-- ─────────────────────────────────────────────────────────────────────────────
do $$ begin
  create type public.report_target_type as enum
    ('comment', 'saved_quote', 'book_rating', 'book_suggestion', 'profile', 'reaction');
exception when duplicate_object then null; end $$;

do $$ begin
  create type public.report_reason as enum
    ('spam', 'assedio', 'abuso', 'conteudo_improprio', 'discurso_odio', 'outro');
exception when duplicate_object then null; end $$;

do $$ begin
  create type public.report_status as enum ('pendente', 'resolvido', 'descartado');
exception when duplicate_object then null; end $$;

-- ─────────────────────────────────────────────────────────────────────────────
-- 2. Colunas de moderação (espelham comments) nas demais tabelas UGC visíveis
-- ─────────────────────────────────────────────────────────────────────────────
alter table public.saved_quotes
  add column if not exists removido boolean not null default false,
  add column if not exists removido_por uuid references public.profiles(id) on delete set null,
  add column if not exists motivo_remocao text;

alter table public.book_ratings
  add column if not exists removido boolean not null default false,
  add column if not exists removido_por uuid references public.profiles(id) on delete set null,
  add column if not exists motivo_remocao text;

alter table public.book_suggestions
  add column if not exists removido boolean not null default false,
  add column if not exists removido_por uuid references public.profiles(id) on delete set null,
  add column if not exists motivo_remocao text;

-- ─────────────────────────────────────────────────────────────────────────────
-- 3. Tabela content_reports
-- ─────────────────────────────────────────────────────────────────────────────
create table if not exists public.content_reports (
  id             uuid not null default gen_random_uuid(),
  reporter_id    uuid not null,
  club_id        uuid not null,
  target_type    public.report_target_type not null,
  target_id      uuid not null,               -- id da linha (ou book_id p/ book_rating)
  target_user_id uuid not null,               -- autor denunciado (denormalizado)
  motivo         public.report_reason not null,
  detalhe        text,
  status         public.report_status not null default 'pendente',
  resolved_by    uuid,
  resolved_at    timestamptz,
  created_at     timestamptz not null default now(),
  constraint content_reports_pkey primary key (id),
  constraint content_reports_reporter_fkey    foreign key (reporter_id)    references public.profiles(id) on delete cascade,
  constraint content_reports_club_fkey        foreign key (club_id)        references public.clubs(id)    on delete cascade,
  constraint content_reports_target_user_fkey foreign key (target_user_id) references public.profiles(id) on delete cascade,
  constraint content_reports_resolved_by_fkey foreign key (resolved_by)    references public.profiles(id) on delete set null,
  constraint content_reports_detalhe_check    check (detalhe is null or char_length(detalhe) <= 1000),
  -- 1 denúncia por usuário por item (anti-flood); reenvio é no-op
  constraint content_reports_unique_reporter_target unique (reporter_id, target_type, target_id)
);

create index if not exists content_reports_club_status_idx
  on public.content_reports (club_id, status);
create index if not exists content_reports_target_idx
  on public.content_reports (target_type, target_id);

alter table public.content_reports enable row level security;

create policy "content_reports insert member"
  on public.content_reports for insert to authenticated
  with check (
    (reporter_id = ( select auth.uid() ))
    and ( select is_club_member(content_reports.club_id) )
  );

create policy "content_reports select admin or reporter"
  on public.content_reports for select to authenticated
  using (
    (reporter_id = ( select auth.uid() ))
    or ( select is_club_admin(content_reports.club_id) )
  );

create policy "content_reports update admin"
  on public.content_reports for update to authenticated
  using (( select is_club_admin(content_reports.club_id) ))
  with check (( select is_club_admin(content_reports.club_id) ));

-- ─────────────────────────────────────────────────────────────────────────────
-- 4. Tabela user_blocks (bloqueio global entre usuários)
-- ─────────────────────────────────────────────────────────────────────────────
create table if not exists public.user_blocks (
  blocker_id uuid not null,
  blocked_id uuid not null,
  created_at timestamptz not null default now(),
  constraint user_blocks_pkey primary key (blocker_id, blocked_id),
  constraint user_blocks_blocker_fkey foreign key (blocker_id) references public.profiles(id) on delete cascade,
  constraint user_blocks_blocked_fkey foreign key (blocked_id) references public.profiles(id) on delete cascade,
  constraint user_blocks_not_self check (blocker_id <> blocked_id)
);

create index if not exists user_blocks_blocked_idx on public.user_blocks (blocked_id);

alter table public.user_blocks enable row level security;

create policy "user_blocks select own"
  on public.user_blocks for select to authenticated
  using (blocker_id = ( select auth.uid() ));

create policy "user_blocks insert own"
  on public.user_blocks for insert to authenticated
  with check (blocker_id = ( select auth.uid() ));

create policy "user_blocks delete own"
  on public.user_blocks for delete to authenticated
  using (blocker_id = ( select auth.uid() ));

-- ─────────────────────────────────────────────────────────────────────────────
-- 5. Helper is_blocked (bidirecional — esconde conteúdo nos dois sentidos)
-- ─────────────────────────────────────────────────────────────────────────────
create or replace function public.is_blocked(a uuid, b uuid)
returns boolean
language sql stable security definer
set search_path to 'public', 'pg_temp'
as $$
  select exists (
    select 1 from public.user_blocks
    where (blocker_id = a and blocked_id = b)
       or (blocker_id = b and blocked_id = a)
  );
$$;

-- ─────────────────────────────────────────────────────────────────────────────
-- 6. Remoção por admin (centraliza o check de permissão) + resolve denúncias
-- ─────────────────────────────────────────────────────────────────────────────
create or replace function public.moderate_remove_content(
  p_type           public.report_target_type,
  p_target_id      uuid,
  p_target_user_id uuid,
  p_club_id        uuid,
  p_motivo         text default null
)
returns void
language plpgsql security definer
set search_path to 'public', 'pg_temp'
as $$
declare
  v_uid uuid := auth.uid();
begin
  if v_uid is null then
    raise exception 'Usuario nao autenticado' using errcode = '42501';
  end if;
  if not public.is_club_admin(p_club_id) then
    raise exception 'Apenas admin pode moderar' using errcode = '42501';
  end if;

  if p_type = 'comment' then
    update public.comments
      set removido = true, removido_por = v_uid, motivo_remocao = p_motivo
      where id = p_target_id and club_id = p_club_id;
  elsif p_type = 'saved_quote' then
    update public.saved_quotes
      set removido = true, removido_por = v_uid, motivo_remocao = p_motivo
      where id = p_target_id and club_id = p_club_id;
  elsif p_type = 'book_suggestion' then
    update public.book_suggestions
      set removido = true, removido_por = v_uid, motivo_remocao = p_motivo
      where id = p_target_id and club_id = p_club_id;
  elsif p_type = 'book_rating' then
    -- book_ratings tem PK composta: p_target_id = book_id
    update public.book_ratings
      set removido = true, removido_por = v_uid, motivo_remocao = p_motivo
      where book_id = p_target_id and club_id = p_club_id and user_id = p_target_user_id;
  else
    raise exception 'Tipo nao suportado para remocao: %', p_type;
  end if;

  update public.content_reports
    set status = 'resolvido', resolved_by = v_uid, resolved_at = now()
    where target_type = p_type and target_id = p_target_id and status = 'pendente';
end;
$$;

-- Descartar denúncia sem remover conteúdo (admin marca como improcedente)
create or replace function public.dismiss_report(p_report_id uuid)
returns void
language plpgsql security definer
set search_path to 'public', 'pg_temp'
as $$
declare
  v_uid uuid := auth.uid();
  v_club uuid;
begin
  select club_id into v_club from public.content_reports where id = p_report_id;
  if v_club is null then
    raise exception 'Denuncia inexistente';
  end if;
  if not public.is_club_admin(v_club) then
    raise exception 'Apenas admin pode descartar' using errcode = '42501';
  end if;
  update public.content_reports
    set status = 'descartado', resolved_by = v_uid, resolved_at = now()
    where id = p_report_id;
end;
$$;

-- ─────────────────────────────────────────────────────────────────────────────
-- 7. Admin pode ler o conteúdo removido (comments já cobria via update policy;
--    aqui garantimos SELECT amplo pra fila de moderação nas 3 novas tabelas
--    manter simples: SELECT já é "membros do clube", e removido é filtrado no
--    cliente. Nada a alterar nas policies de SELECT — remoção usa a função.)
-- ─────────────────────────────────────────────────────────────────────────────

comment on table public.content_reports is 'Denúncias de conteúdo (moderação UGC). Ação em <=24h via app.';
comment on table public.user_blocks is 'Bloqueio entre usuários; conteúdo escondido bidirecionalmente no cliente.';
