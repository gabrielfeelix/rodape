# Changelog — Rodapé

## v1.0.11 — 2026-07-12 (build 12)

### ✨ Capítulos automáticos: comunidade + EPUB (feature completa)
Como as APIs não dão índice de capítulos de ficção, o app agora tem uma **cascata de
fontes** no "buscar online" (Gerenciar clube → Capítulos), na ordem:

1. **Índice compartilhado pela comunidade (crowdsourcing por ISBN)** — quando um admin
   cadastra os capítulos de um livro e marca "compartilhar", aquele índice fica
   disponível **pra todos os clubes** que lerem o mesmo ISBN depois. Um cadastro serve o
   app inteiro. É a melhor fonte pra ficção em português. (Nova tabela `chapter_templates`
   no Supabase, com RLS: qualquer membro lê; contribuições registram o autor.)
2. **Open Library** (`table_of_contents`) — técnico/inglês.
3. **EPUB do Project Gutenberg** — clássicos de domínio público. Testado ao vivo:
   **Dom Casmurro = 153 capítulos**, Frankenstein = 32, Sherlock Holmes = 18. Baixa o
   EPUB, lê o índice (nav.xhtml / toc.ncx) e preenche.
4. **Descrição do Google Books** — último recurso.

O primeiro que acertar vence; o admin revisa e salva. Ao salvar um livro com ISBN, um
checkbox oferece **compartilhar o índice com a comunidade** (marcado por padrão).

## v1.0.10 — 2026-07-12 (build 11)

### ✨ "Buscar online" agora usa o Open Library (índice de capítulos)
Pesquisa a fundo (com testes ao vivo): a ÚNICA fonte pública/grátis/sem-chave com
lista de capítulos é o `table_of_contents` do **Open Library**. Cobertura é enviesada:
ótima pra técnico/inglês (ex.: Clean Code = 358 entradas, Design Patterns = 10), mas
**romances e livros em português quase sempre vêm vazios**. O botão "buscar online" em
Gerenciar clube → Capítulos agora consulta o Open Library por ISBN (filtrando só
entradas de capítulo reais), com fallback pra descrição do Google Books. Quando não
acha, o admin usa o **gerar N capítulos**. (Nenhuma API resolve capítulos de ficção de
forma confiável — é uma limitação dos dados, não do app.)

## v1.0.9 — 2026-07-12 (build 10)

Verificado no banco ao vivo que o servidor **aceita** troca de voto e progresso (upsert
HTTP 200) — os bugs restantes eram client-side.

### 🔴 "Trocar pra esse" virava "teu voto" e voltava, em loop
Causa: a PK LOCAL de votos permite várias linhas por usuário, e o fluxo fazia
delete-remoto + insert-remoto separados; um reload com estado antigo do servidor
re-adicionava o voto anterior e podava o novo. **Correção:** troca ATÔMICA — apaga os
votos do usuário na rodada localmente e faz **um único upsert** com `onConflict` na PK
(round,user), que substitui no servidor numa operação só; o reload de reconciliação só
roda no sucesso. Sem loop.

### 🔴 "Marcar progresso" não fazia nada
Causa: o botão só agia se `próximo_capítulo <= nº_de_capítulos`. Como capítulos não
sincronizavam (bug anterior) e muitos livros estão com **0 capítulos**, o clique era
silenciosamente ignorado. **Correção:** agora avisa "Cadastre os capítulos do livro" em
vez de não fazer nada; com capítulos, funciona normalmente.

### ✨ Gerar capítulos de uma vez
As APIs de livro (Google Books/Open Library) **não fornecem a lista de capítulos** de
forma confiável. Em vez de raspar a descrição (falhava quase sempre), agora dá pra
**informar o número de capítulos e gerar todos** de uma vez em Gerenciar clube →
Capítulos (títulos continuam opcionais). Isso destrava o "marcar progresso".

## v1.0.8 — 2026-07-12 (build 9)

### 🔴 "Clico e não vai" — CAUSA RAIZ REAL (não era rede nem conexão)
Investigação a fundo (código + banco ao vivo + medição de latência + docs supabase):
o banco responde em ~200ms e as escritas **funcionam** no servidor. O problema era
**reatividade da UI** — dois bugs de arquitetura:

- **Votar / "trocar pra esse" não aparecia:** a tela de votação lia os votos de
  `getVotesForClubFlow`, um `MutableStateFlow` preenchido por **um único fetch de
  rede** e que **nunca observava o banco local**. O voto era gravado (local + servidor),
  mas a UI lia de um snapshot congelado — só atualizava se você saísse e voltasse.
  **Correção:** a UI agora usa `votesForActiveRound`, um flow **Room-reativo** escopado
  à rodada ativa. O voto otimista aparece na hora.
- **"Marcar progresso" avançava e revertia:** `insertUserProgress` disparava um reload
  do servidor **antes** de a escrita remota confirmar, sobrescrevendo o valor otimista
  com o antigo ("pisca e some"). Esse fluxo tinha ficado de fora da correção anterior
  dos 12 fluxos. **Correção:** o reload agora só roda no sucesso da escrita.

Isso explica por que os ajustes de rede da 1.0.7 não resolveram: o defeito estava na
camada de estado da UI, não na conexão.

## v1.0.7 — 2026-07-12 (build 8)

### ⚡ Ações de DB agora resolvem na hora (fim do "1 alteração aguardando conexão")
- **Diagnóstico (medido):** o banco/rede está rápido — round-trip ~200ms. O problema
  NÃO era o servidor. Era o caminho de recuperação: quando uma ação falhava por um
  tropeço **transitório** (rádio frio / conexão fria no primeiro toque), ela caía na
  fila offline e o **único** gatilho pra re-sincronizar era o `DrainQueueWorker`
  (WorkManager, não-expedited) — que só roda **minutos** depois. Por isso o
  "1 alteração aguardando conexão" ficava preso.
- **Correção 1 — retry inline:** antes de desistir pra fila, a ação tenta de novo
  2–3× com 350–700ms de intervalo. Como a rede volta em <1s, a maioria das ações
  agora nem chega a mostrar "aguardando".
- **Correção 2 — drenagem imediata em processo:** se ainda assim cair na fila, o app
  drena na hora (retries de 0,25s→5s) no próprio processo, em vez de esperar o
  WorkManager. O worker vira só fallback pra quando o app está fechado.
- Resultado: votar, marcar progresso, RSVP, avaliar, comentar etc. sincronizam em
  ~1s. A UI já era otimista (aparece na hora); agora o selo de sincronização some
  quase imediatamente.

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
