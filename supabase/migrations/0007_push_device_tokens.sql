-- Migration 0007 — 2026-07-12
-- Push notifications (F1 do plano de pendências).
--
-- Arquitetura: TODA notificação do app já cai em public.notifications (voting_closed,
-- meeting_reminder, comment_on_chapter, member_finished…). Então o ponto de entrega
-- de push é UM trigger AFTER INSERT nessa tabela, que chama a Edge Function `send-push`
-- (que resolve os tokens do destinatário e dispara no FCM HTTP v1).
--
-- Isto NÃO liga o push sozinho: exige (1) tabela de tokens (abaixo), (2) o cliente
-- Android registrando o token (ver docs/release/push-fcm-setup.md), (3) a Edge Function
-- deployada com o segredo FCM, e (4) as GUCs de URL/serviço setadas (abaixo). Sem isso,
-- o trigger é um no-op seguro — nunca quebra o insert de notificação.

-- ------------------------------------------------------------
-- 1. Tabela de tokens de device (um usuário pode ter vários aparelhos).
-- ------------------------------------------------------------
create table if not exists public.device_tokens (
  token       text primary key,
  user_id     uuid not null references auth.users(id) on delete cascade,
  platform    text not null default 'android',
  updated_at  timestamptz not null default now()
);

create index if not exists device_tokens_user_idx on public.device_tokens(user_id);

alter table public.device_tokens enable row level security;

-- Cada usuário só enxerga/gerencia os próprios tokens.
do $$ begin
  create policy device_tokens_select_own on public.device_tokens
    for select using (auth.uid() = user_id);
exception when duplicate_object then null; end $$;

do $$ begin
  create policy device_tokens_upsert_own on public.device_tokens
    for insert with check (auth.uid() = user_id);
exception when duplicate_object then null; end $$;

do $$ begin
  create policy device_tokens_update_own on public.device_tokens
    for update using (auth.uid() = user_id) with check (auth.uid() = user_id);
exception when duplicate_object then null; end $$;

do $$ begin
  create policy device_tokens_delete_own on public.device_tokens
    for delete using (auth.uid() = user_id);
exception when duplicate_object then null; end $$;

-- ------------------------------------------------------------
-- 2. Dispatch de push: trigger AFTER INSERT em notifications.
-- ------------------------------------------------------------
-- pg_net faz a chamada HTTP assíncrona (não bloqueia o insert).
create extension if not exists pg_net;

-- URL da Edge Function e a service_role key ficam em GUCs do projeto (setadas pelo
-- admin — NÃO versionar segredo). Ex (uma vez, no SQL editor / dashboard):
--   alter database postgres set app.edge_send_push_url = 'https://<ref>.supabase.co/functions/v1/send-push';
--   alter database postgres set app.service_role_key   = '<SERVICE_ROLE_KEY>';
create or replace function public._dispatch_push()
 returns trigger language plpgsql security definer set search_path to 'public','pg_temp'
as $function$
declare
  v_url text := current_setting('app.edge_send_push_url', true);
  v_key text := current_setting('app.service_role_key', true);
begin
  -- Sem configuração → no-op silencioso (não quebra o insert da notificação).
  if v_url is null or v_url = '' or v_key is null or v_key = '' then
    return new;
  end if;

  perform net.http_post(
    url     := v_url,
    headers := jsonb_build_object(
                 'Content-Type', 'application/json',
                 'Authorization', 'Bearer ' || v_key),
    body    := jsonb_build_object('notification_id', new.id)
  );
  return new;
exception when others then
  -- Push é best-effort: uma falha aqui NUNCA pode derrubar o insert da notificação.
  return new;
end; $function$;

drop trigger if exists trg_dispatch_push on public.notifications;
create trigger trg_dispatch_push
  after insert on public.notifications
  for each row execute function public._dispatch_push();
