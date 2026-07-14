# Auditoria de RLS — 2026-07-14

RLS é a **única** camada de autorização do Rodapé (o cliente usa a chave anon/
publishable; nenhuma decisão de acesso roda no app). Esta auditoria revisou as
~92 policies (`supabase/schema/policies.sql` + migrations 0010/0011) e todas as
funções `SECURITY DEFINER` (`functions.sql`).

## Resumo

**Fundamentos sólidos.** Nenhum vazamento cross-clube encontrado. Correções
aplicadas foram de defense-in-depth, não de furos exploráveis.

## ✅ O que está correto

- **Todas as funções `SECURITY DEFINER` pinam `search_path`** (`'public','pg_temp'`).
  Verificado nas 23 funções — nenhuma vulnerável a search-path hijack.
- **Escopo por clube consistente** via helpers `is_club_member` / `is_club_admin`
  / `is_club_super` (todas STABLE SECURITY DEFINER com search_path). Cada tabela
  de conteúdo exige `is_club_member(club_id)` no SELECT e `is_club_admin` nas
  ações administrativas.
- **Ações sensíveis são RPC `SECURITY DEFINER`** que checam o papel antes de
  agir (create_club, join_club_with_code, promote/demote, remove_member,
  transfer_super_admin, delete_own_account, close_voting_round). O cliente não
  escreve direto em `club_members` além do permitido.
- **Escrita restrita ao dono:** `votes`, `saved_quotes`, `user_progress`,
  `meeting_rsvps`, `reactions`, `book_ratings` exigem `user_id = auth.uid()` no
  INSERT/UPDATE.
- **Moderação (0010) correta:** `content_reports` INSERT exige membro + reporter
  = self; SELECT/UPDATE exige admin do clube. `user_blocks` é self-only.
  `moderate_remove_content` e `dismiss_report` checam `is_club_admin` e pinam
  search_path. `is_blocked` é SECURITY DEFINER com search_path.
- **Serviço nunca no cliente:** confirmado que só a publishable/anon key é
  embutida; `service_role`/`secret` ficam no `.env` só pra scripts admin.

## 🔧 Correções aplicadas (migration 0011)

**Conteúdo removido era legível via API crua.** As tabelas `saved_quotes`,
`book_ratings` e `book_suggestions` retornavam linhas `removido = true` a qualquer
membro do clube (o app filtrava no cliente, mas um cliente adverso com a chave
anon leria o conteúdo abusivo removido). `0011_rls_fixes.sql` reescreve os três
SELECT para esconder `removido` de quem não é **autor** nem **admin**. Enforcement
agora é server-side. (`comments` foi deixado de fora de propósito: o app depende
de ler comments removidos pra renderizar o placeholder e o log de moderação.)

## ⚠️ Achados aceitos para a beta (não corrigidos)

- **`profiles select USING (true)`** — qualquer usuário autenticado pode ler
  todos os perfis (nome, avatar, pronome). Não vaza associação a clubes nem
  conteúdo, mas permite enumerar nomes/avatares da base inteira. **Severidade:
  média.** Não corrigido agora porque a resolução de nomes em várias telas assume
  leitura ampla de `profiles`; apertar pra "só quem compartilha um clube comigo"
  exige refactor cuidadoso e testes. **Recomendação:** endereçar antes de escala
  (não é bloqueador de beta).
- **Snapshots em `supabase/schema/` estão defasados** (header "pós 0003/0004";
  faltam 0005–0011). A fonte de verdade são as **migrations** (`supabase/
  migrations/*.sql`), aplicadas ao banco vivo. Recomendação: regenerar os
  snapshots do banco antes do próximo grande trabalho de schema.

## Testes negativos recomendados (manual, pós-deploy)

1. Membro do clube A não lê `comments`/`votes`/`ratings` do clube B.
2. Não-membro não lê nada de um clube privado.
3. Não-admin não consegue `moderate_remove_content` (RPC deve levantar 42501).
4. Usuário não lê `content_reports` de outro clube; lê só as próprias + as do
   clube que administra.
5. Conteúdo `removido` some do SELECT pra membro comum (0011).
