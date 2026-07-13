-- Migration 0009 — corrige o dispatch de push pra Supabase HOSPEDADO.
--
-- Problema: a 0007 lia a URL/service_role de GUCs (`alter database ... set app.x`),
-- mas em Supabase hospedado o role da API NÃO é superuser → `permission denied to
-- set parameter`. As GUCs nunca podiam ser setadas.
--
-- Correção: a URL da Edge Function NÃO é segredo (deriva do ref público), então fica
-- inline. A service_role (segredo) passa a vir do **Supabase Vault** — o mecanismo
-- oficial de segredo em trigger. O valor NÃO fica versionado: o admin insere uma vez
--   select vault.create_secret('<SERVICE_ROLE_KEY>', 'push_service_role_key');
-- (ou via dashboard → Project Settings → Vault). Sem o secret → no-op seguro.

create extension if not exists supabase_vault;

create or replace function public._dispatch_push()
 returns trigger language plpgsql security definer set search_path to 'public','vault','pg_temp'
as $function$
declare
  -- URL pública da Edge Function (ref não é segredo — já vive no SUPABASE_URL do app).
  v_url text := 'https://zfbywoeajebvasnsrzfh.supabase.co/functions/v1/send-push';
  v_key text;
begin
  -- service_role vem do Vault (encriptado). Ausente → no-op silencioso.
  select decrypted_secret into v_key
    from vault.decrypted_secrets
    where name = 'push_service_role_key'
    limit 1;

  if v_key is null or v_key = '' then
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
  -- Push é best-effort: falha aqui NUNCA derruba o insert da notificação.
  return new;
end; $function$;

-- Trigger já existe (0007); recriação idempotente por segurança.
drop trigger if exists trg_dispatch_push on public.notifications;
create trigger trg_dispatch_push
  after insert on public.notifications
  for each row execute function public._dispatch_push();
