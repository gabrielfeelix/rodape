-- 0001 — delete_own_account (exclusão de conta pelo próprio usuário / LGPD + Play Store)
--
-- A UI do app (Perfil → Excluir minha conta) chama este RPC. Sem ele, só havia
-- o fallback por email.
--
-- Trata os FKs RESTRICT que bloqueariam a exclusão do profile
-- (comments, clubs.criador_id, book_suggestions, voting_rounds, member_removals)
-- e respeita o invariante de super_admin. Se o usuário é super_admin de um clube
-- COM outros membros, levanta erro claro (o app então oferece o fallback por
-- email) — evita deixar um clube sem dono.
--
-- Deleção final: `delete from auth.users` cascateia profiles e daí a maioria das
-- tabelas (ON DELETE CASCADE).

create or replace function public.delete_own_account()
returns void
language plpgsql
security definer
set search_path = public
as $$
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
$$;

grant execute on function public.delete_own_account() to authenticated;
