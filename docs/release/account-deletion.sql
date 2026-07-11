-- delete_own_account — exclusão de conta pelo próprio usuário
--
-- Requisito da Play Store (política de contas): o app precisa permitir que o
-- usuário exclua a própria conta de dentro do app. O cliente Android chama este
-- RPC (RemoteRepository.deleteOwnAccountViaRpc → MainViewModel.deleteAccount).
--
-- Cole no Supabase Dashboard → SQL Editor e rode uma vez. Ajuste a lista de
-- tabelas conforme o schema real do projeto.
--
-- SECURITY DEFINER: roda com privilégios do owner (postgres), necessário pra
-- apagar de auth.users. auth.uid() garante que o usuário só apaga a SI MESMO.

create or replace function public.delete_own_account()
returns void
language plpgsql
security definer
set search_path = public, auth
as $$
declare
  uid uuid := auth.uid();
begin
  if uid is null then
    raise exception 'not authenticated';
  end if;

  -- 1) Remover/anonimizar dados do usuário. Escolha por tabela:
  --    - Dados pessoais → delete
  --    - Conteúdo compartilhado no clube (comentários, frases) → manter, mas
  --      desvincular o autor (anonimizar) pra não quebrar as discussões.

  -- Dados pessoais (delete)
  delete from public.meeting_rsvps      where user_id = uid;
  delete from public.votes              where user_id = uid;
  delete from public.reactions          where user_id = uid;
  delete from public.user_progress      where user_id = uid;
  delete from public.meeting_notes      where user_id = uid;
  delete from public.book_ratings       where user_id = uid;
  delete from public.notifications      where user_id = uid;
  delete from public.club_members       where user_id = uid;
  delete from public.book_suggestions   where suggested_by_user_id = uid;
  delete from public.saved_quotes       where user_id = uid;

  -- Conteúdo compartilhado (anonimizar em vez de apagar) — descomente se
  -- preferir preservar a coerência das discussões:
  -- update public.comments set user_id = null where user_id = uid;

  -- Se preferir apagar tudo:
  delete from public.comments           where user_id = uid;

  -- 2) Perfil
  delete from public.profiles           where id = uid;

  -- 3) Conta de autenticação
  delete from auth.users                where id = uid;
end;
$$;

-- Permitir que usuários autenticados chamem o RPC
grant execute on function public.delete_own_account() to authenticated;
