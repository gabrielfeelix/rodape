-- Migration 0003 — 2026-07-12
-- 1) Enriquece payloads de notificação com nomes legíveis (actorName/clubName/
--    bookTitle/titulos) em vez de UUIDs crus — o app renderizava "{" e textos
--    genéricos porque só tinha o id.
-- 2) Adiciona emissores server-side pros tipos que o app renderiza mas ninguém
--    criava: comment_on_chapter, voting_open, member_finished.
-- 3) Política pra o autor APAGAR o próprio comentário (a UI precisava e o RLS
--    não permitia — só admin via flag removido).

-- ============================================================
-- 1) RPCs com payload enriquecido
-- ============================================================

-- promote_member: + actorName + clubName
create or replace function public.promote_member(p_club_id uuid, p_target_user_id uuid)
 returns club_members language plpgsql security definer set search_path to 'public','pg_temp'
as $function$
declare
  v_uid uuid := auth.uid();
  v_target_role member_role;
  v_caller_role member_role;
  v_member public.club_members;
begin
  if v_uid is null then raise exception 'Nao autenticado' using errcode='42501'; end if;
  select papel into v_caller_role from public.club_members where club_id=p_club_id and user_id=v_uid;
  if v_caller_role not in ('admin','super_admin') then raise exception 'Sem permissao' using errcode='42501'; end if;
  select papel into v_target_role from public.club_members where club_id=p_club_id and user_id=p_target_user_id;
  if v_target_role is null then raise exception 'Alvo nao e membro' using errcode='P0002'; end if;
  if v_target_role='super_admin' then raise exception 'Alvo ja e super_admin' using errcode='23514'; end if;
  if v_target_role='admin' then raise exception 'Use transfer_super_admin para promover admin a super_admin' using errcode='42501'; end if;
  update public.club_members set papel='admin' where club_id=p_club_id and user_id=p_target_user_id returning * into v_member;
  insert into public.notifications (user_id, club_id, tipo, payload)
    values (p_target_user_id, p_club_id, 'promoted_to_admin', jsonb_build_object(
      'by', v_uid,
      'actorName', (select nome from public.profiles where id=v_uid),
      'clubName', (select nome from public.clubs where id=p_club_id)));
  return v_member;
end; $function$;

-- remove_member: + actorName + clubName
create or replace function public.remove_member(p_club_id uuid, p_target_user_id uuid, p_motivo text default null)
 returns void language plpgsql security definer set search_path to 'public','pg_temp'
as $function$
declare
  v_uid uuid := auth.uid();
  v_caller_role member_role;
  v_target_role member_role;
begin
  if v_uid is null then raise exception 'Nao autenticado' using errcode='42501'; end if;
  if v_uid=p_target_user_id then raise exception 'Nao remova a si mesmo; use leave_club' using errcode='42501'; end if;
  select papel into v_caller_role from public.club_members where club_id=p_club_id and user_id=v_uid;
  if v_caller_role not in ('admin','super_admin') then raise exception 'Sem permissao' using errcode='42501'; end if;
  select papel into v_target_role from public.club_members where club_id=p_club_id and user_id=p_target_user_id;
  if v_target_role is null then raise exception 'Alvo nao e membro' using errcode='P0002'; end if;
  if v_target_role='super_admin' then raise exception 'Super_admin nao pode ser removido' using errcode='42501'; end if;
  if v_target_role='admin' and v_caller_role<>'super_admin' then raise exception 'So super_admin remove admin' using errcode='42501'; end if;
  insert into public.member_removals (club_id, user_id, removed_by, motivo) values (p_club_id, p_target_user_id, v_uid, p_motivo);
  insert into public.notifications (user_id, club_id, tipo, payload)
    values (p_target_user_id, p_club_id, 'member_removed', jsonb_build_object(
      'by', v_uid, 'motivo', p_motivo,
      'actorName', (select nome from public.profiles where id=v_uid),
      'clubName', (select nome from public.clubs where id=p_club_id)));
  delete from public.club_members where club_id=p_club_id and user_id=p_target_user_id;
end; $function$;

-- transfer_super_admin: + actorName + clubName
create or replace function public.transfer_super_admin(p_club_id uuid, p_target_user_id uuid)
 returns void language plpgsql security definer set search_path to 'public','pg_temp'
as $function$
declare v_uid uuid := auth.uid();
begin
  if v_uid is null then raise exception 'Nao autenticado' using errcode='42501'; end if;
  if not exists (select 1 from public.club_members where club_id=p_club_id and user_id=v_uid and papel='super_admin')
    then raise exception 'So super_admin pode transferir' using errcode='42501'; end if;
  if not exists (select 1 from public.club_members where club_id=p_club_id and user_id=p_target_user_id and papel='admin')
    then raise exception 'Alvo precisa ser admin' using errcode='23514'; end if;
  update public.club_members set papel='admin' where club_id=p_club_id and user_id=v_uid;
  update public.club_members set papel='super_admin' where club_id=p_club_id and user_id=p_target_user_id;
  insert into public.notifications (user_id, club_id, tipo, payload)
    values (p_target_user_id, p_club_id, 'super_admin_transferred', jsonb_build_object(
      'from', v_uid,
      'actorName', (select nome from public.profiles where id=v_uid),
      'clubName', (select nome from public.clubs where id=p_club_id)));
end; $function$;

-- ============================================================
-- 2) Novos emissores de notificação (triggers)
-- ============================================================

-- comment_on_chapter: ao inserir comentário, avisa quem JÁ LEU aquele capítulo
-- (progresso >= numero do capítulo), exceto o autor. Relevante e sem spoiler.
create or replace function public.notify_comment_on_chapter()
 returns trigger language plpgsql security definer set search_path to 'public'
as $function$
declare
  v_numero int;
  v_book_id uuid;
  v_chap_title text;
  v_book_title text;
  v_author text;
begin
  select ch.numero, ch.book_id, ch.titulo into v_numero, v_book_id, v_chap_title
    from public.chapters ch where ch.id = new.chapter_id;
  if v_numero is null then return new; end if;
  select b.title into v_book_title from public.books b where b.id = v_book_id;
  select nome into v_author from public.profiles where id = new.user_id;
  begin
    insert into public.notifications (user_id, club_id, tipo, payload)
    select up.user_id, new.club_id, 'comment_on_chapter', jsonb_build_object(
             'actorName', v_author, 'chapterId', new.chapter_id,
             'chapterTitle', v_chap_title, 'bookTitle', v_book_title)
      from public.user_progress up
     where up.club_id = new.club_id
       and up.book_id = v_book_id
       and up.current_chapter >= v_numero
       and up.user_id <> new.user_id;
  exception when others then null; -- nunca bloqueia o comentário
  end;
  return new;
end; $function$;

drop trigger if exists comments_notify_on_chapter on public.comments;
create trigger comments_notify_on_chapter
  after insert on public.comments for each row execute function public.notify_comment_on_chapter();

-- voting_open: ao abrir rodada, avisa todos os membros.
create or replace function public.notify_voting_open()
 returns trigger language plpgsql security definer set search_path to 'public'
as $function$
begin
  begin
    insert into public.notifications (user_id, club_id, tipo, payload)
    select cm.user_id, new.club_id, 'voting_open', jsonb_build_object(
             'actorName', (select nome from public.profiles where id=new.criado_por),
             'clubName', (select nome from public.clubs where id=new.club_id),
             'roundId', new.id)
      from public.club_members cm where cm.club_id = new.club_id;
  exception when others then null;
  end;
  return new;
end; $function$;

drop trigger if exists voting_rounds_notify_open on public.voting_rounds;
create trigger voting_rounds_notify_open
  after insert on public.voting_rounds for each row execute function public.notify_voting_open();

-- member_finished: quando o progresso atinge o último capítulo do livro, avisa
-- os OUTROS membros. Guard: só dispara na transição pra "completou".
create or replace function public.notify_member_finished()
 returns trigger language plpgsql security definer set search_path to 'public'
as $function$
declare
  v_total int;
  v_book_title text;
  v_actor text;
begin
  select count(*) into v_total from public.chapters where book_id = new.book_id;
  if v_total = 0 then return new; end if;
  -- só na transição: antes < total e agora >= total
  if new.current_chapter >= v_total
     and (tg_op = 'INSERT' or coalesce(old.current_chapter, 0) < v_total) then
    select b.title into v_book_title from public.books b where b.id = new.book_id;
    select nome into v_actor from public.profiles where id = new.user_id;
    begin
      insert into public.notifications (user_id, club_id, tipo, payload)
      select cm.user_id, new.club_id, 'member_finished', jsonb_build_object(
               'actorName', v_actor, 'bookTitle', v_book_title)
        from public.club_members cm
       where cm.club_id = new.club_id and cm.user_id <> new.user_id;
    exception when others then null;
    end;
  end if;
  return new;
end; $function$;

drop trigger if exists user_progress_notify_finished on public.user_progress;
create trigger user_progress_notify_finished
  after insert or update of current_chapter on public.user_progress
  for each row execute function public.notify_member_finished();

-- close_voting_round: enriquece voting_closed (titulos) + next_book_decided (bookTitle).
create or replace function public.close_voting_round(p_round_id uuid)
 returns voting_rounds language plpgsql security definer set search_path to 'public','pg_temp'
as $function$
declare
  v_uid uuid := auth.uid();
  v_round public.voting_rounds;
  v_winners jsonb;
  v_winner_ids uuid[];
  v_winner_id uuid;
  v_i int := 0;
  v_titulos jsonb;
  v_club_name text;
begin
  if v_uid is null then raise exception 'Nao autenticado' using errcode='42501'; end if;
  select * into v_round from public.voting_rounds where id=p_round_id;
  if v_round is null then raise exception 'Round nao encontrado' using errcode='P0002'; end if;
  if not exists (select 1 from public.club_members where club_id=v_round.club_id and user_id=v_uid and papel in ('admin','super_admin'))
    then raise exception 'So admin pode fechar round' using errcode='42501'; end if;
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
    -- títulos dos vencedores (na ordem do ranking)
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

-- ============================================================
-- 3) Autor pode apagar o PRÓPRIO comentário (hard delete)
-- ============================================================
drop policy if exists "comments delete self" on public.comments;
create policy "comments delete self" on public.comments
  for delete using (user_id = (select auth.uid()));
