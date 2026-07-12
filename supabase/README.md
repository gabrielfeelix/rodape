# Supabase — schema e migrations do Rodapé

Antes de 2026-07-12 o schema do banco (tabelas, RLS, RPCs, triggers) existia
**apenas no dashboard do Supabase** — nada versionado. Isto aqui fecha esse buraco.

## `schema/` — snapshot do estado atual (gerado da Management API)

Fotografia legível do banco de produção, pra auditoria e referência. **Não é
pra editar à mão** — reflita mudanças via arquivos em `migrations/`.

- `types.sql` — enums (`notification_type`, `rsvp_status`, etc.)
- `tables.sql` — tabelas: colunas + constraints (PK/FK/unique/check)
- `functions.sql` — funções/RPCs (SECURITY DEFINER)
- `policies.sql` — todas as RLS policies
- `triggers.sql` — triggers

## `migrations/` — mudanças versionadas

Cada arquivo `NNNN_descricao.sql` é uma mudança aplicada ao banco, em ordem.
Aplicadas via Management API (Project ref no `SUPABASE_URL` do `.env`).

Depois de aplicar uma migration, regenerar o snapshot em `schema/` mantém a
fotografia em dia.
