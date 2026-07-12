-- Favorito PESSOAL de livro (♥), cross-clube. Chave (user_id, book_id).
-- books.id é uuid (gen_random_uuid) e o app já gera UUID → sem risco do 22P02.
create table if not exists public.book_favorites (
  user_id uuid not null references public.profiles(id) on delete cascade,
  book_id uuid not null references public.books(id) on delete cascade,
  created_at timestamptz not null default now(),
  primary key (user_id, book_id)
);

alter table public.book_favorites enable row level security;

-- Cada usuário só enxerga/mexe nos SEUS favoritos.
drop policy if exists "book_favorites select own" on public.book_favorites;
create policy "book_favorites select own" on public.book_favorites
  for select to authenticated using (user_id = (select auth.uid()));

drop policy if exists "book_favorites insert own" on public.book_favorites;
create policy "book_favorites insert own" on public.book_favorites
  for insert to authenticated with check (user_id = (select auth.uid()));

drop policy if exists "book_favorites delete own" on public.book_favorites;
create policy "book_favorites delete own" on public.book_favorites
  for delete to authenticated using (user_id = (select auth.uid()));

grant select, insert, delete on public.book_favorites to authenticated;

-- Realtime (sync cross-device). Ignora se a tabela já estiver na publicação.
do $$ begin
  alter publication supabase_realtime add table public.book_favorites;
exception when duplicate_object then null; when others then null;
end $$;
