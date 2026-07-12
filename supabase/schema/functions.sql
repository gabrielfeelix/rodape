-- Snapshot das funções public (RPCs) — Management API, atualizado 2026-07-12

CREATE OR REPLACE FUNCTION public.auto_close_expired_rounds()
 RETURNS integer
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public', 'pg_temp'
AS $function$
declare
  v_round_id uuid;
  v_count int := 0;
begin
  for v_round_id in
    select id from public.voting_rounds
    where status = 'aberta' and fecha_em <= now()
  loop
    perform public.close_voting_round(v_round_id);
    v_count := v_count + 1;
  end loop;
  return v_count;
end;
$function$
;

CREATE OR REPLACE FUNCTION public.close_voting_round(p_round_id uuid)
 RETURNS voting_rounds
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public', 'pg_temp'
AS $function$
declare
  v_uid uuid := auth.uid();
  v_round public.voting_rounds;
  v_winners jsonb;
  v_winner_ids uuid[];
  v_winner_id uuid;
  v_i int := 0;
begin
  if v_uid is null then raise exception 'Nao autenticado' using errcode = '42501'; end if;

  select * into v_round from public.voting_rounds where id = p_round_id;
  if v_round is null then raise exception 'Round nao encontrado' using errcode = 'P0002'; end if;

  if not exists (
    select 1 from public.club_members
    where club_id = v_round.club_id and user_id = v_uid and papel in ('admin', 'super_admin')
  ) then
    raise exception 'So admin pode fechar round' using errcode = '42501';
  end if;

  if v_round.status = 'fechada' then
    return v_round;
  end if;

  -- Tally: agrupa votos por book, ordena por count desc, depois criado_em asc da sugestao
  with tally as (
    select v.book_id, count(*) as votos, min(bs.created_at) as primeira_sug
    from public.votes v
    left join public.book_suggestions bs
      on bs.voting_round_id = v.voting_round_id and bs.book_id = v.book_id
    where v.voting_round_id = p_round_id
    group by v.book_id
  ),
  ranked as (
    select book_id,
           row_number() over (order by votos desc, primeira_sug asc nulls last) as rank,
           votos
    from tally
  )
  select jsonb_agg(jsonb_build_object('book_id', book_id, 'votos', votos) order by rank)
    into v_winners
    from ranked
    where rank <= v_round.n_livros;

  -- Atualiza round
  update public.voting_rounds
    set status = 'fechada',
        fechada_em = now(),
        vencedores = coalesce(v_winners, '[]'::jsonb)
    where id = p_round_id
    returning * into v_round;

  -- Promove os livros vencedores: 1o vira current (move o antigo current pra finished),
  -- demais viram next
  if v_winners is not null and jsonb_array_length(v_winners) > 0 then
    select array_agg((elem->>'book_id')::uuid order by ordinality)
      into v_winner_ids
      from jsonb_array_elements(v_winners) with ordinality as t(elem, ordinality);

    -- Marca current atual como finished
    update public.club_books
      set status = 'finished', data_encontro = now()
      where club_id = v_round.club_id and status = 'current';

    foreach v_winner_id in array v_winner_ids loop
      v_i := v_i + 1;
      if v_i = 1 then
        -- 1o lugar vira current
        insert into public.club_books (club_id, book_id, status, ordem)
          values (v_round.club_id, v_winner_id, 'current', 0)
          on conflict (club_id, book_id)
          do update set status = 'current', ordem = 0, updated_at = now();
      else
        -- Demais viram next (em ordem)
        insert into public.club_books (club_id, book_id, status, ordem)
          values (v_round.club_id, v_winner_id, 'next', v_i)
          on conflict (club_id, book_id)
          do update set status = 'next', ordem = excluded.ordem, updated_at = now();
      end if;
    end loop;
  end if;

  -- Notifica todos os membros do clube
  insert into public.notifications (user_id, club_id, tipo, payload)
    select cm.user_id, v_round.club_id, 'voting_closed',
           jsonb_build_object('round_id', p_round_id, 'vencedores', coalesce(v_winners, '[]'::jsonb))
    from public.club_members cm
    where cm.club_id = v_round.club_id;

  -- Se houve vencedor, notifica especificamente 'next_book_decided'
  if v_winner_ids is not null and array_length(v_winner_ids, 1) > 0 then
    insert into public.notifications (user_id, club_id, tipo, payload)
      select cm.user_id, v_round.club_id, 'next_book_decided',
             jsonb_build_object('book_id', v_winner_ids[1])
      from public.club_members cm
      where cm.club_id = v_round.club_id;
  end if;

  return v_round;
end;
$function$
;

CREATE OR REPLACE FUNCTION public.club_role(target_club uuid)
 RETURNS member_role
 LANGUAGE sql
 STABLE SECURITY DEFINER
 SET search_path TO 'public', 'pg_temp'
AS $function$
  select papel from public.club_members
  where club_id = target_club and user_id = auth.uid();
$function$
;

CREATE OR REPLACE FUNCTION public.create_club(p_nome text, p_descricao text DEFAULT NULL::text, p_cor text DEFAULT '0'::text, p_privacidade club_privacy DEFAULT 'convidados'::club_privacy)
 RETURNS clubs
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public', 'pg_temp'
AS $function$
declare
  v_uid uuid := auth.uid();
  v_codigo text;
  v_club public.clubs;
begin
  if v_uid is null then
    raise exception 'Usuario nao autenticado' using errcode = '42501';
  end if;

  -- garante que profile existe (caso trigger de auth tenha falhado)
  insert into public.profiles (id, nome)
    values (v_uid, coalesce(
      (select coalesce(raw_user_meta_data->>'nome', split_part(email, '@', 1)) from auth.users where id = v_uid),
      'Usuario'
    ))
    on conflict (id) do nothing;

  v_codigo := public.generate_unique_invite_code();

  insert into public.clubs (nome, descricao, cor, privacidade, criador_id, codigo)
    values (p_nome, p_descricao, p_cor, p_privacidade, v_uid, v_codigo)
    returning * into v_club;

  insert into public.club_members (club_id, user_id, papel)
    values (v_club.id, v_uid, 'super_admin');

  return v_club;
end;
$function$
;

CREATE OR REPLACE FUNCTION public.delete_own_account()
 RETURNS void
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
declare
  uid uuid := auth.uid();
  blocking_club text;
begin
  if uid is null then
    raise exception 'not authenticated';
  end if;

  -- Bloqueio: super_admin de clube com outros membros precisa transferir/sair antes.
  select c.nome into blocking_club
  from public.club_members cm
  join public.clubs c on c.id = cm.club_id
  where cm.user_id = uid
    and cm.papel = 'super_admin'
    and exists (select 1 from public.club_members o
                where o.club_id = cm.club_id and o.user_id <> uid)
  limit 1;

  if blocking_club is not null then
    raise exception
      'Você é o super admin de "%". Transfira a coroa a outro membro ou saia do clube antes de excluir a conta.',
      blocking_club;
  end if;

  -- Clubes onde o usuário é o ÚNICO membro: apaga o clube inteiro (cascata).
  delete from public.clubs c
  where exists (select 1 from public.club_members cm
                where cm.club_id = c.id and cm.user_id = uid)
    and not exists (select 1 from public.club_members o
                    where o.club_id = c.id and o.user_id <> uid);

  -- Conteúdo do próprio usuário com FK RESTRICT: remover.
  delete from public.comments          where user_id = uid;
  delete from public.book_suggestions  where sugerido_por = uid;
  delete from public.voting_rounds     where criado_por = uid;   -- cascata: votos da rodada
  delete from public.member_removals   where user_id = uid or removed_by = uid;

  -- clubs.criador_id RESTRICT: reatribuir a outro membro (garantido existir, pois
  -- clubes só-do-usuário já foram apagados acima).
  update public.clubs c
  set criador_id = (
    select o.user_id from public.club_members o
    where o.club_id = c.id and o.user_id <> uid
    order by case o.papel when 'super_admin' then 0 when 'admin' then 1 else 2 end
    limit 1)
  where c.criador_id = uid;

  -- Resto cascateia via profiles.id -> auth.users (ON DELETE CASCADE):
  -- book_ratings, club_members, meeting_notes, meeting_rsvps, notifications,
  -- reactions, saved_quotes, user_progress, votes.
  delete from auth.users where id = uid;
end;
$function$
;

CREATE OR REPLACE FUNCTION public.demote_admin(p_club_id uuid, p_target_user_id uuid)
 RETURNS club_members
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public', 'pg_temp'
AS $function$
declare
  v_uid uuid := auth.uid();
  v_member public.club_members;
begin
  if v_uid is null then raise exception 'Nao autenticado' using errcode = '42501'; end if;

  if not exists (
    select 1 from public.club_members
    where club_id = p_club_id and user_id = v_uid and papel = 'super_admin'
  ) then
    raise exception 'So super_admin pode rebaixar' using errcode = '42501';
  end if;

  if not exists (
    select 1 from public.club_members
    where club_id = p_club_id and user_id = p_target_user_id and papel = 'admin'
  ) then
    raise exception 'Alvo nao e admin' using errcode = '23514';
  end if;

  update public.club_members set papel = 'member'
    where club_id = p_club_id and user_id = p_target_user_id
    returning * into v_member;

  return v_member;
end;
$function$
;

CREATE OR REPLACE FUNCTION public.enforce_super_admin_invariant()
 RETURNS trigger
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public', 'pg_temp'
AS $function$
declare
  v_super_count int;
  v_club_id uuid;
  v_arquivado boolean;
  v_club_exists boolean;
begin
  if tg_op = 'DELETE' then
    v_club_id := old.club_id;
  else
    v_club_id := new.club_id;
  end if;

  -- Se o clube foi/esta sendo deletado, nao impor invariante (cascata legitima)
  select exists(select 1 from public.clubs where id = v_club_id) into v_club_exists;
  if not v_club_exists then
    return coalesce(new, old);
  end if;

  select arquivado into v_arquivado from public.clubs where id = v_club_id;
  if v_arquivado then
    return coalesce(new, old);
  end if;

  select count(*) into v_super_count from public.club_members
    where club_id = v_club_id and papel = 'super_admin';

  if v_super_count = 0 then
    raise exception 'Clube precisa de exatamente 1 super_admin (atual: 0)' using errcode = '23514';
  end if;
  if v_super_count > 1 then
    raise exception 'Clube nao pode ter mais de 1 super_admin (atual: %)', v_super_count using errcode = '23514';
  end if;

  return coalesce(new, old);
end;
$function$
;

CREATE OR REPLACE FUNCTION public.generate_invite_code()
 RETURNS text
 LANGUAGE plpgsql
 SET search_path TO 'public', 'pg_temp'
AS $function$
declare
  alphabet text := 'ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789';
  result text := '';
  i int;
begin
  for i in 1..6 loop
    result := result || substr(alphabet, 1 + floor(random() * length(alphabet))::int, 1);
  end loop;
  return result;
end;
$function$
;

CREATE OR REPLACE FUNCTION public.generate_unique_invite_code()
 RETURNS text
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public', 'pg_temp'
AS $function$
declare
  candidate text;
  attempts int := 0;
begin
  loop
    candidate := public.generate_invite_code();
    if not exists (select 1 from public.clubs where codigo = candidate) then
      return candidate;
    end if;
    attempts := attempts + 1;
    if attempts > 10 then
      raise exception 'Falha ao gerar codigo unico apos 10 tentativas';
    end if;
  end loop;
end;
$function$
;

CREATE OR REPLACE FUNCTION public.handle_new_user()
 RETURNS trigger
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public', 'pg_temp'
AS $function$
begin
  insert into public.profiles (id, nome, sobrenome, avatar_key)
    values (
      new.id,
      coalesce(
        nullif(trim(new.raw_user_meta_data->>'nome'), ''),
        nullif(trim(new.raw_user_meta_data->>'name'), ''),
        nullif(trim(new.raw_user_meta_data->>'full_name'), ''),
        split_part(coalesce(new.email, 'usuario@rodape'), '@', 1)
      ),
      nullif(trim(new.raw_user_meta_data->>'sobrenome'), ''),
      coalesce(
        nullif(trim(new.raw_user_meta_data->>'avatar_key'), ''),
        'preset:leitor'
      )
    )
    on conflict (id) do nothing;
  return new;
end;
$function$
;

CREATE OR REPLACE FUNCTION public.is_club_admin(target_club uuid)
 RETURNS boolean
 LANGUAGE sql
 STABLE SECURITY DEFINER
 SET search_path TO 'public', 'pg_temp'
AS $function$
  select exists (
    select 1 from public.club_members
    where club_id = target_club
      and user_id = auth.uid()
      and papel in ('admin', 'super_admin')
  );
$function$
;

CREATE OR REPLACE FUNCTION public.is_club_member(target_club uuid)
 RETURNS boolean
 LANGUAGE sql
 STABLE SECURITY DEFINER
 SET search_path TO 'public', 'pg_temp'
AS $function$
  select exists (
    select 1 from public.club_members
    where club_id = target_club and user_id = auth.uid()
  );
$function$
;

CREATE OR REPLACE FUNCTION public.is_club_super(target_club uuid)
 RETURNS boolean
 LANGUAGE sql
 STABLE SECURITY DEFINER
 SET search_path TO 'public', 'pg_temp'
AS $function$
  select exists (
    select 1 from public.club_members
    where club_id = target_club
      and user_id = auth.uid()
      and papel = 'super_admin'
  );
$function$
;

CREATE OR REPLACE FUNCTION public.join_club_with_code(p_codigo text)
 RETURNS club_members
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public', 'pg_temp'
AS $function$
declare
  v_uid uuid := auth.uid();
  v_club_id uuid;
  v_member public.club_members;
begin
  if v_uid is null then
    raise exception 'Usuario nao autenticado' using errcode = '42501';
  end if;

  if p_codigo is null or not (p_codigo ~ '^[A-Z0-9]{6}$') then
    raise exception 'Codigo invalido' using errcode = '22023';
  end if;

  select id into v_club_id
    from public.clubs
    where codigo = upper(p_codigo) and arquivado = false
    limit 1;

  if v_club_id is null then
    raise exception 'Codigo nao encontrado' using errcode = 'P0002';
  end if;

  insert into public.club_members (club_id, user_id, papel)
    values (v_club_id, v_uid, 'member')
    on conflict (club_id, user_id) do update set entrou_em = club_members.entrou_em
    returning * into v_member;

  return v_member;
end;
$function$
;

CREATE OR REPLACE FUNCTION public.leave_club(p_club_id uuid)
 RETURNS void
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public', 'pg_temp'
AS $function$
declare
  v_uid uuid := auth.uid();
  v_papel member_role;
  v_admin_count int;
  v_oldest_admin uuid;
begin
  if v_uid is null then
    raise exception 'Usuario nao autenticado' using errcode = '42501';
  end if;

  select papel into v_papel from public.club_members
    where club_id = p_club_id and user_id = v_uid;

  if v_papel is null then
    raise exception 'Voce nao e membro deste clube' using errcode = 'P0002';
  end if;

  if v_papel = 'super_admin' then
    -- procura outro admin pra promover
    select user_id into v_oldest_admin from public.club_members
      where club_id = p_club_id and papel = 'admin' and user_id <> v_uid
      order by entrou_em asc limit 1;

    if v_oldest_admin is null then
      -- procura qualquer member
      select user_id into v_oldest_admin from public.club_members
        where club_id = p_club_id and user_id <> v_uid
        order by entrou_em asc limit 1;
    end if;

    if v_oldest_admin is null then
      -- ultimo membro saindo: arquiva o clube
      update public.clubs set arquivado = true where id = p_club_id;
    else
      update public.club_members set papel = 'super_admin'
        where club_id = p_club_id and user_id = v_oldest_admin;
    end if;
  end if;

  delete from public.club_members where club_id = p_club_id and user_id = v_uid;
end;
$function$
;

CREATE OR REPLACE FUNCTION public.notify_book_finished()
 RETURNS trigger
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
begin
  if new.status = 'finished'
     and (tg_op = 'INSERT' or old.status is distinct from new.status) then
    begin
      insert into public.notifications (user_id, club_id, tipo, payload)
      select cm.user_id, new.club_id, 'book_finished'::notification_type,
             jsonb_build_object(
               'bookTitle', coalesce((select b.title from public.books b where b.id = new.book_id), '')
             )
      from public.club_members cm
      where cm.club_id = new.club_id;
    exception when others then
      -- Nunca bloquear a conclusão do livro por causa de notificação.
      null;
    end;
  end if;
  return new;
end;
$function$
;

CREATE OR REPLACE FUNCTION public.promote_member(p_club_id uuid, p_target_user_id uuid)
 RETURNS club_members
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public', 'pg_temp'
AS $function$
declare
  v_uid uuid := auth.uid();
  v_target_role member_role;
  v_caller_role member_role;
  v_member public.club_members;
begin
  if v_uid is null then raise exception 'Nao autenticado' using errcode = '42501'; end if;

  select papel into v_caller_role from public.club_members
    where club_id = p_club_id and user_id = v_uid;

  if v_caller_role not in ('admin', 'super_admin') then
    raise exception 'Sem permissao' using errcode = '42501';
  end if;

  select papel into v_target_role from public.club_members
    where club_id = p_club_id and user_id = p_target_user_id;

  if v_target_role is null then
    raise exception 'Alvo nao e membro' using errcode = 'P0002';
  end if;

  if v_target_role = 'super_admin' then
    raise exception 'Alvo ja e super_admin' using errcode = '23514';
  end if;

  -- member -> admin (qualquer admin/super pode)
  -- admin -> super_admin: nao permitido aqui (usar transfer_super_admin)
  if v_target_role = 'admin' then
    raise exception 'Use transfer_super_admin para promover admin a super_admin' using errcode = '42501';
  end if;

  update public.club_members set papel = 'admin'
    where club_id = p_club_id and user_id = p_target_user_id
    returning * into v_member;

  insert into public.notifications (user_id, club_id, tipo, payload)
    values (p_target_user_id, p_club_id, 'promoted_to_admin', jsonb_build_object('by', v_uid));

  return v_member;
end;
$function$
;

CREATE OR REPLACE FUNCTION public.regenerate_invite_code(p_club_id uuid)
 RETURNS text
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public', 'pg_temp'
AS $function$
declare
  v_uid uuid := auth.uid();
  v_codigo text;
begin
  if v_uid is null then raise exception 'Nao autenticado' using errcode = '42501'; end if;

  if not exists (
    select 1 from public.club_members
    where club_id = p_club_id and user_id = v_uid and papel in ('admin', 'super_admin')
  ) then
    raise exception 'So admin pode regenerar codigo' using errcode = '42501';
  end if;

  v_codigo := public.generate_unique_invite_code();
  update public.clubs set codigo = v_codigo, updated_at = now() where id = p_club_id;

  return v_codigo;
end;
$function$
;

CREATE OR REPLACE FUNCTION public.remove_member(p_club_id uuid, p_target_user_id uuid, p_motivo text DEFAULT NULL::text)
 RETURNS void
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public', 'pg_temp'
AS $function$
declare
  v_uid uuid := auth.uid();
  v_caller_role member_role;
  v_target_role member_role;
begin
  if v_uid is null then raise exception 'Nao autenticado' using errcode = '42501'; end if;
  if v_uid = p_target_user_id then
    raise exception 'Nao remova a si mesmo; use leave_club' using errcode = '42501';
  end if;

  select papel into v_caller_role from public.club_members
    where club_id = p_club_id and user_id = v_uid;

  if v_caller_role not in ('admin', 'super_admin') then
    raise exception 'Sem permissao' using errcode = '42501';
  end if;

  select papel into v_target_role from public.club_members
    where club_id = p_club_id and user_id = p_target_user_id;

  if v_target_role is null then
    raise exception 'Alvo nao e membro' using errcode = 'P0002';
  end if;

  if v_target_role = 'super_admin' then
    raise exception 'Super_admin nao pode ser removido' using errcode = '42501';
  end if;

  -- admin nao remove admin (so super)
  if v_target_role = 'admin' and v_caller_role <> 'super_admin' then
    raise exception 'So super_admin remove admin' using errcode = '42501';
  end if;

  insert into public.member_removals (club_id, user_id, removed_by, motivo)
    values (p_club_id, p_target_user_id, v_uid, p_motivo);

  insert into public.notifications (user_id, club_id, tipo, payload)
    values (p_target_user_id, p_club_id, 'member_removed',
      jsonb_build_object('by', v_uid, 'motivo', p_motivo));

  delete from public.club_members where club_id = p_club_id and user_id = p_target_user_id;
end;
$function$
;

CREATE OR REPLACE FUNCTION public.set_updated_at()
 RETURNS trigger
 LANGUAGE plpgsql
 SET search_path TO 'public', 'pg_temp'
AS $function$
begin
  new.updated_at = now();
  return new;
end;
$function$
;

CREATE OR REPLACE FUNCTION public.transfer_super_admin(p_club_id uuid, p_target_user_id uuid)
 RETURNS void
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public', 'pg_temp'
AS $function$
declare
  v_uid uuid := auth.uid();
begin
  if v_uid is null then raise exception 'Nao autenticado' using errcode = '42501'; end if;

  if not exists (
    select 1 from public.club_members
    where club_id = p_club_id and user_id = v_uid and papel = 'super_admin'
  ) then
    raise exception 'So super_admin pode transferir' using errcode = '42501';
  end if;

  if not exists (
    select 1 from public.club_members
    where club_id = p_club_id and user_id = p_target_user_id and papel = 'admin'
  ) then
    raise exception 'Alvo precisa ser admin' using errcode = '23514';
  end if;

  -- Atomic swap via PK: rebaixa primeiro (so existe 1 super), depois promove o novo
  update public.club_members set papel = 'admin'
    where club_id = p_club_id and user_id = v_uid;

  update public.club_members set papel = 'super_admin'
    where club_id = p_club_id and user_id = p_target_user_id;

  insert into public.notifications (user_id, club_id, tipo, payload)
    values (p_target_user_id, p_club_id, 'super_admin_transferred',
      jsonb_build_object('from', v_uid));
end;
$function$
;
