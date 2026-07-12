-- Migration 0004 — 2026-07-12
-- Fecha rodadas de votação expiradas e envia lembretes de encontro via pg_cron.
--
-- Problema: auto_close_expired_rounds existia mas (a) nunca era agendado e (b)
-- chamava close_voting_round, que exige auth.uid() de admin — via cron não há
-- usuário logado, então falharia. Solução: núcleo _do_close_round SEM checagem de
-- auth (system-level), usado tanto pela RPC (com checagem) quanto pelo cron.

create extension if not exists pg_cron;

-- ------------------------------------------------------------
-- Núcleo do fechamento (sem auth). SECURITY DEFINER.
-- ------------------------------------------------------------
create or replace function public._do_close_round(p_round_id uuid)
 returns voting_rounds language plpgsql security definer set search_path to 'public','pg_temp'
as $function$
declare
  v_round public.voting_rounds;
  v_winners jsonb;
  v_winner_ids uuid[];
  v_winner_id uuid;
  v_i int := 0;
  v_titulos jsonb;
  v_club_name text;
begin
  select * into v_round from public.voting_rounds where id=p_round_id;
  if v_round is null then raise exception 'Round nao encontrado' using errcode='P0002'; end if;
  if v_round.status='fechada' then return v_round; end if;

  with tally as (
    select v.book_id, count(*) as votos, min(bs.created_at) as primeira_sug
    from public.votes v
    left join public.book_suggestions bs on bs.voting_round_id=v.voting_round_id and bs.book_id=v.book_id
    where v.voting_round_id=p_round_id group by v.book_id),
  ranked as (
    select book_id, row_number() over (order by votos desc, primeira_sug asc nulls last) as rank, votos from tally)
  select jsonb_agg(jsonb_build_object('book_id', book_id, 'votos', votos) order by rank)
    into v_winners from ranked where rank <= v_round.n_livros;

  update public.voting_rounds set status='fechada', fechada_em=now(), vencedores=coalesce(v_winners,'[]'::jsonb)
    where id=p_round_id returning * into v_round;

  select nome into v_club_name from public.clubs where id=v_round.club_id;

  if v_winners is not null and jsonb_array_length(v_winners) > 0 then
    select array_agg((elem->>'book_id')::uuid order by ordinality) into v_winner_ids
      from jsonb_array_elements(v_winners) with ordinality as t(elem, ordinality);
    update public.club_books set status='finished', data_encontro=now() where club_id=v_round.club_id and status='current';
    foreach v_winner_id in array v_winner_ids loop
      v_i := v_i + 1;
      if v_i = 1 then
        insert into public.club_books (club_id, book_id, status, ordem) values (v_round.club_id, v_winner_id, 'current', 0)
          on conflict (club_id, book_id) do update set status='current', ordem=0, updated_at=now();
      else
        insert into public.club_books (club_id, book_id, status, ordem) values (v_round.club_id, v_winner_id, 'next', v_i)
          on conflict (club_id, book_id) do update set status='next', ordem=excluded.ordem, updated_at=now();
      end if;
    end loop;
    select jsonb_agg(b.title order by arr.ord) into v_titulos
      from unnest(v_winner_ids) with ordinality as arr(id, ord)
      join public.books b on b.id = arr.id;
  end if;

  insert into public.notifications (user_id, club_id, tipo, payload)
    select cm.user_id, v_round.club_id, 'voting_closed', jsonb_build_object(
             'round_id', p_round_id, 'clubName', v_club_name, 'titulos', coalesce(v_titulos,'[]'::jsonb))
    from public.club_members cm where cm.club_id=v_round.club_id;

  if v_winner_ids is not null and array_length(v_winner_ids,1) > 0 then
    insert into public.notifications (user_id, club_id, tipo, payload)
      select cm.user_id, v_round.club_id, 'next_book_decided', jsonb_build_object(
               'book_id', v_winner_ids[1], 'clubName', v_club_name,
               'bookTitle', (select title from public.books where id=v_winner_ids[1]))
      from public.club_members cm where cm.club_id=v_round.club_id;
  end if;

  return v_round;
end; $function$;

-- ------------------------------------------------------------
-- RPC pública: checa auth + admin, delega ao núcleo.
-- ------------------------------------------------------------
create or replace function public.close_voting_round(p_round_id uuid)
 returns voting_rounds language plpgsql security definer set search_path to 'public','pg_temp'
as $function$
declare v_uid uuid := auth.uid(); v_round public.voting_rounds;
begin
  if v_uid is null then raise exception 'Nao autenticado' using errcode='42501'; end if;
  select * into v_round from public.voting_rounds where id=p_round_id;
  if v_round is null then raise exception 'Round nao encontrado' using errcode='P0002'; end if;
  if not exists (select 1 from public.club_members where club_id=v_round.club_id and user_id=v_uid and papel in ('admin','super_admin'))
    then raise exception 'So admin pode fechar round' using errcode='42501'; end if;
  return public._do_close_round(p_round_id);
end; $function$;

-- ------------------------------------------------------------
-- Cron: fecha rodadas expiradas (sem auth, via núcleo).
-- ------------------------------------------------------------
create or replace function public.auto_close_expired_rounds()
 returns integer language plpgsql security definer set search_path to 'public','pg_temp'
as $function$
declare v_round_id uuid; v_count int := 0;
begin
  for v_round_id in
    select id from public.voting_rounds where status='aberta' and fecha_em <= now()
  loop
    perform public._do_close_round(v_round_id);
    v_count := v_count + 1;
  end loop;
  return v_count;
end; $function$;

-- ------------------------------------------------------------
-- Lembretes de encontro (24h antes), dedup por (user, meeting).
-- ------------------------------------------------------------
create or replace function public.send_meeting_reminders()
 returns integer language plpgsql security definer set search_path to 'public'
as $function$
declare v_count int := 0;
begin
  insert into public.notifications (user_id, club_id, tipo, payload)
  select cm.user_id, m.club_id, 'meeting_reminder', jsonb_build_object(
           'meetingId', m.id,
           'clubName', (select nome from public.clubs where id=m.club_id),
           'data', to_char(m.data, 'YYYY-MM-DD"T"HH24:MI:SSOF'))
  from public.meetings m
  join public.club_members cm on cm.club_id = m.club_id
  where m.status='agendado'
    and m.data > now() and m.data <= now() + interval '24 hours'
    and not exists (
      select 1 from public.notifications n
      where n.user_id = cm.user_id and n.tipo='meeting_reminder'
        and n.payload->>'meetingId' = m.id::text);
  get diagnostics v_count = row_count;
  return v_count;
end; $function$;

-- ------------------------------------------------------------
-- Agenda os jobs (idempotente).
-- ------------------------------------------------------------
do $$ begin perform cron.unschedule('rodape-auto-close-rounds'); exception when others then null; end $$;
do $$ begin perform cron.unschedule('rodape-meeting-reminders'); exception when others then null; end $$;
select cron.schedule('rodape-auto-close-rounds', '*/15 * * * *', $$select public.auto_close_expired_rounds();$$);
select cron.schedule('rodape-meeting-reminders', '5 * * * *', $$select public.send_meeting_reminders();$$);
