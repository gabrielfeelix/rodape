-- Pronome opcional no perfil (C6 do plano de pendências). Inclusivo por escolha,
-- nunca imposto: coluna nullable, sem default de gênero. UI oferece ele/ela/elu/—.
-- RLS de UPDATE de perfil próprio já cobre a coluna nova (é row-level).
alter table public.profiles
  add column if not exists pronome text;

-- Limite defensivo (rótulo curto tipo "ela/dela"); NULL = não informado.
alter table public.profiles
  drop constraint if exists profiles_pronome_check;
alter table public.profiles
  add constraint profiles_pronome_check
  check (pronome is null or char_length(pronome) <= 40);
