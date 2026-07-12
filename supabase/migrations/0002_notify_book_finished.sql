-- 0002 — notificação "livro finalizado" via trigger (server-side, seguro)
--
-- Contexto: quando o último encontro de um livro é concluído, o livro vira
-- 'finished' e o app TENTAVA criar um aviso pra cada membro via cliente. Isso
-- falhava por dois motivos: (1) 'book_finished' não existia no enum e (2)
-- notifications não tem INSERT policy pro cliente. Resultado: ninguém era
-- avisado.
--
-- Correção: adicionar 'book_finished' ao enum (feito em passo separado, pois
-- ADD VALUE precisa ser commitado antes do uso) + este trigger que cria os
-- avisos server-side quando club_books passa a 'finished'. Roda como
-- SECURITY DEFINER (contorna a ausência de INSERT policy com segurança) e é
-- à prova de exceção: uma falha de notificação NUNCA bloqueia a conclusão do
-- livro.

-- (enum já garantido: alter type public.notification_type add value if not exists 'book_finished';)

create or replace function public.notify_book_finished()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
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
$$;

drop trigger if exists club_books_notify_finished on public.club_books;
create trigger club_books_notify_finished
  after insert or update of status on public.club_books
  for each row execute function public.notify_book_finished();
