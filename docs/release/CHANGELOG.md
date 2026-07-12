# Changelog — Rodapé

## v1.0.6 — 2026-07-12 (build 7)

Correção dos P0 de dados que estavam pendentes na 1.0.5 — agora **validados contra o
banco Supabase ao vivo** (Management API).

### 🔴 Capítulos e comentários agora sincronizam entre dispositivos (P0-1 + B2)
- **Causa raiz (provada no banco):** o app gerava id de capítulo em TEXTO
  (`ch_<bookId>_<numero>`) e mandava pra coluna `uuid` do servidor → Postgres
  rejeitava com `22P02` (erro engolido) → capítulos **nunca** sincronizavam e os
  comentários iam pra dead-letter. Num aparelho só o Room mascarava; entre membros,
  a discussão por capítulo não aparecia.
- **Correção:** identidade de capítulo agora é um `uuid` estável, gerado na tela e
  aceito pelo servidor. O vínculo comentário→capítulo é o id (uuid), então
  **reordenar/renumerar capítulos não remaneja mais os comentários** (bug B2).
- Validado ao vivo: id de texto → rejeitado (22P02); id uuid + comentário via FK →
  aceitos.

### 🔴 Criação offline não se perde mais (P0-2)
- Criar livro/livro-do-clube/sugestão/encontro **offline** (ou com o servidor
  falhando) agora entra na **fila de sincronização** em vez de virar uma linha
  local órfã que o próximo sync apagava silenciosamente. 4 novos handlers de
  replay drenam essas criações quando a rede volta (encontro empurra o livro antes,
  respeitando a FK).

### 🔴 Votação da 2ª rodada corrigida (B1)
- A votação lia votos do **clube inteiro** (todas as rodadas), então numa segunda
  votação os botões travavam em "Limite de votos atingido" e tocar em "Teu voto"
  (de rodada antiga) criava voto novo. Agora é **voto único por rodada** (como o
  servidor exige, PK round+usuário), escopado à rodada ativa: tocar no próprio voto
  desfaz, votar em outro troca. Sem falso "limite atingido".

## v1.0.5 — 2026-07-12 (build 6)

Revisão de app completa (6 auditorias paralelas do código real) → correções de
sensação de lentidão, bugs de comportamento e telas sem ação. Plano completo em
`docs/PLANO-CORRECOES-2026-07-12.md`.

### Performance (fim do "engasga")
- Discussão: Flows memoizados por capítulo (fim do refetch de rede em loop a cada
  recomposição), listas com `key` estável, `isCurrentUserAdmin` içado pra fora do
  `items{}`, reações/membros memoizados (`groupBy`/`associateBy`)
- Votação e avaliações: contagens/filtros memoizados em vez de recalcular por item
- Capas e avatares: `AsyncImage` (sem subcomposição) — rolagem mais leve em grids
- Realtime: canais desregistrados no `close()` (fim do acúmulo por rotação) e race
  de dedup fechada com `compute` atômico

### Skeletons (sensação de carregamento)
- Placeholders shimmer em todas as telas que carregam dados: Home, aba Livro,
  Estante, Votação/Encontro, Discussão, Detalhe do livro, Frases, Encontro,
  Notificações, Log de moderação, Cadastrar capítulos

### Telas vazias agora têm ação (CTA por papel)
- Aba "Livro" sem livro: ícone + orientação + botões (admin: gerenciar/abrir
  votação; membro: sugerir/ver votação)
- Estante: copy correta pro filtro "Favoritos" vazio + ícone/microcopy
- Capítulos não cadastrados: botão "Gerenciar clube" pro admin
- Votação sem sugestões e encontro sem agenda: CTA/microcopy por papel
- Frases vazias: ícone + explicação do fluxo + saída

### Bugs corrigidos
- Entrar em clube por código não empilha mais `main_tabs` duplicado (voltar sai
  do app em vez de reabrir a tela de código)
- Promover/rebaixar/transferir admin agora mostram erro real (antes: "sucesso"
  falso e nada mudava)
- Formulários (cadastro manual de livro, etc.) não se perdem mais na rotação
- Chat de capítulo rola pro comentário recém-enviado
- Barreira de spoiler não vaza mais numa corrida de carregamento
- Câmera: aviso de permissão não pisca mais atrás do diálogo do sistema
- Erros de login/cadastro nunca mais aparecem crus em inglês
- `MainActivity` com `launchMode=singleTop` (deep link de auth não duplica a tela)

### Pendente (exige validação em 2 dispositivos — ver plano)
- P0 de sincronização de capítulos/comentários (id de texto × uuid), criação
  offline sem fila e modelo de voto da 2ª rodada — corretos por diagnóstico, mas
  só se validam com o app rodando em contas/aparelhos diferentes

## Não lançado — 2026-07-11

### Visual (fidelidade ao Claude Design)
- Fontes Literata/Inter embarcadas (corrige queda silenciosa pra Roboto)
- Sombras suaves tingidas em vez da elevação Material cinza
- Header assinatura (avatar + pill de clube + sino), ícones próprios,
  ticket de encontro perfurado, capas com lombada, chips de leitores com anéis

### UX
- Estados de loading reais (fim dos falsos "não encontrado")
- Feedback de ações (snackbar), indicador de sincronização offline
- Pull-to-refresh, confirmações de ações destrutivas, desfazer exclusão de frase
- Voltar capítulo marcado sem querer

### Correções / robustez
- Testes voltaram a rodar; `GEMINI_API_KEY` não vai mais pro APK
- Texto de privacidade corrigido (condiz com o backend Supabase)
- Progresso de leitura não se perde mais offline (local-first + fila)
- Fila offline não é mais descartada no logout; poison messages têm dead-letter

### Produto
- **Exclusão de conta** dentro do app (requisito da Play Store)
- **Sair do clube** para membro comum

## v1.0.0 — 2026-05-24 (build 1)

**Primeira versão pública.**

### Funcionalidades principais
- Cadastro/login por email+senha ou Google Sign-In
- Criação e administração de clubes de leitura privados
- Convite por código + ingresso em clube
- Estante: livros lidos, atual, fila, sugestões
- Votação no próximo livro do clube
- Comentários por capítulo (sem spoiler pra quem está atrasado)
- Reações em comentários
- Frases salvas e exportação por share sheet
- Reuniões com RSVP
- Tela de perfil com avatar ilustrado (escolha automática por gênero do nome)

### Arquitetura
- Backend Supabase (Postgres + RLS + Realtime + Storage + Auth)
- Cache local Room com Single Source of Truth + Stale-While-Revalidate
- Fila offline de mutations com retry via WorkManager
- 73 policies de RLS validadas, 14 RPCs com `auth.uid()` + `SET search_path`
- R8 ativo no release (encolhe + ofusca + remove recursos)
- Backup automático Android desligado (privacidade)
- Crash logs persistidos localmente (nunca enviados sem autorização)

### Limitações conhecidas
- Sem push notifications (FCM) ainda — futuras versões
- HIBP (proteção contra senhas vazadas no Supabase) requer plano Pro, não
  habilitado nesta versão
- Captcha desabilitado (sem hCaptcha secret configurado)
- Sem dark mode (decisão de design)
