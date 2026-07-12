create table if not exists public.chapter_templates (
  isbn text primary key,
  titulo_livro text,
  chapters jsonb not null,
  contributed_by uuid references public.profiles(id) on delete set null,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

alter table public.chapter_templates enable row level security;

drop policy if exists "chapter_templates select authenticated" on public.chapter_templates;
create policy "chapter_templates select authenticated" on public.chapter_templates
  for select to authenticated using (true);

drop policy if exists "chapter_templates insert self" on public.chapter_templates;
create policy "chapter_templates insert self" on public.chapter_templates
  for insert to authenticated with check (contributed_by = (select auth.uid()));

drop policy if exists "chapter_templates update authenticated" on public.chapter_templates;
create policy "chapter_templates update authenticated" on public.chapter_templates
  for update to authenticated using (true) with check (contributed_by = (select auth.uid()));

grant select, insert, update on public.chapter_templates to authenticated;

-- updated_at automatico
create or replace function public.set_chapter_templates_updated_at() returns trigger
  language plpgsql as $$ begin new.updated_at = now(); return new; end; $$;
drop trigger if exists trg_chapter_templates_updated_at on public.chapter_templates;
create trigger trg_chapter_templates_updated_at before update on public.chapter_templates
  for each row execute function public.set_chapter_templates_updated_at();
