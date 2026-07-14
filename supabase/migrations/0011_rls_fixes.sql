-- 0011_rls_fixes.sql — Hardening de RLS pós-auditoria (2026-07-14)
--
-- Achado: conteúdo marcado como `removido` (moderação 0010) ainda era retornado
-- pelo SELECT a membros comuns via API crua (o app filtra no cliente, mas a
-- policy não). Enforcement server-side: membro comum não lê conteúdo removido;
-- o AUTOR e o ADMIN continuam vendo (autor pra saber que foi removido; admin
-- pra moderar/restaurar).
--
-- Não altera `comments`: o app depende de ler comments removidos pra renderizar
-- o placeholder "comentário removido" e o log de moderação. Tratado à parte.

-- saved_quotes: esconde removido de quem não é autor nem admin.
drop policy if exists "saved_quotes select members" on public.saved_quotes;
create policy "saved_quotes select members"
  on public.saved_quotes for select to authenticated
  using (
    ( select is_club_member(saved_quotes.club_id) )
    and (
      (removido = false)
      or (user_id = ( select auth.uid() ))
      or ( select is_club_admin(saved_quotes.club_id) )
    )
  );

-- book_ratings: idem (autor = user_id).
drop policy if exists "book_ratings select members" on public.book_ratings;
create policy "book_ratings select members"
  on public.book_ratings for select to authenticated
  using (
    ( select is_club_member(book_ratings.club_id) )
    and (
      (removido = false)
      or (user_id = ( select auth.uid() ))
      or ( select is_club_admin(book_ratings.club_id) )
    )
  );

-- book_suggestions: idem (autor = sugerido_por).
drop policy if exists "book_suggestions select members" on public.book_suggestions;
create policy "book_suggestions select members"
  on public.book_suggestions for select to authenticated
  using (
    ( select is_club_member(book_suggestions.club_id) )
    and (
      (removido = false)
      or (sugerido_por = ( select auth.uid() ))
      or ( select is_club_admin(book_suggestions.club_id) )
    )
  );
